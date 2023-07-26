package me.nologic.minespades.battleground.editor.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.editor.PlayerEditSession;
import me.nologic.minespades.battleground.util.BattlegroundDataDriver;
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
        final PlayerEditSession session = editor.editSession(player);
        final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(session.getTargetBattleground());

        final String query = "SELECT loadouts FROM teams WHERE name = ?;";
        try (final ResultSet result = driver.executeQuery(query, session.getTargetTeam())) {

            // Сериализованный JsonArray, хранящий в себе все наборы экипировки редактируемой команды
            final String loadouts = result.getString("loadouts");

            JsonArray array = Minespades.getInstance().getJsonParser().parse(loadouts).getAsJsonArray();
            JsonElement target = null;

            for (JsonElement jsonElement : array) {
                String name = jsonElement.getAsJsonObject().get("name").getAsString();
                if (name.equals(this.loadoutName)) {
                    target = jsonElement;
                }
            }

            array.remove(target);
            driver.executeUpdate("UPDATE teams SET loadouts = ? WHERE name = ?;", array.toString(), session.getTargetTeam());

            if (Objects.equals(session.getTargetLoadout(), loadoutName)) {
                session.setTargetLoadout(null);
            }

            player.sendMessage(String.format("Loadout §3%s §rhas been deleted.", loadoutName));
            this.listEntries();
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
                Minespades.getInstance().getAdventureAPI().player(player).sendMessage(Component.text("Наборы экипировки " + data.getString("name") + ":").color(TextColor.fromHexString("#" + data.getString("color"))));

                JsonArray loadouts = Minespades.getInstance().getJsonParser().parse(data.getString("loadouts")).getAsJsonArray();
                for (JsonElement element : loadouts) {
                    JsonObject loadout = element.getAsJsonObject();
                    String name = loadout.get("name").getAsString();
                    Minespades.getInstance().getAdventureAPI().player(player).sendMessage(
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