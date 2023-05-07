package me.nologic.minespades.battleground;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

@RequiredArgsConstructor
public class BattlegroundPreferences implements Listener {

    private final Battleground battleground;
    private final HashMap<Preference, Boolean> preferences = new HashMap<>();

    public void apply(Preference preference, boolean state) {
        this.preferences.put(preference, state);
    }

    public boolean get(Preference preference) {
        return this.preferences.get(preference);
    }

    @EventHandler
    private void onPotionConsume(PlayerItemConsumeEvent event) {
        if (preferences.get(Preference.DELETE_EMPTY_BOTTLES)) {
            if (event.getItem().getType() == Material.POTION) {
                event.setReplacement(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler
    private void onPortalCreate(PortalCreateEvent event) {
        if (preferences.get(Preference.DISABLE_PORTALS)) {
            if (event.getWorld().equals(battleground.getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (preferences.get(Preference.PREVENT_ITEM_DAMAGE) && BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerBucket(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (preferences.get(Preference.BLOCK_LAVA_USAGE) && BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer()) != null) {
            if (!player.isOp()) {
                event.setCancelled(true);
                player.setFireTicks(100);
            }
        }
    }

    @EventHandler
    private void onPlayerBucket(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (preferences.get(Preference.BLOCK_LAVA_USAGE) && BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer()) != null) {
            if (!player.isOp()) {
                if (event.getBucket() == Material.LAVA_BUCKET) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    private void onPlayerBucket(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (preferences.get(Preference.BLOCK_LAVA_USAGE) && BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer()) != null) {
            if (!player.isOp()) {
                if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.LAVA_BUCKET) {
                    player.setFireTicks(100);
                }
            }
        }
    }

    @EventHandler
    private void onBlockDispense(BlockDispenseEvent event) {
        if (preferences.get(Preference.BLOCK_LAVA_USAGE) && event.getBlock().getWorld().equals(battleground.getWorld()) && event.getItem().getType() == Material.LAVA_BUCKET) {
            event.setCancelled(true);
            event.getBlock().getWorld().createExplosion(event.getBlock().getLocation(), 5);
        }
    }

    public enum Preference {

        AUTO_ASSIGN(true),
        FRIENDLY_FIRE(false),
        KEEP_INVENTORY(true),
        DELETE_EMPTY_BOTTLES(true),
        COLORFUL_ENDING(true),
        FLAG_PARTICLES(true),
        FLAG_STEALER_TRAILS(true),
        DISABLE_PORTALS(true),
        PREVENT_ITEM_DAMAGE(true),
        BLOCK_LAVA_USAGE(true);

        private final boolean defaultValue;

        public boolean getDefaultValue() {
            return this.defaultValue;
        }

        Preference(boolean defaultValue) {
            this.defaultValue = defaultValue;
        }

    }


}