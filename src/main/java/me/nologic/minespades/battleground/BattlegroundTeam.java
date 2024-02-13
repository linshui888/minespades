package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.editor.loadout.BattlegroundLoadout;
import me.nologic.minespades.game.object.TeamBattlegroundFlag;
import me.nologic.minespades.game.object.TeamRespawnPoint;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Configurable;
import net.md_5.bungee.api.ChatColor;
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

    private final ChatColor      color;
    private final String         teamName;
    private @Setter int          lifepool;

    private final int flagLifepoolPenalty;

    private final List<TeamRespawnPoint>    respawnPoints = new ArrayList<>();
    private final List<BattlegroundLoadout> loadouts      = new ArrayList<>();
    private final Set<Player>               players       = new HashSet<>();

    @Setter @Nullable
    private TeamBattlegroundFlag flag;

    /* Team's current score. Scores are obtained for successfully capturing flags. */
    private int score;

    public BattlegroundTeam(final Battleground battleground, final String teamName, final String teamHexColor, final  int startLifepool) {
        this.battleground = battleground;
        this.teamName = teamName;
        this.color = ChatColor.of("#" + teamHexColor);
        this.lifepool = startLifepool;
        this.flagLifepoolPenalty = battleground.getPreference(BattlegroundPreferences.Preference.FLAG_LIFEPOOL_PENALTY).getAsInteger();
        this.init(this, this.getClass(), Minespades.getInstance());
    }

    public int addScore(final int score) {
        return this.score += score;
    }

    public String getDisplayName() {
        return color + teamName;
    }

    public void addRespawnLocation(Location location) {
        this.respawnPoints.add(new TeamRespawnPoint(this, location));
    }

    public void addLoadout(BattlegroundLoadout loadout) {
        this.loadouts.add(loadout);
    }

    public Location getRandomRespawnLocation() {
        return respawnPoints.get((int) (Math.random() * respawnPoints.size())).getRespawnPosition();
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
        return (lifepool <= 0 || !players.stream().allMatch(p -> p.getGameMode() == GameMode.SPECTATOR)) && !players.isEmpty();
    }

}