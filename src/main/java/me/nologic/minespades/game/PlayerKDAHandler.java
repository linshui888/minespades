package me.nologic.minespades.game;

import me.nologic.minespades.game.event.BattlegroundPlayerDeathEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

/**
 * Специализированный класс, который крайне подробно обрабатывает событие смерти игрока.
 * Учёт KDA, киллфид (отправление сообщений об убийстве и пр.) — всё это происходит тут.
 */
public class PlayerKDAHandler {

    public void handlePlayerDeath(BattlegroundPlayerDeathEvent event) {

        Player victim = event.getVictim().getPlayer(), killer = null;
        if (event.getKiller() != null) killer = event.getKiller().getPlayer();

        Component deathMessage;
        if (killer != null) {
            String symbol = this.getDeathSymbol(event);
            deathMessage = killer.name().color(TextColor.fromHexString("#" + event.getKiller().getTeam().getColor()))
                    .append(Component.text(" " + symbol + " ").color(TextColor.color(0xB9B9B9)))
                    .append(victim.name().color(TextColor.fromHexString("#" + event.getVictim().getTeam().getColor())));
        } else {
            deathMessage = Component.text("☠ ")
                    .color(TextColor.color(0xB9B9B9))
                    .append(victim.name().color(TextColor.fromHexString("#" + event.getVictim().getTeam().getColor())))
                    .append(Component.text(" ☠"));
        }

        event.getBattleground().getPlayers().forEach(battlegroundPlayer -> battlegroundPlayer.getPlayer().sendActionBar(deathMessage));
    }
    
    private String getDeathSymbol(BattlegroundPlayerDeathEvent event) {

        if (event.getKiller() != null) {
            String itemName = event.getKiller().getPlayer().getInventory().getItemInMainHand().getType().toString().toLowerCase();
            if (itemName.contains("pickaxe")) {
                return "⛏";
            }
            if (itemName.equals("air")) {
                return "ツ";
            }
        }

        return switch (event.getDamageCause()) {
            case PROJECTILE -> "➴";
            case LAVA, FIRE, FIRE_TICK -> "♨";
            case MAGIC -> "⛒";
            default -> "⚔";
        };
    }

}