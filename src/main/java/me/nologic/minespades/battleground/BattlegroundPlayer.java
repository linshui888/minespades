package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.Setter;
import me.catcoder.sidebar.ProtocolSidebar;
import me.catcoder.sidebar.Sidebar;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.editor.loadout.BattlegroundLoadout;
import me.nologic.minespades.game.flag.BattlegroundFlag;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Translatable;
import me.nologic.minority.annotations.TranslationKey;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Translatable
public class BattlegroundPlayer implements MinorityFeature {

    private @Getter final Battleground       battleground;
    private @Getter final BattlegroundTeam   team;
    private @Getter final Player             bukkitPlayer;

    private @Getter BattlegroundLoadout      loadout;

    private @Setter @Getter int              kills, deaths, assists;
    private @Setter @Getter boolean          carryingFlag;
    private @Setter @Getter BattlegroundFlag flag;

    private Sidebar<Component> sidebar;

    public BattlegroundPlayer(final Battleground battleground, final BattlegroundTeam team, final Player player) {
        this.battleground = battleground;
        this.team         = team;
        this.bukkitPlayer = player;
        Minespades.getInstance().getConfigurationWizard().generate(this.getClass());
        this.init(this, this.getClass(), Minespades.getInstance());
    }

    @TranslationKey(section = "battleground-sidebar", name = "label", value = "Minespades")
    private String sidebarLabel;

    @TranslationKey(section = "battleground-sidebar", name = "KDA", value = "K/D/A:")
    private String sidebarKDALabel;

    @TranslationKey(section = "battleground-sidebar", name = "team", value = "Team")
    private String playerTeamLabel;

    @TranslationKey(section = "battleground-sidebar", name = "lifepool", value = "Lifepool")
    private String lifepoolLabel;

    @TranslationKey(section = "battleground-sidebar", name = "map", value = "Map")
    private String mapLabel;

    public void showSidebar() {
        this.sidebar = ProtocolSidebar.newAdventureSidebar(Component.text(sidebarLabel == null ? "Minespades" : sidebarLabel), Minespades.getPlugin(Minespades.class));
        sidebar.addUpdatableLine(player -> Component.text(sidebarKDALabel + " " + kills + "/" + deaths + "/" + assists));
        sidebar.addBlankLine();

        sidebar.addLine(Component.text(playerTeamLabel + " ").append(team.getDisplayName()));
        sidebar.addUpdatableLine(player -> Component.text(lifepoolLabel + " " + team.getLifepool()));

        sidebar.addBlankLine();
        sidebar.addLine(Component.text(mapLabel + " " + battleground.getBattlegroundName()));

        sidebar.updateLinesPeriodically(0, 10);
        sidebar.addViewer(bukkitPlayer);
    }

    public void removeSidebar() {
        this.sidebar.removeViewer(bukkitPlayer);
    }

    public void setRandomLoadout() {
        loadout = team.getLoadouts().get((int) (Math.random() * team.getLoadouts().size()));
        bukkitPlayer.getInventory().setContents(loadout.getInventory().getContents());
    }

    public static BattlegroundPlayer getBattlegroundPlayer(Player player) {
        return Minespades.getPlugin(Minespades.class).getGameMaster().getPlayerManager().getBattlegroundPlayer(player);
    }

    public void disconnect() {
        Minespades.getInstance().getGameMaster().getPlayerManager().disconnect(this);
    }

}