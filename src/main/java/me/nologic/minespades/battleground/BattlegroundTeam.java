package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.editor.loadout.BattlegroundLoadout;
import me.nologic.minespades.game.object.TeamBattlegroundFlag;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Configurable;
import me.nologic.minority.annotations.ConfigurationKey;
import me.nologic.minority.annotations.Type;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configurable(path = "game-settings") @Getter
public class BattlegroundTeam implements MinorityFeature {

    private final Battleground   battleground;
    private @Setter Team         bukkitTeam;

    private final TextColor      color;
    private final String         name;
    private @Setter int          lifepool;

    // Количество очков, забираемое в случае потери флага
    @ConfigurationKey(name = "flag-lifepool-penalty", type = Type.INTEGER, value = "15", comment = "How many lifepoints will the team lose if their flag is stolen?")
    private @Getter int flagLifepoolPenalty;

    @Getter
    private final List<TeamRespawnPoint>     respawnPoints = new ArrayList<>();
    private final List<BattlegroundLoadout>  loadouts = new ArrayList<>();
    private final Set<Player>                players = new HashSet<>();

    @Setter @Nullable
    private TeamBattlegroundFlag flag;

    public BattlegroundTeam(Battleground battleground, String name, String color, int lifepool) {
        this.battleground = battleground;
        this.name = name;
        this.color = TextColor.fromHexString("#" + color);
        this.lifepool = lifepool;
        this.init(this, this.getClass(), Minespades.getInstance());
    }

    public TextComponent getDisplayName() {
        return Component.text(name).color(color);
    }

    public void addRespawnLocation(Location location) {
        this.respawnPoints.add(new TeamRespawnPoint(this, location));
    }

    public void addLoadout(BattlegroundLoadout loadout) {
        this.loadouts.add(loadout);
    }

    public Location getRandomRespawnLocation() {
        return respawnPoints.get((int) (Math.random() * respawnPoints.size())).getPosition();
    }

    public void resetFlag() {
        if (flag != null) {
            flag.reset();
        }
    }

    @NotNull
    public BattlegroundPlayer join(Player player) {
        this.bukkitTeam.addPlayer(player);
        this.players.add(player);
        BattlegroundPlayer bgPlayer = new BattlegroundPlayer(battleground, this, player);
        player.teleport(this.getRandomRespawnLocation());
        bgPlayer.setRandomLoadout(); // TODO: лоадаут выбирается тут
        return bgPlayer;
    }

    public void kick(Player player) {
        this.bukkitTeam.removePlayer(player);
        this.players.remove(player);
    }

    public int size() {
        return bukkitTeam.getSize();
    }

    /**
     * Returns the defeated state of the team, team qualified as defeated if:
     * 1. Lifepool of the team equals or less than zero.
     * 2. All players in the team having a spectator mode OR there are just no players.
     */
    public boolean isDefeated() {
        return lifepool <= 0 && players.stream().allMatch(p -> p.getGameMode() == GameMode.SPECTATOR) || players.size() == 0;
    }

}