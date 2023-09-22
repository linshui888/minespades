package me.nologic.minespades.battleground.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.BattlegroundPreferences.Preference;
import me.nologic.minespades.battleground.editor.task.*;
import me.nologic.minespades.battleground.util.BattlegroundDataDriver;
import me.nologic.minespades.battleground.util.DatabaseTableHelper;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Translatable;
import me.nologic.minority.annotations.TranslationKey;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Translatable
public class BattlegroundEditor implements MinorityFeature, Listener {

    private final Minespades plugin;
    private final HashMap<Player, PlayerEditSession> sessions = new HashMap<>();

    @Getter @Setter
    private boolean saving;

    public BattlegroundEditor() {
        plugin = Minespades.getPlugin(Minespades.class);
        this.init(this, this.getClass(), plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @TranslationKey(section = "editor-info-messages", name = "battleground-created", value = "Battleground &3%s &rhas been successfully created.")
    private String battlegroundCreatedMessage;

    @TranslationKey(section = "editor-info-messages", name = "team-created", value = "Team &3%s &rhas been successfully created.")
    private String teamCreatedMessage;

    @TranslationKey(section = "editor-info-messages", name = "team-removed", value = "Team &3%s &rhas been successfully removed.")
    private String teamRemovedMessage;

    @TranslationKey(section = "editor-info-messages", name = "supply-removed", value = "Supply &3%s &rhas been successfully removed from loadout &3%s&r.")
    private String supplyRemovedMessage;

    @TranslationKey(section = "editor-info-messages", name = "respawn-point-created", value = "A new respawn point for team &3%s &rhas been successfully created. &8(%f, %f, %f)")
    private String respawnCreatedMessage;

    @TranslationKey(section = "editor-info-messages", name = "team-flag-removed", value = "Team &3%s &rflag has been removed.")
    private String teamFlagRemoved;

    @TranslationKey(section = "editor-info-messages", name = "neutral-flag-removed", value = "Neutral flag at %s has been removed.")
    private String neutralFlagRemoved;

    @TranslationKey(section = "editor-error-messages", name = "team-already-have-a-flag", value = "Error. Team &3%s &ralready have a flag.")
    private String teamFlagErrorMessage;

    @TranslationKey(section = "editor-error-messages", name = "saving-is-running", value = "Error. Wait for the volume to be saved.")
    private String savingIsRunningMessage;

    public PlayerEditSession editSession(final Player player) {
        if (sessions.get(player) == null) this.sessions.put(player, new PlayerEditSession(player));
        return sessions.get(player);
    }

    /**
     * Создание файла арены и первичная инициализация настроек.
     * */
    public void create(Player player, String battlegroundName) {
        BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(battlegroundName);

        // Создание таблиц
        for (DatabaseTableHelper table : DatabaseTableHelper.values()) {
            driver.executeUpdate(table.getCreateStatement());
        }

        // Инициализация параметров арены
        JsonObject parameters = new JsonObject();
        for (Preference preference : Preference.values()) parameters.addProperty(preference.toString(), preference.getDefaultValue());
        driver.executeUpdate("INSERT INTO preferences (world, parameters) VALUES (?, ?);", player.getWorld().getName(), parameters.toString());

        // TODO: Это вообще в другом месте должно быть.
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
        player.sendMessage(String.format(battlegroundCreatedMessage, battlegroundName));

        this.editSession(player).setTargetBattleground(battlegroundName);
        this.editSession(player).setActive(true);
        driver.closeConnection();
    }

    // Создание команды
    public void createTeam(Player player, String teamName) {
        // TODO: Использовать BattlegroundDataDriver вместо этой параши
        try (Connection connection = connect(this.editSession(player).getTargetBattleground()); PreparedStatement statement = connection.prepareStatement(DatabaseTableHelper.TEAMS.getInsertStatement())) {
            statement.setString(1, teamName);
            statement.executeUpdate();

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
            player.sendMessage(String.format(teamCreatedMessage, teamName));

            this.editSession(player).setTargetLoadout(null);
            this.setTargetTeam(player, teamName);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void removeTeam(Player player, String teamName) {
        final PlayerEditSession session = this.editSession(player);
        final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(session.getTargetBattleground());
        driver.executeUpdate("DELETE FROM teams WHERE name = ?;", teamName).closeConnection();
        if (Objects.equals(session.getTargetTeam(), teamName)) {
            session.setTargetLoadout(null);
            session.setTargetTeam(null);
        }
        player.sendMessage(String.format(teamRemovedMessage, teamName));
    }

    @SneakyThrows
    public void addSupply(Player player, String name, int interval, int amount, int maximum, String permission) {
        plugin.getServer().getScheduler().runTask(plugin, new SaveSupplyTask(player, name, interval, amount, maximum, permission));
    }

    public void addRespawnPoint(Player player) {
        String sql = "UPDATE teams SET respawnPoints = ? WHERE name = ?";
        try (Connection connection = connect(this.editSession(player).getTargetBattleground()); PreparedStatement statement = connection.prepareStatement(sql)) {

            Location l = player.getLocation();
            String encodedLocation = Base64Coder.encodeString(String.format("%f; %f; %f; %f; %f", l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));
            statement.setString(1, encodedLocation);
            statement.setString(2, this.editSession(player).getTargetTeam());
            statement.executeUpdate();

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
            player.sendMessage(String.format(respawnCreatedMessage, this.editSession(player).getTargetTeam(), l.getX(), l.getY(), l.getZ()));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @SneakyThrows
    private Connection connect(String battlegroundName) {
        String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/battlegrounds/" + battlegroundName + ".db";
        Connection conn = DriverManager.getConnection(url);
        Statement st = conn.createStatement();
        st.execute("PRAGMA journal_mode=OFF");
        st.execute("PRAGMA synchronous=OFF");
        st.close();
        return conn;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (!this.editSession(player).isVolumeEditor() || event.getClickedBlock() == null || event.getPlayer().getInventory().getItemInMainHand().getType() != Material.GOLDEN_SWORD || event.getHand() != EquipmentSlot.HAND) return;
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK -> this.editSession(player).getCorners()[0] = event.getClickedBlock().getLocation();
            case RIGHT_CLICK_BLOCK -> this.editSession(player).getCorners()[1] = event.getClickedBlock().getLocation();
        }
    }

    public void addLoadout(Player player, String name) {
        plugin.getServer().getScheduler().runTask(plugin, new SaveLoadoutTask(player, name));
    }

    public void removeLoadout(Player player, String name) {
        plugin.getServer().getScheduler().runTask(plugin, new RemoveLoadoutTask(player, name));
    }

    @SneakyThrows
    public boolean removeSupply(final Player player, final String targetSupply) {
        final PlayerEditSession session = plugin.getBattlegrounder().getEditor().editSession(player);
        final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(session.getTargetBattleground());
        try (final ResultSet result = driver.executeQuery("SELECT * FROM teams WHERE name = ?;", session.getTargetTeam())) {
            result.next(); final JsonArray loadouts = JsonParser.parseString(result.getString("loadouts")).getAsJsonArray();
            for (JsonElement loadoutElement : loadouts) {
                JsonObject jsonLoadout = loadoutElement.getAsJsonObject();
                String loadoutName = jsonLoadout.get("name").getAsString();
                if (loadoutName.equals(session.getTargetLoadout())) {
                    JsonArray supplies = jsonLoadout.get("supplies").getAsJsonArray();
                    JsonElement target = null;
                    for (JsonElement supplyElement : supplies) {
                        JsonObject supplyRule = supplyElement.getAsJsonObject();
                        String supplyName = supplyRule.get("name").getAsString();
                        if (supplyName.equals(targetSupply)) {
                            target = supplyElement;
                        }
                    }
                    if (target != null) {
                        supplies.remove(target);
                        driver.executeUpdate("UPDATE teams SET loadouts = ? WHERE name = ?;", loadouts.toString(), session.getTargetTeam());
                    }
                }
            }
        }
        driver.closeConnection();
        player.sendMessage(String.format(supplyRemovedMessage, targetSupply, session.getTargetTeam()));
        return false;
    }

    @SneakyThrows
    public void setTargetTeam(Player player, String teamName) {
        try (Connection connection = this.connect(this.editSession(player).getTargetBattleground())) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;");
            statement.setString(1, teamName);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                this.editSession(player).setTargetTeam(teamName);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0F);
            }
        }
    }

    @SneakyThrows
    public void saveVolume(final Player player) {
        if (!saving) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new SaveVolumeTask(editSession(player).getTargetBattleground(), player, this.editSession(player).getCorners()));
            this.editSession(player).setVolumeEditor(false);
        } else player.sendMessage(savingIsRunningMessage);
    }

