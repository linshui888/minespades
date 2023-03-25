package me.nologic.minespades.battleground;

import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
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
        this.volumeGrids = new HashMap<>();
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

    private Connection connect(String battlegroundName) {
        String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + battlegroundName + ".db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    @SneakyThrows
    public void saveVolume(Player player) {
        Location[] grids = volumeGrids.get(player);
        World world = player.getWorld();
        String battlegroundName = this.battlegroundEditSession.get(player);
        if (grids[0] == null || grids[1] == null) {
            player.sendMessage("Необходимо указать два угла кубоида.");
            return;
        }

        int i = 0;
        long startTime = System.currentTimeMillis();
        try (Connection connection = this.connect(battlegroundName);
             PreparedStatement statement = connection.prepareStatement("INSERT INTO volume(x, y, z, material, data, inventory) VALUES(?,?,?,?,?,?);")) {

            connection.setAutoCommit(false);

            final int minX = Math.min(grids[0].getBlockX(), grids[1].getBlockX()),
                      maxX = Math.max(grids[0].getBlockX(), grids[1].getBlockX()),
                      minY = Math.min(grids[0].getBlockY(), grids[1].getBlockY()),
                      maxY = Math.max(grids[0].getBlockY(), grids[1].getBlockY()),
                      minZ = Math.min(grids[0].getBlockZ(), grids[1].getBlockZ()),
                      maxZ = Math.max(grids[0].getBlockZ(), grids[1].getBlockZ());

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        statement.setInt(1, x);
                        statement.setInt(2, y);
                        statement.setInt(3, z);

                        Block block = world.getBlockAt(x, y, z);
                        statement.setString(4, block.getType().toString());

                        statement.setString(5, block.getBlockData().getAsString());

                        Future<String> future = Bukkit.getServer().getScheduler().callSyncMethod(plugin, () -> {
                            String result = null;
                            if (block.getState() instanceof Container container)
                                result = this.encodeInventory(container.getInventory());
                            return result;
                        });

                        statement.setString(6, future.get());
                        statement.executeUpdate(); i++;
                    }
                }
            }

            connection.commit();
            long totalTime = System.currentTimeMillis() - startTime;
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
            player.sendMessage(String.format("§4§l[!] §7Карта успешно сохранена. §8(§5%dб.§8, §5%dс.§8)", i, totalTime / 1000));
            this.volumeGrids.remove(player);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (volumeGrids.containsKey(event.getPlayer())) {
            if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.GOLDEN_SWORD || event.getHand() != EquipmentSlot.HAND) return;
            switch (event.getAction()) {
                case LEFT_CLICK_BLOCK -> {
                    volumeGrids.get(event.getPlayer())[0] = Objects.requireNonNull(event.getClickedBlock()).getLocation();
                    event.getPlayer().sendMessage("§7Первый угол кубоида: §2" + event.getClickedBlock().getLocation().toVector());
                }
                case RIGHT_CLICK_BLOCK -> {
                    volumeGrids.get(event.getPlayer())[1] = Objects.requireNonNull(event.getClickedBlock()).getLocation();
                    event.getPlayer().sendMessage("§7Второй угол кубоида: §2" + event.getClickedBlock().getLocation().toVector());
                }
            }
        }
    }

    private final HashMap<Player, Location[]> volumeGrids;
    private final HashMap<Player, String> battlegroundEditSession;
    private final HashMap<Player, String> teamEditSession;
    public void addVolumeEditor(Player player) {
        if (volumeGrids.containsKey(player)) {
            player.sendMessage("В данный момент уже редактируется карта " + battlegroundEditSession.get(player) + ".");
            return;
        }

        this.volumeGrids.put(player, new Location[2]);
        player.sendMessage("Вы вошли в режим редактирования карты. Взяв в руки золотой меч, выделите кубоид, после чего напишите /ms save, чтобы сохранить карту.");
    }

    public void setTargetTeam(Player player, String teamName) {
        this.teamEditSession.put(player, teamName);
    }

    @SneakyThrows
    public String encodeInventory(Inventory inventory) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

        // Write the size of the inventory
        dataOutput.writeInt(inventory.getSize());

        // Save every element in the list
        for (int i = 0; i < inventory.getSize(); i++) {
            dataOutput.writeObject(inventory.getItem(i));
        }

        // Serialize that array
        dataOutput.close();
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }

}