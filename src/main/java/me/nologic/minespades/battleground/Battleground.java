package me.nologic.minespades.battleground;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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

    public void addTeam(Team team) {
        this.teams.add(team);
        Bukkit.getLogger().info("Добавлена команда " + team.getName() + "!");
    }

    public World getWorld() {
        return this.world;
    }

    public boolean isLaunchable() {
        return teams.size() >= 2;
    }

    public boolean isConnectable() {
        return launched;
    }

    // TODO: сохранение инвентаря игрока перед подключением, обязательно в дб, дабы игроки не проёбывали вещи
    public void connect(Player player) {
        if (!launched) return;

        Team smallestTeam = null;
        for (Team team : teams)
            if (smallestTeam == null) smallestTeam = team;
            else if (smallestTeam.getPlayers().size() > team.getPlayers().size()) smallestTeam = team;
        if (smallestTeam != null)
            smallestTeam.connect(player);
    }

    public void disconnect(Player player) {

    }

    private boolean launched = false;
    public void setLaunched(boolean launched) {
        this.launched = launched;
    }

    private World world;
    public void setWorld(World world) {
        this.world = world;
    }

    private final String name;
    public String getName() {
        return this.name;
    }

}