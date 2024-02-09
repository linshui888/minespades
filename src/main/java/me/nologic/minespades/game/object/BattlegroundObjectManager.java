package me.nologic.minespades.game.object;

import lombok.Getter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.game.object.base.BattlegroundFlag;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BattlegroundObjectManager implements Listener {

    private final Minespades             plugin = Minespades.getInstance();
    private final List<TeamRespawnPoint> respawnPoints;
    private final List<BattlegroundFlag> flags;

    public BattlegroundObjectManager() {
        this.flags = new ArrayList<>();
        this.respawnPoints = new ArrayList<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onBlockBreak(final BlockBreakEvent event) {

        final Block block = event.getBlock();

        if (flags.stream().anyMatch(flag -> flag.is(block) || flag.isStandingOn(block)))
            event.setCancelled(true);

    }

}
