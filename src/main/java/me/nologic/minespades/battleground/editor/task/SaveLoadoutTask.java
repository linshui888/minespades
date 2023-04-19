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
        try (Connection connection = this.connect()) {
            PreparedStatement loadoutStatement = connection.prepareStatement("SELECT loadouts FROM teams WHERE name = ?;"); // TODO: нужен селектор команды, добавь WHERE

            String targetTeamName = plugin.getBattlegrounder().getEditor().getTargetTeam(player);
            loadoutStatement.setString(1, targetTeamName);
            ResultSet r = loadoutStatement.executeQuery(); r.next();
            String loadouts = r.getString("loadouts");
            loadoutStatement.close();

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE teams SET loadouts = ? WHERE name = ?;");
            if (loadouts == null) {
                /* Если loadouts == null, то вместо конкатенации строки со старым значением, мы перезаписываем нулик. */
                updateStatement.setString(1, inventoryToJSONString(addedLoadoutName, player.getInventory()));
            } else updateStatement.setString(1, loadouts + "\n" + inventoryToJSONString(addedLoadoutName, player.getInventory()));
            updateStatement.setString(2, targetTeamName);
            updateStatement.executeUpdate();
            updateStatement.close();

            // TODO: в другой метод это говно (а ещё лучше класс)
            // Лист лоадаутов с удобной кнопкой удаления. Да, надо убрать его в отдельный метод.
            player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1F, 1.4F);
            PreparedStatement listStatement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;");
            listStatement.setString(1, targetTeamName);
            ResultSet data = listStatement.executeQuery();
            while (data.next()) {
                player.sendMessage(Component.text("\nНаборы " + data.getString("name") + ":").color(TextColor.fromHexString("#" + data.getString("color"))));
                for (String loadoutJSON : data.getString("loadouts").split("\n")) {
                    String loadoutName;

                    try {
                        loadoutName = JsonParser.parseString(loadoutJSON).getAsJsonObject().get("name").getAsString();
                    } catch (IllegalStateException ex) {
                        continue;
                    }

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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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