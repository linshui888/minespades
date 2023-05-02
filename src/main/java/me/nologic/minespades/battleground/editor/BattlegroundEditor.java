package me.nologic.minespades.battleground.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Table;
import me.nologic.minespades.battleground.editor.task.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.sql.*;
import java.util.HashMap;
import java.util.Objects;

public class BattlegroundEditor implements Listener {

    private final Minespades plugin;
    private final HashMap<Player, Location[]> volumeCorners = new HashMap<>();
    private final HashMap<Player, String> teamEditSession = new HashMap<>();
    private final HashMap<Player, String> loadoutEditSession = new HashMap<Player, String>();
    private final HashMap<Player, String> battlegroundEditSession  = new HashMap<>();

    {
        plugin = Minespades.getPlugin(Minespades.class);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Создание файла арены.
     * */
    public void create(Player player, String battlegroundName) {
        try (Connection connection = connect(battlegroundName); Statement statement = connection.createStatement()) {

            for (Table table : Table.values()) {
                statement.executeUpdate(table.getCreateStatement());
            }

            PreparedStatement pst = connection.prepareStatement(Table.PREFERENCES.getInsertStatement());
            pst.setString(1, player.getWorld().getName());
            pst.executeUpdate();

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
            player.sendMessage("§4§l[!] §7Арена успешно создана, но теперь её нужно настроить. Минимальные требования для запуска арены: §cдве команды с настроенными точками возрождения и карта блоков§r.");
            player.sendMessage("§7Чтобы обновить карту блоков, напишите §6/ms edit volume");
            player.sendMessage("§7Чтобы создать команду, напишите §6/ms create team <название_команды>");
            player.sendMessage("§7Для запуска арены используйте §6/ms launch <название_арены>");

            battlegroundEditSession.put(player, battlegroundName);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Создание команды
    public void createTeam(Player player, String teamName) {
        try (Connection connection = connect(battlegroundEditSession.get(player)); PreparedStatement statement = connection.prepareStatement(Table.TEAMS.getInsertStatement())) {
            statement.setString(1, teamName);
            statement.executeUpdate();

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
            player.sendMessage("§7Команда §4" + teamName + " §7успешно создана! Для того, чтобы команда считалась рабочей, укажите хотя бы одну точку респавна.");
            player.sendMessage("§7Используйте §6/ms add respawn §7для этого.");
            player.sendMessage("§7Кроме того, рекомендуется указать базовое обмундирование с помощью §6/ms add loadout");

            this.setTargetTeam(player, teamName);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @SneakyThrows
    public void addSupply(Player player, String name, int interval, int amount, int maximum, String permission) {
        plugin.getServer().getScheduler().runTask(plugin, new SaveSupplyTask(player, name, interval, amount, maximum, permission));
    }

    public void addRespawnPoint(Player player) {
        String sql = "UPDATE teams SET respawnPoints = ? WHERE name = ?";
        try (Connection connection = connect(battlegroundEditSession.get(player)); PreparedStatement statement = connection.prepareStatement(sql)) {

            Location l = player.getLocation();
            String encodedLocation = Base64Coder.encodeString(String.format("%f; %f; %f; %f; %f", l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));
            statement.setString(1, encodedLocation);
            statement.setString(2, this.teamEditSession.get(player));
            statement.executeUpdate();

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
            player.sendMessage(String.format("§7[§5%s§7] Добавлена точка возрождения: §2%f, %f, %f", this.teamEditSession.get(player), l.getX(), l.getY(), l.getZ()));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @SneakyThrows
    private Connection connect(String battlegroundName) {
        String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + battlegroundName + ".db";
        Connection conn = DriverManager.getConnection(url);
        Statement st = conn.createStatement();
        st.execute("PRAGMA journal_mode=OFF");
        st.execute("PRAGMA synchronous=OFF");
        st.close();
        return conn;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (volumeCorners.containsKey(event.getPlayer())) {
            if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.GOLDEN_SWORD || event.getHand() != EquipmentSlot.HAND) return;
            switch (event.getAction()) {
                case LEFT_CLICK_BLOCK -> {
                    volumeCorners.get(event.getPlayer())[0] = Objects.requireNonNull(event.getClickedBlock()).getLocation();
                    event.getPlayer().sendMessage("§7Первый угол кубоида: §2" + event.getClickedBlock().getLocation().toVector());
                }
                case RIGHT_CLICK_BLOCK -> {
                    volumeCorners.get(event.getPlayer())[1] = Objects.requireNonNull(event.getClickedBlock()).getLocation();
                    event.getPlayer().sendMessage("§7Второй угол кубоида: §2" + event.getClickedBlock().getLocation().toVector());
                }
            }
        }
    }

    public void addLoadout(Player player, String name) {
        plugin.getServer().getScheduler().runTask(plugin, new SaveLoadoutTask(player, name));
    }

    public void removeLoadout(Player player, String name) {
        plugin.getServer().getScheduler().runTask(plugin, new RemoveLoadoutTask(player, name));
    }

    public void setTargetBattleground(Player player, String battlegroundName) {
        this.battlegroundEditSession.put(player, battlegroundName);
    }

    public void setTargetLoadout(Player player, String loadoutName) {
        this.loadoutEditSession.put(player, loadoutName);
    }

    public String getTargetLoadout(Player player) {
        return this.loadoutEditSession.get(player);
    }

    public void setAsVolumeEditor(Player player) {

        if (!this.battlegroundEditSession.containsKey(player)) {
            player.sendMessage("Сперва нужно выбрать редактируемую арену. Сделайте это с помощью /ms edit battleground <название_арены>.");
            return;
        }

        if (volumeCorners.containsKey(player)) {
            player.sendMessage("В данный момент уже редактируется карта " + battlegroundEditSession.get(player) + ".");
            return;
        }

        this.volumeCorners.put(player, new Location[2]);
        player.sendMessage("§7Вы вошли в режим редактирования карты. Взяв в руки золотой меч, выделите кубоид, после чего напишите §6/ms save§7, чтобы сохранить карту.");
    }

    @SneakyThrows
    public void setTargetTeam(Player player, String teamName) {
        try (Connection connection = this.connect(battlegroundEditSession.get(player))) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;");
            statement.setString(1, teamName);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                this.teamEditSession.put(player, teamName);
                player.sendMessage("§2Успех. Редактируемая команда: " + teamName);
            } else player.sendMessage("§4Ошибка. Несуществующая команда: " + teamName);
        }
    }
    public String getTargetTeam(Player player) {
        return teamEditSession.get(player);
    }

    public boolean isTeamSelected(Player player) {
        return teamEditSession.containsKey(player) && teamEditSession.get(player) != null;
    }

    @SneakyThrows
    public void saveVolume(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new SaveVolumeTask(player, this.volumeCorners.get(player)));
    }

    public String getTargetBattleground(Player player) {
        return this.battlegroundEditSession.get(player);
    }

    @SneakyThrows
    public void setTeamColor(Player player, String hexColor) {
        Connection connection = this.connect(getTargetBattleground(player));
        PreparedStatement statement = connection.prepareStatement("UPDATE teams SET color = ? WHERE name = ?;");
        statement.setString(1, hexColor);
        statement.setString(2, getTargetTeam(player));
        statement.executeUpdate();
    }

    @SneakyThrows
    public boolean isLoadoutExist(Player player, String loadoutName) {
        try (Connection connection = connect(getTargetBattleground(player))) {
            PreparedStatement listStatement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;");
            listStatement.setString(1, this.getTargetTeam(player));
            ResultSet data = listStatement.executeQuery(); data.next();
            JsonArray loadouts = JsonParser.parseString(data.getString("loadouts")).getAsJsonArray();

            for (JsonElement loadoutElement : loadouts) {
                JsonObject loadout = loadoutElement.getAsJsonObject();
                if (loadoutName.equals(loadout.get("name").getAsString())) return true;
            }

            return false;
        }
    }

    @SneakyThrows
    public void addFlag(Player player) {
        try (Connection connection = connect(getTargetBattleground(player))) {
            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;");
            selectStatement.setString(1, this.getTargetTeam(player));
            ResultSet result = selectStatement.executeQuery(); result.next();

            if (result.getString("flag") != null) {
                player.sendMessage(String.format("§4Ошибка. У команды %s уже есть флаг.", this.getTargetTeam(player)));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, new AddFlagTask(player));

        }
    }
}