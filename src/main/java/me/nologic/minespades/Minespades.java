package me.nologic.minespades;

import lombok.Getter;
import co.aikar.commands.PaperCommandManager;

import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.Multiground;
import me.nologic.minespades.command.MinespadesCommand;
import me.nologic.minespades.game.EventDrivenGameMaster;
import me.nologic.minespades.util.MinespadesPlaceholderExpansion;
import me.nologic.minority.MinorityExtension;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Translatable;
import me.nologic.minority.annotations.TranslationKey;

import java.io.File;
import java.util.Random;

@Getter @Translatable
public final class Minespades extends MinorityExtension implements MinorityFeature {

    @Getter
    private static Minespades instance;

    @Getter
    private Random random;

    private EventDrivenGameMaster gameMaster;
    private BattlegroundManager   battlegrounder;
    private PaperCommandManager   commandManager;

    @TranslationKey(section = "log-info-messages", name = "battleground-folder-created", value = "Minespades created a battlegrounds folder.")
    private String battlegroundFolderMessage;

    @TranslationKey(section = "log-info-messages", name = "battleground-load", value = "Minespades is trying to load battleground %s.")
    private String battlegroundLoadMessage;

    @TranslationKey(section = "log-info-messages", name = "plugin-disable", value = "Minespades will be disabled. All running battlegrounds will be disabled.")
    private String minespadesDisableMessage;

    @Override
    public void onEnable() {
        instance = this;
        random = new Random();
        super.getConfigurationWizard().generate(this.getClass());
        this.init(this, this.getClass(), this);

        File maps = new File(super.getDataFolder() + "/battlegrounds");
        if (!maps.exists()) {
            if (maps.mkdir()) {
                super.getLogger().info(battlegroundFolderMessage);
            }
        }

        this.battlegrounder = new BattlegroundManager(this);
        this.gameMaster = new EventDrivenGameMaster();
        this.commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new MinespadesCommand(this));
        getServer().getPluginManager().registerEvents(gameMaster, this);
        this.enableBattlegrounds();

        if (super.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null)
            new MinespadesPlaceholderExpansion().register();
    }

    // TODO: Необходимо починить загрузку арен на старте.
    private void enableBattlegrounds() {
        super.getConfig().getStringList("battlegrounds").forEach(name -> {

            // FIXME: почини меня, дай мне свежесть
            super.getLogger().info(String.format(battlegroundLoadMessage, name));
            battlegrounder.enable(name);
        });
    }

    @Override
    public void onDisable() {

        super.getLogger().info(minespadesDisableMessage);

        for (Multiground multiground : battlegrounder.getMultigrounds()) {
            battlegrounder.disable(multiground.getBattleground());
        }

        for (Battleground battleground : battlegrounder.getLoadedBattlegrounds()) {
            battlegrounder.disable(battleground);
        }

    }

}