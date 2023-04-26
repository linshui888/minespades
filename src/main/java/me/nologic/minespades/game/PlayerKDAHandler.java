package me.nologic.minespades.game;

import me.nologic.minespades.game.event.BattlegroundPlayerDeathEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

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
            textComponent = Component.text("")
                    .color(TextColor.color(0xFFFFFF))
                    .append(victim.name().color(TextColor.fromHexString("#" + event.getVictim().getTeam().getColor())))
                    .append(Component.text(" ⚔ ").decorate(TextDecoration.BOLD))
                    .append(killer.name().color(TextColor.fromHexString("#" + event.getKiller().getTeam().getColor())));
        } else {
            textComponent = Component.text("")
                    .color(TextColor.color(0xFFFFFF))
                    .append(victim.name().color(TextColor.fromHexString("#" + event.getVictim().getTeam().getColor())))
                    .append(Component.text(" ☠").decorate(TextDecoration.BOLD));
        }

        event.getBattleground().getPlayers().forEach(battlegroundPlayer -> battlegroundPlayer.getPlayer().sendActionBar(textComponent));
    }

}