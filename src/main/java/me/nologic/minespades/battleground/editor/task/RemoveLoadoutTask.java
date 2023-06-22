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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

public class RemoveLoadoutTask extends BaseEditorTask implements Runnable {

    private final String loadoutName;

    public RemoveLoadoutTask(final Player player, final String name) {
        super(player);
        this.loadoutName = name;
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

            JsonArray array = JsonParser.parseString(loadouts).getAsJsonArray();
            JsonElement target = null;

            for (JsonElement jsonElement : array) {
                String name = jsonElement.getAsJsonObject().get("name").getAsString();
                if (name.equals(this.loadoutName)) {
                    target = jsonElement;
                }
            }

            array.remove(target);

            PreparedStatement updateStatement = connection.prepareStatement("UPDATE teams SET loadouts = ? WHERE name = ?;");
            updateStatement.setString(1, array.toString());
            updateStatement.setString(2, editor.editSession(player).getTargetTeam());
            updateStatement.executeUpdate();

            if (Objects.equals(editor.editSession(player).getTargetLoadout(), loadoutName)) {
                editor.editSession(player).setTargetLoadout(null);
            }

            player.sendMessage(String.format("Loadout §3%s §rhas been deleted.", loadoutName));
            this.listEntries();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SneakyThrows
    private void listEntries() { // TODO: Убрать в отдельный класс (но какой?..)
        try (Connection connection = connect(); PreparedStatement listStatement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;")) {

            // Воспроизводим звук как показатель успешного выполнения команды и отображения листа
            player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1F, 1.4F);

            listStatement.setString(1, editor.editSession(player).getTargetTeam());
            ResultSet data = listStatement.executeQuery();

            // Проходимся по всем элементам JSON-массива и отправляем игроку-редактору лист со всеми наборами экипировки редактируемой команды
            while (data.next()) {
                player.sendMessage(Component.text("Наборы экипировки " + data.getString("name") + ":").color(TextColor.fromHexString("#" + data.getString("color"))));

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

}