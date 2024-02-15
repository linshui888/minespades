package me.nologic.castlewars.battleground.editor.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SaveLoadoutTask extends BaseEditorTask implements Runnable {

    private final String name;

    public SaveLoadoutTask(Player player, String name) {
        super(player);
        this.name = name;
    }

    @Override
    @SneakyThrows
    public void run() {
        try (Connection connection = this.connect()) {

            PreparedStatement selectStatement = connection.prepareStatement("SELECT loadouts FROM teams WHERE name = ?;");
            selectStatement.setString(1, editor.editSession(player).getTargetTeam());
            ResultSet result = selectStatement.executeQuery(); result.next();

            // Сериализованный JsonArray, хранящий в себе все наборы экипировки редактируемой команды
            String loadouts = result.getString("loadouts");

            JsonArray array = (loadouts != null) ? JsonParser.parseString(loadouts).getAsJsonArray() : new JsonArray();
            array.add(this.inventoryToJSON(name, player.getInventory()));

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE teams SET loadouts = ? WHERE name = ?;");
            updateStatement.setString(1, array.toString());
            updateStatement.setString(2, editor.editSession(player).getTargetTeam());
            updateStatement.executeUpdate();

            editor.editSession(player).setTargetLoadout(name);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SneakyThrows
    public JsonObject inventoryToJSON(final String loadoutName, final PlayerInventory inventory) {
        JsonObject obj = new JsonObject();

        obj.addProperty("name", loadoutName);
        obj.addProperty("type", inventory.getType().name());
        obj.addProperty("size", inventory.getSize());

        JsonArray items = new JsonArray();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                JsonObject jitem = new JsonObject();
                jitem.addProperty("slot", i);
                String itemData = serializeItemStack(item);
                jitem.addProperty("data", itemData);
                items.add(jitem);
            }
        }
        obj.add("items", items);

        JsonArray supplies = new JsonArray();
        obj.add("supplies", supplies);

        return obj;
    }

}