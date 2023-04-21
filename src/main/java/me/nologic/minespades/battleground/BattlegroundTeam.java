package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scoreboard.Team;

import java.util.*;

@Getter
public class BattlegroundTeam {

    private final Battleground battleground;
    private @Setter Team bukkitTeam;

    private final String       name, color;
    private final int          lifepool;

    private final List<Location> respawnLocations;
    private final List<Inventory> loadouts;

    public BattlegroundTeam(Battleground battleground, String name, int lifepool, String hexColor) {
        this.battleground = battleground;
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
        this.bukkitTeam.addPlayer(player);
        BattlegroundPlayer bgPlayer = new BattlegroundPlayer(battleground, this, player);
        player.teleport(this.getRandomRespawnLocation());
        bgPlayer.setRandomLoadout();
        return bgPlayer;
    }

    public void kick(Player player) {
        this.bukkitTeam.removePlayer(player);
    }

    public int size() {
        return bukkitTeam.getSize();
    }

}