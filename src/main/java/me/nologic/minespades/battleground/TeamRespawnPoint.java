package me.nologic.minespades.battleground;

import lombok.Getter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.game.event.PlayerCarriedFlagEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.Objects;

public class TeamRespawnPoint implements Listener {

    private final Minespades plugin = Minespades.getPlugin(Minespades.class);

    private final Battleground battleground;
    private final BattlegroundTeam team;

    @Getter
    private final Location position;
    private final BoundingBox box;

    @Getter
    private final BukkitRunnable tick;

    public TeamRespawnPoint(BattlegroundTeam team, Location position) {
        this.battleground = team.getBattleground();
        this.team = team;
        this.position = position;
        box = BoundingBox.of(position, 2, 2, 2);
        this.tick = new BukkitRunnable() {

            @Override
            public void run() {
                for (Entity entity : battleground.getWorld().getNearbyEntities(box)) {
                    if (entity instanceof Player player) {
                        if (battleground.getScoreboard().equals(player.getScoreboard())) {
                            if (Objects.equals(player.getScoreboard().getPlayerTeam(player), team.getBukkitTeam())) {
                                BattlegroundPlayer bgPlayer = BattlegroundPlayer.getBattlegroundPlayer(player);
                                if (bgPlayer.isCarryingFlag()) {
                                    Bukkit.getServer().getPluginManager().callEvent(new PlayerCarriedFlagEvent(battleground, bgPlayer, bgPlayer.getFlag()));
                                }
                            }
                        }
                    }
                }
            }

        };
        tick.runTaskTimer(plugin, 0, 10);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * С целью безопасности игроков, разрушать блоки на точке возрождения запрещено.
     */
    @EventHandler
    private void preventBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameMaster().getPlayerManager().getBattlegroundPlayer(player) != null) {
            if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                if (box.contains(event.getBlock().getLocation().toVector())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * С целью безопасности игроков, ставить блоки на точке возрождения запрещено.
     */
    @EventHandler
    private void preventBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameMaster().getPlayerManager().getBattlegroundPlayer(player) != null) {
            if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                if (box.contains(event.getBlock().getLocation().toVector())) {
                    event.setCancelled(true);
                }
            }
        }
    }

}