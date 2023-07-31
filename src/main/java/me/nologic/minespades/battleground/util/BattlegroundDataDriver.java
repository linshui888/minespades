package me.nologic.minespades.battleground.util;

import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;

import java.sql.*;

public class BattlegroundDataDriver {

    private final Minespades plugin = Minespades.getInstance();

    @SneakyThrows
    public ResultSet executeQuery(String sql) {
        return this.connection.createStatement().executeQuery(sql);
    }

    @SneakyThrows
    public ResultSet executeQuery(final String sql, final Object... args) {
        final PreparedStatement statement = this.connection.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
        }
        return statement.executeQuery();
    }

    @SneakyThrows
    public BattlegroundDataDriver executeUpdate(String sql, Object... args) {
        PreparedStatement statement = this.connection.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
        }
        statement.executeUpdate();
        return this;
    }

    private Connection connection; @SneakyThrows
    public BattlegroundDataDriver connect(final String battlegroundName) {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + battlegroundName + ".db");
        this.checksum();
        return this;
    }

    public BattlegroundDataDriver connect(Battleground battleground) {
        return this.connect(battleground.getBattlegroundName());
    }

    private void checksum() {
        for (DatabaseTableHelper table : DatabaseTableHelper.values()) {
            this.executeUpdate(table.getCreateStatement());
        }
    }

    @SneakyThrows
    public void closeConnection() {
        this.connection.close();
    }

    public Connection getConnection() {
        return this.connection;
    }

}