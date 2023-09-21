package me.nologic.minespades.battleground.editor.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.editor.PlayerEditSession;
import me.nologic.minespades.battleground.util.BattlegroundDataDriver;

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

            JsonArray array = JsonParser.parseString(loadouts).getAsJsonArray();
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
        }
    }

}