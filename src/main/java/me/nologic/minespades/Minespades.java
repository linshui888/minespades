package me.nologic.minespades;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import me.nologic.minespades.command.MinespadesCommand;
import me.nologic.minespades.game.EventDrivenGameMaster;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@Getter
public final class Minespades extends JavaPlugin {

    private EventDrivenGameMaster gameMaster;
    private BattlegroundManager battlegrounder;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        File maps = new File(super.getDataFolder() + "/battlegrounds");
        if (!maps.exists()) {
            if (maps.mkdir()) {
                getLogger().info("Minespades хранит карты в виде одной датабазы, см. папку battlegrounds.");
            }
        }
        this.battlegrounder = new BattlegroundManager(this);
        this.gameMaster = new EventDrivenGameMaster();
        PaperCommandManager pcm = new PaperCommandManager(this);
        pcm.registerCommand(new MinespadesCommand(this.battlegrounder));
        pcm.getCommandCompletions().registerCompletion("battlegrounds", c -> battlegrounder.getEnabledBattlegrounds());
        getServer().getPluginManager().registerEvents(gameMaster, this);
        this.enableBattlegrounds();
    }

    private void enableBattlegrounds() {
        Bukkit.getLogger().info("Minespades пытается автоматически загрузить сохранённые арены.");
        getConfig().getStringList("Battlegrounds").forEach(name -> {
            getLogger().info("Плагин пытается загрузить " + name + "...");
            battlegrounder.enable(name);
        });
    }

}