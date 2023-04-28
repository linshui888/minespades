package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Getter
public final class Battleground {

    private final Scoreboard scoreboard;

    private final String battlegroundName;
    private @Setter boolean enabled = false;
    private @Setter World world;

    private final Set<BattlegroundPlayer> players;
    private final List<BattlegroundTeam> teams;

    // TODO: Возможно стоит перенести параметры арены в отдельный класс?
    private boolean autoAssign, allowFriendlyFire, maxTeamSize, keepInventory, useCorpses, skipDeathScreen, colorfulEnding, airStrike;

    public Battleground(String battlegroundName) {
        this.battlegroundName = battlegroundName;
        this.players = new HashSet<>();
        this.teams = new ArrayList<>();
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    }

    public BattlegroundPlayer connect(Player player) {
        player.setScoreboard(scoreboard);
        BattlegroundPlayer bgPlayer = this.getSmallestTeam().join(player);
        players.add(bgPlayer);
        return bgPlayer;
    }

    public void kick(BattlegroundPlayer player) {
        players.remove(player);
        player.getTeam().kick(player.getPlayer());
        player.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void addTeam(BattlegroundTeam team) {
        Team bukkitTeam = scoreboard.registerNewTeam(team.getName());
        bukkitTeam.setAllowFriendlyFire(false);
        bukkitTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
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

    public void broadcast(TextComponent message) {
        players.forEach(p -> p.getPlayer().sendMessage(message));
    }

}