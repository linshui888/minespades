package me.nologic.minespades.battleground.util;

import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;

import java.io.File;
import java.sql.*;

public class BattlegroundValidator {

    private static final Minespades plugin = Minespades.getPlugin(Minespades.class);

    // Арена считается валидной в том случае, если у неё есть как минимум две команды с как минимум одной точкой
    // респавна и как минимум одним набором экипировки. Набор экипировки может быть пустым, но он должен быть
    @SneakyThrows
    public static boolean isValid(String battlegroundName) {
        // Сперва стоит проверить, существует ли арена
        if (plugin.getBattlegrounder().isBattlegroundExist(battlegroundName)) {
            try (Connection connection = connect(battlegroundName)) {
                Statement statement = connection.createStatement();
                ResultSet teams = statement.executeQuery("SELECT count(*) AS rows FROM teams;"); teams.next();
                if (teams.getInt("rows") >= 2) {
                    ResultSet respawnPoints = statement.executeQuery("SELECT count(respawnPoints) AS respawns FROM teams WHERE respawnPoints IS NOT NULL;"); respawnPoints.next();
                    if (respawnPoints.getInt("respawns") > 0) {
                        ResultSet loadouts = statement.executeQuery("SELECT count(loadouts) AS loadoutsCount FROM teams WHERE loadouts IS NOT NULL;"); loadouts.next();
                        if (loadouts.getInt("loadoutsCount") > 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isExist(String battlegroundName) {
        return new File(plugin.getDataFolder() + "/battlegrounds/" + battlegroundName + ".db").exists();
    }

    @SneakyThrows
    private static Connection connect(String name) {
        return DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + name + ".db");
    }


}