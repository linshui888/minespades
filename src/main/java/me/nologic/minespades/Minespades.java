package me.nologic.minespades;

import co.aikar.commands.PaperCommandManager;
import me.nologic.minespades.command.MinespadesCommand;
import me.nologic.minespades.game.EventDrivenGameMaster;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Minespades extends JavaPlugin {

    private EventDrivenGameMaster gameMaster;
    private BattlegroundManager battlegrounder;

    @Override
    public void onEnable() {
        super.saveDefaultConfig();
        this.battlegrounder = new BattlegroundManager(this);
        this.gameMaster = new EventDrivenGameMaster();
        PaperCommandManager pcm = new PaperCommandManager(this);
        pcm.registerCommand(new MinespadesCommand(this));
        pcm.getCommandCompletions().registerCompletion("battlegrounds", c -> battlegrounder.getEnabledBattlegrounds());
        getServer().getPluginManager().registerEvents(gameMaster, this);
        this.enableBattlegrounds();
    }

    public BattlegroundManager getBattlegroundManager() {
        return this.battlegrounder;
    }

    public EventDrivenGameMaster getGameMaster() {
        return this.gameMaster;
    }

    private void enableBattlegrounds() {
        Bukkit.getLogger().info("Minespades пытается автоматически загрузить сохранённые арены.");
        getConfig().getStringList("Battlegrounds").forEach(name -> battlegrounder.enable(name));
    }

}