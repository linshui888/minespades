package me.nologic.minespades.battleground.builder;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.util.BattlegroundDataDriver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Улучшенный считыватель блоков из датабазы карты. Примечателен тем, что данные из датабазы считываются асинхронно от основного потока сервера,
 * а ещё каждые BLOCKS_PER_TICK делается пауза в один тик, дабы скармливать серверу блоки из ДБ по чуть-чуть, а не все разом. В теории, это
 * позволит избежать пролагивания при загрузке арены. Зачем терпеть лагспайки, если можно оптимизировать работу плагина?
 * */
@RequiredArgsConstructor
public class VolumeDeserializerThread extends BukkitRunnable {

    private static final int BLOCKS_PER_TICK = 25 * 1000;
    private final Minespades plugin = Minespades.getInstance();

    private final Battleground battleground;

    @Override @SneakyThrows
    public void run() {
        BattlegroundDataDriver connector = new BattlegroundDataDriver().connect(battleground);

        final List<Object[]> data = new ArrayList<>();
        final StateDataDeserializer blockState = new StateDataDeserializer();
        try (ResultSet result = connector.executeQuerry("SELECT * FROM volume")) {
            while (result.next()) {
                data.clear();
                while (data.size() <= BLOCKS_PER_TICK) data.add(this.readBlockData(result));
                new BukkitRunnable() {
                    
                    @Override @SneakyThrows
                    public void run() {
                    for (Object[] array : data) {
                        Block     block    = (Block)     array[0];
                        Material  material = (Material)  array[1];
                        BlockData data     = (BlockData) array[2];
                        block.setType(material, false);
                        block.setBlockData(data, false);
                        blockState.deserialize(block.getState(), result.getString("content"));
                    }
                    }
                    
                }.runTask(plugin);
            }

        }
    }

    /**
     * Десериализатор блоков. Возвращает массив объектов, где [0] является блоком, [1] является материалом, а [2]
     * опционально является BlockData. BlockState обрабатывается в потоке сервера, а не тут, так как при
     * асинхронном доступе block#getState() выбросит исключение.
     * */
    @SneakyThrows
    private Object[] readBlockData(ResultSet result) {
        int x = result.getInt("x"), y = result.getInt("y"), z = result.getInt("z");

        Block     block     = battleground.getWorld().getBlockAt(x, y, z);
        Material  material  = Material.valueOf(result.getString("material"));
        BlockData data      = Bukkit.createBlockData(result.getString("data"));

        return new Object[]{ block, material, data };
    }

}