package me.nologic.minespades.game.object;

import lombok.Getter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.battleground.BattlegroundTeam;
import me.nologic.minespades.game.event.PlayerCarryFlagEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

@Getter
public class TeamRespawnPoint {

    private static final Minespades plugin = Minespades.getInstance();

    private final Battleground     battleground;
    private final BattlegroundTeam team;
    private final BoundingBox      boundingBox;
    private final Location         respawnPosition;

    public TeamRespawnPoint(final BattlegroundTeam team, final Location position) {
        plugin.getGameMaster().getObjectManager().getRespawnPoints().add(this);
        this.battleground = team.getBattleground();
        this.respawnPosition = position;
        this.team = team;
        this.boundingBox = BoundingBox.of(position.getBlock().getLocation().add(0.5, 0.5, 0.5), 2.5, 2.5, 2.5);

        Bukkit.getServer().getScheduler().runTaskTimer(plugin, (task) -> {
            if (!battleground.isEnabled()) task.cancel();
            this.tick();;
        }, 0, 10L);
    }

    private void tick() {
        for (Entity entity : battleground.getWorld().getNearbyEntities(boundingBox)) {
            BattlegroundPlayer battlegroundPlayer;
            if (entity instanceof Player player && (battlegroundPlayer = BattlegroundPlayer.getBattlegroundPlayer(player)) != null) {
                if (this.team == battlegroundPlayer.getTeam() && battlegroundPlayer.isCarryingFlag())
                    Bukkit.getServer().getPluginManager().callEvent(new PlayerCarryFlagEvent(battleground, battlegroundPlayer, battlegroundPlayer.getFlag()));
            }
        }
    }

    public boolean contains(final Block block) {
        return boundingBox.contains(block.getLocation().toVector());
    }

}