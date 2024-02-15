package me.nologic.castlewars.battleground.editor.loadout;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.nologic.castlewars.CastleWars;
import me.nologic.castlewars.battleground.BattlegroundPlayer;
import me.nologic.castlewars.battleground.BattlegroundTeam;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor @Getter
public class BattlegroundLoadout {

    private final String           name;
    private final Inventory        inventory;
    private final BattlegroundTeam team;

    private final List<LoadoutSupplyRule> supplyRules = new ArrayList<>();
    private final List<BukkitRunnable> tasks = new ArrayList<>();

    public void addSupplyRule(LoadoutSupplyRule rule) {
        this.supplyRules.add(rule);
    }

    public void acceptSupplyRules() {
        for (LoadoutSupplyRule rule : this.supplyRules) {
            BukkitRunnable runnable = new BukkitRunnable() {

                @Override
                public void run() {
                    for (BattlegroundPlayer player : CastleWars.getPlugin(CastleWars.class).getGameMaster().getPlayerManager().getPlayersInGame()) {
                        if (!player.getBukkitPlayer().hasPermission(rule.getPermission())) return;
                        if (player.getLoadout().equals(rule.getLoadout())) {
                            ItemStack item = rule.getItemStack();
                            PlayerInventory inventory = player.getBukkitPlayer().getInventory();
                            if (!inventory.containsAtLeast(item, rule.getMaximum())) {
                                inventory.addItem(item);
                            }
                        }
                    }
                }

            };

            runnable.runTaskTimer(CastleWars.getPlugin(CastleWars.class),0, rule.getInterval());
            this.tasks.add(runnable);
        }
    }

}