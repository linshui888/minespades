package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

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

    // TODO: сохранение инвентаря игрока перед подключением, обязательно в дб, дабы игроки не проёбывали вещи
    public BattlegroundPlayer connect(Player player) {
        player.setScoreboard(scoreboard);
        BattlegroundPlayer bgPlayer = this.getSmallestTeam().join(player);
        players.add(bgPlayer);
        return bgPlayer;
    }

    public void kickPlayer(BattlegroundPlayer player) {
        player.getTeam().getBukkitTeam().removePlayer(player.getPlayer());
        player.getTeam().kick(player.getPlayer());
        player.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void addTeam(BattlegroundTeam team) {
        Team bukkitTeam = scoreboard.registerNewTeam(team.getName());
        bukkitTeam.setAllowFriendlyFire(false);
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

    // TODO: отправку сообщений логично будет разметить и разграничить (what?) внутри этого класса, а не в местах вызова
    public void broadcast(TextComponent message) {
        players.forEach(p -> p.getPlayer().sendMessage(message));
    }

}