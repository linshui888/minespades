package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.BattlegroundPreferences.Preference;
import me.nologic.minespades.game.object.NeutralBattlegroundFlag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
public final class Battleground {

    private @Setter boolean  enabled = false;

    private final Scoreboard scoreboard;
    private final String     battlegroundName;
    private @Setter World    world;

    @NotNull
    private final BattlegroundPreferences preferences;

    private final List<BattlegroundTeam>  teams;
    private final List<NeutralBattlegroundFlag> neutralFlags;

    @Setter
    private Multiground multiground;

    @Setter
    private BoundingBox boundingBox;

    @SneakyThrows
    public Battleground(String battlegroundName) {
        this.battlegroundName = battlegroundName;
        this.teams = new ArrayList<>();
        this.neutralFlags = new ArrayList<>();
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective tabKillCounter = scoreboard.registerNewObjective("kill_counter", "DUMMY", "0");
        tabKillCounter.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        this.preferences = BattlegroundPreferences.loadPreferences(this);
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
        player.getBukkitPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void addTeam(BattlegroundTeam team) {
        Team bukkitTeam = scoreboard.registerNewTeam(team.getTeamName());
        bukkitTeam.setAllowFriendlyFire(false);
        bukkitTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        bukkitTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        team.setBukkitTeam(bukkitTeam);
        this.teams.add(team);
    }

    // Валидация арены. Если арена невалидна, то к ней нельзя подключиться.
    public boolean isValid() {
        return teams.size() >= 2;
    }

    public BattlegroundTeam getSmallestTeam() {
        return teams.stream().min(Comparator.comparingInt(BattlegroundTeam::size)).orElse(null);
    }

    /**
     * Поиск команды по названию.
     * @return BattlegroundTeam или null, если команда не найдена
     * */
    @Nullable
    public BattlegroundTeam getTeamByName(String name) {
        return this.teams.stream().filter(b -> b.getTeamName().equals(name)).findFirst().orElse(null);
    }

    public List<BattlegroundPlayer> getPlayers() {
        final List<BattlegroundPlayer> players = new ArrayList<>();
        for (BattlegroundTeam team : teams) {
            for (Player player : team.getPlayers()) {
                players.add(BattlegroundPlayer.getBattlegroundPlayer(player));
            }
        }
        return players;
    }

    public void broadcast(String message) {
        this.getPlayers().forEach(p -> p.getBukkitPlayer().sendMessage(message));
    }

    public boolean getPreference(Preference preference) {
        return this.preferences.get(preference);
    }

}