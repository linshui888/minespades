package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import me.catcoder.sidebar.ProtocolSidebar;
import me.catcoder.sidebar.Sidebar;
import me.catcoder.sidebar.text.TextIterators;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.editor.loadout.BattlegroundLoadout;
import me.nologic.minespades.game.flag.BattlegroundFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class BattlegroundPlayer {

    private @Getter final Battleground       battleground;
    private @Getter final BattlegroundTeam   team;
    private @Getter final Player             bukkitPlayer;

    private @Getter BattlegroundLoadout      loadout;

    private @Setter @Getter int              kills, deaths, assists;
    private @Setter @Getter boolean          carryingFlag;
    private @Setter @Getter BattlegroundFlag flag;

    public void showSidebar() {
        Sidebar<Component> sidebar = ProtocolSidebar.newAdventureSidebar(TextIterators.textFadeHypixel("SIDEBAR"), Minespades.getPlugin(Minespades.class));
        // let's add some lines
        sidebar.addLine(Component.text("Just a static line").color(NamedTextColor.GREEN));

        // add an empty line
        sidebar.addBlankLine();// also you can add updatable lines which applies to all players

        sidebar.addUpdatableLine(
                player -> Component.text("покушать-то: ")
                        .append(Component.text(player.getFoodLevel())
                                .color(NamedTextColor.GREEN))
        );

        sidebar.addBlankLine();
        sidebar.addUpdatableLine(
                player -> Component.text("не стукай: ")
                        .append(Component.text(player.getHealth())
                                .color(NamedTextColor.GREEN))
        );
        sidebar.addBlankLine();

        // update all lines except static ones every 10 ticks
        sidebar.updateLinesPeriodically(0, 10);

        // show to the player
        sidebar.addViewer(bukkitPlayer);
    }

    public void setRandomLoadout() {
        loadout = team.getLoadouts().get((int) (Math.random() * team.getLoadouts().size()));
        bukkitPlayer.getInventory().setContents(loadout.getInventory().getContents());
    }

    public static BattlegroundPlayer getBattlegroundPlayer(Player player) {
        return Minespades.getPlugin(Minespades.class).getGameMaster().getPlayerManager().getBattlegroundPlayer(player);
    }

}