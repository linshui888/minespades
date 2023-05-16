package me.nologic.minespades.battleground.util;

import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BattlegroundDataDriver {

    private final Minespades plugin = Minespades.getInstance();

    @SneakyThrows
    public ResultSet executeQuery(String sql) {
        return this.connection.createStatement().executeQuery(sql);
    }

    @SneakyThrows
    public BattlegroundDataDriver executeUpdate(String sql, Object... args) {
        PreparedStatement statement = this.connection.prepareStatement(sql);
        for (int i = 1; i < args.length; i++) {
            statement.setObject(i, args[i--]);
        }
        statement.executeUpdate();
        return this;
    }

    private Connection connection; @SneakyThrows
    public BattlegroundDataDriver connect(String battlegroundName) {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + battlegroundName + ".db");
        return this;
    }

    public BattlegroundDataDriver connect(Battleground battleground) {
        return this.connect(battleground.getBattlegroundName());
    }

    @SneakyThrows
    public void closeConnection() {
        this.connection.close();
    }

    public Connection getConnection() {
        return this.connection;
    }

}