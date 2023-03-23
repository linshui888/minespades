package me.nologic.minespades.battleground;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Team {

    private final Battleground battleground;
    private final String       name;
    private final int          lifepool;
    private final List<Player> players;
    private final String       hexColor;

    private final Random random;

    private final List<Location> respawnLocations;

    private final List<Inventory> loadouts;

    public Team(Battleground battleground, String name, int lifepool, String hexColor) {
        this.battleground = battleground;
        this.players = new ArrayList<>();
        this.loadouts = new ArrayList<>();
        this.respawnLocations = new ArrayList<>();
        this.name = name;
        this.lifepool = lifepool;
        this.hexColor = hexColor;
        this.random = new Random();
    }

    public void addRespawnLocation(Location location) {
        this.respawnLocations.add(location);
    }

    public void addLoadout(Inventory loadout) {
        this.loadouts.add(loadout);
    }

    private int size = 0;
    public int size() {
        return size;
    }

    public BattlegroundPlayer join(Player player) {
        this.players.add(player); size++;
        player.teleport(getRandomRespawnLocation());
        return new BattlegroundPlayer(battleground, this, player);
    }

    public Location getRandomRespawnLocation() {
        return respawnLocations.get(random.nextInt(respawnLocations.size()));
    }

    public void disconnect(Player player) {
        this.players.remove(player); size--;
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