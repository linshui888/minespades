package me.nologic.minespades.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.BattlegroundEditor;
import org.bukkit.entity.Player;

@CommandAlias("minespades|ms")
public class MinespadesCommand extends BaseCommand {

    @Dependency("Minespades")
    private Minespades plugin;

    @Subcommand("create")
    public class Create {

        @Subcommand("battleground|arena") @Syntax("ШЛОМО ХОТЕТЬ КУШАТЬ") @CommandCompletion("foobar")
        public void onCreateBattleground(String name) {
            plugin.getBattlegroundManager().getEditor().create(name);
            plugin.getBattlegroundManager().load(name);
        }

        @Subcommand("team")
        public void onCreateTeam(Player player, @Optional String battlegroundName, String name) {

        }

    }

    @Subcommand("update")
    public class Update {

        @Subcommand("battleground|arena")
        public class Battleground {

            @Subcommand("volume")
            public void onUpdateBattlegroundVolume(Player player, String battlegroundName) {
                plugin.getBattlegroundManager().getEditor().addVolumeEditor(player, battlegroundName);
                player.sendMessage("Вы вошли в режим редактирования карты. Взяв в руки золотой меч, выделите кубоид, после чего напишите /ms save, чтобы сохранить карту.");
            }

        }

    }

    @Subcommand("save")
    public void onSave(Player player) {
        BattlegroundEditor editor = plugin.getBattlegroundManager().getEditor();
        editor.updateVolume(player);
        editor.removeVolumeEditor(player);
        player.sendMessage("Обновление карты завершено. (Действительно ли?)");
    }

}