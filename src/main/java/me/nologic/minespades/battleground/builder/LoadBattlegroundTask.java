package me.nologic.minespades.battleground.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundTeam;
import me.nologic.minespades.battleground.util.DatabaseTableHelper;
import me.nologic.minespades.battleground.editor.loadout.BattlegroundLoadout;
import me.nologic.minespades.battleground.editor.loadout.LoadoutSupplyRule;
import me.nologic.minespades.battleground.util.BattlegroundDataDriver;
import me.nologic.minespades.game.event.BattlegroundSuccessfulLoadEvent;
import me.nologic.minespades.game.object.NeutralBattlegroundFlag;
import me.nologic.minespades.game.object.TeamBattlegroundFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Улучшенный считыватель блоков из датабазы карты. Примечателен тем, что данные из датабазы считываются асинхронно от основного потока сервера,
 * а ещё каждые BLOCKS_PER_TICK делается пауза в один тик, дабы скармливать серверу блоки из ДБ по чуть-чуть, а не все разом. В теории, это
 * позволит избежать пролагивания при загрузке арены. Зачем терпеть лагспайки, если можно оптимизировать работу плагина?
 * */
@RequiredArgsConstructor
public class LoadBattlegroundTask extends BukkitRunnable {

    private static final int BLOCKS_PER_TICK = 15 * 1000;
    private final Minespades plugin = Minespades.getInstance();

    private final Battleground battleground;

    @Override @SneakyThrows
    public void run() {
        BattlegroundDataDriver connector = new BattlegroundDataDriver().connect(battleground);

        // Задаём BoundingBox и чистим блоки внутри
        this.clearCorners(connector.executeQuery("SELECT * FROM corners;"));

        List<Object[]> data = new ArrayList<>();
        StateDataDeserializer blockState = new StateDataDeserializer();
        try (ResultSet result = connector.executeQuery("SELECT * FROM volume;")) {
            while (result.next()) {

                data.add(this.readBlockData(result));
                if (data.size() < BLOCKS_PER_TICK) {
                    continue;
                }

                final List<Object[]> finalData = List.copyOf(data);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        for (Object[] array : finalData) {
                            Block     block    = (Block)     array[0];
                            Material  material = (Material)  array[1];
                            BlockData bd       = (BlockData) array[2];
                            String    content  = (String)    array[3];
                            block.setType(material, false);
                            block.setBlockData(bd, false);
                            blockState.deserialize(block.getState(), content);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                data = new ArrayList<>(); Thread.sleep(100);
            }

            // Last chance for blocks that might be skipped
            final List<Object[]> finalData = List.copyOf(data);
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    for (Object[] array : finalData) {
                        Block     block    = (Block)     array[0];
                        Material  material = (Material)  array[1];
                        BlockData bd       = (BlockData) array[2];
                        String    content  = (String)    array[3];
                        block.setType(material, false);
                        block.setBlockData(bd, false);
                        blockState.deserialize(block.getState(), content);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        }

        // После размещения блоков и тайл-энтитей, приступаем к загрузке команд. На этом этапе размещаются флаги.
        this.loadTeams();
        this.loadObjects();

        // Синхронно удаляем всех энтитей внутри арены (выпавшие блоки, возможные монстры и пр.)
        Bukkit.getScheduler().runTask(plugin, () -> {
            battleground.getWorld().getNearbyEntities(battleground.getBoundingBox()).forEach(e -> { if (!(e instanceof Player)) e.remove(); });
            Bukkit.getServer().getPluginManager().callEvent(new BattlegroundSuccessfulLoadEvent(battleground));
        });

    }

    @SneakyThrows
    private void loadObjects() {
        final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(battleground);
        try (final ResultSet objects = driver.executeQuery("SELECT * FROM objects;")) {
            while(objects.next()) {
                int x = objects.getInt("x"), y = objects.getInt("y"), z = objects.getInt("z");
                if (objects.getString("type").equals("NEUTRAL_FLAG")) {
                    final ItemStack flagItem = this.deserializeItemStack(JsonParser.parseString(objects.getString("data")).getAsJsonObject().get("item").getAsString());
                    ItemMeta meta = flagItem.getItemMeta();
                    assert meta != null;
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
                    flagItem.setItemMeta(meta);
                    final NeutralBattlegroundFlag flag = new NeutralBattlegroundFlag(battleground, new Location(battleground.getWorld(), x, y, z), flagItem);
                    flag.reset();
                }
            }
        }
    }

    @SneakyThrows
    private void clearCorners(ResultSet corners) {
        while(corners.next()) {
            int minX = corners.getInt("x1"), maxX = corners.getInt("x2"), minY = corners.getInt("y1"), maxY = corners.getInt("y2"), minZ = corners.getInt("z1"), maxZ = corners.getInt("z2");
            Block corner1 = battleground.getWorld().getBlockAt(minX, minY, minZ), corner2 = battleground.getWorld().getBlockAt(maxX, maxY, maxZ);
            this.battleground.setBoundingBox(BoundingBox.of(corner1, corner2));

            List<Block> blocks = new ArrayList<>();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {

                        blocks.add(battleground.getWorld().getBlockAt(x, y, z));
                        if (blocks.size() < BLOCKS_PER_TICK) continue;

                        final List<Block> clearable = List.copyOf(blocks);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            for (Block block : clearable) {
                                block.setType(Material.AIR, false);
                            }
                        });
                        blocks = new ArrayList<>();
                        Thread.sleep(50);

                    }
                }
            }

