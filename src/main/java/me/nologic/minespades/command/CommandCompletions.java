package me.nologic.minespades.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import me.nologic.minespades.BattlegroundManager;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.BattlegroundPreferences;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CommandCompletions является утилитарным классом, который содержит все необходимые методы для удобного
 * автоматического завершения команд.
 */
public class CommandCompletions {

    private final Minespades          plugin         = Minespades.getPlugin(Minespades.class);
    private final BattlegroundManager battlegrounder = plugin.getBattlegrounder();

    // Возвращает список названий запущенных арен. (арена не должна быть запущена через мультиграунд)
    public List<String> getEnabledBattlegrounds() {
        List<String> battlegroundNames = new ArrayList<>();
        battlegrounder.getLoadedBattlegrounds().forEach(b -> {
            if (!b.getPreferences().get(BattlegroundPreferences.Preference.JOIN_ONLY_FROM_MULTIGROUND)) {
                battlegroundNames.add(b.getBattlegroundName());
            }
        });
        return battlegroundNames;
    }

    // Возвращает список всех файлов (предположительно арен), которые находятся в папке battlegrounds
    public List<String> getBattlegroundFileList() {
        List<String> battlegroundNames = new ArrayList<>();

        String[] files = new File(plugin.getDataFolder() + "/battlegrounds/").list();
        if (files != null) {
            for (String file : files) {
                battlegroundNames.add(file.replace(".db", ""));
            }
        }

        return battlegroundNames;
    }

    // TODO: списки команд редактируемой арены

    // Возвращает список лоадаутов команды, редактируемой игроком в данный момент.
    public List<String> getTargetTeamLoadouts(Player player) {
        List<String> loadoutNames = new ArrayList<>();

        String targetTeamName = battlegrounder.getEditor().getTargetTeam(player);
        if (targetTeamName == null) return loadoutNames;

        try (Connection connection = this.connect(player); PreparedStatement statement = connection.prepareStatement("SELECT loadouts FROM teams WHERE name = ?;")) {
            statement.setString(1, battlegrounder.getEditor().getTargetTeam(player));
            ResultSet result = statement.executeQuery(); result.next();

            if (result.getString("loadouts") == null) return loadoutNames;

            JsonArray loadouts = JsonParser.parseString(result.getString("loadouts")).getAsJsonArray();
            for (JsonElement loadoutElement : loadouts) {
                JsonObject loadout = loadoutElement.getAsJsonObject();
                loadoutNames.add(loadout.get("name").getAsString());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return loadoutNames;
    }

    @SneakyThrows
    private Connection connect(Player player) {
        return DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + battlegrounder.getEditor().getTargetBattleground(player) + ".db");
    }

}
