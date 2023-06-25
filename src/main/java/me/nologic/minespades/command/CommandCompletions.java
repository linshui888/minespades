package me.nologic.minespades.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import me.nologic.minespades.BattlegroundManager;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPreferences;
import me.nologic.minespades.battleground.BattlegroundPreferences.Preference;
import me.nologic.minespades.battleground.Multiground;
import me.nologic.minespades.battleground.editor.PlayerEditSession;
import me.nologic.minespades.battleground.util.BattlegroundDataDriver;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
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
            if (!b.getPreferences().get(BattlegroundPreferences.Preference.IS_MULTIGROUND)) {
                battlegroundNames.add(b.getBattlegroundName());
            } else if (b.getMultiground() != null) battlegroundNames.add(b.getMultiground().getName());
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

    // TODO: списки команд редактируемой арены, список мультиграундов (напоминаю, что они хранятся в multigrounds.yml)
    /*
        TODO: getTargetBattlegroundTeams(из датабазы)
              getMultigrounds()
    * */

    // Возвращает список лоадаутов команды, редактируемой игроком в данный момент.
    public List<String> getTargetTeamLoadouts(Player player) {
        List<String> loadoutNames = new ArrayList<>();

        String targetTeamName = battlegrounder.getEditor().editSession(player).getTargetTeam();
        if (targetTeamName == null) return loadoutNames;

        try (Connection connection = this.connect(player); PreparedStatement statement = connection.prepareStatement("SELECT loadouts FROM teams WHERE name = ?;")) {
            statement.setString(1, battlegrounder.getEditor().editSession(player).getTargetTeam());
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

    public List<String> getBattlegroundPreferences() {
        List<String> prefs = new ArrayList<>();
        Arrays.stream(BattlegroundPreferences.Preference.values()).forEach(p -> prefs.add(p.toString()));
        return prefs;
    }

    @SneakyThrows
    public List<String> getTargetedBattlegroundTeams(final Player player) {

        final List<String> teams = new ArrayList<>();

        final String battleground = battlegrounder.getEditor().editSession(player).getTargetBattleground();
        if (battleground == null) {
            return teams;
        }

        final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(battleground);
        try (final ResultSet result = driver.executeQuery("SELECT * FROM teams;")) {
            while (result.next()) {
                teams.add(result.getString("name"));
            }
        }

        driver.closeConnection();
        return teams;
    }

    @SneakyThrows
    public List<String> getTargetTeamSupplies(final Player player) {
        final List<String> supplies = new ArrayList<>();
        final PlayerEditSession session = plugin.getBattlegrounder().getEditor().editSession(player);
        final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(session.getTargetBattleground());
        try (final ResultSet result = driver.executeQuery("SELECT * FROM teams WHERE name = ?;", session.getTargetTeam())) {
            final JsonArray loadouts = JsonParser.parseString(result.getString("loadouts")).getAsJsonArray();
            for (JsonElement loadoutElement : loadouts) {
                JsonObject jsonLoadout = loadoutElement.getAsJsonObject();
                String loadoutName = jsonLoadout.get("name").getAsString();
                if (loadoutName.equals(session.getTargetLoadout())) {
                    JsonArray suppliesArray = jsonLoadout.get("supplies").getAsJsonArray();
                    for (JsonElement supplyElement : suppliesArray) {
                        JsonObject supplyRule = supplyElement.getAsJsonObject();
                        String supplyName = supplyRule.get("name").getAsString();
                        suppliesArray.add(supplyName);
                    }
                }
            }
        }
        driver.closeConnection();
        return supplies;
    }

    public List<String> getBattlegroundTeamsOnJoinCommand(Player sender, String battlegroundName) {
        final List<String> teams = new ArrayList<>();

        if (!sender.hasPermission("minespades.team.pick")) return teams;

        Battleground battleground = battlegrounder.getBattlegroundByName(battlegroundName);
        Multiground multiground = battlegrounder.getMultiground(battlegroundName);
        if (battleground != null && !battleground.getPreference(Preference.FORCE_AUTO_ASSIGN)) {
            battleground.getTeams().forEach(team -> teams.add(team.getName()));
        } else if (multiground != null && !multiground.getBattleground().getPreference(Preference.FORCE_AUTO_ASSIGN)) {
            multiground.getBattleground().getTeams().forEach(team -> teams.add(team.getName()));
        }

        return teams;
    }

    @SneakyThrows
    private Connection connect(Player player) {
        return DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + battlegrounder.getEditor().editSession(player).getTargetBattleground() + ".db");
    }

}
