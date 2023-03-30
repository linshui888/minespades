package me.nologic.minespades.battleground.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.Callable;

public class AddLoadoutTask extends BaseEditorTask implements Callable<Boolean> {

    public AddLoadoutTask(Minespades plugin, Player player, String name) {
        super(plugin, player, name);
    }

    @Override
    public Boolean call() throws Exception {
        Connection connection = this.connect();
        Statement stmt = connection.createStatement();
        String loadouts = stmt.executeQuery("SELECT loadouts FROM teams;").getString("loadouts");

        PreparedStatement statement = connection.prepareStatement("UPDATE teams SET loadouts = ? WHERE name = ?;");
        if (loadouts == null) {
            statement.setString(1, jsonifyInventory(name, player.getInventory()));
        } else statement.setString(1, loadouts + "\n" + jsonifyInventory(name, player.getInventory()));
        statement.setString(2, plugin.getBattlegrounder().getEditor().getTargetTeam(player));
        statement.executeUpdate();
        connection.close();

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
        player.sendMessage(String.format("§7[§5%s§7] Добавлено обмундирование: §6%s", plugin.getBattlegrounder().getEditor().getTargetTeam(player), name));
        return true;
    }

    @SneakyThrows
    public String jsonifyInventory(String name, PlayerInventory inventory) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
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

}