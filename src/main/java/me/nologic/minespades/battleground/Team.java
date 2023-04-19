package me.nologic.minespades.battleground;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

@Getter
public class Team {

    private final Battleground battleground;

    private final String       name, color;
    private final int          lifepool;
    private final Set<Player>  players;

    private final List<Location> respawnLocations;
    private final List<Inventory> loadouts;

    public Team(Battleground battleground, String name, int lifepool, String hexColor) {
        this.battleground = battleground;
        this.players = new HashSet<>();
        this.loadouts = new ArrayList<>();
        this.respawnLocations = new ArrayList<>();
        this.name = name;
        this.lifepool = lifepool;
        this.color = hexColor;
    }

    public void addRespawnLocation(Location location) {
        this.respawnLocations.add(location);
    }

    public void addLoadout(Inventory loadout) {
        this.loadouts.add(loadout);
    }

    public Location getRandomRespawnLocation() {
        return respawnLocations.get((int) (Math.random() * respawnLocations.size()));
    }

    public BattlegroundPlayer join(Player player) {
        this.players.add(player);
        player.teleport(this.getRandomRespawnLocation());
        return new BattlegroundPlayer(battleground, this, player);
    }

    public void kick(Player player) {
        this.players.remove(player);
    }

    public int size() {
        return players.size();
    }

}