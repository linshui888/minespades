package me.nologic.minespades.battleground;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class Team {

    private final String       name;
    private final int          lifepool;
    private final List<Player> players;
    private final String       hexColor;

    private final List<Location> respawnLocations;

    private final List<Inventory> loadouts;

    public Team(String name, int lifepool, String hexColor) {
        this.players = new ArrayList<>();
        this.loadouts = new ArrayList<>();
        this.respawnLocations = new ArrayList<>();
        this.name = name;
        this.lifepool = lifepool;
        this.hexColor = hexColor;
    }

    public void addRespawnPoint(Location location) {
        this.respawnLocations.add(location);
    }

    public void addLoadout(Inventory loadout) {
        this.loadouts.add(loadout);
    }

    public void connect(Player player) {
        if (players.size() < 32) this.players.add(player); else player.sendMessage("В команде нет места.");
    }

    public void disconnect(Player player) {
        this.players.remove(player);
    }

    public List<Player> getPlayers() {
        return this.players;
    }

    public String getName() {
        return this.name;
    }

    public int getLifepool() {
        return lifepool;
    }

}