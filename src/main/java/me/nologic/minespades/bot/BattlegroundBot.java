package me.nologic.minespades.bot;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.xephi.authme.api.v3.AuthMeApi;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.battleground.BattlegroundTeam;
import me.nologic.minespades.bot.BattlegroundBot.Controller.ControllerCommand;
import me.nologic.minespades.bot.behaviour.Behaviour;
import me.nologic.minespades.bot.data.AsyncInputHandler;
import me.nologic.minespades.bot.data.BotAnswerEvent;
import me.nologic.minespades.game.event.BattlegroundPlayerDeathEvent;
import me.nologic.minespades.game.event.PlayerQuitBattlegroundEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;

@Getter                      // TODO: Бот не должен быть листенером (или надо анрегать)
public class BattlegroundBot implements Listener {

    private final Minespades plugin = Minespades.getInstance();

    private       Battleground     battleground;
    private       BattlegroundTeam team;

    private final Controller        controller;
    private Behaviour               behaviour;

    private Player             bukkitPlayer;
    private BattlegroundPlayer battlegroundPlayer;

    private DecideGenerator    decideGenerator;

    @Nullable @Getter @Setter
    private Location destination;

    @Setter
    private boolean fleeing = false;
    private boolean consuming = false;

    @Getter @Setter @Nullable
    private Player target;

    private final BukkitRunnable drawer;

    @SneakyThrows
    public BattlegroundBot(Socket socket) {
        this.controller = new Controller(this, socket);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.drawer = new BukkitRunnable() {

            @Override
            public void run() {
                if (battlegroundPlayer != null && destination != null) {
                    if (bukkitPlayer.getLocation().distance(destination) <= 1)
                        destination = null;
                }
            }

        };

        drawer.runTaskTimerAsynchronously(plugin, 0, 20);
    }

    @EventHandler
    public void onBotAnswer(BotAnswerEvent event) {

        if (!event.getBattlegroundBot().equals(this)) return;

        final JsonObject json = JsonParser.parseString(event.getAnswerData()).getAsJsonObject();
        final String     command = json.get("command").getAsString();

        switch (command) {
            case "AUTH"  -> {
                final Player player = Bukkit.getPlayer(json.get("username").getAsString());
                this.battleground = plugin.getBattlegrounder().getBattlegroundByName(json.get("battleground").getAsString());
                this.forceLogin(player, battleground);
            }
            case "DONE" -> {
                this.target = null;
                this.fleeing = false;
            }
        }

    }

    @EventHandler
    public void onBotQuitBattleground(PlayerQuitBattlegroundEvent event) {
        if (event.getPlayer().equals(this.bukkitPlayer)) {
            this.controller.shutdown();
            event.getPlayer().kick();
        }
    }

    @EventHandler
    public void onBotDeath(BattlegroundPlayerDeathEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getVictim().equals(this.battlegroundPlayer)) {
                this.controller.sendCommand(ControllerCommand.DEATH);
                this.consuming = false;
                this.target = null;
                this.destination = null;
                this.fleeing = false;
            } else if (event.getVictim().getBukkitPlayer().equals(this.target)) {
                this.target = null;
                this.destination = null;
            }
        }, 5);
    }

    // Регистрация/логин, а так же инициализация поля.
    private void forceLogin(final Player player, final Battleground battleground) {
        this.bukkitPlayer = player;
        if (plugin.getServer().getPluginManager().isPluginEnabled("AuthMe")) {

            if (!AuthMeApi.getInstance().isRegistered(player.getName())) {
                plugin.getLogger().info(String.format("Автоматически регистрируем бота %s!", player.getName()));
                AuthMeApi.getInstance().forceRegister(player, "imabot", true);
            } else {
                plugin.getLogger().info(String.format("Автоматически авторизовываем бота %s!", player.getName()));
                AuthMeApi.getInstance().forceLogin(player);
            }

            // TODO: У бота обязательно должен быть пермишен grim.exempt, иначе античит не даст даже дышать
            this.join(battleground);
        }
    }

    public void join(Battleground battleground) {
        this.battleground = battleground;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.battlegroundPlayer = Minespades.getInstance().getGameMaster().getPlayerManager().connect(bukkitPlayer, battleground, battleground.getSmallestTeam());
            this.team = battlegroundPlayer.getTeam();
            this.behaviour = Behaviour.getRandomBehaviour(this);
            this.decideGenerator = new DecideGenerator(this);
            this.decideGenerator.runTaskTimer(plugin, 0, 10);
        }, 20);
    }

    public void held(int slot) {
        this.controller.sendCommand(ControllerCommand.HELD, String.valueOf(slot));
    }

    public void consume(int slot) {
        this.controller.sendCommand(ControllerCommand.CONSUME, String.valueOf(slot));
        this.consuming = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> this.consuming = false, 40);
    }

    public void moveTo(@NotNull Location location, int... range) {
        if (destination != null && range.length != 0 && destination.distance(location) <= range[0]) return;
        this.destination = location;
        this.controller.sendCommand(ControllerCommand.GOTO, location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
    }

    /**
     * Приказывает боту атаковать указанного игрока. Если игрок == target, то метод не выполнится.
     * */
    public void fight(@NotNull Player player) {
        if (player.equals(target)) return;
        this.destination = null;
        this.target = player;
        this.controller.sendCommand(ControllerCommand.FIGHT, player.getName());
    }

    public void shot(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> this.controller.sendCommand(ControllerCommand.SHOT, player.getName()), 5);
    }

    public static class Controller {

        @Getter
        private final BattlegroundBot bot;

        @Getter
        private final Socket socket;

        private final AsyncInputHandler in;
        private final BufferedWriter    out;

        @SneakyThrows
        public Controller(BattlegroundBot bot, Socket socket) {
            this.bot = bot;
            this.socket = socket;
            this.in  = new AsyncInputHandler(bot, socket); in.start();
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }

        @SneakyThrows /* [0] is always a command, [1] may be anything */
        private void sendCommand(ControllerCommand commandType, String... args) {
            StringBuilder builder = new StringBuilder();
            for (String string : args) {
                builder.append(string);
                if (args.length > 1) builder.append(" ");
            }

            final JsonObject json = new JsonObject();
            json.addProperty("command", commandType.toString());
            json.addProperty("data", builder.toString());

            out.write(json.toString());
            out.newLine();
            out.flush();
        }

        @SneakyThrows
        public void shutdown() {
            socket.close();
            this.in.setEnabled(false);
            this.bot.decideGenerator.cancel();
            this.out.close();
            HandlerList.unregisterAll(bot);
        }

        public enum ControllerCommand {

            JOIN,
            CHAT,
            DEATH,
            HELD,
            CONSUME,
            GOTO,
            FIGHT,
            SHOT,
            QUIT

        }

    }

}