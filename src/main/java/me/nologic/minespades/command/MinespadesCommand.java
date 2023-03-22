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

    @Subcommand("launch")
    @CommandCompletion("@battlegrounds")
    public void launch(Player player, String battlegroundName) {
        plugin.getBattlegroundManager().enable(battlegroundName);
    }

    @Subcommand("add")
    public class Add extends BaseCommand {

        @Subcommand("respawn")
        public void onAddRespawn(Player player) {
            BattlegroundEditor editor = plugin.getBattlegroundManager().getEditor();
            editor.addRespawnPoint(player);
        }

    }

    @Subcommand("create")
    public class Create extends BaseCommand {

        @Subcommand("battleground")
        public void onCreateBattleground(Player player, String battlegroundName) {
            plugin.getBattlegroundManager().getEditor().create(player, battlegroundName);
        }

        @Subcommand("team")
        public void onCreateTeam(Player player, String teamName) {
            plugin.getBattlegroundManager().getEditor().createTeam(player, teamName);
        }

    }

    @Subcommand("edit")
    public class Edit extends BaseCommand {

        @Subcommand("battleground")
        public class Battleground extends BaseCommand {

            @Subcommand("volume")
            public void onEditBattlegroundVolume(Player player) {
                plugin.getBattlegroundManager().getEditor().addVolumeEditor(player);
            }

            @Subcommand("team")
            public void onEditTeam(Player player, String teamName) {
                plugin.getBattlegroundManager().getEditor().setTargetTeam(player, teamName);
            }

        }

    }

    @Subcommand("save")
    public void onSave(Player player) {
        player.sendMessage("§7Сохранение карты займёт какое-то время.");
        new Thread(() -> plugin.getBattlegroundManager().getEditor().saveVolume(player)).start();
    }

    @Subcommand("join")
    @CommandCompletion("@battlegrounds")
    public void onJoin(Player player, String battlegroundName) {
        plugin.getGameMaster().connect(player, battlegroundName);
    }

}