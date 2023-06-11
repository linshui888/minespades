package me.nologic.minespades.battleground.editor;

import lombok.Getter;
import lombok.Setter;
import me.catcoder.sidebar.ProtocolSidebar;
import me.catcoder.sidebar.Sidebar;
import me.catcoder.sidebar.text.TextIterators;
import me.nologic.minespades.Minespades;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class PlayerEditSession {

    private final Player player;

    @Getter
    private boolean active = false;

    @Setter @Getter
    private String targetBattleground, targetTeam, targetLoadout;

    @Setter @Getter
    private boolean volumeEditor;

    @Getter
    private final Location[] corners = new Location[] { null, null };

    private final Sidebar<Component> sidebar;

    public PlayerEditSession(Player p) {
        this.player = p;

        this.sidebar = ProtocolSidebar.newAdventureSidebar(TextIterators.textFadeHypixel("Editor"), Minespades.getInstance());

        sidebar.addConditionalLine(player -> Component.text("Выбери арену для редактирования!")
                .color(NamedTextColor.WHITE), player -> targetBattleground == null);

        sidebar.addConditionalLine(player -> Component.text("§7Battleground: §3%s".formatted(targetBattleground))
                .color(NamedTextColor.WHITE), player -> targetBattleground != null);

        sidebar.addBlankLine().setDisplayCondition(player -> corners[0] != null || corners[1] != null);
        sidebar.addConditionalLine(player -> Component.text("§7Corner §3#1§7: " + this.stringifyLocation(corners[0])), player -> corners[0] != null);
        sidebar.addConditionalLine(player -> Component.text("§7Corner §3#2§7: " + this.stringifyLocation(corners[1])), player -> corners[1] != null);

        sidebar.addBlankLine().setDisplayCondition(player -> targetTeam != null);
        sidebar.addConditionalLine(player -> Component.text("§7Team: §3" + targetTeam), player -> targetTeam != null);
        sidebar.addConditionalLine(player -> Component.text("§7Loadout: §3" + targetLoadout), player -> targetLoadout != null);

        sidebar.updateLinesPeriodically(0, 5);
        sidebar.addViewer(player);

    }


    private String stringifyLocation(final @Nullable Location location) {
        if (location == null) return null;
        return String.format("§7x§3%s§7, y§3%s§7, z§3%s", location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public void displaySidebar() {
        sidebar.addViewer(player);
    }

    public void hideSidebar() {
        sidebar.removeViewer(player);
    }

    public void setActive(final boolean active) {
        this.active = active;
        if (active) displaySidebar();

        else {
            hideSidebar();
            this.volumeEditor = false;
        }
    }

    public boolean isBattlegroundSelected() {
        return targetBattleground != null;
    }

    public boolean isTeamSelected() {
        return targetTeam != null;
    }

    public boolean isLoadoutSelected() {
        return targetLoadout != null;
    }

}