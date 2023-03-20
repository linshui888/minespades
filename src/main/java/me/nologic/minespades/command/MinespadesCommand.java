package me.nologic.minespades.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.BattlegroundEditor;
import org.bukkit.entity.Player;

@CommandAlias("minespades|ms")
public class MinespadesCommand extends BaseCommand {

    private final Minespades plugin;

    public MinespadesCommand(Minespades plugin) {
        this.plugin = plugin;
    }

    @Subcommand("create")
    public class Create extends BaseCommand {

        @Subcommand("battleground|arena") @CommandCompletion("<name>")
        public void onCreateBattleground(String name) {
            plugin.getBattlegroundManager().getEditor().create(name);
            plugin.getBattlegroundManager().load(name);
        }

        @Subcommand("team")
        public void onCreateTeam(Player player, @Optional String battlegroundName, String name) {

        }

    }

    @Subcommand("update")
    public class Update extends BaseCommand {

        @Subcommand("battleground|arena")
        public class Battleground extends BaseCommand {

            @Subcommand("volume")
            public void onUpdateBattlegroundVolume(Player player, String battlegroundName) {
                plugin.getBattlegroundManager().getEditor().addVolumeEditor(player, battlegroundName);
            }

        }

    }

    @Subcommand("save")
    public void onSave(Player player) {
        BattlegroundEditor editor = plugin.getBattlegroundManager().getEditor();
        editor.updateVolume(player);
        editor.removeVolumeEditor(player);
    }

}