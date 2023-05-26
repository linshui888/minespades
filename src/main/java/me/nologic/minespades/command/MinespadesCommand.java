package me.nologic.minespades.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import lombok.SneakyThrows;
import me.nologic.minespades.BattlegroundManager;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.*;
import me.nologic.minespades.battleground.BattlegroundPreferences.Preference;
import me.nologic.minespades.battleground.util.BattlegroundValidator;
import me.nologic.minespades.bot.BattlegroundBot;
import me.nologic.minespades.game.event.BattlegroundGameOverEvent;
import me.nologic.minespades.game.event.PlayerEnterBattlegroundEvent;
import me.nologic.minespades.game.event.PlayerQuitBattlegroundEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.List;

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
        plugin.getCommandManager().getCommandCompletions().registerCompletion("enabledBattlegrounds", c -> completions.getEnabledBattlegrounds());
        plugin.getCommandManager().getCommandCompletions().registerCompletion("battlegrounds", c -> completions.getBattlegroundFileList());
        plugin.getCommandManager().getCommandCompletions().registerCompletion("loadouts", c -> completions.getTargetTeamLoadouts(c.getPlayer()));
        plugin.getCommandManager().getCommandCompletions().registerCompletion("battlegroundPreferences", c -> completions.getBattlegroundPreferences());
        plugin.getCommandManager().getCommandCompletions().registerCompletion("battlegroundTeamsOnJoin", c -> completions.getBattlegroundTeamsOnJoinCommand(c.getPlayer(), c.getContextValue(String.class, 1)));
    }

    @Subcommand("launch")
    @CommandCompletion("@battlegrounds")
    @CommandPermission("minespades.editor")
    public void onLaunch(Player player, String name) {
        if (!battlegrounder.enable(name.toLowerCase())) {
            player.sendMessage("§4Ошибка. Арена настроена как часть мультиграунда, её нельзя запустить напрямую.");
        }
    }

    // Test commands
    @Subcommand("forcegameover")
    @CommandPermission("minespades.editor")
    public void onForceGameOver(Player player) {
        BattlegroundPlayer bgPlayer = BattlegroundPlayer.getBattlegroundPlayer(player);
        if (bgPlayer != null) {
            Bukkit.getServer().getPluginManager().callEvent(new BattlegroundGameOverEvent(bgPlayer.getBattleground()));
        }
    }

    @Subcommand("bot")
    @CommandPermission("minespader.editor")
    public void onBot(Player player) {
        BattlegroundPlayer bgPlayer = BattlegroundPlayer.getBattlegroundPlayer(player);
        if (bgPlayer != null) {
            BattlegroundBot bot = new BattlegroundBot(bgPlayer.getBattleground());
        }
    }


    @Subcommand("config")
    @CommandCompletion("@battlegroundPreferences")
    @CommandPermission("minespades.editor")
    public void onConfig(Player player, String preference, boolean value) {
        Battleground battleground = battlegrounder.getBattlegroundByName(battlegrounder.getEditor().getTargetBattleground(player));

        if (battleground == null) {
            player.sendMessage("§4Ошибка. Не выбрана арена для редактирования.");
            player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT_CLOSED, 1F, 0F);
            return;
        }

        if (Preference.isValid(preference)) {
            battleground.getPreferences().set(Preference.valueOf(preference), value);
            player.sendMessage(String.format("§2Успех. §7Параметр §6%s §7теперь равняется §3%s§7.", preference, value));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
        }
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


        // TODO: добавить проверку на нуллики
        @Subcommand("loadout")
        @CommandCompletion("loadouts")
        public void onDeleteLoadout(Player player, String loadoutName) {
            battlegrounder.getEditor().removeLoadout(player, loadoutName);
        }

    }

    @Subcommand("multiground")
    @CommandPermission("minespades.editor")
    public class MultigroundCommand extends BaseCommand {

        @Subcommand("create") @SneakyThrows
        public void onMultigroundCreate(Player player, String multigroundName) {
            multigroundName = multigroundName.toLowerCase();
            File path = new File(plugin.getDataFolder(), "multigrounds.yml");
            if (path.createNewFile()) plugin.getLogger().info("Creating multigrounds.yml...");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path);
            if (yaml.getConfigurationSection(multigroundName) == null) {
                yaml.createSection(multigroundName);
                yaml.save(path);
                player.sendMessage(String.format("Мультиграунд %s успешно создан. Теперь нужно добавть в него арены. Используйте /ms multiground add <название>", multigroundName));
            } else player.sendMessage("Мультиграунд с таким названием уже существует.");
        }

        @Subcommand("add") @SneakyThrows
        public void onMultigroundAdd(Player player, String multigroundName, String battlegroundName) {
            File path = new File(plugin.getDataFolder(), "multigrounds.yml");
            if (path.createNewFile()) plugin.getLogger().info("Creating multigrounds.yml...");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path);
            ConfigurationSection section = yaml.getConfigurationSection(multigroundName);
            if (section == null) {
                player.sendMessage("Несуществующий мультиграунд.");
                return;
            }
            if (BattlegroundValidator.isValid(battlegroundName)) {
                List<String> battlegrounds = section.getStringList("battlegrounds");
                battlegrounds.add(battlegroundName);
                section.set("battlegrounds", battlegrounds);
                yaml.save(path);
                player.sendMessage(String.format("Арена %s успешно добавлена в мультиграунд %s.", battlegroundName, multigroundName));
            } else player.sendMessage(String.format("Арена %s невалидна.", battlegroundName));
        }

        @Subcommand("remove") @SneakyThrows
        public void onMultigroundRemove(Player player, String multigroundName, String battlegroundName) {
            File path = new File(plugin.getDataFolder(), "multigrounds.yml");
            if (path.createNewFile()) plugin.getLogger().info("Creating multigrounds.yml...");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path);
            ConfigurationSection section = yaml.getConfigurationSection(multigroundName);
            if (section == null) {
                player.sendMessage("Несуществующий мультиграунд.");
                return;
            }
            if (section.getStringList("battlegrounds").contains(battlegroundName)) {
                List<String> battlegrounds = section.getStringList("battlegrounds");
                battlegrounds.remove(battlegroundName);
                section.set("battlegrounds", battlegrounds);
                yaml.save(path);
                player.sendMessage(String.format("Арена %s успешно удалена из мультиграунда %s.", battlegroundName, multigroundName));
            } else player.sendMessage("В мультиграунде нет такой арены.");
        }

        @Subcommand("launch") @SneakyThrows
        public void onMultigroundLaunch(Player player, String multigroundName) {
            File path = new File(plugin.getDataFolder(), "multigrounds.yml");
            if (path.createNewFile()) plugin.getLogger().info("Creating multigrounds.yml...");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path);
            ConfigurationSection section = yaml.getConfigurationSection(multigroundName);
            if (section != null) {
                if (plugin.getBattlegrounder().getMultigrounds().stream().noneMatch(m -> m.getName().equals(multigroundName))) {
                    plugin.getBattlegrounder().launchMultiground(multigroundName, section.getStringList("battlegrounds"));
                } else player.sendMessage(String.format("Ошибка. Мультиграунд %s уже запущен.", multigroundName));
            } else player.sendMessage("Несуществующий мультиграунд.");
        }

        // TODO: launchOnStart

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

        @Subcommand("volume")
        public void onEditBattlegroundVolume(Player player) {
            battlegrounder.getEditor().setAsVolumeEditor(player);
        }

        @Subcommand("team")
        public void onEditTeam(Player player, String teamName) {
            if (battlegrounder.getEditor().getTargetBattleground(player) == null) {
                player.sendMessage("§4Ошибка. Не выбрана арена для редактирования.");
                return;
            }
            battlegrounder.getEditor().setTargetTeam(player, teamName);
        }

        @Subcommand("color")
        public void onEditColor(Player player, String hexColor) {
            if (battlegrounder.getEditor().getTargetBattleground(player) == null) {
                player.sendMessage("§4Ошибка. Не выбрана арена для редактирования.");
                return;
            }
            if (battlegrounder.getEditor().getTargetTeam(player) == null) {
                player.sendMessage("§4Ошибка. Не выбрана команда для редактирования.");
                return;
            }
            battlegrounder.getEditor().setTeamColor(player, hexColor);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
            player.sendMessage(Component.text(String.format("Новый цвет команды %s: ", battlegrounder.getEditor().getTargetTeam(player)))
                    .append(Component.text(hexColor).color(TextColor.fromHexString("#" + hexColor))));
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
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
        }

    }

    @Subcommand("save")
    @CommandPermission("minespades.editor")
    public void onSave(Player player) {
        player.sendMessage("§7Сохранение карты займёт какое-то время.");
        battlegrounder.getEditor().saveVolume(player);
    }

    @Subcommand("join")
    @CommandCompletion("@enabledBattlegrounds @battlegroundTeamsOnJoin")
    public void onJoin(Player player, String battlegroundName, @Optional String targetTeamName) {
        battlegroundName = battlegroundName.toLowerCase();

        // Проверка на мультиграунд
        Multiground multiground = battlegrounder.getMultiground(battlegroundName);
        if (multiground != null) {
            BattlegroundTeam targetTeam = multiground.getBattleground().getTeamByName(targetTeamName);
            Battleground battleground = multiground.getBattleground();
            multiground.connect(player, targetTeam != null && !battleground.getPreference(Preference.FORCE_AUTO_ASSIGN) ? targetTeam : battleground.getSmallestTeam());
            return;
        }

        Battleground battleground = battlegrounder.getBattlegroundByName(battlegroundName);
        if (battleground == null) {
            player.sendMessage("§4Ошибка. Несуществующая арена: " + battlegroundName + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT_CLOSED, 1F, 1F);
            return;
        }

        if (battleground.getPreferences().get(BattlegroundPreferences.Preference.IS_MULTIGROUND)) {
            player.sendMessage("§4Ошибка. К этой арене нельзя подключиться напрямую.");
            player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT_CLOSED, 1F, 1F);
            return;
        }

        // Если targetTeam != null, то подключаем плеера к указанной команде
        // Если включено принудительное автораспределение, то кидаем игрока в команду с наименьшим кол-вом игроков
        BattlegroundTeam team = targetTeamName != null && !battleground.getPreference(Preference.FORCE_AUTO_ASSIGN) ? battleground.getTeamByName(targetTeamName) : battleground.getSmallestTeam();
        plugin.getGameMaster().getPlayerManager().connect(player, battleground, team);
    }

    @Subcommand("quit|leave|q")
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
        Battleground battleground = battlegrounder.getBattlegroundByName(name);

        if (battleground == null) {
            player.sendMessage("§4Ошибка. Несуществующая арена: " + name + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT_CLOSED, 1F, 0F);
            return;
        }

        if (battleground.getPreference(Preference.IS_MULTIGROUND)) {
            player.sendMessage("§4Ошибка. Арена управляется мультиграундом.");
            player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT_CLOSED, 1F, 0F);
            return;
        }

        battleground.broadcast(String.format("§7Арена §6%s §7принудительно перезагружена игроком §73%s§7.", StringUtils.capitalise(name), player.getName()));
        battleground.getPlayers().stream().toList().forEach(bgPlayer -> Bukkit.getServer().getPluginManager().callEvent(new PlayerQuitBattlegroundEvent(bgPlayer.getBattleground(), bgPlayer.getTeam(), player)));
        battlegrounder.reset(battleground);
    }

}