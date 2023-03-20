package me.nologic.minespades.battleground;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

/**
 * BattlegroundData является удобной обёрткой для локальной sqlite-датабазы, хранящей в себе основную информацию об арене.
 * Будь то блоки, команды, лоадауты, точки респавна, вся информация об арене может быть получена через этот класс.
 */
public class BattlegroundData {

    public final JavaPlugin plugin;
    private final String name;
    private Connection connection;

    public BattlegroundData(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.openConnection();
    }

    private void openConnection() {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + name + ".db");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public ResultSet getData(Table table) throws SQLException {
        return connection.createStatement().executeQuery(table.getSelectStatement());
    }

    public String getName() {
        return this.name;
    }

}