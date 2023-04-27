package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import me.nologic.minespades.battleground.editor.loadout.BattlegroundLoadout;
import me.nologic.minespades.game.flag.BattlegroundFlag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class BattlegroundTeam {

    private final Battleground   battleground;
    private @Setter Team         bukkitTeam;

    private final String         name, color;
    private @Setter int          lifepool;

    private final List<Location>             respawnLocations = new ArrayList<>();
    private final List<BattlegroundLoadout>  loadouts = new ArrayList<>();
    private final Set<Player>                players = new HashSet<>();

    @Nullable
    private final BattlegroundFlag flag;

    public BattlegroundTeam(Battleground battleground, String name, String color, int lifepool, BattlegroundFlag flag) {
        this.battleground = battleground;
        this.name = name;
        this.color = color;
        this.lifepool = lifepool;
        this.flag = flag;
    }

    public void addRespawnLocation(Location location) {
        this.respawnLocations.add(location);
    }

    public void addLoadout(BattlegroundLoadout loadout) {
        this.loadouts.add(loadout);
    }

    public Location getRandomRespawnLocation() {
        return respawnLocations.get((int) (Math.random() * respawnLocations.size()));
    }

    public void resetFlag() {
        if (flag != null) {
            flag.reset();
        }
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