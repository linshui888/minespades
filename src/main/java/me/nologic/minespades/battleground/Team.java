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

    // Ну тут всё ясно, не?
    private final List<Location> respawnLocations;

    // Лоадауты, или "стартовые наборы"
    private final List<Inventory> loadouts;

    public Team(String name, int lifepool, String hexColor) {
        this.players = new ArrayList<>();
        this.loadouts = new ArrayList<>();
        this.respawnLocations = new ArrayList<>();
        this.name = name;
        this.lifepool = lifepool;
        this.hexColor = hexColor;
    }

    public void add(Location location) {
        this.respawnLocations.add(location);
    }

    public void add(Inventory loadout) {
        this.loadouts.add(loadout);
    }

    public void connect(Player player) {
        if (players.size() == 32) {
            player.sendMessage("Команда заполнена.");
            return;
        }
        this.players.add(player);
        player.sendMessage("Вы зашли за команду " + name + "!");
    }

    public void disconnect(Player player) {
        this.players.remove(player);
        player.sendMessage("Вы покинули команду " + name + ".");
    }

    public List<Player> getPlayers() {
        return this.players;
    }

    public String getName() {
        return this.name;
    }

}