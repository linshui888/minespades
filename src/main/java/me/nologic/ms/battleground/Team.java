package me.nologic.ms.battleground;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Team {

    private String name;
    private int size;
    private List<Player> players;
    private int lifepool;

    private List<Loadout> loadouts;

    public Team(String name, int maxPlayers, int lifepool) {
        players = new ArrayList<>();
        this.name = name;
        this.size = maxPlayers;
        this.lifepool = lifepool;
    }

    public void join(Player player) {
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

}
