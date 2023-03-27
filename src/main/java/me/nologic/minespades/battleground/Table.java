package me.nologic.minespades.battleground;

public enum Table {

    VOLUME("CREATE TABLE IF NOT EXISTS volume (x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, material VARCHAR(64) NOT NULL, data TEXT, content TEXT);",
            "",
            "SELECT * FROM volume"),
    ENTITIES("CREATE TABLE IF NOT EXISTS entities (x FLOAT NOT NULL, y INTEGER NOT NULL, z FLOAT NOT NULL, entity_type VARCHAR(32) NOT NULL, inventory TEXT);",
            "",
            ""),
    TEAMS("CREATE TABLE IF NOT EXISTS teams (name VARCHAR(16) NOT NULL UNIQUE, lifepool INTEGER DEFAULT 100, color CHAR(6) DEFAULT 'FFFFFF', loadouts TEXT, respawnPoints TEXT);",
            "INSERT INTO teams (name) VALUES (?);",
            "SELECT * FROM teams"),
    PREFERENCES("CREATE TABLE IF NOT EXISTS preferences (autoAssign BOOLEAN DEFAULT FALSE, friendlyFire BOOLEAN DEFAULT FALSE, world VARCHAR(64) NOT NULL);",
            "INSERT INTO preferences (world) VALUES (?);",
            "SELECT * FROM preferences");

    private final String createStatement;
    private final String insertStatement;
    private final String selectStatement;

    Table(String createStatement, String insertStatement, String selectStatement) {
        this.createStatement = createStatement;
        this.insertStatement = insertStatement;
        this.selectStatement = selectStatement;
    }

    public String getCreateStatement() {
        return createStatement;
    }

    public String getInsertStatement() {
        return insertStatement;
    }

    public String getSelectStatement() {
        return selectStatement;
    }

}