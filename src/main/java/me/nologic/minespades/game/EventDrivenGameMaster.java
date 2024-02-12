package me.nologic.minespades.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.SneakyThrows;
import me.nologic.minespades.BattlegroundManager;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.battleground.BattlegroundPreferences;
import me.nologic.minespades.battleground.BattlegroundPreferences.Preference;
import me.nologic.minespades.battleground.BattlegroundTeam;
import me.nologic.minespades.game.event.*;
import me.nologic.minespades.game.object.BattlegroundObjectManager;
import me.nologic.minespades.game.object.NeutralBattlegroundFlag;
import me.nologic.minespades.game.object.TeamBattlegroundFlag;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.*;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Getter @Translatable @Configurable(path = "game-master-settings")
public class EventDrivenGameMaster implements MinorityFeature, Listener {

    private final Minespades          plugin         = Minespades.getInstance();
    private final BattlegroundManager battlegrounder = plugin.getBattlegrounder();

    private final BattlegroundObjectManager objectManager = new BattlegroundObjectManager();
    private final BattlegroundPlayerManager playerManager = new BattlegroundPlayerManager();
    private final PlayerKDAHandler          playerKDA     = new PlayerKDAHandler(this);

    private final HashMap<Player, Player> lastAttackerMap = new HashMap<>();

    public EventDrivenGameMaster() {
        this.init(this, this.getClass(), Minespades.getInstance());
    }

    @Nullable
    public Player getLastAttacker(final Player victim) {
        return this.lastAttackerMap.get(victim);
    }

    public void resetAttacker(final Player victim) {
        this.lastAttackerMap.remove(victim);
    }

    @EventHandler
    private void onPlayerQuitBattleground(PlayerQuitBattlegroundEvent event) {
        BattlegroundPlayer battlegroundPlayer = playerManager.getBattlegroundPlayer(event.getPlayer());
        if (battlegroundPlayer != null) {
            this.playerManager.disconnect(battlegroundPlayer);
        }
    }