            // Last chance for blocks that might be skipped
            final List<Block> clearable = List.copyOf(blocks);
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Block block : clearable) {
                    block.setType(Material.AIR, false);
                }
            });

        }
    }

    /* Загружаем команды, правила автовыдачи вещей и размещаем флаги. */
    private void loadTeams() {
        List<BattlegroundTeam> list = new ArrayList<>();
        try (Connection connection = new BattlegroundDataDriver().connect(battleground).getConnection(); Statement statement = connection.createStatement()) {
            ResultSet teams = statement.executeQuery(DatabaseTableHelper.TEAMS.getSelectStatement());
            while (teams.next()) {

                final BattlegroundTeam team = new BattlegroundTeam(battleground, teams.getString("name"), teams.getString("color"), teams.getInt("lifepool"));

                if (teams.getString("flag") != null) {
                    JsonObject jsonFlag = JsonParser.parseString(teams.getString("flag")).getAsJsonObject();
                    int x = jsonFlag.get("x").getAsInt(), y = jsonFlag.get("y").getAsInt(), z = jsonFlag.get("z").getAsInt();
                    ItemStack itemFlag = this.deserializeItemStack(jsonFlag.get("item").getAsString());
                    ItemMeta meta = itemFlag.getItemMeta();
                    assert meta != null;
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
                    itemFlag.setItemMeta(meta);

                    final TeamBattlegroundFlag flag = new TeamBattlegroundFlag(battleground, team, new Location(battleground.getWorld(), x, y, z), itemFlag);
                    team.setFlag(flag);
                }


                final JsonArray loadouts = JsonParser.parseString(teams.getString("loadouts")).getAsJsonArray();
                for (JsonElement loadoutElement : loadouts) {
                    JsonObject jsonLoadout = loadoutElement.getAsJsonObject();
                    String loadoutName = jsonLoadout.get("name").getAsString();
                    Inventory inventory = this.readInventory(jsonLoadout.toString());
                    BattlegroundLoadout loadout = new BattlegroundLoadout(loadoutName, inventory, team);
                    JsonArray supplies = jsonLoadout.get("supplies").getAsJsonArray();
                    for (JsonElement supplyElement : supplies) {
                        JsonObject supplyRule = supplyElement.getAsJsonObject();
                        String supplyName = supplyRule.get("name").getAsString();
                        loadout.addSupplyRule(new LoadoutSupplyRule(loadout, supplyName, supplyRule.get("item").getAsString(), supplyRule.get("permission").getAsString(), supplyRule.get("interval").getAsInt(), supplyRule.get("amount").getAsInt(), supplyRule.get("maximum").getAsInt()));
                    }
                    loadout.acceptSupplyRules();
                    team.addLoadout(loadout);
                }
                Arrays.stream(teams.getString("respawnPoints").split(", ")).toList().forEach(loc -> team.addRespawnLocation(decodeLocation(loc)));
                list.add(team);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        for (BattlegroundTeam team : list) battleground.addTeam(team);
    }

    /** Десериализатор локации. Конвертирует Base64-строку в Location. */
    private Location decodeLocation(final String base64) {
        String decoded = Base64Coder.decodeString(base64);
        String[] split = decoded.replace(',', '.').split("; ");

        double x = Double.parseDouble(split[0]), y = Double.parseDouble(split[1]), z = Double.parseDouble(split[2]);
        float yaw = Float.parseFloat(split[3]), pitch = Float.parseFloat(split[4]);

        return new Location(battleground.getWorld(), x, y, z, yaw, pitch);
    }

    private Inventory readInventory(String inventoryJson) {
        JsonObject obj = JsonParser.parseString(inventoryJson).getAsJsonObject();
        Inventory inv = Bukkit.createInventory(null, InventoryType.valueOf(obj.get("type").getAsString()));
        JsonArray items = obj.get("items").getAsJsonArray();
        for (JsonElement element: items) {
            JsonObject jitem = element.getAsJsonObject();
            ItemStack item = deserializeItemStack(jitem.get("data").getAsString());
            inv.setItem(jitem.get("slot").getAsInt(), item);
        }
        return inv;
    }

    @SneakyThrows
    private ItemStack deserializeItemStack(String base64) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack item = (ItemStack) dataInput.readObject();
        dataInput.close();
        return item;
    }

    /**
     * Десериализатор блоков. Возвращает массив объектов, где [0] это Block, [1] это Material,
     * [2] это BlockState и [3] это String, который содержит инфу о состоянии. BlockState обрабатывается
     * в основном потоке сервера, так как при асинхронном доступе Block#getState() выбросит исключение.
     * */
    @SneakyThrows
    private Object[] readBlockData(ResultSet result) {
        int x = result.getInt("x"), y = result.getInt("y"), z = result.getInt("z");

        Block     block    = battleground.getWorld().getBlockAt(x, y, z);
        Material  material = Material.valueOf(result.getString("material"));
        BlockData data     = Bukkit.createBlockData(result.getString("data"));
        String    content  = result.getString("content");

        return new Object[] { block, material, data, content };
    }

}