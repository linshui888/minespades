package me.nologic.minespades;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.battleground.BattlegroundTeam;
import me.nologic.minespades.battleground.Multiground;
import me.nologic.minespades.battleground.editor.BattlegroundEditor;
import me.nologic.minespades.battleground.editor.PlayerEditSession;
import me.nologic.minespades.battleground.editor.task.AddTeamFlagTask;
import me.nologic.minespades.battleground.util.BattlegroundValidator;
import me.nologic.minespades.command.MinespadesCommand;
import me.nologic.minespades.game.EventDrivenGameMaster;
import me.nologic.minespades.game.PlayerKDAHandler;
import me.nologic.minespades.util.MinespadesPlaceholderExpansion;
import me.nologic.minority.MinorityExtension;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Translatable;
import me.nologic.minority.annotations.TranslationKey;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Random;

@Getter @Translatable
public final class Minespades extends MinorityExtension implements MinorityFeature {

    @Getter
    private boolean disabling = false;

    @Getter
    private static Minespades instance;

    private BukkitAudiences adventureAPI;
    private final Random random = new Random();

    private EventDrivenGameMaster gameMaster;
    private BattlegroundManager   battlegrounder;
    private PaperCommandManager   commandManager;

    @TranslationKey(section = "log-info-messages", name = "battleground-folder-created", value = "Minespades created a battlegrounds folder.")
    private String battlegroundFolderMessage;

    @TranslationKey(section = "log-info-messages", name = "battleground-load", value = "Minespades is trying to load battleground %s.")
    private String battlegroundLoadMessage;

    @TranslationKey(section = "log-info-messages", name = "plugin-disable", value = "Minespades will be disabled. All running battlegrounds will be stopped.")
    private String minespadesDisableMessage;

    @Override
    public void onLoad() {
        super.getConfigurationWizard().generate(this.getClass());
        super.getConfigurationWizard().generate(EventDrivenGameMaster.class);
        super.getConfigurationWizard().generate(MinespadesCommand.class);
        super.getConfigurationWizard().generate(PlayerEditSession.class);
        super.getConfigurationWizard().generate(BattlegroundPlayer.class);
        super.getConfigurationWizard().generate(BattlegroundEditor.class);
        super.getConfigurationWizard().generate(BattlegroundValidator.class);
        super.getConfigurationWizard().generate(AddTeamFlagTask.class);
        super.getConfigurationWizard().generate(PlayerKDAHandler.class);
        super.getConfigurationWizard().generate(BattlegroundTeam.class);
    }

    @Override
    public void onEnable() {
        instance = this;
        this.adventureAPI = BukkitAudiences.create(this);

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

        if (super.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            final MinespadesPlaceholderExpansion mpe = new MinespadesPlaceholderExpansion();
            if (!mpe.isRegistered()) mpe.register();
        }
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

        this.disabling = true;
        super.getLogger().info(minespadesDisableMessage);

        for (final PlayerEditSession session : battlegrounder.getEditor().getEditSessionList()) {
            session.setActive(false);
        }

        for (Multiground multiground : battlegrounder.getMultigrounds()) {
            battlegrounder.disable(multiground.getBattleground());
        }

        for (Battleground battleground : battlegrounder.getLoadedBattlegrounds()) {
            battlegrounder.disable(battleground);
        }

        if (this.adventureAPI != null) {
            this.adventureAPI.close();
            this.adventureAPI = null;
        }

    }

    public void broadcast(final Component message) {
        for (Player p : getServer().getOnlinePlayers()) {
            adventureAPI.player(p).sendMessage(message);
        }
    }

}