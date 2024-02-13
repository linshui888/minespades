package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.nologic.minespades.BattlegroundManager;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.BattlegroundPreferences.Preference;
import me.nologic.minespades.game.EventDrivenGameMaster;
import me.nologic.minespades.game.object.NeutralBattlegroundFlag;
import me.nologic.minespades.game.object.TeamBattlegroundFlag;
import me.nologic.minespades.game.object.base.BattlegroundFlag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
public final class Battleground {

    private final Minespades            plugin         = Minespades.getInstance();
    private final BattlegroundManager   battlegrounder = plugin.getBattlegrounder();
    private final EventDrivenGameMaster gameMaster     = plugin.getGameMaster();

    private @Setter boolean  enabled = false;

    private final Scoreboard scoreboard;
    private final String     battlegroundName;
    private @Setter World    world;

    @NotNull
    private final BattlegroundPreferences preferences;

    private final List<NeutralBattlegroundFlag> neutralFlags = new ArrayList<>();
    private final List<BattlegroundTeam>        teams        = new ArrayList<>();

    @Setter
    private Multiground multiground;

    @Setter
    private BoundingBox boundingBox;

    @SneakyThrows
    public Battleground(final String battlegroundName) {
        this.battlegroundName = battlegroundName;
        assert Bukkit.getScoreboardManager() != null;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective tabKillCounter = scoreboard.registerNewObjective("kill_counter", "DUMMY", "0");
        tabKillCounter.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        this.preferences = BattlegroundPreferences.loadPreferences(this);
        this.startGameTick();
    }

    private void startGameTick() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, (task) -> {
            final List<BattlegroundTeam> undefeatedTeams = this.teams.stream().filter(team -> !team.isDefeated()).toList();
            if (undefeatedTeams.size() == 1) {
                task.cancel();
                this.handleGameOver(undefeatedTeams.get(0));
            }
        }, 0, 20L);
    }

    /* Game over due to last stand. */
    public void handleGameOver(final BattlegroundTeam winnerTeam) {
        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), gameMaster.getBroadcastSoundGameOver(), 1F, 2F));
        this.broadcast(gameMaster.getTeamWinGameLastStand().formatted(winnerTeam.getDisplayName()));
        this.handleRewards(winnerTeam);
        this.shutdown();
    }

    /* Game over due to capturing the flag and getting the required number of scores. */
    public void handleGameOver(final BattlegroundTeam winnerTeam, final BattlegroundPlayer completionist, final BattlegroundFlag capturedFlag) {

        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), gameMaster.getBroadcastSoundGameOver(), 1F, 2F));

        if (capturedFlag instanceof TeamBattlegroundFlag flag)
            this.broadcast(gameMaster.getTeamWinGameTeamFlagCapturedMessage().formatted(completionist.getTeam().getDisplayName(), flag.getTeam().getDisplayName(), completionist.getTeam().getDisplayName(), completionist.getDisplayName()));

        if (capturedFlag instanceof NeutralBattlegroundFlag)
            this.broadcast(gameMaster.getTeamWinGameNeutralFlagCapturedMessage().formatted(completionist.getTeam().getDisplayName(), completionist.getTeam().getDisplayName(), completionist.getDisplayName()));

        this.handleRewards(winnerTeam);
        this.shutdown();
    }

    private void shutdown() {
        if (this.getPreference(Preference.IS_MULTIGROUND).getAsBoolean()) {
            battlegrounder.disable(this);
            this.getMultiground().launchNextInOrder();
        } else battlegrounder.resetBattleground(this);
    }

    private void handleRewards(final BattlegroundTeam winnerTeam) {

        /* Execution of commands at the end of the battle. */
        if (gameMaster.isRewardCommandsEnabled()) {
            this.getBattlegroundPlayers().forEach(battlegroundPlayer -> gameMaster.getRewardCommandsForEveryone().forEach(commandLine -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine.formatted(battlegroundPlayer.getBukkitPlayer().getName()))));
            winnerTeam.getPlayers().forEach(winner -> gameMaster.getRewardCommandsForWinners().forEach(commandLine -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine.formatted(winner.getName()))));
        }

        /* Money giveaway at the end of the battle. */
        if (battlegrounder.getEconomyManager() != null) {
            for (final BattlegroundPlayer player : this.getBattlegroundPlayers()) {

                final boolean isWinner   = player.getTeam().equals(winnerTeam);
                final double  killReward = player.getKills() * gameMaster.getRewardPerKill();

                double reward = 0.0;
                if (isWinner) reward += gameMaster.getRewardForWinning();

                reward += killReward;
                battlegrounder.getEconomyManager().depositPlayer(player.getBukkitPlayer(), reward);

                if (plugin.getGameMaster().isMoneyRewardMessageEnabled() || reward > 0)
                    player.getBukkitPlayer().sendMessage(String.format(gameMaster.getMoneyRewardForPlayingMessage(), reward));
            }
        }

    }

    public static BattlegroundPreferences getPreferences(final String battlegroundName) {
        return new Battleground(battlegroundName).getPreferences();
    }

    @NotNull
    public BattlegroundPlayer connectPlayer(Player player, @NotNull BattlegroundTeam team) {
        player.setScoreboard(scoreboard);
        return team.join(player);
    }

    public void kick(BattlegroundPlayer player) {
        player.getTeam().kick(player.getBukkitPlayer());
        if (Bukkit.getScoreboardManager() != null) {
            player.getBukkitPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void addTeam(BattlegroundTeam team) {
        Team bukkitTeam = scoreboard.registerNewTeam(team.getTeamName());
        bukkitTeam.setAllowFriendlyFire(this.getPreference(Preference.FRIENDLY_FIRE).getAsBoolean());
        bukkitTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        bukkitTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        team.setBukkitTeam(bukkitTeam);
        this.teams.add(team);
    }

    // TODO Checking if a player can connect. A player cannot connect to a filled arena (or if the defeated team is specified).
    public boolean isConnectable(final BattlegroundTeam team) {

        if (team.isDefeated())
            return false;

        return team.getPlayers().size() < this.getPreference(Preference.MAX_PLAYERS_PER_TEAM).getAsInteger();
    }

    public @Nullable BattlegroundTeam getSmallestTeam() {
        return teams.stream().filter(this::isConnectable).min(Comparator.comparingInt(BattlegroundTeam::size)).orElse(null);
    }

    /**
     * Поиск команды по названию.
     * @return BattlegroundTeam или null, если команда не найдена
     * */
    @Nullable
    public BattlegroundTeam getTeamByName(String name) {
        return this.teams.stream().filter(b -> b.getTeamName().equals(name)).findFirst().orElse(null);
    }

    public List<BattlegroundPlayer> getBattlegroundPlayers() {
        final List<BattlegroundPlayer> players = new ArrayList<>();
        for (BattlegroundTeam team : teams) {
            for (Player player : team.getPlayers()) {
                players.add(BattlegroundPlayer.getBattlegroundPlayer(player));
            }
        }
        return players;
    }

    public void broadcast(String message) {
        this.getBattlegroundPlayers().forEach(p -> p.getBukkitPlayer().sendMessage(message));
    }

    public Preference.PreferenceValue getPreference(Preference preference) {
        return this.preferences.get(preference);
    }

}