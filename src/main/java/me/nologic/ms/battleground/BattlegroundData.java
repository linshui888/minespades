package me.nologic.ms.battleground;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

/**
 * BattlegroundData является удобной обёрткой для локальной sqlite-датабазы, хранящей в себе основную информацию об арене.
 * Будь то блоки, команды, лоадауты, точки респавна, вся информация об арене может быть получена и изменена через этот класс.
 */
public class BattlegroundData {

    public final JavaPlugin plugin;

    private final String name;

    private Connection connection;

    public BattlegroundData(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.load();
    }


    private void load() {
        Bukkit.getLogger().info("Loading battleground with name " + name + "...");
        this.openConnection().organiseDatabase(); // Открываем соединение и проверяем таблицы
    }

    /** Открытие соединения с базой данных. */
    private BattlegroundData openConnection() {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + name + "/data.db");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return this;
    }

    /**
     * Создание таблиц. Таблицы будут созданы если только они не существуют.
     * @see Table
     */
    private void organiseDatabase() {
        try {
            Statement statement = connection.createStatement();
            for (Table table : Table.values()) statement.addBatch(table.createStatement);
            statement.executeBatch();
            statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public ResultSet getData(Table table) throws SQLException {
        return connection.createStatement().executeQuery(table.selectStatement);
    }

    /** Название арены. */
    public String getName() {
        return this.name;
    }

    enum Table {
        VOLUME("CREATE TABLE IF NOT EXISTS volume (x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, material VARCHAR(64) NOT NULL);",
                "SELECT * FROM volume"),
        ENTITIES("CREATE TABLE IF NOT EXISTS entities (x FLOAT NOT NULL, y INTEGER NOT NULL, z FLOAT NOT NULL, entity_type VARCHAR(32) NOT NULL, inventory TEXT);", ""),
        TEAMS("CREATE TABLE IF NOT EXISTS teams (name VARCHAR(16) NOT NULL UNIQUE, lifepool INTEGER DEFAULT 100, color CHAR(6) DEFAULT 'FFFFFF', loadouts TEXT, respawn_points TEXT NOT NULL);",
                "SELECT * FROM teams"),
        PREFERENCES("CREATE TABLE IF NOT EXISTS preferences (autoAssign BOOLEAN DEFAULT FALSE, friendlyFire BOOLEAN DEFAULT FALSE);",
                "SELECT * FROM preferences");

        private final String createStatement;
        private final String selectStatement;

        Table(String createStatement, String selectStatement) {
            this.createStatement = createStatement;
            this.selectStatement = selectStatement;
        }
    }

}