package me.nologic.minespades.battleground.editor;

import lombok.Getter;
import lombok.Setter;
import me.catcoder.sidebar.ProtocolSidebar;
import me.catcoder.sidebar.Sidebar;
import me.catcoder.sidebar.text.TextIterators;
import me.nologic.minespades.Minespades;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Translatable;
import me.nologic.minority.annotations.TranslationKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Translatable
public class PlayerEditSession implements MinorityFeature {

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

    @TranslationKey(section = "editor-sidebar", name = "label", value = "Battleground Editor")
    private String editorSidebarLabel;

    @TranslationKey(section = "editor-sidebar", name = "select-battleground", value = "§cSelect battleground to edit!")
    private String selectBattlegroundMessage;

    @TranslationKey(section = "editor-sidebar", name = "battleground", value = "§7Battleground: §6§l%s §8[%s§8]")
    private String battleground;

    @TranslationKey(section = "editor-sidebar", name = "team", value = "§7Team: §3%s")
    private String team;

    @TranslationKey(section = "editor-sidebar", name = "loadout", value = "§7Loadout: §3%s")
    private String loadout;

    @TranslationKey(section = "editor-sidebar", name = "corner", value = "§7Corner")
    private String corner;

    public PlayerEditSession(Player p) {

        Minespades.getInstance().getConfigurationWizard().generate(this.getClass());
        this.init(this, this.getClass(), Minespades.getInstance());

        this.player = p;
        this.sidebar = ProtocolSidebar.newAdventureSidebar(TextIterators.textFadeHypixel(editorSidebarLabel == null ? "Editor" : editorSidebarLabel), Minespades.getInstance());

        sidebar.addConditionalLine(player -> Component.text(selectBattlegroundMessage)
                .color(NamedTextColor.WHITE), player -> targetBattleground == null);

        sidebar.addConditionalLine(player -> Component.text(String.format(battleground, targetBattleground, this.validationMark()))
                .color(NamedTextColor.WHITE), player -> targetBattleground != null);

        sidebar.addBlankLine().setDisplayCondition(player -> corners[0] != null || corners[1] != null);
        sidebar.addConditionalLine(player -> Component.text(corner + " §3#1§7: " + this.stringifyLocation(corners[0])), player -> corners[0] != null);
        sidebar.addConditionalLine(player -> Component.text(corner + " §3#2§7: " + this.stringifyLocation(corners[1])), player -> corners[1] != null);

        sidebar.addBlankLine().setDisplayCondition(player -> targetTeam != null);
        sidebar.addConditionalLine(player -> Component.text(String.format(team, targetTeam)), player -> targetTeam != null);
        sidebar.addConditionalLine(player -> Component.text(String.format(loadout, targetLoadout)), player -> targetLoadout != null);

        sidebar.updateLinesPeriodically(0, 5);
    }

    private String validationMark() {
        if (Minespades.getInstance().getBattlegrounder().getEditor().isSaving()) return "§e♻";
        final boolean valid = Minespades.getInstance().getBattlegrounder().getValidator().isValid(targetBattleground);
        if (valid) return "§2✔";
        else return "§4✘";
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
            this.targetBattleground = null;
            this.targetTeam = null;
            this.targetLoadout = null;
            this.resetCorners();
        }
    }

    public void resetCorners() {
        this.corners[0] = null;
        this.corners[1] = null;
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