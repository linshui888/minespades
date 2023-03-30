package me.nologic.minespades.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Subcommand;
import lombok.RequiredArgsConstructor;
import me.nologic.minespades.BattlegroundManager;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.Team;
import me.nologic.minespades.game.event.PlayerEnterBattlegroundEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
@CommandAlias("minespades|ms")
public class MinespadesCommand extends BaseCommand {

    private final BattlegroundManager battlegrounder;

    @Subcommand("launch")
    @CommandCompletion("@battlegrounds")
    public void launch(Player player, String name) {
        battlegrounder.enable(name.toLowerCase());
    }

    @Subcommand("add")
    public class Add extends BaseCommand {

        @Subcommand("respawn")
        public void onAddRespawn(Player player) {
            if (battlegrounder.getEditor().isTeamSelected(player)) {
                battlegrounder.getEditor().addRespawnPoint(player);
            } else player.sendMessage("§4Ошибка. Не выбрана команда для редактирования.");
        }

        @Subcommand("loadout")
        public void onAddLoadout(Player player, String name) {
            if (battlegrounder.getEditor().isTeamSelected(player)) {
                battlegrounder.getEditor().addLoadout(player, name);
            } else player.sendMessage("§4Ошибка. Не выбрана команда для редактирования.");
        }

    }

    @Subcommand("list")
    public class List extends BaseCommand {

    }

    @Subcommand("create")
    public class Create extends BaseCommand {

        @Subcommand("battleground")
        public void onCreateBattleground(Player player, String name) {
            name = name.toLowerCase();
            if (battlegrounder.isBattlegroundExist(name))  {
                player.sendMessage(String.format("§4Ошибка. Арена с названием %s уже существует.", name));
                return;
            }
            battlegrounder.getEditor().create(player, name);
        }

        @Subcommand("team")
        public void onCreateTeam(Player player, String teamName) {
            battlegrounder.getEditor().createTeam(player, teamName);
        }

    }

    @Subcommand("edit")
    public class Edit extends BaseCommand {

        @Subcommand("battleground")
        public void onEditBattleground(Player player, String name) {
            name = name.toLowerCase();
            if (battlegrounder.isBattlegroundExist(name)) {
                battlegrounder.getEditor().setTargetBattleground(player, name);
                player.sendMessage(Component.text(String.format("Арена %s успешно выбрана для редактирования.", name)).color(TextColor.color(155, 197, 90)));
            } else player.sendMessage("§4Ошибка. Несуществующая арена: " + name + ".");
        }

        @Subcommand("volume")
        public void onEditBattlegroundVolume(Player player) {
            battlegrounder.getEditor().setVolumeEditor(player);
        }

        @Subcommand("team")
        public void onEditTeam(Player player, String teamName) {
            battlegrounder.getEditor().setTargetTeam(player, teamName);
        }

    }

    @Subcommand("save")
    public void onSave(Player player) {
        player.sendMessage("§7Сохранение карты займёт какое-то время.");
        new Thread(() -> battlegrounder.getEditor().saveVolume(player)).start();
    }

    @Subcommand("join")
    @CommandCompletion("@battlegrounds")
    public void onJoin(Player player, String name) {
        try {
            name = name.toLowerCase();
            Battleground battleground = battlegrounder.getBattlegroundByName(name);
            Team team = battleground.getSmallestTeam();
            Bukkit.getServer().getPluginManager().callEvent(new PlayerEnterBattlegroundEvent(battleground, team, player));
        } catch (NullPointerException ex) {
            player.sendMessage("§4Ошибка. Несуществующая арена: " + name + ".");
        }

    }

}