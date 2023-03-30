package me.nologic.minespades.battleground;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.editor.BattlegroundEditor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
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
                Arrays.stream(teams.getString("loadouts").split("\n")).toList().forEach(inv -> team.addLoadout(readInventory(inv)));
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
        try (Connection connection = connect()) {

            // Clear
            Statement clear = connection.createStatement();
            ResultSet corners = clear.executeQuery(Table.CORNERS.getSelectStatement());
            while(corners.next()) {
                int minX = corners.getInt("x1"), maxX = corners.getInt("x2"), minY = corners.getInt("y1"), maxY = corners.getInt("y2"), minZ = corners.getInt("z1"), maxZ = corners.getInt("z2");
                Block corner1 = battleground.getWorld().getBlockAt(minX, minY, minZ), corner2 = battleground.getWorld().getBlockAt(maxX, maxY, maxZ);
                battleground.getWorld().getNearbyEntities(BoundingBox.of(corner1, corner2)).forEach(e -> {
                    if (!(e instanceof Player)) e.remove();
                });
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            battleground.getWorld().getBlockAt(x, y, z).setType(Material.AIR);
                        }
                    }
                }
            }

            // Blocks
            Statement statement = connection.createStatement();
            ResultSet blocks = statement.executeQuery(Table.VOLUME.getSelectStatement());
            while(blocks.next()) {
                int x = blocks.getInt("x"), y = blocks.getInt("y"), z = blocks.getInt("z");
                Block b = battleground.getWorld().getBlockAt(x, y, z);
                Material material = Material.valueOf(blocks.getString("material"));
                b.setType(material);
                b.setBlockData(Bukkit.createBlockData(blocks.getString("data")));
                BlockState state = b.getState();

                // Tile entities
                // TODO: Возможно, стоит перенести это в другое место.
                if (state instanceof Container container) {
                    container.getInventory().setContents(this.readInventory(blocks.getString("content")).getContents());
                } else if (state instanceof Sign sign) {
                    JsonObject obj = JsonParser.parseString(blocks.getString("content")).getAsJsonObject();
                    sign.setGlowingText(obj.get("glow").getAsBoolean());
                    String[] lines = obj.get("content").getAsString().split("\n");
                    for (int i = 0; i < lines.length; i++)
                        sign.line(i, Component.text(lines[i]));
                    sign.setColor(DyeColor.valueOf(obj.get("color").getAsString()));
                    sign.update(true, false);
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

    private Inventory readInventory(String string) {
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

    public BattlegroundLoader(Minespades plugin) {
        this.plugin = plugin;
    }

}