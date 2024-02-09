package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import me.catcoder.sidebar.ProtocolSidebar;
import me.catcoder.sidebar.Sidebar;
import me.clip.placeholderapi.PlaceholderAPI;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.editor.loadout.BattlegroundLoadout;
import me.nologic.minespades.game.object.base.BattlegroundFlag;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.*;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Configurable(path = "battleground-sidebar") @Translatable
public class BattlegroundPlayer implements MinorityFeature {

    private @Getter final Battleground       battleground;
    private @Getter final BattlegroundTeam   team;
    private @Getter final Player             bukkitPlayer;

    private @Getter BattlegroundLoadout      loadout;

    private @Setter @Getter int              kills, deaths, assists;
    private @Setter @Getter boolean          carryingFlag;
    private @Setter @Getter BattlegroundFlag flag;

    private Sidebar<BaseComponent[]> sidebar;

    public BattlegroundPlayer(final Battleground battleground, final BattlegroundTeam team, final Player player) {
        this.battleground = battleground;
        this.team         = team;
        this.bukkitPlayer = player;
        Minespades.getInstance().getConfigurationWizard().generate(this.getClass());
        this.init(this, this.getClass(), Minespades.getInstance());
    }

    @ConfigurationKey(name = "enabled", type = Type.BOOLEAN, value = "true")
    private boolean enabled;

    @ConfigurationKey(name = "update-period", type = Type.INTEGER, value = "10", comment = "Ticks between each sidebar update")
    private int updateTicks;

    @ConfigurationKey(name = "label", value = "#fcd617Minespades")
    private String sidebarLabel;

    @ConfigurationKey(name = "lines", type = Type.LIST_OF_STRINGS, comment = "Battleground sidebar lines, use placeholders to provide information.", value = {
            "K/D/A: &c%minespades_player_current_kill_score%/%minespades_player_current_death_score%/%minespades_player_current_assist_score%",
            "",
            "Team %minespades_player_current_team%",
            "Lifepool %minespades_player_current_lifepool%",
            "",
            "Map %minespades_player_current_map%" })
    private List<String> sidebarLines;

    public void showSidebar() {
        this.sidebar = ProtocolSidebar.newBungeeChatSidebar(TextComponent.fromLegacyText(sidebarLabel), Minespades.getPlugin(Minespades.class));
        for (final String string : sidebarLines) {
            if (string.isBlank()) {
                sidebar.addBlankLine();
            } else {
                sidebar.addUpdatableLine(player -> TextComponent.fromLegacyText(PlaceholderAPI.setPlaceholders(bukkitPlayer, string)));
            }
        }
        sidebar.updateLinesPeriodically(0, updateTicks);
        sidebar.addViewer(bukkitPlayer);
    }

    public void removeSidebar() {
        this.sidebar.removeViewer(bukkitPlayer);
    }

    public void setRandomLoadout() {
        loadout = team.getLoadouts().get((int) (Math.random() * team.getLoadouts().size()));
        bukkitPlayer.getInventory().setContents(loadout.getInventory().getContents());
    }

    @Nullable
    public static BattlegroundPlayer getBattlegroundPlayer(Player player) {
        return Minespades.getPlugin(Minespades.class).getGameMaster().getPlayerManager().getBattlegroundPlayer(player);
    }

    public String getDisplayName() {
        return team.getColor() + bukkitPlayer.getName();
    }

    public void disconnect() {
        Minespades.getInstance().getGameMaster().getPlayerManager().disconnect(this);
    }

    public void addKills(final int score) {
        this.kills += score;
    }

}