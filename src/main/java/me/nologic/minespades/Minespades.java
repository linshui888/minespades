package me.nologic.minespades;

import co.aikar.commands.PaperCommandManager;
import me.nologic.minespades.command.MinespadesCommand;
import me.nologic.minespades.game.GameMaster;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Minespades extends JavaPlugin {

    private GameMaster gameMaster;
    private BattlegroundManager battlegrounder;

    @Override
    public void onEnable() {
        super.saveDefaultConfig();
        this.battlegrounder = new BattlegroundManager(this);
        this.gameMaster = new GameMaster();
        PaperCommandManager pcm = new PaperCommandManager(this);
        pcm.registerCommand(new MinespadesCommand(this));
        pcm.getCommandCompletions().registerCompletion("battlegrounds", c -> getConfig().getStringList("Battlegrounds"));
        this.enableBattlegrounds();
    }

    public BattlegroundManager getBattlegroundManager() {
        return this.battlegrounder;
    }

    private void enableBattlegrounds() {
        Bukkit.getLogger().info("Minespades пытается автоматически загрузить сохранённые арены.");
        getConfig().getStringList("Battlegrounds").forEach(name -> {
            gameMaster.addBattleground(battlegrounder.enable(name));
        });
    }

}