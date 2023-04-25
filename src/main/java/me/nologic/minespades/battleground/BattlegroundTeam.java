package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import me.nologic.minespades.battleground.editor.loadout.Loadout;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class BattlegroundTeam {

    private final Battleground battleground;
    private @Setter Team bukkitTeam;

    private final String       name, color;
    private @Setter int          lifepool;

    private final List<Location> respawnLocations;
    private final List<Loadout>  loadouts;
    private final Set<Player>    players;

    public BattlegroundTeam(Battleground battleground, String name, int lifepool, String hexColor) {
        this.battleground = battleground;
        this.loadouts = new ArrayList<>();
        this.respawnLocations = new ArrayList<>();
        this.players = new HashSet<>();
        this.name = name;
        this.lifepool = lifepool;
        this.color = hexColor;
    }

    public void addRespawnLocation(Location location) {
        this.respawnLocations.add(location);
    }

    public void addLoadout(Loadout loadout) {
        this.loadouts.add(loadout);
    }

    public Location getRandomRespawnLocation() {
        return respawnLocations.get((int) (Math.random() * respawnLocations.size()));
    }

    public BattlegroundPlayer join(Player player) {
        this.bukkitTeam.addPlayer(player);
        this.players.add(player);
        BattlegroundPlayer bgPlayer = new BattlegroundPlayer(battleground, this, player);
        player.teleport(this.getRandomRespawnLocation());
        bgPlayer.setRandomLoadout();
        player.sendMessage("§eПодключение успешно! Ваша команда: " + bgPlayer.getTeam().name);
        return bgPlayer;
    }

    public void kick(Player player) {
        this.bukkitTeam.removePlayer(player);
        this.players.remove(player);
    }

    public int size() {
        return bukkitTeam.getSize();
    }

}