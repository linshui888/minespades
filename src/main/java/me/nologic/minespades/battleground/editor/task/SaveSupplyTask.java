package me.nologic.minespades.battleground.editor.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import me.nologic.minespades.battleground.editor.loadout.LoadoutSupplyRule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

public class SaveSupplyTask extends BaseEditorTask implements Runnable {

    private final LoadoutSupplyRule supplyRule;

    public SaveSupplyTask(Player player, String name, int interval, int amount, int maximum, String permission) {
        super(player);
        ItemStack itemStack = player.getInventory().getItemInMainHand().clone();
        itemStack.setAmount(1);
        this.supplyRule = new LoadoutSupplyRule(null, name, super.serializeItemStack(itemStack), permission, interval, amount, maximum);
    }

    @Override
    @SneakyThrows
    public void run() {

        // Сериализованное в JSON-строку правило автовыдачи вещей
        String supplyRuleJSON = gson.toJson(supplyRule);

        try (Connection connection = this.connect()) {

            // Считываем все наборы экипировок у команды, редактируемой в данный момент
            PreparedStatement loadoutStatement = connection.prepareStatement("SELECT loadouts FROM teams WHERE name = ?;");
            loadoutStatement.setString(1, editor.editSession(player).getTargetTeam());
            ResultSet result = loadoutStatement.executeQuery(); result.next();

            // Проходимся по каждому набору, сравнивая названия
            JsonArray loadouts = JsonParser.parseString(result.getString("loadouts")).getAsJsonArray();
            for (JsonElement element : loadouts) {
                JsonObject loadout = element.getAsJsonObject();

                // Если названия совпадают, добавляем правило автовыдачи в лоадаут
                String name = loadout.get("name").getAsString();
                if (Objects.equals(name, editor.editSession(player).getTargetLoadout())) {
                    JsonArray supplies = loadout.get("supplies").getAsJsonArray();
                    JsonObject supplyRule = JsonParser.parseString(supplyRuleJSON).getAsJsonObject();

                    for (JsonElement supplyElement : supplies) {
                        if (Objects.equals(supplyElement.getAsJsonObject().get("name").getAsString(), supplyRule.get("name").getAsString())) {
                            player.sendMessage(String.format("&4Ошибка. Название %s уже занято.", supplyRule.get("name").getAsString()));
                            return;
                        }
                    }

                    supplies.add(supplyRule);
                }

            }

            // Сохраняем модифицированную JSON-строку в датабазу
            PreparedStatement statement = connection.prepareStatement("UPDATE teams SET loadouts = ? WHERE name = ?;");
            statement.setString(1, loadouts.toString());
            statement.setString(2, editor.editSession(player).getTargetTeam());
            statement.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}