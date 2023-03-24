package me.nologic.minespades.game;

import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.game.event.BattlegroundPlayerDeathEvent;
import me.nologic.minespades.game.event.PlayerEnterBattlegroundEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.List;

public class EventDrivenGameMaster implements Listener {

    private final List<BattlegroundPlayer> playersInGame = new ArrayList<>();

    @EventHandler
    private void onPlayerEnterBattleground(PlayerEnterBattlegroundEvent event) {
        Battleground battleground = event.getBattleground();
        if (battleground.isConnectable()) {
            playersInGame.add(battleground.join(event.getPlayer()));
        }
    }

    @EventHandler
    private void onBattlegroundPlayerDeath(BattlegroundPlayerDeathEvent event) {
        final TextComponent textComponent = Component.text(" > ")
                .color(TextColor.color(0xCACAD9))
                .append(event.getPlayer().name().color(TextColor.color(0xA7B85E)))
                .append(Component.text(" был убит "))
                .append(event.getKiller().name().color(TextColor.color(0xB97A5A)))
                .append(Component.text("!"));

        event.getBattleground().broadcast(textComponent);
        switch (event.getRespawnMethod()) {
            case QUICK -> event.getPlayer().teleport(event.getTeam().getRandomRespawnLocation());
            case AOS -> event.getPlayer().sendMessage("не реализовано...");
            case NORMAL -> event.getPlayer().sendMessage("lol ok");
        }
    }

    @EventHandler
    private void onPlayerTakesDeadlyDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof Player killer) {
            for (BattlegroundPlayer p : playersInGame) {
                if (player.equals(p.getPlayer()) && player.getHealth() <= event.getFinalDamage()) {
                    event.setCancelled(true);
                    Bukkit.getServer().getPluginManager().callEvent(new BattlegroundPlayerDeathEvent(p.getBattleground(), p.getPlayer(), killer, p.getTeam(), true, BattlegroundPlayerDeathEvent.RespawnMethod.QUICK));
                }
            }
        }
    }

}