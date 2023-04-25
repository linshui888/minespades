package me.nologic.minespades.battleground.editor.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

public class SaveLoadoutTask extends BaseEditorTask implements Runnable {

    private final String addedLoadoutName;

    public SaveLoadoutTask(Player player, String name) {
        super(player);
        this.addedLoadoutName = name;
    }

    @Override
    @SneakyThrows
    public void run() {
        try (Connection connection = this.connect(); PreparedStatement loadoutStatement = connection.prepareStatement("SELECT loadouts FROM teams WHERE name = ?;"); PreparedStatement updateStatement = connection.prepareStatement("UPDATE teams SET loadouts = ? WHERE name = ?;")) {
            loadoutStatement.setString(1, editor.getTargetTeam(player));
            ResultSet result = loadoutStatement.executeQuery(); result.next();

            String loadoutsJSON = result.getString("loadouts");
            JsonArray array = (loadoutsJSON != null) ? JsonParser.parseString(loadoutsJSON).getAsJsonArray() : new JsonArray();
            JsonElement loadout = inventoryToJSONString(addedLoadoutName, player.getInventory());
            array.add(loadout);
            loadoutsJSON = array.toString();

            updateStatement.setString(1, loadoutsJSON);
            updateStatement.setString(2, editor.getTargetTeam(player));
            updateStatement.executeUpdate();

            this.listEntries();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SneakyThrows
    private void listEntries() {
        try (Connection connection = connect(); PreparedStatement listStatement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;")) {

            // Воспроизводим звук как показатель успешного выполнения команды и отображения листа
            player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1F, 1.4F);

            listStatement.setString(1, editor.getTargetTeam(player));
            ResultSet data = listStatement.executeQuery();

            // Проходимся по всем элементам JSON-массива и отправляем игроку-редактору лист со всеми наборами экипировки редактируемой команды
            while (data.next()) {
                player.sendMessage(Component.text("Наборы экипировки" + data.getString("name") + ":").color(TextColor.fromHexString("#" + data.getString("color"))));

                JsonArray loadouts = JsonParser.parseString(data.getString("loadouts")).getAsJsonArray();
                for (JsonElement element : loadouts) {
                    JsonObject loadout = element.getAsJsonObject();
                    String name = loadout.get("name").getAsString();
                    player.sendMessage(
                            Component.text(" - " + name, TextColor.color(172, 127, 67))
                                    .append(Component.text(" [x]")
                                            .color(TextColor.color(187, 166, 96))
                                            .hoverEvent(HoverEvent.showText(Component.text("Нажмите, чтобы удалить " + name).color(TextColor.color(193, 186, 80))))
                                            .clickEvent(ClickEvent.runCommand("/ms delete loadout " + name))
                                    )
                    );
                }
            }
        }
    }

    @SneakyThrows
    public JsonObject inventoryToJSONString(String loadoutName, PlayerInventory inventory) {
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