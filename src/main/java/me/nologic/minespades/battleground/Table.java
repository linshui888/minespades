package me.nologic.minespades.battleground;

public enum Table {

    VOLUME("CREATE TABLE IF NOT EXISTS volume (x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, material VARCHAR(64) NOT NULL, data TEXT, content TEXT);",
            "",
            "SELECT * FROM volume"),
    CORNERS ("CREATE TABLE IF NOT EXISTS corners (x1 INTEGER NOT NULL, y1 INTEGER NOT NULL, z1 INTEGER NOT NULL, x2 INTEGER NOT NULL, y2 INTEGER NOT NULL, z2 INTEGER NOT NULL);",
            "INSERT INTO corners (x1,y1,z1,x2,y2,z2) VALUES (?,?,?,?,?,?);"
            ,"SELECT * FROM corners"),
    ENTITIES("CREATE TABLE IF NOT EXISTS entities (x FLOAT NOT NULL, y INTEGER NOT NULL, z FLOAT NOT NULL, entity_type VARCHAR(32) NOT NULL, inventory TEXT);",
            "",
            ""),
    TEAMS("CREATE TABLE IF NOT EXISTS teams (name VARCHAR(16) NOT NULL UNIQUE, lifepool INTEGER DEFAULT 100, color CHAR(6) DEFAULT 'FFFFFF', loadouts TEXT, respawnPoints TEXT, flag TEXT);",
            "INSERT INTO teams (name) VALUES (?);",
            "SELECT * FROM teams"),
    // TODO: Возможно, стоит хранить настройки как JsonArray. Это экономно и удобно.
    PREFERENCES("CREATE TABLE IF NOT EXISTS preferences (world VARCHAR(64) NOT NULL, autoAssign BOOLEAN DEFAULT TRUE, friendlyFire BOOLEAN DEFAULT FALSE, deleteEmptyBottles BOOLEAN DEFAULT TRUE, flagParticles BOOLEAN DEFAULT TRUE, flagStealerTrails BOOLEAN DEFAULT TRUE, keepInventory BOOLEAN DEFAULT TRUE, colorfulEnding BOOLEAN DEFAULT TRUE);",
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