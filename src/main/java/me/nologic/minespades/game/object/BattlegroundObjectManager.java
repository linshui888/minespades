package me.nologic.minespades.game.object;

import lombok.Getter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.battleground.BattlegroundPreferences;
import me.nologic.minespades.game.object.base.BattlegroundFlag;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

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

        final Player player = event.getPlayer();
        final Block  block  = event.getBlock();

        /* If the player is not an arena player, ignore. */
        if (!BattlegroundPlayer.isBattlegroundPlayer(player))
            return;

        final BattlegroundPlayer battlegroundPlayer = BattlegroundPlayer.getBattlegroundPlayer(player);
        assert battlegroundPlayer != null;

        /* If the player tries to break the flag (or the block the flag is on), cancel. */
        if (flags.stream().anyMatch(flag -> flag.is(block) || flag.isStandingOn(block)))
            event.setCancelled(true);

        /* If a non-creative player tries to break respawn, and if battleground has respawn protection enabled, cancel. */
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE && respawnPoints.stream().anyMatch(respawn -> respawn.contains(block)))
            if (battlegroundPlayer.getBattleground().getPreference(BattlegroundPreferences.Preference.PROTECT_RESPAWN).getAsBoolean())
                event.setCancelled(true);

    }

    @EventHandler
    private void onBlockPlace(final BlockPlaceEvent event) {

        final Player player = event.getPlayer();
        final Block  block  = event.getBlock();

        /* If the player is not a battleground player, ignore. */
        if (!BattlegroundPlayer.isBattlegroundPlayer(player))
            return;

        final BattlegroundPlayer battlegroundPlayer = BattlegroundPlayer.getBattlegroundPlayer(player);
        assert battlegroundPlayer != null;

        /* If a non-creative player tries to place block on respawn, and if battleground has respawn protection enabled, cancel. */
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE && respawnPoints.stream().anyMatch(respawn -> respawn.contains(block)))
            if (battlegroundPlayer.getBattleground().getPreference(BattlegroundPreferences.Preference.PROTECT_RESPAWN).getAsBoolean())
                event.setCancelled(true);

    }

}
