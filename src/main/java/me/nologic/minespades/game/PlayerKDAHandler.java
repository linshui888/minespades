package me.nologic.minespades.game;

import me.nologic.minespades.game.event.BattlegroundPlayerDeathEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Специализированный класс, который крайне подробно обрабатывает событие смерти игрока.
 * Учёт KDA, киллфид (отправление сообщений об убийстве и пр.) — всё это происходит тут.
 */
public class PlayerKDAHandler {

    public void handlePlayerDeath(BattlegroundPlayerDeathEvent event) {

        Player victim = event.getVictim().getBukkitPlayer(), killer = null;
        if (event.getKiller() != null) killer = event.getKiller().getBukkitPlayer();

        Component deathMessage;
        if (killer != null) {
            String symbol = this.getDeathSymbol(event);
            deathMessage = killer.displayName()
                    .append(Component.text(" " + symbol + " ").color(NamedTextColor.WHITE))
                    .append(victim.displayName());
        } else {
            deathMessage = Component.text("☠ ")
                    .append(victim.displayName())
                    .append(Component.text(" ☠"));
        }

        event.getBattleground().getPlayers().forEach(battlegroundPlayer -> battlegroundPlayer.getBukkitPlayer().sendActionBar(deathMessage));
    }
    
    private String getDeathSymbol(BattlegroundPlayerDeathEvent event) {

        if (event.getKiller() != null) {
            String itemName = event.getKiller().getBukkitPlayer().getInventory().getItemInMainHand().getType().toString().toLowerCase();
            if (itemName.contains("pickaxe")) {
                return "⛏";
            }
            if (itemName.equals("air")) {
                return "ツ";
            }
        }

        return switch (event.getDamageCause()) {
            case PROJECTILE -> "➴";
            case LAVA, FIRE, FIRE_TICK -> "♨♨♨";
            case MAGIC -> "⚡";
            default -> "⚔";
        };
    }

}