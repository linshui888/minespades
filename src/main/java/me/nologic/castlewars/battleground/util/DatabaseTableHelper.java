package me.nologic.castlewars.battleground.util;

import lombok.Getter;

@Getter
public enum DatabaseTableHelper {

    VOLUME("CREATE TABLE IF NOT EXISTS volume (x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, material VARCHAR(64) NOT NULL, data TEXT, content TEXT);",
            "",
            "SELECT * FROM volume"),
    CORNERS ("CREATE TABLE IF NOT EXISTS corners (x1 INTEGER NOT NULL, y1 INTEGER NOT NULL, z1 INTEGER NOT NULL, x2 INTEGER NOT NULL, y2 INTEGER NOT NULL, z2 INTEGER NOT NULL);",
            "INSERT INTO corners (x1,y1,z1,x2,y2,z2) VALUES (?,?,?,?,?,?);"
            ,"SELECT * FROM corners"),
    TEAMS("CREATE TABLE IF NOT EXISTS teams (name VARCHAR(16) NOT NULL UNIQUE, lifepool INTEGER DEFAULT 100, color CHAR(6) DEFAULT 'FFFFFF', loadouts TEXT, respawnPoints TEXT, flag TEXT);",
            "INSERT INTO teams (name) VALUES (?);",
            "SELECT * FROM teams"),
    PREFERENCES("CREATE TABLE IF NOT EXISTS preferences (world VARCHAR(64) NOT NULL, parameters TEXT);",
            "INSERT INTO preferences (world) VALUES (?);",
            "SELECT * FROM preferences"),
    GAME_OBJECTS("CREATE TABLE IF NOT EXISTS objects (x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, type TEXT NOT NULL, data TEXT NOT NULL);", "", "");

    private final String createStatement;
    private final String insertStatement;
    private final String selectStatement;

    DatabaseTableHelper(String createStatement, String insertStatement, String selectStatement) {
        this.createStatement = createStatement;
        this.insertStatement = insertStatement;
        this.selectStatement = selectStatement;
    }

}