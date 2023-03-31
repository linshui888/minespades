package me.nologic.minespades.battleground.editor.task;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@RequiredArgsConstructor
public abstract class BaseEditorTask {

    protected final Minespades plugin = Minespades.getPlugin(Minespades.class);
    protected final Player     player;

    @SneakyThrows
    protected final Connection connect() {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + plugin.getBattlegrounder().getEditor().getTargetBattleground(player) + ".db");
        Statement statement = connection.createStatement();
        statement.execute("PRAGMA journal_mode=OFF");
        statement.execute("PRAGMA synchronous=OFF");
        statement.close();
        return connection;
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