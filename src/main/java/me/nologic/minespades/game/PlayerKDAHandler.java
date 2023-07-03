package me.nologic.minespades.game;

import me.nologic.minespades.Minespades;
import me.nologic.minespades.game.event.BattlegroundPlayerDeathEvent;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Специализированный класс, который крайне подробно обрабатывает событие смерти игрока.
 * Учёт KDA, киллфид (отправление сообщений об убийстве и пр.) — всё это происходит тут.
 */
public class PlayerKDAHandler {

    public void handlePlayerDeath(BattlegroundPlayerDeathEvent event) {

        Player victim = event.getVictim().getBukkitPlayer(), killer = null;
        if (event.getKiller() != null) killer = event.getKiller().getBukkitPlayer();
        event.getVictim().setDeaths(event.getVictim().getDeaths() + 1);

        String deathMessage;
        if (killer != null) {
            String symbol = this.getDeathSymbol(event);
            deathMessage = String.format(event.getKiller().getColorizedName() + " §f%s " + event.getVictim().getColorizedName(), symbol);
            event.getKiller().setKills(event.getKiller().getKills() + 1);

            // Обновляем счётчик киллов для убийцы в таблисте
            Scoreboard scoreboard = event.getBattleground().getScoreboard();
            Objective objective = scoreboard.getObjective("kill_counter");
            if (objective != null) {
                objective.getScore(killer.getName()).setScore(event.getKiller().getKills());
            }

        } else {
            deathMessage = String.format("§f☠ %s §f☠", event.getVictim().getColorizedName());
        }

        // Sending message
        event.getBattleground().getPlayers().forEach(battlegroundPlayer -> battlegroundPlayer.getBukkitPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(deathMessage)));
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
            case PROJECTILE -> "➸";
            case LAVA, FIRE, FIRE_TICK -> "♨";
            case MAGIC -> "⚡";
            default -> "⚔";
        };
    }

}