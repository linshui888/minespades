package me.nologic.minespades.game.object;

import lombok.Getter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class NeutralBattlegroundFlag extends BattlegroundFlag implements Listener {

    private boolean particle = false;
    private BukkitRunnable flagRecoveryTimer;

    @Getter
    private BossBar recoveryBossBar;

    public NeutralBattlegroundFlag(final Battleground battleground, final Location base, final ItemStack flag) {
        super(battleground, base, flag);

        Bukkit.getPluginManager().registerEvents(this, Minespades.getPlugin(Minespades.class));

        this.tick = new BukkitRunnable() {

            // Каждые 5 тиков внутри BoundingBox'а проверяются энтити.
            // Если энтитя == игрок, скорборды совпадают, но разные команды, то pickup
            @Override
            public void run() {
                if (currentPosition != null && boundingBox != null && particle) {
                    battleground.getWorld().spawnParticle(Particle.SMALL_FLAME, currentPosition.clone().add(0.5, 0.5, 0.5), 9, 0.5, 1, 0.5);
                    for (Entity entity : battleground.getWorld().getNearbyEntities(boundingBox)) {
                        if (entity instanceof Player player) {
                            if (battleground.getScoreboard().equals(player.getScoreboard())) {
                                if (!BattlegroundPlayer.getBattlegroundPlayer(player).isCarryingFlag()) {
                                    if (player.getGameMode().equals(GameMode.SURVIVAL)) {
                                        pickup(BattlegroundPlayer.getBattlegroundPlayer(player));
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }

        };

        tick.runTaskTimer(Minespades.getPlugin(Minespades.class), 0, 5);
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {

        // Флаг должен быть неразрушаемым
        if (Objects.equals(event.getBlock().getLocation(), currentPosition)) {
            event.setCancelled(true);
        }

        // Блок под флагом тоже должен быть неразрушаемым
        if (currentPosition != null) {
            if (Objects.equals(event.getBlock().getLocation(), currentPosition.getBlock().getRelative(BlockFace.DOWN).getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Когда игрок входит в box, должен вызываться этот метод.
     */
    @Override
    protected void pickup(final BattlegroundPlayer carrier) {
        this.carrier = carrier;

        carrier.setFlag(this);
        carrier.setCarryingFlag(true);
        carrier.getBukkitPlayer().setGlowing(true);

        Player player = carrier.getBukkitPlayer();
        player.getInventory().setHelmet(flagItem);

        if (currentPosition != null) {
            currentPosition.getBlock().setType(Material.AIR);
            currentPosition = null;
        }

        boundingBox = null;
        if (flagRecoveryTimer != null) {
            flagRecoveryTimer.cancel();

            // Не забываем скрывать таймер, если флаг был поднят
            Bukkit.getScheduler().runTask(Minespades.getPlugin(Minespades.class), () -> {
                Bukkit.getOnlinePlayers().forEach(p -> Minespades.getInstance().getAdventureAPI().player(p).hideBossBar(recoveryBossBar));
                recoveryBossBar = null;
            });
            flagRecoveryTimer = null;
        }
    }

    // TODO: дроп флага в воздухе и в лаве должен обрабатываться отдельно
    /**
     * Когда вор флага умирает, должен вызываться этот метод.
     */
    public void drop() {

        if (carrier == null)
            return;

        Player player = carrier.getBukkitPlayer();
        if (player.getLastDamageCause() != null && Objects.equals(player.getLastDamageCause().getCause(), EntityDamageEvent.DamageCause.LAVA) || player.getLocation().getBlock().getType() == Material.LAVA) {
            this.reset();
            return;
        }

        player.setGlowing(false);
        carrier.setFlag(null);
        carrier.setCarryingFlag(false);

        currentPosition = player.getLocation().getBlock().getLocation();
        this.updateBoundingBox();

        // FIXME: Необходимо сохранять предыдущий шлем игрока, дабы он не исчезал, как слёзы во время дождя. (што)
        player.getInventory().setHelmet(new ItemStack(Material.AIR));
        carrier = null;

        this.validateBannerData();

        // Запускаем таймер, который отсчитывает время до ресета флага. Если флаг лежит на земле слишком долго, целесообразно восстановить его изначальную позицию.
        this.flagRecoveryTimer = new BukkitRunnable() {

            final int timeToReset = 45;
            int timer = timeToReset * 20;

            final BossBar bossBar = BossBar.bossBar(Component.text(String.format("Нейтральный флаг пропадёт через §e%sс§f..", timer / 20)), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_20);

            @Override
            public void run() {
                NeutralBattlegroundFlag.this.recoveryBossBar = bossBar;
                timer = timer - 20;
                bossBar.name(Component.text(String.format("Нейтральный флаг пропадёт через §e%sс§f..", timer / 20)));

                if (timer != 0) {
                    bossBar.progress(bossBar.progress() - 1.0f / timeToReset);
                }

                if (timer <= 100 && timer != 0) {
                    for (BattlegroundPlayer bgPlayer : battleground.getPlayers()) {
                        bgPlayer.getBukkitPlayer().playSound(bgPlayer.getBukkitPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2 / (timer / 10f));
                    }
                }

                if (timer == 0) {

                    // Попытаемся сделать всё красиво, нам нужно заполнить боссбар, изменить сообщение на ФЛАГ ВОССТАНОВЛЕН! и сменить цвет
                    bossBar.name(Component.text("Флаг восстановлен!").decorate(TextDecoration.BOLD));
                    bossBar.color(BossBar.Color.RED);
                    bossBar.progress(0);

                    NeutralBattlegroundFlag.this.reset();

                    BukkitRunnable smoothFillerTask = new BukkitRunnable() {

                        @Override
                        public void run() {
                            // Число, на которое увеличивается прогресс боссбара каждый тик
                            float number = 0.03f;
                            if (bossBar.progress() + number < 1.0f) {
                                bossBar.progress(bossBar.progress() + number);
                            } else  {
                                // Скрываем боссбар через полторы секунды после заполнения
                                bossBar.progress(1.0f);
                                bossBar.color(BossBar.Color.GREEN);
                                Bukkit.getScheduler().runTaskLater(Minespades.getPlugin(Minespades.class), () -> Bukkit.getOnlinePlayers().forEach(p -> Minespades.getInstance().getAdventureAPI().player(p).hideBossBar(bossBar)), 30);
                                this.cancel();
                            }
                        }

                    };

                    smoothFillerTask.runTaskTimer(Minespades.getPlugin(Minespades.class), 0, 1);
                    this.cancel();
                }

                for (BattlegroundPlayer bgPlayer : battleground.getPlayers()) {
                    Player player = bgPlayer.getBukkitPlayer();
                    Minespades.getInstance().getAdventureAPI().player(player).showBossBar(bossBar);
                }
            }

        };

        flagRecoveryTimer.runTaskTimer(Minespades.getPlugin(Minespades.class), 0, 20);
    }

    /**
     * Возвращение флага к изначальному состоянию.
     */
    public void reset() {
        if (currentPosition != null) currentPosition.getBlock().setType(Material.AIR);
        currentPosition = basePosition.clone();
        if (carrier != null) {
            carrier.getBukkitPlayer().getInventory().setHelmet(new ItemStack(Material.AIR));
            carrier.setFlag(null);
            carrier.setCarryingFlag(false);
            carrier = null;
        }
        updateBoundingBox();
        validateBannerData();
        particle = true;

        if (flagRecoveryTimer != null) {
            flagRecoveryTimer.cancel();
            recoveryBossBar = null;
        }
    }

}