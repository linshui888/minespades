package me.nologic.minespades;

import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundEditor;
import me.nologic.minespades.battleground.BattlegroundLoader;

public class BattlegroundManager {

    private final BattlegroundEditor editor;
    private final BattlegroundLoader initializer;

    public BattlegroundManager(Minespades plugin) {
        this.editor = new BattlegroundEditor(plugin);
        this.initializer = new BattlegroundLoader(plugin);
    }

    public BattlegroundEditor getEditor() {
        return editor;
    }

    public Battleground load(String name) {
        return initializer.load(name);
    }

}