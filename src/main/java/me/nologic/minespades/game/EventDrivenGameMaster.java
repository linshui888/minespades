package me.nologic.minespades.game;

import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.game.event.BattlegroundPlayerDeathEvent;
import me.nologic.minespades.game.event.PlayerEnterBattlegroundEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class EventDrivenGameMaster implements Listener {

    @EventHandler
    private void onPlayerEnterBattleground(PlayerEnterBattlegroundEvent event) {
        Battleground battleground = event.getBattleground();
        if (battleground.isConnectable()) {
            battleground.join(event.getPlayer());
            event.getPlayer().sendMessage(String.format("Подключение к арене %s успешно.", battleground));
        }
    }

    @EventHandler
    private void onBattlegroundPlayerDeath(BattlegroundPlayerDeathEvent event) {
        event.getBattleground().broadcast(String.format("%s был убит игроком %s..", event.getPlayer().name().color(TextColor.fromHexString("FF8000")), event.getKiller().name().color(TextColor.fromHexString("61de2a"))));
        switch (event.getRespawnMethod()) {
            case QUICK -> event.getPlayer().teleport(event.getTeam().getRandomRespawnLocation());
            case AOS -> event.getPlayer().sendMessage("не реализовано...");
            case NORMAL -> event.getPlayer().sendMessage("lol ok");
        }
    }

}