    @EventHandler
    private void onBattlegroundSuccessfulLoad(final BattlegroundSuccessfulLoadEvent event) {

        final Battleground battleground = event.getBattleground();
        battleground.getTeams().stream().filter(t -> t.getFlag() != null).forEach(t -> t.getFlag().reset());

        // Если арена является частью мультиграунда, то вместо настоящего названия арены мы используем название мультиграунда
        final String name = battleground.getPreference(BattlegroundPreferences.Preference.IS_MULTIGROUND).getAsBoolean() ? battleground.getMultiground().getName() : battleground.getBattlegroundName();

        // TODO: Добавить игрокам возможность отказываться от авто-коннекта
        // Автоматическое подключение после запуска арены
        if (battleground.getPreference(Preference.FORCE_AUTOJOIN).getAsBoolean()) {
            Bukkit.getOnlinePlayers().forEach(p -> {
                Minespades.getInstance().getGameMaster().getPlayerManager().connect(p, battleground, battleground.getSmallestTeam());
            });
        }

        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), broadcastSound, 1F, 1F));

        plugin.broadcast(String.format(battlegroundLaunchedBroadcastMessage, name));
    }

    @EventHandler
    private void onBattlegroundPlayerDeath(BattlegroundPlayerDeathEvent event) {

        final Player player = event.getVictim().getBukkitPlayer();

        // Обработка смерти происходит в другом классе
        this.playerKDA.handlePlayerDeath(event);

        // Довольно простая механика лайфпулов. После смерти игрока, лайфпул его команды уменьшается.
        // Если игрок умер, а очков жизней больше нет — игрок становится спектатором.
        // Если в команде умершего игрока все игроки в спеке, то значит команда проиграла.
        int lifepool = event.getVictim().getTeam().getLifepool();

        if (lifepool >= 1) {
            event.getVictim().getTeam().setLifepool(lifepool - 1);

            if (!event.isKeepInventory()) {
                event.getVictim().setRandomLoadout();
            }

            // Если не сделать задержку в 1 тик, то некоторые изменения состояния игрока не применятся (fireTicks, tp)
            Bukkit.getScheduler().runTask(playerManager.plugin, () -> {

                if (event.getVictim().isCarryingFlag())
                    event.getVictim().getFlag().drop();

                player.teleport(event.getVictim().getTeam().getRandomRespawnLocation());
                player.setNoDamageTicks(20);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setFireTicks(0);
                player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
                player.setGameMode(GameMode.SURVIVAL);
            });
        } else {
            if (event.getVictim().isCarryingFlag())
                event.getVictim().getFlag().drop();
            event.getVictim().getBukkitPlayer().setGameMode(GameMode.SPECTATOR);
            boolean everyPlayerInTeamIsSpectator = true;
            for (Player p : event.getVictim().getTeam().getPlayers()) {
                if (p.getGameMode() == GameMode.SURVIVAL) {
                    everyPlayerInTeamIsSpectator = false;
                    break;
                }
            }
            if (everyPlayerInTeamIsSpectator)
                event.getBattleground().broadcast(String.format(teamLoseGameMessage, event.getVictim().getTeam().getDisplayName()));
        }

    }

    @EventHandler
    private void onPlayerCarryFlag(final PlayerCarryFlagEvent event) {

        final ChatColor          carrierTeamColor = event.getPlayer().getTeam().getColor();
        final String             carrierName      = event.getPlayer().getBukkitPlayer().getName();
        final BattlegroundPlayer carrier          = event.getPlayer();

        // Team flag behaviour
        if (event.getFlag() instanceof final TeamBattlegroundFlag flag) {

            final BattlegroundTeam team          = flag.getTeam();
            final ChatColor        flagTeamColor = flag.getTeam().getColor();

            event.getBattleground().broadcast(String.format(teamFlagCarriedMessage, carrierTeamColor + carrierName + "§r", flagTeamColor + flag.getTeam().getTeamName()));
            event.getBattleground().broadcast(String.format(teamLostLivesMessage, flag.getTeam().getDisplayName() + "§r", team.getFlagLifepoolPenalty()));
            team.setLifepool(team.getLifepool() - team.getFlagLifepoolPenalty());
        }

        // Neutral flag behaviour
        if (event.getFlag() instanceof NeutralBattlegroundFlag) {
            event.getBattleground().broadcast(String.format(neutralFlagCarriedMessage, carrierTeamColor + carrierName));
        }

        event.getPlayer().getBukkitPlayer().setGlowing(false);
        event.getFlag().reset();

        /* Scores and victory handling. */
        if (carrier.getTeam().addScore(1) >= event.getBattleground().getPreference(Preference.TEAM_WIN_SCORE).getAsInteger()) {
            event.getBattleground().broadcast(teamWinGameMessage.formatted(carrier.getTeam().getDisplayName()));
            event.getBattleground().gameOver(carrier.getTeam());
        }

    }

    @EventHandler
    private void onPlayerDamagePlayer(final EntityDamageByEntityEvent event) {
        if (!event.isCancelled() && event.getEntity() instanceof Player victim) {

            // If victim is not a battleground player, return
            if (BattlegroundPlayer.getBattlegroundPlayer(victim) == null) return;

            // We must store last attacker!
            if (event.getDamager() instanceof Player killer && BattlegroundPlayer.getBattlegroundPlayer(killer) != null) {
                this.lastAttackerMap.put(victim, killer);
            } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player killer) {
                this.lastAttackerMap.put(victim, killer);
            } else if (event.getDamager() instanceof TNTPrimed tnt && tnt.getSource() instanceof Player killer) {
                this.lastAttackerMap.put(victim, killer);
            }

        }
    }

    @EventHandler
    private void onPlayerDamage(final EntityDamageEvent event) {
        if (!event.isCancelled() && event.getEntity() instanceof Player player) {

            final BattlegroundPlayer bgPlayer = BattlegroundPlayer.getBattlegroundPlayer(player);
            if (bgPlayer != null && player.getHealth() <= event.getFinalDamage()) {
                BattlegroundPlayerDeathEvent battlegroundPlayerDeathEvent = new BattlegroundPlayerDeathEvent(bgPlayer, event.getCause(), bgPlayer.getBattleground().getPreference(Preference.KEEP_INVENTORY).getAsBoolean(), BattlegroundPlayerDeathEvent.RespawnStrategy.QUICK);
                Bukkit.getServer().getPluginManager().callEvent(battlegroundPlayerDeathEvent);
                event.setCancelled(true);
            }

        }
    }

    @EventHandler // Отмена телепортации на арене в режиме наблюдателя
    private void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            BattlegroundPlayer battlegroundPlayer = BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer());
            if (battlegroundPlayer != null && event.getTo() != null) {
                if (!Objects.equals(event.getTo().getWorld(), battlegroundPlayer.getBattleground().getWorld())) {
                    event.setCancelled(true);
                }
            }

        }
    }

    @EventHandler
    private void whenPlayerQuitServer(PlayerQuitEvent event) {
        BattlegroundPlayer battlegroundPlayer = BattlegroundPlayer.getBattlegroundPlayer(event.getPlayer());
        if (battlegroundPlayer != null)
            Bukkit.getServer().getPluginManager().callEvent(new PlayerQuitBattlegroundEvent(battlegroundPlayer.getBattleground(), battlegroundPlayer.getTeam(), event.getPlayer()));
    }

    /**
     * Когда игрок подключается к арене, то его инвентарь перезаписывается лоадаутом. Дабы игроки не теряли
     * свои вещи, необходимо сохранять старый инвентарь в датабазе и загружать его, когда игрок покидает арену.
     * И не только инвентарь! Кол-во хитпоинтов, голод, координаты, активные баффы и дебаффы и т. д.
     * */
    public class BattlegroundPlayerManager {

        @Getter
        private final List<BattlegroundPlayer> playersInGame = new ArrayList<>();
        private final Minespades plugin = Minespades.getPlugin(Minespades.class);

        /**
         * Лёгкий способ получить обёртку игрока.
         * @return BattlegroundPlayer или null, если игрок не на арене
         * */
        @Nullable
        public BattlegroundPlayer getBattlegroundPlayer(Player player) {
            for (BattlegroundPlayer bgPlayer : playersInGame) {
                if (bgPlayer.getBukkitPlayer().equals(player)) {
                    return bgPlayer;
                }
            }
            return null;
        }

        /**
         * Подключает игроков к баттлграунду.
         */
        public void connect(final Player player, final Battleground battleground, @Nullable final BattlegroundTeam team) {

            /* In case the player is already playing on some battleground. */
            if (this.getBattlegroundPlayer(player) != null) {
                player.sendMessage(battlegroundConnectionCancelledAlreadyInTheGameMessage);
                return;
            }

            /* In case AUTO_ASSIGN is enabled but no proper command is found. */
            if (team == null) {
                player.sendMessage(battlegroundConnectionCancelledAutoAssignIsFuckedUp);
                return;
            }

            /* In case a player tries to connect by hand, but he has selected a defeated or full team. */
            if (!battleground.isConnectable(team)) {
                player.sendMessage(battlegroundConnectionCancelledTeamFullOrDefeatedMessage.formatted(team.getDisplayName()));
                return;
            }

            this.save(player);
            final BattlegroundPlayer battlegroundPlayer = battleground.connectPlayer(player, team);
            this.getPlayersInGame().add(battlegroundPlayer);

            final String name = battlegroundPlayer.getDisplayName();
            player.setGameMode(GameMode.SURVIVAL);
            player.setDisplayName(name);
            player.setPlayerListName(name);
            player.setHealth(20);
            player.setFoodLevel(20);
            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));

            /* ProtocolSidebar check. */
            if (battlegroundPlayer.isSidebarEnabled()) {
                if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    battlegroundPlayer.showSidebar();
                } else {
                    plugin.getLogger().warning("Player sidebar enabled but no PlaceholderAPI found, personal sidebar support is disabled.");
                }
            }

            for (BattlegroundTeam t : battleground.getTeams()) {
                if (t.getFlag() != null && t.getFlag().getRecoveryBossBar() != null) {
                    t.getFlag().getRecoveryBossBar().addViewer(player);
                }
            }

            player.sendTitle(successfullyConnectedTitle, String.format(successfullyConnectedSubtitle, team.getDisplayName()), 20, 20, 20);
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2F);
            connectedToBattlegroundMessage.forEach(player::sendMessage);

        }

        /** Отключает игрока от баттлграунда. */
        public void disconnect(@NotNull BattlegroundPlayer battlegroundPlayer) {

            Player player = battlegroundPlayer.getBukkitPlayer();

            if (battlegroundPlayer.isCarryingFlag() && !plugin.isDisabling()) {
                battlegroundPlayer.getFlag().drop();
            }

            // todo neutral flag sidebar fix
            battlegroundPlayer.removeSidebar();
            for (BattlegroundTeam team : battlegroundPlayer.getBattleground().getTeams()) {
                if (team.getFlag() != null && team.getFlag().getRecoveryBossBar() != null) {
                    team.getFlag().getRecoveryBossBar().removeViewer(player);
                }
            }

            this.getPlayersInGame().remove(battlegroundPlayer);
            battlegroundPlayer.getBattleground().kick(battlegroundPlayer);
            this.load(player);

            final String name = ChatColor.WHITE + player.getName();
            player.setDisplayName(name);
            player.setPlayerListName(name);

            // Проверяем игроков на спектаторов. Если в команде начали появляться спектаторы, то
            // значит у неё закончились жизни. Если последний живой игрок ливнёт, а мы не обработаем
            // событие выхода, то игра встанет. Поэтому нужно всегда проверять команду.
            if (battlegroundPlayer.getTeam().getLifepool() == 0 && battlegroundPlayer.getTeam().getPlayers().size() > 1) {
                boolean everyPlayerInTeamIsSpectator = true;
                for (Player p : battlegroundPlayer.getTeam().getPlayers()) {
                    if (p.getGameMode() == GameMode.SURVIVAL) {
                        everyPlayerInTeamIsSpectator = false;
                        break;
                    }
                }
                if (everyPlayerInTeamIsSpectator)
                    battlegroundPlayer.getBattleground().broadcast(String.format(teamLoseGameMessage, battlegroundPlayer.getTeam().getDisplayName()));
            }
        }

        /**
         * Сохранение состояния указанного игрока: в датабазе сохраняется инвентарь, координаты, здоровье и голод.
         */
        @SneakyThrows
        private void save(Player player) {
            try (Connection connection = connect()) {

                // Сперва убеждаемся, что в датабазе есть нужная таблица (если нет, то создаём)
                String sql = "CREATE TABLE IF NOT EXISTS players (name VARCHAR(32) NOT NULL, world VARCHAR(64) NOT NULL, location TEXT NOT NULL, inventory TEXT NOT NULL, health DOUBLE NOT NULL, hunger INT NOT NULL);";
                connection.createStatement().executeUpdate(sql);

                // С целью избежания багов и путанницы, удаляем старое значение
                PreparedStatement deleteOldValue = connection.prepareStatement("DELETE FROM players WHERE name = ?;");
                deleteOldValue.setString(1, player.getName());
                deleteOldValue.executeUpdate();

                // Готовимся сохранить данные игрока в датабазе (имя, мир, локация в Base64, инвентарь в JSON, здоровье, голод)
                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO players (name, world, location, inventory, health, hunger) VALUES (?,?,?,?,?,?);");

                Location l = player.getLocation();
                String encodedLocation = Base64Coder.encodeString(String.format("%f; %f; %f; %f; %f", l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));

                preparedStatement.setString(1, player.getName());
                preparedStatement.setString(2, player.getWorld().getName());
                preparedStatement.setString(3, encodedLocation);
                preparedStatement.setString(4, inventoryToJSONString(player.getInventory()));
                preparedStatement.setDouble(5, player.getHealth());
                preparedStatement.setDouble(6, player.getFoodLevel());
                preparedStatement.executeUpdate();
            }
        }

        @SneakyThrows
        public void load(Player player) {
            try (Connection connection = connect()) {
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM players WHERE name = ?;");
                preparedStatement.setString(1, player.getName());
                ResultSet r = preparedStatement.executeQuery(); r.next();

                World     world     = Bukkit.getWorld(r.getString("world"));
                Location  location  = this.decodeLocation(world, r.getString("location"));
                Inventory inventory = this.parseJSONToInventory(r.getString("inventory"));
                double    health    = r.getDouble("health");
                int       hunger    = r.getInt("hunger");

                player.teleport(location);
                player.getInventory().setContents(inventory.getContents());
                player.setHealth(health);
                player.setFoodLevel(hunger);
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        @SneakyThrows
        private Connection connect() {
            return DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/player.db");
        }

        private Location decodeLocation(World world, String encoded) {
            String decoded = Base64Coder.decodeString(encoded);
            String[] split = decoded.replace(',', '.').split("; ");

            double x = Double.parseDouble(split[0]), y = Double.parseDouble(split[1]), z = Double.parseDouble(split[2]);
            float yaw = Float.parseFloat(split[3]), pitch = Float.parseFloat(split[4]);

            return new Location(world, x, y, z, yaw, pitch);
        }

        @NotNull
        private Inventory parseJSONToInventory(String string) {
            JsonObject obj = JsonParser.parseString(string).getAsJsonObject();

            Inventory inv = Bukkit.createInventory(null, InventoryType.valueOf(obj.get("type").getAsString()));

            JsonArray items = obj.get("items").getAsJsonArray();
            for (JsonElement itemele: items) {
                JsonObject jitem = itemele.getAsJsonObject();
                ItemStack item = decodeItem(jitem.get("data").getAsString());
                inv.setItem(jitem.get("slot").getAsInt(), item);
            }

            return inv;
        }

        @SneakyThrows
        private ItemStack decodeItem(String base64) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        }

        @SneakyThrows
        private String inventoryToJSONString(PlayerInventory inventory) {
            JsonObject obj = new JsonObject();

            obj.addProperty("type", inventory.getType().name());
            obj.addProperty("size", inventory.getSize());

            JsonArray items = new JsonArray();
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null) {
                    JsonObject jitem = new JsonObject();
                    jitem.addProperty("slot", i);
                    String itemData = itemStackToBase64(item);
                    jitem.addProperty("data", itemData);
                    items.add(jitem);
                }
            }
            obj.add("items", items);
            return obj.toString();
        }

        @SneakyThrows
        protected final String itemStackToBase64(ItemStack item) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        }

    }

    @TranslationKey(section = "regular-messages", name = "successfully-connected-title", value = "&6Successfully connected!")
    private String successfullyConnectedTitle;

    @TranslationKey(section = "regular-messages", name = "successfully-connected-subtitle", value = "&fYour team is %s&r!")
    private String successfullyConnectedSubtitle;

    @TranslationKey(section = "regular-messages", name = "battleground-connection-cancelled.already-in-the-game", value = "&cYou can not connect to this battleground because you're already in game.")
    private String battlegroundConnectionCancelledAlreadyInTheGameMessage;

    @TranslationKey(section = "regular-messages", name = "battleground-connection-cancelled.full-or-defeated-team", value = "&cYou can not connect to this battleground because team %s &cis full or defeated.")
    private String battlegroundConnectionCancelledTeamFullOrDefeatedMessage;

    @TranslationKey(section = "regular-messages", name = "battleground-connection-cancelled.no-playable-team", value = "&cYou can't connect to this battleground because the auto-assigner can't find a playable team.")
    private String battlegroundConnectionCancelledAutoAssignIsFuckedUp;

    @TranslationKey(section = "regular-messages", name = "battleground-launched-broadcast", value = "A new battle begins on the battleground #ed18c6%s&f!")
    private String battlegroundLaunchedBroadcastMessage;

    @TranslationKey(section = "regular-messages", name = "player-carried-team-flag", value = "%s &rhas carried the flag of team %s&f!")
    private String teamFlagCarriedMessage;

    @TranslationKey(section = "regular-messages", name = "player-carried-neutral-flag", value = "%s &rhas carried the neutral flag! His team is one step closer to victory!")
    private String neutralFlagCarriedMessage;

    @TranslationKey(section = "regular-messages", name = "team-lost-lives", value = "Team %s &rhas lost %s lives because they lost their flag!")
    private String teamLostLivesMessage;

    @TranslationKey(section = "regular-messages", name = "team-win-game", value = "Team %s &rwins this battle! Congratulations!")
    private String teamWinGameMessage;

    @TranslationKey(section = "regular-messages", name = "team-lose-game", value = "Team %s &rlose, vae victis!")
    private String teamLoseGameMessage;

    @TranslationKey(section = "regular-messages", name = "money-reward", value = "Congratulations, you get %s money for ending the game!")
    private String moneyRewardForWinningMessage;

    @ConfigurationKey(name = "broadcast-sound", value = "ITEM_GOAT_HORN_SOUND_6", type = Type.ENUM, comment = "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html")
    private Sound broadcastSound;

    @ConfigurationKey(name = "show-reward-message", value = "true", type = Type.BOOLEAN)
    private boolean rewardMessageEnabled;

    @ConfigurationKey(name = "win-money-reward", value = "0.0", type = Type.DOUBLE, comment = "Money reward for winning the game. Will work only if Vault is installed.")
    private double rewardForWinning;

    @ConfigurationKey(name = "blood-money-reward", value = "0.0", type = Type.DOUBLE, comment = "Money reward for one player kill, calculated at the end of the game. Will work only if Vault is installed.")
    private double rewardPerKill;

    @ConfigurationKey(name = "game-settings.battleground-join-message", type = Type.LIST_OF_STRINGS, comment = "A multi-line message that will be sent to players after they connect to battleground.", value = {
            "#2fd4a8&l[⁉] &7You are on the battleground. In order to lead your team to victory, you must get rid of all enemy teams. You can do this by lowering their &elifepool &7to zero &8(killing them)&7, or by &ecapturing flags&7.",
            "#2fd4b8&l[⁉] &7Flags are usually somewhere in the center of the battleground &8(or sometimes on enemy bases)&7, to pick up a flag you need to &estep on it&7. Flags fall out after death, and also give a &3glow effect &7to the carrier!",
            "#2fd4a8&l[⁉] &7If you want to &3disconnect &7from the game, use &3/ms q&7."})
    private List<String> connectedToBattlegroundMessage;

}