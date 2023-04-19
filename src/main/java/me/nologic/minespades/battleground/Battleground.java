package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
public final class Battleground {

    private final String battlegroundName;
    private final boolean launched = false;
    private @Setter World world;

    private final Set<Player> players;
    private final List<Team> teams;

    // TODO: Возможно стоит перенести параметры арены в отдельный класс?
    private boolean autoAssign, allowFriendlyFire, maxTeamSize, keepInventory, useCorpses, skipDeathScreen, colorfulEnding, airStrike;

    public Battleground(String battlegroundName) {
        this.battlegroundName = battlegroundName;
        this.players = new HashSet<>();
        this.teams = new ArrayList<>();
    }

    // TODO: сохранение инвентаря игрока перед подключением, обязательно в дб, дабы игроки не проёбывали вещи
    public BattlegroundPlayer join(Player player) {
        if (!players.contains(player)) {
            players.add(player);
            return this.getSmallestTeam().join(player);
        }
        return null;
    }

    public void kick(Player player) {

    }

    public void addTeam(Team team) {
        this.teams.add(team);
    }

    // Валидация арены. Если арена невалидна, то к ней нельзя подключиться.
    public boolean isValid() {
        return teams.size() >= 2;
    }

    public Team getSmallestTeam() {
        return teams.stream().min(Comparator.comparingInt(Team::size)).orElse(null);
    }

    // TODO: отправку сообщений логично будет разметить и разграничить (what?) внутри этого класса, а не в местах вызова
    public void broadcast(TextComponent message) {
        for (Team team : teams) {
            team.getPlayers().forEach(p -> p.sendMessage(message));
        }
    }

    public boolean havePlayer(Player player) {
        return players.contains(player);
    }

}