    @SneakyThrows
    public void setTeamColor(Player player, String hexColor) {
        try (Connection connection = this.connect(this.editSession(player).getTargetBattleground())) {
            PreparedStatement statement = connection.prepareStatement("UPDATE teams SET color = ? WHERE name = ?;");
            statement.setString(1, hexColor);
            statement.setString(2, this.editSession(player).getTargetTeam());
            statement.executeUpdate();

            // Изменяем цвет команды в карте цветов
            colorPalette.get(this.editSession(player).getTargetBattleground()).put(this.editSession(player).getTargetTeam(), ChatColor.of("#" + hexColor));
        }
    }

    private final HashMap<String, HashMap<String, ChatColor>> colorPalette = new HashMap<>();
    public ChatColor getTeamColor(final String battleground, final String team) {
        return colorPalette.computeIfAbsent(battleground, k -> new HashMap<>()).computeIfAbsent(team, color -> {
            final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(battleground);
            try (final ResultSet result = driver.executeQuery("SELECT color FROM teams WHERE name = ?;", team)) {
                result.next();
                return ChatColor.of("#" + result.getString("color"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return ChatColor.of("#FFFFFF");
        });
    }

    public void setTargetTeamLifepool(final Player player, final int lifepool) {
        final PlayerEditSession session = this.editSession(player);
        final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(session.getTargetBattleground());
        driver.executeUpdate("UPDATE teams SET lifepool = ? WHERE name = ?;", lifepool, session.getTargetTeam());
        lifepoolData.get(session.getTargetBattleground()).put(session.getTargetTeam(), lifepool);
    }

    private final HashMap<String, HashMap<String, Integer>> lifepoolData = new HashMap<>();
    public int getTeamLifepool(final String battleground, final String team) {
        if (battleground == null) return 0;
        return lifepoolData.computeIfAbsent(battleground, k -> new HashMap<>()).computeIfAbsent(team, lifepool -> {
            final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(battleground);
            try (final ResultSet result = driver.executeQuery("SELECT lifepool FROM teams WHERE name = ?;", team)) {
                if (result.next()) {
                    return result.getInt("lifepool");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            driver.closeConnection();
            return 100;
        });
    }

    @SneakyThrows
    public void addTeamFlag(Player player) {
        try (Connection connection = connect(this.editSession(player).getTargetBattleground())) {
            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;");
            selectStatement.setString(1, this.editSession(player).getTargetTeam());
            ResultSet result = selectStatement.executeQuery(); result.next();

            if (result.getString("flag") != null) {
                player.sendMessage(String.format(teamFlagErrorMessage, this.editSession(player).getTargetTeam()));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, new AddTeamFlagTask(player));
        }
    }

    @SneakyThrows
    public void addNeutralFlag(Player player) {
        Bukkit.getScheduler().runTask(plugin, new AddNeutralFlagTask(player));
    }

    public void removeFlag(Player player, String targetTeam) {
        final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(this.editSession(player).getTargetBattleground());
        driver.executeUpdate("UPDATE teams SET flag = ? WHERE name = ?;", null, targetTeam).closeConnection();
        player.sendMessage(String.format(teamFlagRemoved, targetTeam));
    }

    public void removeNeutralFlag(Player player, final int x, final int y, final int z) {
        final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(this.editSession(player).getTargetBattleground());
        driver.executeUpdate("DELETE FROM objects WHERE x = ? AND y = ? AND z = ?;", x, y, z).closeConnection();
        player.sendMessage(String.format(neutralFlagRemoved, String.format("§ex§b%s§f, §ey§b%s§f, §ez§b%s§f", x, y, z)));
    }

    public List<PlayerEditSession> getEditSessionList() {
        return this.sessions.values().stream().toList();
    }

}