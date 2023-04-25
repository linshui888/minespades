package me.nologic.minespades.battleground.editor.task;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import me.nologic.minespades.battleground.editor.loadout.LoadoutSupplyRule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

public class SaveSupplyTask extends BaseEditorTask implements Runnable {

    private final String    supplyName;
    private final ItemStack itemStack;
    private final int       amount, interval, maximum;

    private final LoadoutSupplyRule supplyRule;

    public SaveSupplyTask(Player player, String name, int amount, int interval, int maximum, String permission) {
        super(player);
        this.supplyName = name;
        this.itemStack = player.getInventory().getItemInMainHand().clone(); itemStack.setAmount(1);
        this.amount = amount; this.interval = interval; this.maximum = maximum;
        this.supplyRule = new LoadoutSupplyRule(editor.getTargetLoadout(player), name, super.serializeItemStack(itemStack), permission, interval, amount, maximum);
    }

    @Override
    @SneakyThrows
    public void run() {
        String supplyRuleJSON = gson.toJson(supplyRule);
        String targetTeamName = plugin.getBattlegrounder().getEditor().getTargetTeam(player);
        try (Connection connection = this.connect(); PreparedStatement loadoutStatement = connection.prepareStatement("SELECT loadouts FROM teams WHERE name = ?;")) {
            loadoutStatement.setString(1, targetTeamName);
            ResultSet result = loadoutStatement.executeQuery(); result.next();
            String loadouts = result.getString("loadouts");

            // Ищем нужную строку в псевдо-массиве
            for (String loadout : loadouts.split("\n")) {
                String loadoutName;

                try {
                    loadoutName = JsonParser.parseString(loadout).getAsJsonObject().get("name").getAsString();
                } catch (IllegalStateException ex) {
                    continue;
                }

                String targetLoadoutName = plugin.getBattlegrounder().getEditor().getTargetLoadout(player);
                if (Objects.equals(targetLoadoutName, loadoutName)) {
                    // Нашли JSON-строку! Теперь надо её пропарсить.
                    JsonObject obj = JsonParser.parseString(loadout).getAsJsonObject();
                    JsonArray supplies = obj.get("supplies").getAsJsonArray();
                    supplies.add(supplyRuleJSON);
                    obj.remove("supplies");
                    obj.add("supplies", supplies);
                }
            }

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE teams SET loadouts = ? WHERE name = ?;");
            if (loadouts == null) {
                /* Если loadouts == null, то вместо конкатенации строки со старым значением, мы перезаписываем нулик. */
                updateStatement.setString(1, inventoryToJSONString(supplyName, player.getInventory()));
            } else updateStatement.setString(1, loadouts + "\n" + inventoryToJSONString(supplyName, player.getInventory()));
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

}
