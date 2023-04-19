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

import java.util.ArrayList;
import java.util.List;

public class EventDrivenGameMaster implements Listener {

    private final List<BattlegroundPlayer> playersInGame = new ArrayList<>();

    @EventHandler
    private void onPlayerEnterBattleground(PlayerEnterBattlegroundEvent event) {
        Battleground battleground = event.getBattleground();
        if (battleground.isValid() && !battleground.havePlayer(event.getPlayer())) {
            BattlegroundPlayer player = battleground.connect(event.getPlayer());
            player.setRandomLoadout();
            this.playersInGame.add(player);
        }
    }

    @EventHandler
    private void onBattlegroundPlayerDeath(BattlegroundPlayerDeathEvent event) {

        TextComponent textComponent;
        Player player = event.getPlayer();

        if (event.getKiller() != null) {
            textComponent = Component.text(" > ")
                    .color(TextColor.color(0xCACAD9))
                    .append(player.name().color(TextColor.fromHexString("#" + event.getTeam().getColor())))
                    .append(Component.text(" был убит "))
                    .append(event.getKiller().getPlayer().name().color(TextColor.fromHexString("#" + event.getKiller().getTeam().getColor())))
                    .append(Component.text("!"));
        } else {
            textComponent = Component.text(" > ")
                    .color(TextColor.color(0xCACAD9))
                    .append(player.name().color(TextColor.fromHexString("#" + event.getTeam().getColor())))
                    .append(Component.text(" умер.."));
        }

        switch (event.getRespawnMethod()) {
            case QUICK -> player.teleport(event.getTeam().getRandomRespawnLocation());
            case AOS -> player.sendMessage("не реализовано...");
            case NORMAL -> player.sendMessage("lol ok");
        }

        event.getBattleground().broadcast(textComponent);
        player.setNoDamageTicks(20);
        player.setFireTicks(0);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        event.getVictim().setRandomLoadout();
    }

    @EventHandler
    private void whenPlayerKillPlayer(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof Player killer) {
            for (BattlegroundPlayer p : playersInGame) {
                if (player.equals(p.getPlayer()) && player.getHealth() <= event.getFinalDamage()) {
                    event.setCancelled(true);
                    Bukkit.getServer().getPluginManager().callEvent(new BattlegroundPlayerDeathEvent(p.getBattleground(), p.getPlayer(), killer, true, BattlegroundPlayerDeathEvent.RespawnMethod.QUICK));
                }
            }
        }
    }

    @EventHandler
    private void whenPlayerShouldDie(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK)
                for (BattlegroundPlayer p : playersInGame) {
                    if (player.equals(p.getPlayer()) && player.getHealth() <= event.getFinalDamage()) {
                        event.setCancelled(true);
                        Bukkit.getServer().getPluginManager().callEvent(new BattlegroundPlayerDeathEvent(p.getBattleground(), p.getPlayer(), p.getTeam(), true, BattlegroundPlayerDeathEvent.RespawnMethod.QUICK));
                    }
                }
        }
    }

}