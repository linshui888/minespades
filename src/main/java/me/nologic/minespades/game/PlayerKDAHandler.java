package me.nologic.minespades.game;

import me.nologic.minespades.game.event.BattlegroundPlayerDeathEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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

        TextComponent textComponent;
        if (killer != null) {
            textComponent = Component.text(" > ")
                    .color(TextColor.color(0xCED4C8))
                    .append(victim.name().color(TextColor.fromHexString("#" + event.getVictim().getTeam().getColor())))
                    .append(Component.text(" был убит "))
                    .append(killer.name().color(TextColor.fromHexString("#" + event.getKiller().getTeam().getColor())))
                    .append(Component.text("!"));
        } else {
            textComponent = Component.text(" > ")
                    .color(TextColor.color(0xCACAD9))
                    .append(victim.name().color(TextColor.fromHexString("#" + event.getVictim().getTeam().getColor())))
                    .append(Component.text(" умер своей смертью.."));
        }

        event.getBattleground().broadcast(textComponent);
        event.getBattleground().broadcast(Component.text("Причина: " + event.getDamageCause().name()).color(TextColor.color(196, 43, 39)));
    }

}
