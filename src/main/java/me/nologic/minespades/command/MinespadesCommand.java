package me.nologic.minespades.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
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
import org.codehaus.plexus.util.StringUtils;

@CommandAlias("minespades|ms")
@CommandPermission("minespades.player")
public class MinespadesCommand extends BaseCommand {

    private final Minespades          plugin;
    private final CommandCompletions  completions;
    private final BattlegroundManager battlegrounder;

    {
        this.plugin = Minespades.getPlugin(Minespades.class);
        this.battlegrounder = plugin.getBattlegrounder();
        this.completions = new CommandCompletions();
        this.registerCompletions();
    }

    private void registerCompletions() {
        plugin.getCommandManager().getCommandCompletions().registerCompletion("battlegrounds", c -> completions.getEnabledBattlegroundNames());
        plugin.getCommandManager().getCommandCompletions().registerCompletion("loadouts", c -> completions.getTargetTeamLoadoutNames(c.getPlayer()));
    }

    @Subcommand("launch")
    @CommandCompletion("@battlegrounds")
    @CommandPermission("minespades.editor")
    public void onLaunch(Player player, String name) {
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

        @Subcommand("supply")
        public void onAddSupply(Player player, String name, int interval, int amount, int maximum, String permission) {
            if (!battlegrounder.getEditor().isTeamSelected(player)) {
                player.sendMessage("§4Ошибка. Не выбрана команда для редактирования.");
                return;
            }

            if (battlegrounder.getEditor().getTargetLoadout(player) == null) {
                player.sendMessage("§4Ошибка. Для редактирования не выбран набор экипировки.");
                return;
            }

            battlegrounder.getEditor().addSupply(player, name, interval, amount, maximum, permission);
        }

        @Subcommand("flag")
        public void onAddFlag(Player player) {
            if (!player.getInventory().getItemInMainHand().getType().toString().toLowerCase().contains("banner")) {
                player.sendMessage("§4Ошибка. Возьмите в руки флаг.");
                return;
            }

            battlegrounder.getEditor().addFlag(player);
        }

    }

    @Subcommand("delete")
    @CommandPermission("minespades.editor")
    public class Delete extends BaseCommand {

        @Subcommand("loadout")
        @CommandCompletion("loadouts")
        public void onDeleteLoadout(Player player, String loadoutName) {
            battlegrounder.getEditor().removeLoadout(player, loadoutName);
        }

    }

    @Subcommand("list")
    public class List extends BaseCommand {
        // TODO
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
                player.sendMessage(Component.text(String.format("Арена %s успешно выбрана для редактирования.", StringUtils.capitalise(name))).color(TextColor.color(197, 184, 41)));
            } else player.sendMessage("§4Ошибка. Несуществующая арена: " + name + ".");
        }

        // TODO: add logback

        @Subcommand("volume")
        public void onEditBattlegroundVolume(Player player) {
            battlegrounder.getEditor().setAsVolumeEditor(player);
        }

        @Subcommand("team")
        public void onEditTeam(Player player, String teamName) {
            battlegrounder.getEditor().setTargetTeam(player, teamName);
        }

        @Subcommand("color")
        public void onEditColor(Player player, String hexColor) {
            battlegrounder.getEditor().setTeamColor(player, hexColor);
        }

        @Subcommand("loadout")
        @CommandCompletion("@loadouts")
        public void onEditLoadout(Player player, String name) {

            if (battlegrounder.getEditor().getTargetTeam(player) == null) {
                player.sendMessage("§4Ошибка. Не выбрана команда для редактирования.");
                return;
            }

            if (!battlegrounder.getEditor().isLoadoutExist(player, name)) {
                player.sendMessage(String.format("§4Ошибка. Набор экипировки с названием %s у команды %s не найден.", name, battlegrounder.getEditor().getTargetTeam(player)));
                return;
            }

            battlegrounder.getEditor().setTargetLoadout(player, name);
            player.sendMessage(String.format("§2Успех. Редактируемый набор экипировки: %s.", name));
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

    @Subcommand("reset")
    @CommandCompletion("@battlegrounds")
    @CommandPermission("minespades.editor")
    public void onForceReset(Player player, String name) {
        try {
            Battleground battleground = battlegrounder.getBattlegroundByName(name);
            battleground.broadcast(Component.text(String.format("Арена %s принудительно перезагружена игроком %s.", StringUtils.capitalise(name), player.getName())).color(TextColor.color(187, 166, 96)));
            battleground.getPlayers().stream().toList().forEach(bgPlayer -> Bukkit.getServer().getPluginManager().callEvent(new PlayerQuitBattlegroundEvent(bgPlayer.getBattleground(), bgPlayer.getTeam(), player)));
            battlegrounder.reset(battleground);
        } catch (NullPointerException ex) {
            player.sendMessage("§4Ошибка. Несуществующая арена: " + name + ".");
        }
    }

}