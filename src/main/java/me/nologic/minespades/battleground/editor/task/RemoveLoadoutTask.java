package me.nologic.minespades.battleground.editor.task;

import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

public class RemoveLoadoutTask extends BaseEditorTask implements Runnable {

    private final String targetLoadoutName;
    private final String targetTeamName;

    public RemoveLoadoutTask(Player player, String targetLoadoutName) {
        super(player);
        this.targetLoadoutName = targetLoadoutName;
        this.targetTeamName = plugin.getBattlegrounder().getEditor().editSession(player).getTargetTeam();
    }

    @Override @SneakyThrows
    public void run() {
        Connection connection = super.connect();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;");
        statement.setString(1, targetTeamName);

        ResultSet result = statement.executeQuery(); result.next();
        String loadouts = result.getString("loadouts"); // Получаем мульти-строку, которую нужно сплитнуть
        StringBuilder reformed = new StringBuilder();
        int i = 0; for (String loadout : loadouts.split("\n")) {
            String loadoutName;

            try {
                loadoutName = JsonParser.parseString(loadout).getAsJsonObject().get("name").getAsString();
            } catch (IllegalStateException ex) {
                continue;
            }

            if (!Objects.equals(targetLoadoutName, loadoutName)) {
                if (i == 0) {
                    reformed.append(loadout);
                } else reformed.append("\n").append(loadout);
                i++;
            }
        }
        PreparedStatement updateLoadouts = connection.prepareStatement("UPDATE teams SET loadouts = ? WHERE name = ?;");
        updateLoadouts.setString(1, reformed.toString().equals("\n") ? null : reformed.toString());
        updateLoadouts.setString(2, targetTeamName);
        updateLoadouts.executeUpdate();

        connection.close();
    }

}