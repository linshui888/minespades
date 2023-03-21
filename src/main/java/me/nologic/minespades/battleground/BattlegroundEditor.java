package me.nologic.minespades.battleground;

import me.nologic.minespades.Minespades;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.sql.*;
import java.util.HashMap;
import java.util.Objects;

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
        this.session = new HashMap<>();
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

            player.sendMessage("§7Арена успешно создана, но теперь её нужно настроить. Минимальные требования для запуска арены таковы: §cдве команды с настроенными точками возрождения и карта блоков§r.");
            player.sendMessage("Чтобы обновить карту блоков, напишите §6/ms edit volume");
            player.sendMessage("Чтобы создать команды, напишите §6/ms create team <название_команды>");
            player.sendMessage("Для запуска арены используйте §6/ms launch <название_арены>");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void createTeam(Player player, String teamName) {

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

    public void updateVolume(Player player) {
        Location[] grids = volumeGrids.get(player);
        World world = player.getWorld();
        String battlegroundName = this.session.get(player);
        if (grids[0] == null || grids[1] == null) {
            player.sendMessage("Необходимо указать два угла кубоида.");
            return;
        }

        try (Connection connection = this.connect(battlegroundName);
             PreparedStatement statement = connection.prepareStatement("INSERT INTO volume(x, y, z, material) VALUES(?,?,?,?);")) {
            for (int x = Math.min(grids[0].getBlockX(), grids[1].getBlockX()); x != Math.max(grids[0].getBlockX(), grids[1].getBlockX()); x++) {
                for (int y = Math.min(grids[0].getBlockY(), grids[1].getBlockY()); y != Math.max(grids[0].getBlockY(), grids[1].getBlockY()); y++) {
                    for (int z = Math.min(grids[0].getBlockZ(), grids[1].getBlockZ()); z != Math.max(grids[0].getBlockZ(), grids[1].getBlockZ()); z++) {
                        statement.setInt(1, x);
                        statement.setInt(2, y);
                        statement.setInt(3, z);
                        statement.setString(4, world.getBlockAt(x, y, z).getType().toString());
                        statement.addBatch();
                    }
                }
            }
            statement.executeBatch();
            player.sendMessage("Обновление карты завершено. (Действительно ли?)");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    /* Листинг кликов работает только если игрок находится в volumeGrids и держит в руках золотой меч. */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (volumeGrids.containsKey(event.getPlayer())) {
            if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.GOLDEN_SWORD && event.getHand() == EquipmentSlot.OFF_HAND) return;
            switch (event.getAction()) {
                case LEFT_CLICK_BLOCK -> {
                    volumeGrids.get(event.getPlayer())[0] = Objects.requireNonNull(event.getClickedBlock()).getLocation();
                    event.getPlayer().sendMessage("Указан первый угол кубоида: " + event.getClickedBlock().getLocation().toVector());
                }
                case RIGHT_CLICK_BLOCK -> {
                    volumeGrids.get(event.getPlayer())[1] = Objects.requireNonNull(event.getClickedBlock()).getLocation();
                    event.getPlayer().sendMessage("Указан второй угол кубоида: " + event.getClickedBlock().getLocation().toVector());
                }
            }
        }
    }

    private final HashMap<Player, Location[]> volumeGrids;
    private final HashMap<Player, String> session;
    public void addVolumeEditor(Player player, String battlegroundName) {
        if (volumeGrids.containsKey(player) || session.containsKey(player)) {
            player.sendMessage("В данный момент уже редактируется карта " + session.get(player) + ".");
            return;
        }

        this.volumeGrids.put(player, new Location[2]);
        this.session.put(player, battlegroundName);
        player.sendMessage("Вы вошли в режим редактирования карты. Взяв в руки золотой меч, выделите кубоид, после чего напишите /ms save, чтобы сохранить карту.");
    }

    public void removeVolumeEditor(Player player) {
        this.volumeGrids.remove(player);
        this.session.remove(player);
    }

}