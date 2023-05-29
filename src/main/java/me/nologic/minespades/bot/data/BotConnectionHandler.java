package me.nologic.minespades.bot.data;

import lombok.Getter;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.bot.BattlegroundBot;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class BotConnectionHandler implements Listener {

    private final Minespades plugin = Minespades.getInstance();
    private final List<BattlegroundBot> bots = new ArrayList<>();

    @Getter
    private final ServerSocket server;

    @Getter
    private final Thread acceptor;

    @SneakyThrows
    public BotConnectionHandler() {
        this.server = new ServerSocket(40526);
        this.acceptor = new Thread(() -> {
            try {
                Socket socket;
                while ((socket = server.accept()) != null) {
                    final BotConnectEvent connectEvent = new BotConnectEvent(socket);
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getServer().getPluginManager().callEvent(connectEvent));
                }
            } catch (Exception ex) {
                plugin.getLogger().info("Перестаём прослушивать входящие подключения.");
            }
        });
        this.listen();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void listen() {
        Minespades.getInstance().getLogger().info("Начинаем прослушивать входящие подключения.");
        this.acceptor.start();
    }

    @EventHandler
    private void onBotConnect(BotConnectEvent event) {
        bots.add(new BattlegroundBot(event.getSocket()));
    }

}