package me.nologic.minespades.game.object;

import lombok.Getter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.battleground.BattlegroundPreferences;
import me.nologic.minespades.battleground.BattlegroundTeam;
import me.nologic.minespades.game.object.base.BattlegroundFlag;
import me.nologic.minespades.util.BossBar;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Translatable;
import me.nologic.minority.annotations.TranslationKey;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

@Translatable
public class TeamBattlegroundFlag extends BattlegroundFlag implements MinorityFeature {

    @Getter
    private final BattlegroundTeam team;

    private BukkitRunnable flagRecoveryTimer;

    @Getter
    private BossBar recoveryBossBar;

    public TeamBattlegroundFlag(final Battleground battleground, final BattlegroundTeam team, final Location base, final ItemStack flagBannerItem) {
        super(battleground, base, flagBannerItem, new Particle.DustOptions(Color.fromRGB(team.getColor().getColor().getRed(), team.getColor().getColor().getGreen(), team.getColor().getColor().getBlue()), 1.2F)); // todo particles from config
        this.init(this, this.getClass(), Minespades.getInstance());
        this.team = team;
        Bukkit.getServer().getScheduler().runTaskTimer(plugin, (task) -> {
            if (!battleground.isEnabled()) task.cancel();
            this.tick(this);
        }, 0, 5L);
    }

    /* A team flag may be carried only by a player from opposite team. */
    @Override
    protected void tick(final BattlegroundFlag flag) {
        if (flag.isOnGround()) {
            flag.playParticles();
            flag.getCollidingPlayers().stream().filter(player -> !player.isCarryingFlag() && player.getTeam() != this.team).findFirst().ifPresent(flag::pickup);
        }
    }

    /**
     * Когда игрок входит в box, должен вызываться этот метод.
     */
    @Override
    public void pickup(final BattlegroundPlayer carrier) {
        this.carrier = carrier;

        super.playFlagEquipSound();

        carrier.setFlag(this);
        carrier.setCarryingFlag(true);
        battleground.broadcast(String.format(teamFlagStolenMessage, carrier.getDisplayName(), this.team.getDisplayName()));

        if (battleground.getPreferences().get(BattlegroundPreferences.Preference.FLAG_CARRIER_GLOW)) {
            carrier.getBukkitPlayer().setGlowing(true);
        }

        Player player = carrier.getBukkitPlayer();
        player.getInventory().setHelmet(flagBannerItem);

        if (currentPosition != null) {
            currentPosition.getBlock().setType(Material.AIR);
            currentPosition = null;
        }

        boundingBox = null;
        if (flagRecoveryTimer != null) {
            flagRecoveryTimer.cancel();

            // Не забываем скрывать таймер, если флаг был поднят
            Bukkit.getScheduler().runTask(Minespades.getPlugin(Minespades.class), () -> {
                recoveryBossBar.cleanViewers().visible(false);
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

        if (battleground.getPreferences().get(BattlegroundPreferences.Preference.FLAG_CARRIER_GLOW)) {
            carrier.getBukkitPlayer().setGlowing(false);
        }

        Player player = carrier.getBukkitPlayer();
        if (player.getLastDamageCause() != null && Objects.equals(player.getLastDamageCause().getCause(), EntityDamageEvent.DamageCause.LAVA) || player.getLocation().getBlock().getType() == Material.LAVA) {
            this.reset();
            return;
        }

        carrier.setFlag(null);
        carrier.setCarryingFlag(false);
        battleground.broadcast(String.format(teamFlagDropMessage, carrier.getDisplayName(), this.team.getDisplayName()));

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

            final BossBar bossBar = BossBar.bossBar(String.format(restorationCount, team.getDisplayName(), timer / 20), 1.0f, BarColor.BLUE, BarStyle.SEGMENTED_20);

            @Override
            public void run() {
                TeamBattlegroundFlag.this.recoveryBossBar = bossBar;
                timer = timer - 20;
                bossBar.title(String.format(restorationCount, team.getDisplayName(), timer / 20));

                if (timer != 0) {
                    bossBar.progress(bossBar.progress() - 1.0f / timeToReset);
                }

                if (timer <= 100 && timer != 0) {
                    for (BattlegroundPlayer bgPlayer : battleground.getBattlegroundPlayers()) {
                        bgPlayer.getBukkitPlayer().playSound(bgPlayer.getBukkitPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2 / (timer / 10f));
                    }
                }

                if (timer == 0) {

                    // Попытаемся сделать всё красиво, нам нужно заполнить боссбар, изменить сообщение на ФЛАГ ВОССТАНОВЛЕН! и сменить цвет
                    bossBar.title(flagRestoredTitle);
                    bossBar.color(BarColor.RED);
                    bossBar.progress(0);

                    TeamBattlegroundFlag.this.reset();

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
                                bossBar.color(BarColor.GREEN);
                                Bukkit.getScheduler().runTaskLater(Minespades.getPlugin(Minespades.class), () -> bossBar.cleanViewers().visible(false), 30);
                                this.cancel();
                            }
                        }

                    };

                    smoothFillerTask.runTaskTimer(Minespades.getPlugin(Minespades.class), 0, 1);
                    this.cancel();
                }


                battleground.getBattlegroundPlayers().forEach(battlegroundPlayer -> bossBar.addViewer(battlegroundPlayer.getBukkitPlayer()));
                bossBar.visible(true);
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

        if (flagRecoveryTimer != null) {
            flagRecoveryTimer.cancel();
            recoveryBossBar = null;
        }
    }

    @TranslationKey(section = "regular-messages", name = "player-stole-team-flag", value = "%s &rstole the flag of team %s&r!")
    private String teamFlagStolenMessage;

    @TranslationKey(section = "regular-messages", name = "player-drop-team-flag", value = "%s &rdrop the flag of team %s&r!")
    private String teamFlagDropMessage;

    @TranslationKey(section = "regular-messages", name = "team-flag-restoration-counter", value = "Team %s &rflag will be restored in &e%ss&r!..")
    private String restorationCount;

    @TranslationKey(section = "regular-messages", name = "flag-is-restored", value = "&lFlag has restored!")
    private String flagRestoredTitle;

}