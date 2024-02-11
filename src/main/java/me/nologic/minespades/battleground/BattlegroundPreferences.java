package me.nologic.minespades.battleground;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.xephi.authme.events.LoginEvent;
import lombok.Getter;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.util.BattlegroundDataDriver;
import me.nologic.minespades.game.object.TeamRespawnPoint;
import me.nologic.minespades.battleground.BattlegroundPreferences.Preference.PreferenceValue;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.HashMap;

public class BattlegroundPreferences implements Listener {

    private final Battleground battleground;
    private final HashMap<Preference, Preference.PreferenceValue> preferences = new HashMap<>();

    public BattlegroundPreferences(final Battleground battleground) {
        this.battleground = battleground;
        if (Bukkit.getPluginManager().isPluginEnabled("AuthMe")) {
            Bukkit.getServer().getPluginManager().registerEvents(new AuthMeLoginListener(), Minespades.getInstance());
        }
    }

    @SneakyThrows
    public void set(Preference preference, final Object value, final boolean writeChangesToDatabaseInstantly) {
        this.preferences.put(preference, new Preference.PreferenceValue(String.valueOf(value)));
        if (writeChangesToDatabaseInstantly) {
            BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(battleground);
            try (ResultSet result = driver.executeQuery("SELECT parameters FROM preferences;", true)) {
                JsonObject parameters = JsonParser.parseString(result.getString("parameters")).getAsJsonObject();
                parameters.remove(preference.toString());
                parameters.addProperty(preference.toString(), preferences.get(preference).getAsString());
                driver.executeUpdate("UPDATE preferences SET parameters = ?;", parameters.toString());
                driver.closeConnection();
            }
        }
    }

    @NotNull /* Возвращает либо значение из карты preferences, либо дефолтное. */
    public PreferenceValue get(final Preference preference) {
        final PreferenceValue result = this.preferences.get(preference);
        return result != null ? result : preference.getDefaultValue();
    }

    /**
     * Статический метод, с ним можно довольно легко инициализировать настройки любой арены.
     * Автоматически добавляет в датабазу новые Preference, которые ранее отсутствовали.
     *  */
    @SneakyThrows
    public static BattlegroundPreferences loadPreferences(final Battleground battleground) {

        final BattlegroundPreferences battlegroundPreferences = new BattlegroundPreferences(battleground);
        final BattlegroundDataDriver  driver                  = new BattlegroundDataDriver().connect(battleground);

        try (final ResultSet result = driver.executeQuery("SELECT * FROM preferences;", true)) {

            battleground.setWorld(Bukkit.getWorld(result.getString("world")));

            /* We're taking the battleground preferences from the database. */
            final JsonObject parameters = JsonParser.parseString(result.getString("parameters")).getAsJsonObject();

            /* Lazy loop initialization followed by serialization to JSON and saving to the database. */
            for (final Preference preference : Preference.values()) {
                if (parameters.get(preference.toString()) == null) {
                    battlegroundPreferences.set(preference, preference.getDefaultValue().getAsString(), false);
                    parameters.addProperty(preference.toString(), preference.getDefaultValue().getAsString());
                } else {
                    battlegroundPreferences.set(preference, parameters.get(preference.toString()).getAsString(), false);
                }
            }

            driver.executeUpdate("UPDATE preferences SET parameters = ?;", parameters.toString());
        }
        driver.closeConnection();
        return battlegroundPreferences;
    }

