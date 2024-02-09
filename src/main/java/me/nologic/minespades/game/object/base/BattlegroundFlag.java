package me.nologic.minespades.game.object.base;

import lombok.Getter;
import lombok.Setter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Configurable;
import me.nologic.minority.annotations.ConfigurationKey;
import me.nologic.minority.annotations.Type;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter @Configurable(path = "game-settings")
public abstract class BattlegroundFlag implements MinorityFeature {

    protected final static Minespades plugin = Minespades.getInstance();

    /** Battleground, on which an instance of this flag is on. */
    protected final @NotNull Battleground battleground;

    /**
     * The starting position of the flag.
     * <p>The flag appears on it after initialization, when stolen, or after position reset.</p>
     * */
    protected final @NotNull Location basePosition;

    /** Banner representation as an ItemStack. */
    protected final @NotNull ItemStack flagBannerItem;

    /** Flag particles that appear every tick of flag behavior. */
    protected final @NotNull Particle.DustOptions particleOptions;

    /** Current location of the flag. If null, it means that the flag is currently carried by some player. */
    protected @Nullable Location currentPosition;

    /** Flag thief. If null, the flag is currently on the ground. */
    protected @Nullable @Setter BattlegroundPlayer carrier;

    /** BoundingBox is used for collision detection.
     * <p>If null, it means that the flag is currently being carried by some player. */
    protected @Nullable BoundingBox boundingBox;

    /** The implementation of this tick determines the interaction between the flag and the players. */
    protected BukkitRunnable behaviorTick;

    @ConfigurationKey(name = "flag-equip-sound", type = Type.ENUM, value = "ITEM_ARMOR_EQUIP_LEATHER", comment = "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html")
    private Sound flagEquipSound;

    public BattlegroundFlag(@NotNull Battleground battleground, @NotNull final Location basePosition, @NotNull final ItemStack flagBannerItem, @NotNull final Particle.DustOptions particleOptions) {
        plugin.getGameMaster().getObjectManager().getFlags().add(this);
        this.battleground    = battleground;
        this.basePosition    = basePosition;
        this.flagBannerItem  = flagBannerItem;
        this.currentPosition = basePosition;
        this.particleOptions = particleOptions;
    }

    protected abstract void tick(BattlegroundFlag flag);

    public abstract void pickup(BattlegroundPlayer carrier);
    public abstract void drop();
    public abstract void reset();

    /**
     * Apply pattern meta to the banner block.
     */
    protected void validateBannerData() {
        if (Minespades.getInstance().isEnabled()) {

            if (currentPosition == null) return;

            Bukkit.getScheduler().runTask(Minespades.getInstance(), () -> {
                currentPosition.getBlock().setType(flagBannerItem.getType());
                BannerMeta meta = (BannerMeta) flagBannerItem.getItemMeta();
                Banner banner = (Banner) currentPosition.getBlock().getState();
                if (meta != null) {
                    banner.setPatterns(meta.getPatterns());
                    banner.update();
                }
            });
        }
    }

    protected void updateBoundingBox() {
        if (currentPosition != null) {
            this.boundingBox = BoundingBox.of(currentPosition.getBlock(), currentPosition.getBlock().getRelative(BlockFace.UP));
        }
    }

    protected void playFlagEquipSound() {
        if (currentPosition != null) {
            battleground.getWorld().playSound(currentPosition, (this.flagEquipSound == null ? (this.flagEquipSound = Sound.valueOf(Minespades.getInstance().getConfig().getString("game-settings.flag-equip-sound"))) : this.flagEquipSound), 1.0F, 1.1F);
        }
    }

    public boolean isOnGround() {
        return currentPosition != null && boundingBox != null;
    }

    public List<@NotNull BattlegroundPlayer> getCollidingPlayers() throws IllegalStateException {

        if (boundingBox == null || currentPosition == null || carrier != null)
            throw new IllegalStateException("Trying to get the colliding players when the flag is not on the ground!");

        final List<BattlegroundPlayer> players = new ArrayList<>();
        for (Entity entity : battleground.getWorld().getNearbyEntities(boundingBox)) {
            if (entity instanceof Player player) {
                final BattlegroundPlayer battlegroundPlayer = BattlegroundPlayer.getBattlegroundPlayer(player);
                if (battlegroundPlayer != null) players.add(battlegroundPlayer);
            }
        }

        return players;
    }

    public void playParticles() {

        if (boundingBox == null || currentPosition == null || carrier != null)
            throw new IllegalStateException("Trying to play flag particles when the flag is not on the ground!");

        battleground.getWorld().spawnParticle(Particle.REDSTONE, currentPosition.clone().add(0.5, 0.5, 0.5), 2, 0.5, 1, 0.5, particleOptions);
    }

    public boolean isStandingOn(final Block block) {
        return currentPosition != null && currentPosition.getBlock().getRelative(BlockFace.DOWN).equals(block);
    }

    public boolean is(final Block block) {
        return Objects.equals(block.getLocation(), this.currentPosition);
    }

}