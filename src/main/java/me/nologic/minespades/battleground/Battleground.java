package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.bukkit.util.BoundingBox;
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

    private final List<BattlegroundTeam> teams;
    private BattlegroundPreferences      preferences;

    @Getter @Setter
    private BoundingBox insideBox;

    public Battleground(String battlegroundName) {
        this.battlegroundName = battlegroundName;
        this.teams = new ArrayList<>();
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective killCounter = scoreboard.registerNewObjective("kill_counter", Criteria.DUMMY, Component.text(0));
        killCounter.setDisplaySlot(DisplaySlot.PLAYER_LIST);
    }

    public BattlegroundPlayer connect(Player player) {
        player.setScoreboard(scoreboard);
        return this.getSmallestTeam().join(player);
    }

    public void kick(BattlegroundPlayer player) {
        player.getTeam().kick(player.getBukkitPlayer());
        player.getBukkitPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void addTeam(BattlegroundTeam team) {
        Team bukkitTeam = scoreboard.registerNewTeam(team.getName());
        bukkitTeam.setAllowFriendlyFire(false);
        bukkitTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);
        team.setBukkitTeam(bukkitTeam);

        if (team.getFlag() != null)
            team.getFlag().setTeam(team);

        team.resetFlag();
        this.teams.add(team);
    }

    // Валидация арены. Если арена невалидна, то к ней нельзя подключиться.
    public boolean isValid() {
        return teams.size() >= 2;
    }

    public BattlegroundTeam getSmallestTeam() {
        return teams.stream().min(Comparator.comparingInt(BattlegroundTeam::size)).orElse(null);
    }

    @Nullable
    public BattlegroundTeam getTeamByName(String name) {
        return this.teams.stream().filter(b -> b.getName().equals(name)).findFirst().orElse(null);
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

    public void broadcast(TextComponent message) {
        this.getPlayers().forEach(p -> p.getBukkitPlayer().sendMessage(message));
    }

    public void setPreferences(BattlegroundPreferences preferences) {
        this.preferences = preferences;
    }
}