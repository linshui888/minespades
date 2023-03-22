package me.nologic.minespades;

import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundEditor;
import me.nologic.minespades.battleground.BattlegroundLoader;

public class BattlegroundManager {

    private final BattlegroundEditor editor;
    private final BattlegroundLoader loader;

    public BattlegroundManager (Minespades plugin) {
        this.editor = new BattlegroundEditor(plugin);
        this.loader = new BattlegroundLoader(plugin);
    }

    public BattlegroundEditor getEditor() {
        return editor;
    }

    public Battleground enable(String name) {
        return loader.load(name);
    }

}