package me.nologic.minespades.game.object;

import lombok.Getter;
import lombok.Setter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BattlegroundFlag {

    @Getter @NotNull
    protected final Battleground battleground;

    @Getter @NotNull
    protected final Location basePosition;

    @Getter @NotNull
    protected final ItemStack flagItem;

    @Getter @Nullable
    protected Location currentPosition;

    @Nullable
    protected BoundingBox boundingBox;

    @Getter @Setter @Nullable
    protected BattlegroundPlayer carrier;

    @Getter
    protected BukkitRunnable tick;

    public BattlegroundFlag(@NotNull Battleground battleground, @NotNull Location basePosition, @NotNull ItemStack flagItem) {
        this.battleground = battleground;
        this.basePosition = basePosition;
        this.flagItem = flagItem;
        this.currentPosition = basePosition;
    }

    protected abstract void pickup(BattlegroundPlayer carrier);
    public abstract void drop();
    public abstract void reset();

    /**
     * Apply pattern meta to the banner block.
     */
    protected void validateBannerData() {
        if (Minespades.getInstance().isEnabled()) {

            if (currentPosition == null) return;

            Bukkit.getScheduler().runTask(Minespades.getInstance(), () -> {
                currentPosition.getBlock().setType(flagItem.getType());
                BannerMeta meta = (BannerMeta) flagItem.getItemMeta();
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


}