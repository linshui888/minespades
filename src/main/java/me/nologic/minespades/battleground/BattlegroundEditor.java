package me.nologic.minespades.battleground;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Future;

/**
 * BattlegroundEditor берёт на себя задачу создания новых арен и редактирования существующих.
 * <p> Для удобства пользования BattlegroundEditor и BattlegroundLoader были разделены
 * на два класса. Что эти классы делают — вполне ясно по названию. В редакторе редактируется
 * не сам объект арены, загруженный в JVM, а информация о ней, хранимая в директории плагина.
 * Очевидно, что для применения изменённых настроек, необходима перезагрузка рабочей арены.
 * @see BattlegroundLoader
 */
public class BattlegroundEditor implements Listener {

    private final Minespades plugin;

    public BattlegroundEditor(Minespades plugin) {
        this.plugin = plugin;
        this.volumeCorners = new HashMap<>();
        this.battlegroundEditSession = new HashMap<>();
        this.teamEditSession = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

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
            plugin.getConfig().set("Battlegrounds", plugin.getConfig().getStringList("Battlegrounds").add(battlegroundName));
            plugin.saveConfig();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

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

    @SneakyThrows
    public void saveVolume(Player player) {
        Location[] corners = volumeCorners.get(player);
        World world = player.getWorld();
        String battlegroundName = this.battlegroundEditSession.get(player);
        if (corners[0] == null || corners[1] == null) {
            player.sendMessage("§4Необходимо указать два угла кубоида.");
            return;
        }

        int i = 0;
        long startTime = System.currentTimeMillis();
        Connection connection = this.connect(battlegroundName);
        PreparedStatement bSt = connection.prepareStatement("INSERT INTO volume(x, y, z, material, data, content) VALUES(?,?,?,?,?,?);");

        // Начало SQL-транзакции
        connection.setAutoCommit(false);

        // Вычисление углов
        final int minX = Math.min(corners[0].getBlockX(), corners[1].getBlockX()), maxX = Math.max(corners[0].getBlockX(), corners[1].getBlockX()), minY = Math.min(corners[0].getBlockY(), corners[1].getBlockY()), maxY = Math.max(corners[0].getBlockY(), corners[1].getBlockY()), minZ = Math.min(corners[0].getBlockZ(), corners[1].getBlockZ()), maxZ = Math.max(corners[0].getBlockZ(), corners[1].getBlockZ());

        // Сохранение блоков
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    bSt.setInt(1, x);
                    bSt.setInt(2, y);
                    bSt.setInt(3, z);

                    Block block = world.getBlockAt(x, y, z);

                    if (block.getType().isAir()) continue;

                    bSt.setString(4, block.getType().toString());
                    bSt.setString(5, block.getBlockData().getAsString());

                    bSt.setString(6, null);
                    bSt.addBatch();

                    if (++i % 5000 == 0) {
                        bSt.executeBatch();
                    }
                }
            }
        }

        bSt.executeBatch();
        connection.commit();

        // Сохранение тайлов
        Future<List<BlockState>> future = Bukkit.getServer().getScheduler().callSyncMethod(plugin, () -> {
            List<BlockState> tiles = new ArrayList<>();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType().isAir()) continue;
                        if (block.getState() instanceof Container || block.getState() instanceof Sign) {
                            tiles.add(block.getState());
                        }
                    }
                }
            }

            return tiles;
        });

        // Добавление в датабазу
        PreparedStatement tSt = connection.prepareStatement("UPDATE volume SET content = ? WHERE x = ? AND y = ? AND z = ?;");
        for (BlockState state : future.get()) {
            if (state instanceof Container container) {
                tSt.setString(1, this.save(container.getInventory()));
                tSt.setInt(2, container.getX());
                tSt.setInt(3, container.getY());
                tSt.setInt(4, container.getZ());
                tSt.addBatch();
            } else if (state instanceof Sign sign) {
                tSt.setString(1, this.save(sign));
                tSt.setInt(2, sign.getX());
                tSt.setInt(3, sign.getY());
                tSt.setInt(4, sign.getZ());
                tSt.addBatch();
            }

        }

        tSt.executeBatch();
        connection.commit();
        connection.close();
        long totalTime = System.currentTimeMillis() - startTime;
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
        player.sendMessage(String.format("§4§l[!] §7Карта успешно сохранена. §8(§53%dб.§8, §3%dс.§8)", i, totalTime / 1000));
        this.volumeCorners.remove(player);
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

    private final HashMap<Player, Location[]> volumeCorners;
    private final HashMap<Player, String> battlegroundEditSession;
    private final HashMap<Player, String> teamEditSession;
    public void addVolumeEditor(Player player) {
        if (volumeCorners.containsKey(player)) {
            player.sendMessage("В данный момент уже редактируется карта " + battlegroundEditSession.get(player) + ".");
            return;
        }

        this.volumeCorners.put(player, new Location[2]);
        player.sendMessage("§7Вы вошли в режим редактирования карты. Взяв в руки золотой меч, выделите кубоид, после чего напишите §6/ms save§7, чтобы сохранить карту.");
    }

    public void setTargetTeam(Player player, String teamName) {
        this.teamEditSession.put(player, teamName);
    }

    @SneakyThrows
    private String save(Inventory inventory) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", inventory.getType().name());
        obj.addProperty("size", inventory.getSize());

        JsonArray items = new JsonArray();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                JsonObject jitem = new JsonObject();
                jitem.addProperty("slot", i);
                String itemData = itemStackToString(item);
                jitem.addProperty("data", itemData);
                items.add(jitem);
            }
        }
        obj.add("items", items);
        return obj.toString();
    }

    private String save(Sign sign) {
        JsonObject obj = new JsonObject();
        obj.addProperty("content", StringUtils.join(sign.getLines(), "\n"));
        obj.addProperty("glow", sign.isGlowingText());
        obj.addProperty("color", Objects.requireNonNull(sign.getColor()).name());
        return obj.toString();
    }

    @SneakyThrows
    public String itemStackToString(ItemStack item) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeObject(item);
        dataOutput.close();
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }

}