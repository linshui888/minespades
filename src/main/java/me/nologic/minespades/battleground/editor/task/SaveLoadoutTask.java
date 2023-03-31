package me.nologic.minespades.battleground.editor.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SaveLoadoutTask extends BaseEditorTask implements Runnable {

    // Loadout name
    private final String addedLoadoutName;

    public SaveLoadoutTask(Player player, String name) {
        super(player);
        this.addedLoadoutName = name;
    }

    @Override
    @SneakyThrows
    public void run() {
        Connection connection = this.connect();
        Statement stmt = connection.createStatement();
        String loadouts = stmt.executeQuery("SELECT loadouts FROM teams;").getString("loadouts");

        PreparedStatement statement = connection.prepareStatement("UPDATE teams SET loadouts = ? WHERE name = ?;");
        if (loadouts == null) {
            /* Если loadouts == null, то вместо конкатенации строки со старым значением, мы перезаписываем нулик. */
            statement.setString(1, inventoryToJSONString(addedLoadoutName, player.getInventory()));
        } else statement.setString(1, loadouts + "\n" + inventoryToJSONString(addedLoadoutName, player.getInventory()));
        statement.setString(2, plugin.getBattlegrounder().getEditor().getTargetTeam(player));
        statement.executeUpdate();

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
        ResultSet data = connection.createStatement().executeQuery("SELECT * FROM teams;");
        while (data.next()) {
            player.sendMessage(Component.text("\n" + data.getString("name") + ":").color(TextColor.fromHexString("#" + data.getString("color"))));
            for (String loadoutJSON : data.getString("loadouts").split("\n")) {
                String loadoutName = JsonParser.parseString(loadoutJSON).getAsJsonObject().get("name").getAsString();
                player.sendMessage(
                    Component.text(" - " + loadoutName, TextColor.color(172, 127, 67))
                            .append(Component.text(" [x]")
                                    .color(TextColor.color(187, 166, 96))
                                    .hoverEvent(HoverEvent.showText(Component.text("Нажмите, чтобы удалить " + loadoutName).color(TextColor.color(193, 186, 80))))
                                    .clickEvent(ClickEvent.runCommand("/ms delete loadout " + loadoutName))
                            )
                );
            }
        }
        data.close();
        connection.close();
    }

    @SneakyThrows
    public String inventoryToJSONString(String loadoutName, PlayerInventory inventory) {
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
                String itemData = itemStackToBase64(item);
                jitem.addProperty("data", itemData);
                items.add(jitem);
            }
        }
        obj.add("items", items);
        return obj.toString();
    }

}