    @EventHandler
    private void onPotionConsume(final PlayerItemConsumeEvent event) {
        if (preferences.get(Preference.REMOVE_EMPTY_BOTTLES).getAsBoolean()) {

            if (!event.getItem().getType().equals(Material.POTION))
                return;

            final Player player = event.getPlayer();

            Bukkit.getScheduler().runTaskLater(Minespades.getInstance(), () -> {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item.getType().equals(Material.GLASS_BOTTLE)) {
                        player.getInventory().remove(Material.GLASS_BOTTLE);
                    }
                }
            }, 1L);

        }
    }

    @EventHandler
    private void onPortalCreate(PortalCreateEvent event) {
        if (preferences.get(Preference.DISABLE_PORTALS).getAsBoolean()) {
            if (event.getWorld().equals(battleground.getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onPistonExtend(BlockPistonExtendEvent event) {
        if (preferences.get(Preference.PROTECT_RESPAWN).getAsBoolean()) {
            for (BattlegroundTeam team : battleground.getTeams()) {
                for (TeamRespawnPoint respawnPoint : team.getRespawnPoints()) {
                    for (Block block : event.getBlocks()) {
                        if (respawnPoint.getBoundingBox().contains(block.getBoundingBox().getCenter())) {
                            battleground.getWorld().createExplosion(event.getBlock().getLocation(), 1, false, false);
                            event.getBlock().breakNaturally();
                            event.getBlocks().forEach(Block::breakNaturally);
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onPistonRetract(BlockPistonRetractEvent event) {
        if (preferences.get(Preference.PROTECT_RESPAWN).getAsBoolean()) {
            for (BattlegroundTeam team : battleground.getTeams()) {
                for (TeamRespawnPoint respawnPoint : team.getRespawnPoints()) {
                    for (Block block : event.getBlocks()) {
                        if (respawnPoint.getBoundingBox().contains(block.getBoundingBox().getCenter())) {
                            battleground.getWorld().createExplosion(event.getBlock().getLocation(), 1, false, false);
                            event.getBlock().breakNaturally();
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (preferences.get(Preference.PREVENT_ITEM_DAMAGE).getAsBoolean() && BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockFall(EntityChangeBlockEvent event) {
        if (preferences.get(Preference.PROTECT_RESPAWN).getAsBoolean() && event.getBlock().getWorld().equals(battleground.getWorld())) {
            if (event.getEntityType() == EntityType.FALLING_BLOCK && event.getTo() == Material.AIR) {
                event.setCancelled(true);
            }
        }

    }

    @EventHandler
    private void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (preferences.get(Preference.BLOCK_LAVA_USAGE).getAsBoolean() && BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer()) != null) {
            if (!player.isOp()) {
                event.setCancelled(true);
                player.setFireTicks(100);
            }
        }
    }

    @EventHandler
    private void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (preferences.get(Preference.BLOCK_LAVA_USAGE).getAsBoolean() && BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer()) != null) {
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
        if (preferences.get(Preference.BLOCK_LAVA_USAGE).getAsBoolean() && BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer()) != null) {
            if (!player.isOp()) {
                if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.LAVA_BUCKET) {
                    player.setFireTicks(100);
                }
            }
        }
    }

    @EventHandler
    private void onBlockDispense(BlockDispenseEvent event) {
        if (preferences.get(Preference.BLOCK_LAVA_USAGE).getAsBoolean() && event.getBlock().getWorld().equals(battleground.getWorld()) && event.getItem().getType() == Material.LAVA_BUCKET) {
            event.setCancelled(true);


            event.getBlock().setType(Material.AIR);
            event.getBlock().getWorld().createExplosion(event.getBlock().getLocation(), 5);
        }
    }

    @EventHandler
    private void onPlayerLeaveBed(PlayerInteractEvent event) {
        if (preferences.get(Preference.DENY_BED_SLEEP).getAsBoolean() && BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer()) != null) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                if (event.getClickedBlock().getType().toString().toLowerCase().contains("_bed")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(Minespades.getInstance(), () -> {
            // Подключаем игрока к арене через 1 тик после логина, дабы избежать багов
            if (preferences.get(Preference.FORCE_AUTOJOIN).getAsBoolean() && !Bukkit.getPluginManager().isPluginEnabled("AuthMe")) {
                Minespades.getInstance().getGameMaster().getPlayerManager().connect(event.getPlayer(), battleground, battleground.getSmallestTeam());
                event.getPlayer().sendMessage("&7Вы были автоматически подключены к арене. Чтобы покинуть арену, напишите &3/ms q&7.");
            }
        }, 1);
    }

    private final class AuthMeLoginListener implements Listener {

        // Подержка AuthMe
        @EventHandler
        private void onPlayerLogin(LoginEvent event) {
            // Подключаем игрока к арене через 1 тик после логина, дабы избежать багов
            Bukkit.getScheduler().runTaskLater(Minespades.getInstance(), () -> {
                if (preferences.get(Preference.FORCE_AUTOJOIN).getAsBoolean()) {
                    Minespades.getInstance().getGameMaster().getPlayerManager().connect(event.getPlayer(), battleground, battleground.getSmallestTeam());
                    event.getPlayer().sendMessage("&7Вы были автоматически подключены к арене. Чтобы покинуть арену, напишите &3/ms q&7.");
                }
            }, 1);
        }

    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (preferences.get(Preference.NO_DAMAGE_COOLDOWN).getAsBoolean()) {
            if (event.getEntity() instanceof Player player && BattlegroundPlayer.getBattlegroundPlayer(player) != null) {
                player.setNoDamageTicks(0);
            }
        }
    }

    {
        BukkitRunnable cowardTracker = new BukkitRunnable() {

            @Override
            public void run() {
                if (!preferences.get(Preference.PUNISH_COWARDS).getAsBoolean()) return;
                for (BattlegroundPlayer player : battleground.getBattlegroundPlayers()) {
                    if (!battleground.getBoundingBox().contains(player.getBukkitPlayer().getLocation().toVector())) {
                        if (!player.getBukkitPlayer().isOp() && player.getBukkitPlayer().getGameMode() == GameMode.SURVIVAL) {

                            // Существует странный эксплойт с телепортацией в транспорт, не знаю что это, но это легко пофиксить
                            if (player.getBukkitPlayer().getVehicle() != null) {
                                player.getBukkitPlayer().getVehicle().remove();
                            }

                            player.getBukkitPlayer().teleport(player.getTeam().getRandomRespawnLocation());
                            player.getBukkitPlayer().playSound(player.getBukkitPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                        }
                    }
                }
            }
        };

        cowardTracker.runTaskTimer(Minespades.getPlugin(Minespades.class), 0, 20);
    }

    @Getter
    public enum Preference {

        FORCE_AUTO_ASSIGN(PreferenceValue.Type.BOOLEAN, true),
        FRIENDLY_FIRE(PreferenceValue.Type.BOOLEAN, false),
        KEEP_INVENTORY(PreferenceValue.Type.BOOLEAN, true),
        REMOVE_EMPTY_BOTTLES(PreferenceValue.Type.BOOLEAN, true),
        COLORFUL_ENDING(PreferenceValue.Type.BOOLEAN, true),
        FLAG_PARTICLES(PreferenceValue.Type.BOOLEAN, true),
        DISABLE_PORTALS(PreferenceValue.Type.BOOLEAN, true),
        NO_DAMAGE_COOLDOWN(PreferenceValue.Type.BOOLEAN, true),
        PREVENT_ITEM_DAMAGE(PreferenceValue.Type.BOOLEAN, true),
        BLOCK_LAVA_USAGE(PreferenceValue.Type.BOOLEAN, true),
        PROTECT_RESPAWN(PreferenceValue.Type.BOOLEAN, true),
        DENY_BED_SLEEP(PreferenceValue.Type.BOOLEAN, true),
        PUNISH_COWARDS(PreferenceValue.Type.BOOLEAN, true),
        IS_MULTIGROUND(PreferenceValue.Type.BOOLEAN, false),
        FORCE_AUTOJOIN(PreferenceValue.Type.BOOLEAN, false),
        FLAG_CARRIER_GLOW(PreferenceValue.Type.BOOLEAN, true),
        TEAM_WIN_SCORE(PreferenceValue.Type.INT, 10),
        FLAG_LIFEPOOL_PENALTY(PreferenceValue.Type.INT, 15);

        private final PreferenceValue.Type type;
        private final PreferenceValue defaultValue;

        Preference(PreferenceValue.Type type, final Object defaultValue) {
            this.defaultValue = new PreferenceValue(String.valueOf(defaultValue));
            this.type = type;
        }

        public static boolean isValid(String preference) {
            try {
                valueOf(preference);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        public record PreferenceValue(String value) {

            public boolean getAsBoolean() {
                return Boolean.parseBoolean(value);
            }

            public int getAsInteger() {
                return Integer.parseInt(value);
            }

            public String getAsString() {
                return value;
            }

            public enum Type {

                INT, BOOLEAN, STRING;

            }

        }



    }


}