package me.nologic.minespades;

import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundEditor;
import me.nologic.minespades.battleground.BattlegroundLoader;
import me.nologic.minespades.game.event.BattlegroundPlayerDeathEvent;

import java.util.HashMap;
import java.util.List;

public class BattlegroundManager {

    private final HashMap<String, Battleground> enabledBattlegrounds;

    private final BattlegroundEditor editor;
    private final BattlegroundLoader loader;

    public BattlegroundManager (Minespades plugin) {
        this.editor = new BattlegroundEditor(plugin);
        this.loader = new BattlegroundLoader(plugin);
        this.enabledBattlegrounds = new HashMap<>();
    }

    public BattlegroundEditor getEditor() {
        return editor;
    }

    public List<String> getEnabledBattlegrounds() {
        return enabledBattlegrounds.keySet().stream().toList();
    }

    public void enable(String name) {
        Battleground battleground = loader.load(name);
        this.enabledBattlegrounds.put(battleground.getBattlegroundName(), battleground);
        battleground.setLaunched(true);
    }

}