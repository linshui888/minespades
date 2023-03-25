package me.nologic.minespades.battleground;

import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BattlegroundLoader подготавливает арены к использованию по их прямому назначению.
 * В этом классе создаются новые инстанции арен, загружаются их настройки и прочая
 * информация, которая требуется для игры: блоки арены, команды, расположение
 * точек респавна, мета-данные тайл-энтитей и т. д.
 * <p> Редактирование информации об уже существующих аренах, а так же само создание
 * этих арен происходит в отдельном классе BattlegroundEditor.
 * @see BattlegroundEditor
 * */
public class BattlegroundLoader {

    private final Minespades plugin;
    private Battleground battleground;

    public Battleground load(String name) {
        this.battleground = new Battleground(name);
        this.loadPreferences();
        this.loadVolume();
        this.loadTeams().forEach(team -> battleground.addTeam(team));
        return battleground;
    }

    private List<Team> loadTeams() {
        List<Team> list = new ArrayList<>();
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            ResultSet teams = statement.executeQuery(Table.TEAMS.getSelectStatement());
            while (teams.next()) {
                Team team = new Team(battleground, teams.getString("name"), teams.getInt("lifepool"), teams.getString("color"));
                // Arrays.stream(teams.getString("loadouts").split(", ")).toList().forEach(inv -> team.add(decodeInventory(inv)));
                Arrays.stream(teams.getString("respawnPoints").split(", ")).toList().forEach(loc -> team.addRespawnLocation(decodeLocation(loc)));
                list.add(team);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + battleground.getBattlegroundName() + ".db");
    }

    /* Инициализация полей арены. */
    private void loadPreferences() {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            ResultSet prefs = statement.executeQuery(Table.PREFERENCES.getSelectStatement());
            prefs.next();
            this.battleground.setWorld(Bukkit.getWorld(prefs.getString("world")));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadVolume() {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            ResultSet blocks = statement.executeQuery(Table.VOLUME.getSelectStatement());
            while(blocks.next()) {
                int x = blocks.getInt("x"), y = blocks.getInt("y"), z = blocks.getInt("z");
                Block b = battleground.getWorld().getBlockAt(x, y, z);
                Material material = Material.valueOf(blocks.getString("material"));
                b.setType(material);
                b.setBlockData(Bukkit.createBlockData(blocks.getString("data")));
                BlockState state = b.getState();
                if (!(state instanceof Furnace) && state instanceof Container container) {
                    container.getInventory().setContents(this.decodeContainer(blocks.getString("inventory")).getContents());
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private Location decodeLocation(String encoded) {
        String decoded = Base64Coder.decodeString(encoded);
        String[] split = decoded.replace(',', '.').split("; ");

        double x = Double.parseDouble(split[0]), y = Double.parseDouble(split[1]), z = Double.parseDouble(split[2]);
        float yaw = Float.parseFloat(split[3]), pitch = Float.parseFloat(split[4]);

        return new Location(battleground.getWorld(), x, y, z, yaw, pitch);
    }

    @SneakyThrows
    public Inventory decodeContainer(String data){
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt());

        // Read the serialized inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, (ItemStack) dataInput.readObject());
        }

        dataInput.close();
        return inventory;
    }

    public BattlegroundLoader(Minespades plugin) {
        this.plugin = plugin;
    }

}