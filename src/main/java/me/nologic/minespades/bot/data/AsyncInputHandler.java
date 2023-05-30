package me.nologic.minespades.bot.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.bot.BattlegroundBot;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

@RequiredArgsConstructor
public class AsyncInputHandler extends Thread {

    private final BattlegroundBot bot;
    private final Socket socket;

    @Getter @Setter
    private boolean enabled = true;

    @Override @SneakyThrows
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String data;
            while (enabled && (data = input.readLine()) != null) {

                String finalData = data;
                Bukkit.getScheduler().runTask(Minespades.getInstance(),
                        () -> Bukkit.getServer().getPluginManager().callEvent(
                                new BotAnswerEvent(bot, finalData))
                );
            }
        } catch (Exception ex) {
            Minespades.getInstance().getLogger().info(String.format("Перестаём принимать дату от бота %s.", bot.getBukkitPlayer().getName()));
        } finally {
            socket.close();
        }
    }

}