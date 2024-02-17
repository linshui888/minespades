package me.nologic.castlewars.battleground.editor;

import lombok.Getter;
import lombok.Setter;
import me.catcoder.sidebar.ProtocolSidebar;
import me.catcoder.sidebar.Sidebar;
import me.catcoder.sidebar.text.TextIterators;
import me.nologic.castlewars.CastleWars;
import me.nologic.castlewars.battleground.Battleground;
import me.nologic.castlewars.battleground.BattlegroundPreferences;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Translatable;
import me.nologic.minority.annotations.TranslationKey;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
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

    /* BattlegroundSnippet is basically not launched battleground instance. */
    @Setter
    private Battleground battlegroundSnippet;

    @Setter @Getter
    private boolean volumeEditor;

    @Getter
    private final Location[] corners = new Location[] { null, null };

    private final Sidebar<BaseComponent[]> sidebar;

    @TranslationKey(section = "editor-sidebar", name = "label", value = "Battleground Editor")
    private String editorSidebarLabel;

    @TranslationKey(section = "editor-sidebar", name = "select-battleground", value = "&cSelect battleground to edit!")
    private String selectBattlegroundMessage;

    @TranslationKey(section = "editor-sidebar", name = "battleground", value = "&7Battleground: &b&l%s &8[%s&8]")
    private String battleground;

    @TranslationKey(section = "editor-sidebar", name = "team", value = "&7Team: ")
    private String team;

    @TranslationKey(section = "editor-sidebar", name = "lifepool", value = "&8╚ &7Lifepool: &e%s")
    private String lifepool;

    @TranslationKey(section = "editor-sidebar", name = "loadout", value = "&7Loadout: &3%s")
    private String loadout;

    @TranslationKey(section = "editor-sidebar", name = "corner", value = "&7Corner")
    private String corner;

    public PlayerEditSession(Player p) {

        CastleWars.getInstance().getConfigurationWizard().generate(this.getClass());
        this.init(this, this.getClass(), CastleWars.getInstance());

        this.player = p;
        this.sidebar = ProtocolSidebar.newBungeeChatSidebar(TextIterators.textFadeHypixel(editorSidebarLabel == null ? "Editor" : editorSidebarLabel), CastleWars.getInstance());

        sidebar.addConditionalLine(player -> TextComponent.fromLegacyText(selectBattlegroundMessage), player -> targetBattleground == null);
        sidebar.addConditionalLine(player -> TextComponent.fromLegacyText((String.format(battleground, targetBattleground, this.validationMark()))), k -> targetBattleground != null);

        sidebar.addBlankLine().setDisplayCondition(player -> corners[0] != null || corners[1] != null);
        sidebar.addConditionalLine(player -> TextComponent.fromLegacyText(corner + " §3#1§7: " + this.stringifyLocation(corners[0])), player -> corners[0] != null);
        sidebar.addConditionalLine(player -> TextComponent.fromLegacyText(corner + " §3#2§7: " + this.stringifyLocation(corners[1])), player -> corners[1] != null);

        // Team
        sidebar.addBlankLine().setDisplayCondition(player -> targetTeam != null);
        sidebar.addConditionalLine(player -> TextComponent.fromLegacyText(team + this.getColoredTeam() + " §7≡ " + this.flagState()), player -> targetTeam != null);
        sidebar.addConditionalLine(player -> TextComponent.fromLegacyText(String.format(lifepool, battlegroundSnippet.getPreference(BattlegroundPreferences.Preference.TEAM_LIFEPOOL).getAsInteger())), player -> targetTeam != null);

        sidebar.addBlankLine().setDisplayCondition(player -> targetLoadout != null);
        sidebar.addConditionalLine(player -> TextComponent.fromLegacyText(String.format(loadout, targetLoadout)), player -> targetLoadout != null);

        sidebar.updateLinesPeriodically(0, 10);
    }

    private String validationMark() {
        if (CastleWars.getInstance().getBattlegrounder().getEditor().isSaving()) return "§e♻";
        final boolean valid = CastleWars.getInstance().getBattlegrounder().getValidator().isValid(targetBattleground);
        if (valid) return "§2✔";
        else return "§4✘";
    }

    // TODO: Брать значение не из датабазы. Карта или просто переменная, что угодно. Но не из ДБ.
    private String flagState() {
        if (CastleWars.getInstance().getBattlegrounder().getEditor().isSaving()) return "§e♻";
        if (targetTeam != null && CastleWars.getInstance().getBattlegrounder().getValidator().isTeamHaveFlag(targetBattleground, targetTeam)) return "§2⚑";
        else return "§4§m⚑";
    }

    private String getColoredTeam() {
        if (targetTeam == null) {
            return "";
        } else return CastleWars.getInstance().getBattlegrounder().getEditor().getTeamColor(targetBattleground, targetTeam) + targetTeam;
    }

    private String stringifyLocation(final @Nullable Location location) {
        if (location == null) return null;
        return String.format("§7x§b%s§7, y§b%s§7, z§b%s", location.getBlockX(), location.getBlockY(), location.getBlockZ());
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
        return targetBattleground == null;
    }

    public boolean isTeamSelected() {
        return targetTeam == null;
    }

    public boolean isLoadoutSelected() {
        return targetLoadout == null;
    }

}