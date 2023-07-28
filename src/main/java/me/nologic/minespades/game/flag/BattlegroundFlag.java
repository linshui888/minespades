package me.nologic.minespades.game.flag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.battleground.BattlegroundTeam;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@RequiredArgsConstructor
public class BattlegroundFlag implements Listener {

    private final Battleground battleground;

    @Setter @Getter
    private BattlegroundTeam team;
    private final Location   base;
    private final ItemStack  flag;

    @Getter @Setter
    private BukkitRunnable     tick;

    @Getter
    private BattlegroundPlayer carrier;

    @Getter @Nullable
    private Location           position;
    private BoundingBox        box;

    private boolean particle = false;
    private BukkitRunnable flagRecoveryTimer;

    @Getter
    private BossBar recoveryBossBar;
    private Particle.DustOptions dust;

    {
        Bukkit.getPluginManager().registerEvents(this, Minespades.getPlugin(Minespades.class));
        this.tick = new BukkitRunnable() {

            // Каждые 5 тиков внутри BoundingBox'а проверяются энтити.
            // Если энтитя == игрок, скорборды совпадают, но разные команды, то pickup
            @Override
            public void run() {
                if (box != null && particle) {

                    if (team != null && dust == null) {
                        dust = new Particle.DustOptions(Color.fromRGB(team.getColor().red(), team.getColor().green(), team.getColor().blue()), 1.1F);
                    }

                    battleground.getWorld().spawnParticle(Particle.REDSTONE, position.add(0.5, 0.5, 0.5), 7, 0.5, 1, 0.5, dust);
                    for (Entity entity : battleground.getWorld().getNearbyEntities(box)) {
                        if (entity instanceof Player player) {
                            if (battleground.getScoreboard().equals(player.getScoreboard())) {
                                if (!Objects.equals(player.getScoreboard().getPlayerTeam(player), team.getBukkitTeam())) {
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

        // TODO: нормальные проверки
        if (Objects.equals(event.getBlock().getLocation(), position)) {
            event.setCancelled(true);
        }

        // Блок под флагом тоже должен быть неразрушаемым
        if (position != null) {
            if (Objects.equals(event.getBlock().getLocation(), position.getBlock().getRelative(BlockFace.DOWN).getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Когда игрок входит в box, должен вызываться этот метод.
     */
    private void pickup(BattlegroundPlayer carrier) {
        this.carrier = carrier;

        // TODO: Это шутка. Да? Нет?
        carrier.getBukkitPlayer().sendMessage("§6[Подсказка] §7Ты взял вражеский флаг! Теперь принеси его на свою респу, чтобы получить очки!");

        carrier.setFlag(this);
        carrier.setCarryingFlag(true);
        carrier.getBukkitPlayer().setGlowing(true);
        Player player = carrier.getBukkitPlayer();
        player.getInventory().setHelmet(flag);

        TextComponent stealMessage = Component.text(player.getName()).color(carrier.getTeam().getColor())
                .append(Component.text(" крадёт флаг команды ").color(NamedTextColor.WHITE))
                .append(Component.text(team.getName()).color(team.getColor())).append(Component.text("!").color(NamedTextColor.WHITE));

        battleground.broadcast(stealMessage);

        position.getBlock().setType(Material.AIR);
        position = null;
        box = null;
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

        // TODO: сообщения нужно куда-то убрать, код станет гораздо чище
        TextComponent flagDropMessage = Component.text(carrier.getBukkitPlayer().getName()).color(carrier.getTeam().getColor())
                .append(Component.text(" теряет флаг команды ").color(NamedTextColor.WHITE))
                .append(Component.text(team.getName()).color(team.getColor())).append(Component.text("!").color(NamedTextColor.WHITE));

        battleground.broadcast(flagDropMessage);

        Player player = carrier.getBukkitPlayer();
        if (player.getLastDamageCause() != null && Objects.equals(player.getLastDamageCause().getCause(), EntityDamageEvent.DamageCause.LAVA) || player.getLocation().getBlock().getType() == Material.LAVA) {
            this.reset();
            return;
        }

        player.setGlowing(false);
        carrier.setFlag(null);
        carrier.setCarryingFlag(false);

        position = player.getLocation().getBlock().getLocation();
        this.updateBoundingBox();

        // FIXME: Необходимо сохранять предыдущий шлем игрока, дабы он не исчезал, как слёзы во время дождя. (што)
        player.getInventory().setHelmet(new ItemStack(Material.AIR));
        carrier = null;

        this.validateBannerData();

        // Запускаем таймер, который отсчитывает время до ресета флага. Если флаг лежит на земле слишком долго, целесообразно восстановить его изначальную позицию.
        this.flagRecoveryTimer = new BukkitRunnable() {

            final int timeToReset = 45;
            int timer = timeToReset * 20;

            final BossBar bossBar = BossBar.bossBar(Component.text("Флаг ").append(team.getDisplayName()).append(Component.text(String.format(" пропадёт через §e%sс§f..", timer / 20))), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_20)
                    .addFlag(BossBar.Flag.CREATE_WORLD_FOG);

            @Override
            public void run() {
                BattlegroundFlag.this.recoveryBossBar = bossBar;
                timer = timer - 20;
                bossBar.name(Component.text("Флаг ").append(team.getDisplayName()).append(Component.text(String.format(" пропадёт через §e%sс§f..", timer / 20))));

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

                    BattlegroundFlag.this.reset();

                    BukkitRunnable smoothFillerTask = new BukkitRunnable() {

                        // Число, на которое увеличивается прогресс боссбара каждый тик
                        private final float number = 0.03f;

                        @Override
                        public void run() {
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
        if (position != null) position.getBlock().setType(Material.AIR);
        position = base;
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

    /**
     * Обновление BoundingBox, должно вызываться только после обновления позиции.
     */
    private void updateBoundingBox() {
        this.box = BoundingBox.of(position.getBlock(), position.getBlock().getRelative(BlockFace.UP));
    }

    /**
     * Применяет к флагу сохранённый цвет и паттерны. Имеет смысл вызывать только после смены позиции.
     */
    private void validateBannerData() {
        if (Minespades.getInstance().isEnabled()) {

            if (position == null) return;

            Bukkit.getScheduler().runTask(Minespades.getInstance(), () -> {
                position.getBlock().setType(flag.getType());
                BannerMeta meta = (BannerMeta) flag.getItemMeta();
                Banner banner = (Banner) position.getBlock().getState();
                banner.setPatterns(meta.getPatterns());
                banner.update();
            });
        }
    }

}