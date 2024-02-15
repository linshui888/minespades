package me.nologic.castlewars.battleground.editor.task;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.nologic.castlewars.CastleWars;
import me.nologic.castlewars.battleground.editor.BattlegroundEditor;
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

    protected final CastleWars plugin = CastleWars.getPlugin(CastleWars.class);
    protected final BattlegroundEditor editor = plugin.getBattlegrounder().getEditor();
    protected final Player             player;

    protected final Gson               gson = new Gson();

    @SneakyThrows
    protected final Connection connect() {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + editor.editSession(player).getTargetBattleground() + ".db");
        Statement statement = connection.createStatement();
        statement.execute("PRAGMA journal_mode=OFF");
        statement.execute("PRAGMA synchronous=OFF");
        statement.close();
        return connection;
    }

    @SneakyThrows
    protected final String serializeItemStack(ItemStack item) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeObject(item);
        dataOutput.close();
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }

}