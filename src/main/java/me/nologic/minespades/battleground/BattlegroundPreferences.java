package me.nologic.minespades.battleground;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.xephi.authme.events.LoginEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.util.BattlegroundDataDriver;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.HashMap;

@RequiredArgsConstructor
public class BattlegroundPreferences implements Listener {

    private final Battleground battleground;
    private final HashMap<Preference, Boolean> preferences = new HashMap<>();

    // init() применяется при инициализации, set() через команду /ms config
    @SneakyThrows
    public void set(Preference preference, boolean state) {
        BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(battleground);
        this.preferences.put(preference, state);
        // Пишем изменение в дб
        try (ResultSet result = driver.executeQuery("SELECT parameters FROM preferences;")) {
            result.next();
            JsonObject parameters = JsonParser.parseString(result.getString("parameters")).getAsJsonObject();
            parameters.remove(preference.toString());
            parameters.addProperty(preference.toString(), state);
            driver.executeUpdate("UPDATE preferences SET parameters = ?;", parameters.toString());
            driver.closeConnection();
        }
    }

    public void init(Preference preference, boolean state) {
        this.preferences.put(preference, state);
    }

    @NotNull /* Возвращает либо значение из карты preferences, либо дефолтное. */
    public Boolean get(Preference preference) {
        Boolean result = this.preferences.get(preference);
        return result != null ? result : preference.getDefaultValue();
    }

    /**
     * Статический метод, с ним можно довольно легко инициализировать настройки любой арены.
     * Автоматически добавляет в датабазу новые Preference, которые ранее отсутствовали.
     *  */
    @SneakyThrows
    public static BattlegroundPreferences loadPreferences(Battleground battleground) {
        BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(battleground);
        BattlegroundPreferences bp = new BattlegroundPreferences(battleground);
        try (ResultSet result = driver.executeQuery("SELECT * FROM preferences;")) {
            result.next();
            battleground.setWorld(Bukkit.getWorld(result.getString("world")));

            JsonObject parameters = JsonParser.parseString(result.getString("parameters")).getAsJsonObject();
            // Preference является енумом буликов (лол). Если в загруженной json-строке не найдено искомое значение,
            // то возьмётся дефолтное значение (preference.getDefaultValue()), так же оно добавится в датабазу.
            for (Preference preference : Preference.values()) {
                if (parameters.get(preference.toString()) != null) {
                    bp.init(preference, parameters.get(preference.toString()).getAsBoolean());
                } else {
                    bp.init(preference, preference.getDefaultValue());
                    parameters.addProperty(preference.toString(), preference.getDefaultValue());
                }
            }
            driver.executeUpdate("UPDATE preferences SET parameters = ?;", parameters.toString());
        }
        driver.closeConnection();
        return bp;
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

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(Minespades.getInstance(), () -> {
            // Подключаем игрока к арене через 1 тик после логина, дабы избежать багов
            if (preferences.get(Preference.FORCE_AUTOJOIN) && !Bukkit.getPluginManager().isPluginEnabled("AuthMe")) {
                Minespades.getInstance().getGameMaster().getPlayerManager().connect(event.getPlayer(), battleground, battleground.getSmallestTeam());
                event.getPlayer().sendMessage("§7Вы были автоматически подключены к арене. Чтобы покинуть арену, напишите §3/ms q§7.");
            }
        }, 1);
    }

    // Подержка AuthMe
    @EventHandler
    private void onPlayerLogin(LoginEvent event) {
        // Подключаем игрока к арене через 1 тик после логина, дабы избежать багов
        Bukkit.getScheduler().runTaskLater(Minespades.getInstance(), () -> {
            if (preferences.get(Preference.FORCE_AUTOJOIN)) {
                Minespades.getInstance().getGameMaster().getPlayerManager().connect(event.getPlayer(), battleground, battleground.getSmallestTeam());
                event.getPlayer().sendMessage("§7Вы были автоматически подключены к арене. Чтобы покинуть арену, напишите §3/ms q§7.");
            }
        }, 1);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (preferences.get(Preference.NO_DAMAGE_COOLDOWN)) {
            if (event.getEntity() instanceof Player player && BattlegroundPlayer.getBattlegroundPlayer(player) != null) {
                player.setNoDamageTicks(0);
            }
        }
    }

    @EventHandler
    public void onShieldUsage(PlayerInteractEvent event) {
        if (preferences.get(Preference.NO_SHIELD_DELAY) && event.getMaterial().equals(Material.SHIELD)) {
            event.getPlayer().setShieldBlockingDelay(0);
        }
    }

    {
        BukkitRunnable cowardTracker = new BukkitRunnable() {

            @Override
            public void run() {
                if (!preferences.get(Preference.PUNISH_COWARDS)) return;
                for (BattlegroundPlayer player : battleground.getPlayers()) {
                    if (!battleground.getBoundingBox().contains(player.getBukkitPlayer().getLocation().toVector())) {
                        if (!player.getBukkitPlayer().isOp() && player.getBukkitPlayer().getGameMode() == GameMode.SURVIVAL) {

                            // Существует странный эксплойт с телепортацией в транспорт, не знаю что это, но это легко пофиксить
                            if (player.getBukkitPlayer().getVehicle() != null) {
                                player.getBukkitPlayer().getVehicle().remove();
                            }

                            player.getBukkitPlayer().teleport(player.getTeam().getRandomRespawnLocation());
                            player.getBukkitPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 1));
                            player.getBukkitPlayer().playSound(player.getBukkitPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                        }
                    }
                }
            }
        };

        cowardTracker.runTaskTimer(Minespades.getPlugin(Minespades.class), 0, 20);
    }

    public enum Preference {

        FORCE_AUTO_ASSIGN(true),
        FRIENDLY_FIRE(false),
        KEEP_INVENTORY(true),
        DELETE_EMPTY_BOTTLES(true),
        COLORFUL_ENDING(true),
        FLAG_PARTICLES(true),
        FLAG_STEALER_TRAILS(true), // TODO: не реализовано
        DISABLE_PORTALS(true),
        NO_DAMAGE_COOLDOWN(true),
        NO_SHIELD_DELAY(true),
        PREVENT_ITEM_DAMAGE(true),
        BLOCK_LAVA_USAGE(true),
        PROTECT_RESPAWN(true),
        DENY_BED_SLEEP(true),
        PUNISH_COWARDS(true),
        IS_MULTIGROUND(false),
        FORCE_AUTOJOIN(false);

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