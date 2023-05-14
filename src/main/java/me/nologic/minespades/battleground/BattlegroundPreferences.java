package me.nologic.minespades.battleground;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.HashMap;

@RequiredArgsConstructor
public class BattlegroundPreferences implements Listener {

    private final Battleground battleground;
    private final HashMap<Preference, Boolean> preferences = new HashMap<>();

    // init() применяется при инициализации, set() через команду конфиг
    @SneakyThrows
    public void set(Preference preference, boolean state) {
        this.preferences.put(preference, state);
        // Пишем изменение в дб
        try (Connection connection = this.connect()) {
            Statement selectPreferencesStatement = connection.createStatement();
            ResultSet prefsSet = selectPreferencesStatement.executeQuery("SELECT parameters FROM preferences;"); prefsSet.next();
            JsonObject jsonPrefs = JsonParser.parseString(prefsSet.getString("parameters")).getAsJsonObject();
            selectPreferencesStatement.close();
            prefsSet.close();

            jsonPrefs.remove(preference.toString());
            jsonPrefs.addProperty(preference.toString(), state);
            PreparedStatement updatePrefsStatement = connection.prepareStatement("UPDATE preferences SET parameters = ?;");
            updatePrefsStatement.setString(1, jsonPrefs.toString());
            updatePrefsStatement.executeUpdate();
        }
    }

    public void init(Preference preference, boolean state) {
        this.preferences.put(preference, state);
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + Minespades.getPlugin(Minespades.class).getDataFolder() + "/battlegrounds/" + battleground.getBattlegroundName() + ".db");
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
    private void onPistonExtend(BlockPistonExtendEvent event) {
        if (preferences.get(Preference.PROTECT_RESPAWN)) {
            for (BattlegroundTeam team : battleground.getTeams()) {
                for (TeamRespawnPoint respawnPoint : team.getRespawnPoints()) {
                    for (Block block : event.getBlocks()) {
                        if (respawnPoint.getBoundingBox().contains(block.getBoundingBox().getCenter())) {
                            event.setCancelled(true);
                            event.getBlock().getLocation().createExplosion(1, false, false);
                            event.getBlock().breakNaturally();
                            event.getBlocks().forEach(Block::breakNaturally);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onPistonRetract(BlockPistonRetractEvent event) {
        if (preferences.get(Preference.PROTECT_RESPAWN)) {
            for (BattlegroundTeam team : battleground.getTeams()) {
                for (TeamRespawnPoint respawnPoint : team.getRespawnPoints()) {
                    for (Block block : event.getBlocks()) {
                        if (respawnPoint.getBoundingBox().contains(block.getBoundingBox().getCenter())) {
                            event.setCancelled(true);
                            event.getBlock().getLocation().createExplosion(1, false, false);
                            event.getBlock().breakNaturally();
                        }
                    }
                }
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
    private void onBlockFall(EntityChangeBlockEvent event) {
        if (preferences.get(Preference.PROTECT_RESPAWN) && event.getBlock().getWorld().equals(battleground.getWorld())) {
            if (event.getEntityType() == EntityType.FALLING_BLOCK && event.getTo() == Material.AIR) {
                event.setCancelled(true);
            }
        }

    }

    @EventHandler
    private void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (preferences.get(Preference.BLOCK_LAVA_USAGE) && BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer()) != null) {
            if (!player.isOp()) {
                event.setCancelled(true);
                player.setFireTicks(100);
            }
        }
    }

    @EventHandler
    private void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
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
    private void onPlayerHeldLavaBucket(PlayerItemHeldEvent event) {
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


            event.getBlock().setType(Material.AIR);
            event.getBlock().getWorld().createExplosion(event.getBlock().getLocation(), 5);
        }
    }

    @EventHandler
    private void onPlayerLeaveBed(PlayerInteractEvent event) {
        if (preferences.get(Preference.DENY_BED_SLEEP) && BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer()) != null) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                if (event.getClickedBlock().getType().toString().toLowerCase().contains("_bed")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    {
        BukkitRunnable cowardTracker = new BukkitRunnable() {
            @Override
            public void run() {
                if (!preferences.get(Preference.PUNISH_COWARDS)) return;
                for (BattlegroundPlayer player : battleground.getPlayers()) {
                    if (!battleground.getInsideBox().contains(player.getBukkitPlayer().getLocation().toVector())) {
                        if (!player.getBukkitPlayer().isOp() && player.getBukkitPlayer().getGameMode() == GameMode.SURVIVAL) {
                            player.getBukkitPlayer().damage(4);
                            player.getBukkitPlayer().setNoDamageTicks(0);
                        }
                    }
                }
            }
        };

        cowardTracker.runTaskTimer(Minespades.getPlugin(Minespades.class), 0, 20);
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
        BLOCK_LAVA_USAGE(true),
        PROTECT_RESPAWN(true),
        DENY_BED_SLEEP(true),
        PUNISH_COWARDS(true),
        JOIN_ONLY_FROM_MULTIGROUND(false);

        private final boolean defaultValue;

        public boolean getDefaultValue() {
            return this.defaultValue;
        }

        public static boolean isValid(String preference) {
            try {
                valueOf(preference);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        Preference(boolean defaultValue) {
            this.defaultValue = defaultValue;
        }

    }


}