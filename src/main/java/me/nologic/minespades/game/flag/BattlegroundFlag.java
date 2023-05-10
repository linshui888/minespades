package me.nologic.minespades.game.flag;

import com.destroystokyo.paper.ParticleBuilder;
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
    private BattlegroundPlayer carrier;

    @Getter
    private Location           position;
    private BoundingBox        box;

    private ParticleBuilder particle;
    private BukkitRunnable flagRecoveryTimer;

    {
        Bukkit.getPluginManager().registerEvents(this, Minespades.getPlugin(Minespades.class));
        this.tick = new BukkitRunnable() {

            // Каждые 5 тиков внутри BoundingBox'а проверяются энтити.
            // Если энтитя == игрок, скорборды совпадают, но разные команды, то pickup
            @Override
            public void run() {
                if (box != null) {
                    particle.spawn();
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
        carrier.setFlag(this);
        carrier.setCarryingFlag(true);
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

        carrier.setFlag(null);
        carrier.setCarryingFlag(false);

        position = player.getLocation().getBlock().getLocation();
        particle.location(position.toCenterLocation());
        this.updateBoundingBox();

        // FIXME: Необходимо сохранять предыдущий шлем игрока, дабы он не исчезал, как слёзы во время дождя. (што)
        player.getInventory().setHelmet(new ItemStack(Material.AIR));
        carrier = null;

        this.validateBannerData();

        // Запускаем таймер, который отсчитывает время до ресета флага. Если флаг лежит на земле слишком долго, целесообразно восстановить его изначальную позицию.
        this.flagRecoveryTimer = new BukkitRunnable() {

            final int timeToReset = 45;
            int timer = timeToReset * 20;

            final BossBar bossBar = BossBar.bossBar(Component.text(String.format("Флаг исчезнет через %s...", timer / 20)), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_20)
                    .addFlag(BossBar.Flag.CREATE_WORLD_FOG);

            @Override
            public void run() {
                timer = timer - 20;
                bossBar.name(Component.text(String.format("Флаг исчезнет через §e%s§fс...", timer / 20)));

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
                        private final float number = 0.02f;

                        @Override
                        public void run() {
                            if (bossBar.progress() + number < 1.0f) {
                                bossBar.progress(bossBar.progress() + number);
                            } else  {
                                // Скрываем боссбар через полторы секунды после заполнения
                                bossBar.progress(1.0f);
                                bossBar.color(BossBar.Color.GREEN);
                                Bukkit.getScheduler().runTaskLater(Minespades.getPlugin(Minespades.class), () -> Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bossBar)), 30);
                                this.cancel();
                            }
                        }

                    };

                    smoothFillerTask.runTaskTimer(Minespades.getPlugin(Minespades.class), 0, 1);
                    this.cancel();
                }

                for (BattlegroundPlayer bgPlayer : battleground.getPlayers()) {
                    Player player = bgPlayer.getBukkitPlayer();
                    player.showBossBar(bossBar);
                }
            }

        };

        flagRecoveryTimer.runTaskTimer(Minespades.getPlugin(Minespades.class), 0, 20);
    }

    /**
     * Возвращение флага к изначальному состоянию.
     */
    public void reset() {
        position.getBlock().setType(Material.AIR);
        position = base;
        if (carrier != null) {
            carrier.getBukkitPlayer().getInventory().setHelmet(new ItemStack(Material.AIR));
            carrier.setFlag(null);
            carrier.setCarryingFlag(false);
            carrier = null;
        }
        updateBoundingBox();
        validateBannerData();
        prepareFlagParticle();

        if (flagRecoveryTimer != null) {
            flagRecoveryTimer.cancel();
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
        position.getBlock().setType(flag.getType());
        BannerMeta meta = (BannerMeta) flag.getItemMeta();
        Banner banner = (Banner) position.getBlock().getState();
        banner.setPatterns(meta.getPatterns());
        banner.update();
    }

    private void prepareFlagParticle() {
        particle = new ParticleBuilder(Particle.REDSTONE);
        particle.color(team.getColor().red(), team.getColor().green(), team.getColor().blue());
        particle.location(position.toCenterLocation()).allPlayers().offset(0.5, 1, 0.5).count(7);
    }

}