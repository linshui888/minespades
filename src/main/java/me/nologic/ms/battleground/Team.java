package me.nologic.ms.battleground;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public class Team {

    private String name;
    private List<Player> players;
    private int lifepool;
    private String hexColor;

    // Ну тут всё ясно, не?
    private List<Location> respawnLocations;

    // Лоадауты, или "стартовые наборы"
    private List<PlayerInventory> loadouts;

    public Team(String name, int lifepool, String hexColor) {
        players = new ArrayList<>();
        this.name = name;
        this.lifepool = lifepool;
    }

    public void addLoadout(String encodedLoadoutBase64) {
        // декодирование из b64
        this.loadouts.add();
    }

    public void addRespawnPoint(Location location) {
        this.respawnLocations.add(location);
    }

    public void connect(Player player) {
        if (players.size() == size) {
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