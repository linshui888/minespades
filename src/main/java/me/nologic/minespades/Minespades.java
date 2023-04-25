package me.nologic.minespades;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import me.nologic.minespades.command.MinespadesCommand;
import me.nologic.minespades.game.EventDrivenGameMaster;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
public final class Minespades extends JavaPlugin {

    private EventDrivenGameMaster gameMaster;
    private BattlegroundManager battlegrounder;

    private PaperCommandManager commandManager;

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
        this.commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new MinespadesCommand());
        getServer().getPluginManager().registerEvents(gameMaster, this);
        this.enableBattlegrounds();
    }

    private void enableBattlegrounds() {
        Bukkit.getLogger().info("Minespades пытается автоматически загрузить сохранённые арены.");
        getConfig().getStringList("Battlegrounds").forEach(name -> {

            // FIXME: почини меня, дай мне свежесть
            getLogger().info("Плагин пытается загрузить " + name + "...");
            battlegrounder.enable(name);
        });
    }

    @Override
    public void onDisable() {
        battlegrounder.getEnabledBattlegrounds().forEach(battleground -> {
            battleground.getPlayers().forEach(battleground::kick);
            battleground.getPlayers().clear();
        });
    }

}