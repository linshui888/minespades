package me.nologic.minespades.game.flag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.battleground.BattlegroundTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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

    @Setter
    private BattlegroundTeam team;
    private final Location   base;
    private final ItemStack  flag;

    @Getter @Setter
    private BukkitRunnable   tick;

    private BattlegroundPlayer carrier;
    private Location           position;
    private BoundingBox        box;

    {
        Bukkit.getPluginManager().registerEvents(this, Minespades.getPlugin(Minespades.class));
        this.tick = new BukkitRunnable() {

            // Каждые 5 тиков внутри BoundingBox'а проверяются энтити.
            // Если энтитя == игрок, скорборды совпадают, но разные команды, то pickup
            @Override
            public void run() {
                if (box != null) {
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
    }

    /**
     * Когда игрок входит в box, должен вызываться этот метод.
     */
    private void pickup(BattlegroundPlayer carrier) {
        this.carrier = carrier;
        carrier.setFlag(this);
        carrier.setCarryingFlag(true);
        Player player = carrier.getPlayer();
        ItemStack flag = position.getBlock().getState().getDrops().stream().findFirst().orElseThrow();
        player.getInventory().setHelmet(flag);
        battleground.broadcast(Component.text(String.format("%s крадёт флаг команды %s!", player.getName(), team.getName())).color(TextColor.color(255, 255, 255)));
        position.getBlock().setType(Material.AIR);
        position = null;
        box = null;
    }

    // TODO: дроп флага в воздухе и в лаве должен обрабатываться отдельно
    /**
     * Когда вор флага умирает, должен вызываться этот метод.
     */
    public void drop() {

        Player player = carrier.getPlayer();
        if (Objects.equals(Objects.requireNonNull(player.getLastDamageCause()).getCause(), EntityDamageEvent.DamageCause.LAVA)) {
            this.reset();
            return;
        }

        carrier.setFlag(null);
        carrier.setCarryingFlag(false);

        position = player.getLocation().getBlock().getLocation();
        this.updateBoundingBox();

        player.getInventory().setHelmet(new ItemStack(Material.AIR));
        carrier = null;

        battleground.broadcast(Component.text(String.format("%s теряет флаг команды %s!", player.getName(), team.getName())).color(TextColor.color(255, 255, 255)));
        this.validateBannerData();
    }

    /**
     * Возвращение флага к изначальному состоянию.
     */
    public void reset() {
        position = base;
        if (carrier != null) {
            carrier.getPlayer().getInventory().setHelmet(new ItemStack(Material.AIR));
            carrier.setFlag(null);
            carrier.setCarryingFlag(false);
            carrier = null;
        }
        updateBoundingBox();
        validateBannerData();
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

}