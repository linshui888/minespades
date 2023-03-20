package me.nologic.minespades;

import co.aikar.commands.PaperCommandManager;
import com.google.common.collect.ImmutableList;
import me.nologic.minespades.command.MinespadesCommand;

import org.bukkit.plugin.java.JavaPlugin;

public final class Minespades extends JavaPlugin {

    private BattlegroundManager bm;

    @Override
    public void onEnable() {
        this.bm = new BattlegroundManager(this);
        PaperCommandManager pcm = new PaperCommandManager(this);
        pcm.registerDependency(Minespades.class, this);
        pcm.registerCommand(new MinespadesCommand());
        pcm.getCommandCompletions().registerCompletion("battlegrounds", c -> ImmutableList.copyOf(getConfig().getStringList("Battlegrounds")));
    }

    public BattlegroundManager getBattlegroundManager() {
        return this.bm;
    }



}