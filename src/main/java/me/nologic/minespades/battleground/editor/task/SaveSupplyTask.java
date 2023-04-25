package me.nologic.minespades.battleground.editor.task;

import com.google.gson.*;
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

    private final LoadoutSupplyRule supplyRule;

    public SaveSupplyTask(Player player, String name, int interval, int amount, int maximum, String permission) {
        super(player);
        ItemStack itemStack = player.getInventory().getItemInMainHand().clone();
        itemStack.setAmount(1);
        this.supplyRule = new LoadoutSupplyRule(editor.getTargetLoadout(player), name, super.serializeItemStack(itemStack), permission, interval, amount, maximum);
    }

    @Override
    @SneakyThrows
    public void run() {

        // Сериализованное в JSON-строку правило автовыдачи вещей
        String supplyRuleJSON = gson.toJson(supplyRule);

        try (Connection connection = this.connect()) {

            // Считываем все наборы экипировок у команды, редактируемой в данный момент
            PreparedStatement loadoutStatement = connection.prepareStatement("SELECT loadouts FROM teams WHERE name = ?;");
            loadoutStatement.setString(1, editor.getTargetTeam(player));
            ResultSet result = loadoutStatement.executeQuery(); result.next();

            // Проходимся по каждому набору, сравнивая названия
            JsonArray loadouts = JsonParser.parseString(result.getString("loadouts")).getAsJsonArray();
            for (JsonElement element : loadouts) {
                JsonObject loadout = element.getAsJsonObject();

                // Если названия совпадают, добавляем правило автовыдачи в лоадаут
                String name = loadout.get("name").getAsString();
                if (Objects.equals(name, editor.getTargetLoadout(player))) {
                    JsonArray supplies = loadout.get("supplies").getAsJsonArray();
                    supplies.add(supplyRuleJSON);
                }

            }

            // Сохраняем модифицированную JSON-строку в датабазу
            PreparedStatement statement = connection.prepareStatement("UPDATE teams SET loadouts = ? WHERE name = ?;");
            statement.setString(1, loadouts.toString());
            statement.setString(2, editor.getTargetTeam(player));
            statement.executeUpdate();

            // И выводим список правил автовыдачи
            this.listEntries();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SneakyThrows
    private void listEntries() { // TODO: Убрать в отдельный класс (но какой?..)
        try (Connection connection = connect()) {

            // Воспроизводим звук как показатель успешного выполнения команды и отображения листа
            player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1F, 1.4F);

            PreparedStatement listStatement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;");
            listStatement.setString(1, editor.getTargetTeam(player));
            ResultSet data = listStatement.executeQuery(); data.next();

            JsonArray loadouts = JsonParser.parseString(data.getString("loadouts")).getAsJsonArray();

            // Проходимся по всем элементам JSON-массива и отправляем игроку-редактору лист со всеми наборами экипировки редактируемой команды
            for (JsonElement loadoutArrayElement : loadouts) {
                JsonObject loadout = loadoutArrayElement.getAsJsonObject();
                String loadoutName = loadout.get("name").getAsString();

                if (Objects.equals(loadoutName, editor.getTargetLoadout(player))) {
                    player.sendMessage(Component.text(String.format("Правила автовыдачи вещей набора экипировки %s команды %s: ", loadoutName, editor.getTargetTeam(player))).color(TextColor.fromHexString("#" + data.getString("color"))));
                    JsonArray supplies = loadout.get("supplies").getAsJsonArray();
                    for (JsonElement supplyArrayElement : supplies) {
                        JsonObject supplyRule = supplyArrayElement.getAsJsonObject();
                        String supplyName = supplyRule.get("name").getAsString();
                        player.sendMessage(
                                Component.text(" - " + supplyName, TextColor.color(172, 127, 67))
                                        .append(Component.text(" [x]")
                                                .color(TextColor.color(187, 166, 96))
                                                .hoverEvent(HoverEvent.showText(Component.text("Нажмите, чтобы удалить " + supplyName).color(TextColor.color(193, 186, 80))))
                                                .clickEvent(ClickEvent.runCommand("/ms delete supply " + supplyName))
                                        )
                        );
                    }
                }
            }
        }
    }

}