package me.nologic.minespades.game;

import lombok.AccessLevel;
import lombok.Getter;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.game.event.BattlegroundPlayerDeathEvent;
import me.nologic.minespades.game.event.PlayerEnterBattlegroundEvent;
import me.nologic.minespades.game.event.PlayerQuitBattlegroundEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;

public class EventDrivenGameMaster implements Listener {

    @Getter
    private final PlayerManager playerManager = new PlayerManager();

    @EventHandler
    private void whenPlayerEnterBattleground(PlayerEnterBattlegroundEvent event) {
        Battleground battleground = event.getBattleground();
        if (battleground.isValid() && playerManager.getBattlegroundPlayer(event.getPlayer()) == null) {
            playerManager.getPlayersInGame().add(battleground.connect(event.getPlayer()));
        }
    }

    @EventHandler
    private void whenPlayerQuitBattleground(PlayerQuitBattlegroundEvent event) {
        BattlegroundPlayer battlegroundPlayer = playerManager.getBattlegroundPlayer(event.getPlayer());
        if (battlegroundPlayer != null) {
            battlegroundPlayer.getBattleground().kick(battlegroundPlayer);
            playerManager.getPlayersInGame().remove(battlegroundPlayer);
        }
    }

    @EventHandler
    private void onBattlegroundPlayerDeath(BattlegroundPlayerDeathEvent event) {

        TextComponent textComponent;
        Player player = event.getPlayer();

        // TODO: создать класс Killfeed, который бы отправлял игрокам сообщения об игровых событиях
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
            for (BattlegroundPlayer p : playerManager.getPlayersInGame()) {
                if (player.equals(p.getPlayer()) && player.getHealth() <= event.getFinalDamage()) {
                    event.setCancelled(true);
                    Bukkit.getServer().getPluginManager().callEvent(new BattlegroundPlayerDeathEvent(p.getBattleground(), p.getPlayer(), killer, true, BattlegroundPlayerDeathEvent.RespawnMethod.QUICK));
                }
            }
        } else if (event.getEntity() instanceof Player player && event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player killer) {
                for (BattlegroundPlayer p : playerManager.getPlayersInGame()) {
                    if (player.equals(p.getPlayer()) && player.getHealth() <= event.getFinalDamage()) {
                        event.setCancelled(true);
                        Bukkit.getServer().getPluginManager().callEvent(new BattlegroundPlayerDeathEvent(p.getBattleground(), p.getPlayer(), killer, true, BattlegroundPlayerDeathEvent.RespawnMethod.QUICK));
                    }
                }
            }
        }

    }

    @EventHandler
    private void whenPlayerShouldDie(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE)
                for (BattlegroundPlayer p : playerManager.getPlayersInGame()) {
                    if (player.equals(p.getPlayer()) && player.getHealth() <= event.getFinalDamage()) {
                        event.setCancelled(true);
                        Bukkit.getServer().getPluginManager().callEvent(new BattlegroundPlayerDeathEvent(p.getBattleground(), p.getPlayer(), p.getTeam(), true, BattlegroundPlayerDeathEvent.RespawnMethod.QUICK));
                    }
                }
        }
    }

    /**
     * Когда игрок подключается к арене, то его инвентарь перезаписывается лоадаутом. Дабы игроки не теряли
     * свои вещи, необходимо сохранять старый инвентарь в датабазе и загружать его, когда игрок покидает арену.
     * И не только инвентарь! Кол-во хитпоинтов, голод, координаты, активные баффы и дебаффы и т. д.
     * */
    public static class PlayerManager implements Listener {

        @Getter (AccessLevel.PUBLIC)
        private final List<BattlegroundPlayer> playersInGame = new ArrayList<>();

        /**
         * Лёгкий способ получить обёртку игрока.
         * @return BattlegroundPlayer или null, если игрок не на арене
         * */
        public BattlegroundPlayer getBattlegroundPlayer(Player player) {
            for (BattlegroundPlayer bgPlayer : playersInGame) {
                if (bgPlayer.getPlayer().equals(player)) {
                    return bgPlayer;
                }
            }
            return null;
        }

        @EventHandler
        private void whenPlayerEnterBattleground(PlayerEnterBattlegroundEvent event) {

        }

        @EventHandler
        private void whenPlayerQuitBattleground(PlayerQuitBattlegroundEvent event) {

        }

        private void save(Player player) {

        }

    }

}