package me.nologic.minespades.battleground;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Battleground содержит все необходимые методы для управления процессом игры и игроками.
 * <p> Чтобы игроки могли подключаться к арене, она должна быть запущена и проверена на минимальные
 * требования: как минимум две команды с установленными респавн-поинтами, инициализированные настройки,
 * загруженный мир, метод isLaunched должен возвращать true.
 * */
public final class Battleground {

    private final List<Team> teams;
    private boolean autoAssign, allowFriendlyFire, maxTeamSize, keepInventory, useCorpses, skipDeathScreen, colorfulEnding, airStrike;

    public Battleground(String battlegroundName) {
        this.name = battlegroundName;
        this.teams = new ArrayList<>();
    }

    // TODO: сохранение инвентаря игрока перед подключением, обязательно в дб, дабы игроки не проёбывали вещи
    public void join(Player player) {
        this.getSmallestTeam().join(player);
    }

    public void quit(Player player) {

    }

    public void addTeam(Team team) {
        this.teams.add(team);
    }

    public boolean isLaunchable() {
        return teams.size() >= 2;
    }
    public boolean isConnectable() {
        return launched;
    }

    private Team getSmallestTeam() {
        return teams.stream().min(Comparator.comparingInt(Team::size)).orElse(null);
    }

    public void broadcast(String message) {
        for (Team team : teams) {
            team.getPlayers().forEach(p -> p.sendMessage(message));
        }
    }

    /* Fields. */

    private boolean launched = false;
    public void setLaunched(boolean launched) {
        this.launched = launched;
    }

    private World world;
    public void setWorld(World world) {
        this.world = world;
    }
    public World getWorld() {
        return this.world;
    }

    private final String name;
    public String getBattlegroundName() {
        return this.name;
    }

    @Override
    public String toString() {
        return name;
    }

}