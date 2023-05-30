package me.nologic.minespades.bot.behaviour;

import lombok.Getter;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.bot.BattlegroundBot;
import me.nologic.minespades.bot.SituationKnowledge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public abstract class Behaviour {

    protected final Minespades plugin;
    protected final Random     random;

    protected final BattlegroundBot bot;
    protected final Battleground    battleground;

    public Behaviour(BattlegroundBot bot) {
        this.plugin = Minespades.getInstance();
        this.random = plugin.getRandom();
        this.bot = bot;
        this.battleground = bot.getBattleground();
    }

    public abstract void behave(SituationKnowledge knowledge);

    public void heldWeapon(boolean sword) {
        if (!sword) { // TODO: Стоит добавить сортировку мечей.
            bot.held(this.getRawSlot(item -> item != null && item.getType().toString().contains("_AXE")));
        } else bot.held(this.getRawSlot(item -> item != null && item.getType().toString().contains("_SWORD")));
    }

    public int getSlotForHealingPotion() {
        return this.getRawSlot(item -> item != null && item.getType().equals(Material.POTION)
                && item.getItemMeta() instanceof PotionMeta potion
                && (potion.getBasePotionData().getType().equals(PotionType.INSTANT_HEAL)
                || potion.getBasePotionData().getType().equals(PotionType.REGEN)));
    }

    private int getRawSlot(Predicate<ItemStack> predicate) {
        final List<ItemStack> items = Arrays.asList(bot.getBukkitPlayer().getInventory().getContents());

        final ItemStack item = items.stream().filter(predicate).findAny().orElse(null);

        if (item == null) return -1;
        int slot = bot.getBukkitPlayer().getInventory().first(item);

        if (slot <= 8) {
            slot += 36;
        }

        return slot;
    }

    @SneakyThrows
    public static Behaviour getRandomBehaviour(BattlegroundBot bot) {
        return Type.AGGRESSIVE.getClazz().getConstructor(BattlegroundBot.class).newInstance(bot);
    }

    private enum Type {

        AGGRESSIVE(AggressiveBehaviour.class);

        @Getter
        private final Class<? extends Behaviour> clazz;

        Type(Class<? extends Behaviour> clazz) {
            this.clazz = clazz;
        }
    }

}