package me.nologic.minespades.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import lombok.RequiredArgsConstructor;
import me.nologic.minespades.BattlegroundManager;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.battleground.BattlegroundTeam;
import me.nologic.minespades.game.event.PlayerEnterBattlegroundEvent;
import me.nologic.minespades.game.event.PlayerQuitBattlegroundEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
@CommandAlias("minespades|ms")
@CommandPermission("minespades.player")
public class MinespadesCommand extends BaseCommand {

    private final BattlegroundManager battlegrounder;

    @Subcommand("launch")
    @CommandCompletion("@battlegrounds")
    @CommandPermission("minespades.editor")
    public void launch(Player player, String name) {
        battlegrounder.enable(name.toLowerCase());
    }

    @Subcommand("add")
    @CommandPermission("minespades.editor")
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

    @Subcommand("delete")
    @CommandPermission("minespades.editor")
    public class Delete extends BaseCommand {

        @Subcommand("loadout")
        public void onDeleteLoadout(Player player, String loadoutName) {
            battlegrounder.getEditor().removeLoadout(player, loadoutName);
        }

    }

    @Subcommand("list")
    public class List extends BaseCommand {

    }

    @Subcommand("create")
    @CommandPermission("minespades.editor")
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
    @CommandPermission("minespades.editor")
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

        @Subcommand("color")
        public void onEditColor(Player player, String hexColor) {
            battlegrounder.getEditor().setTeamColor(player, hexColor);
        }

    }

    @Subcommand("save")
    @CommandPermission("minespades.editor")
    public void onSave(Player player) {
        player.sendMessage("§7Сохранение карты займёт какое-то время.");
        battlegrounder.getEditor().saveVolume(player);
    }

    @Subcommand("join")
    @CommandCompletion("@battlegrounds")
    public void onJoin(Player player, String name) {
        try {
            name = name.toLowerCase();
            Battleground battleground = battlegrounder.getBattlegroundByName(name);
            if (battleground.havePlayer(player)) return;
            BattlegroundTeam team = battleground.getSmallestTeam();
            Bukkit.getServer().getPluginManager().callEvent(new PlayerEnterBattlegroundEvent(battleground, team, player));
        } catch (NullPointerException ex) {
            player.sendMessage("§4Ошибка. Несуществующая арена: " + name + ".");
        }
    }

    @Subcommand("quit|leave")
    public void onQuit(Player player) {
        BattlegroundPlayer bgPlayer = Minespades.getPlugin(Minespades.class).getGameMaster().getPlayerManager().getBattlegroundPlayer(player);
        if (bgPlayer != null)
            Bukkit.getServer().getPluginManager().callEvent(new PlayerQuitBattlegroundEvent(bgPlayer.getBattleground(), bgPlayer.getTeam(), player));
        else player.sendMessage("§4Бананы из ушей вытащи!");
    }

}