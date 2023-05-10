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

    private Sidebar<Component> sidebar;

    public void showSidebar() {
        this.sidebar = ProtocolSidebar.newAdventureSidebar(Component.text("Minespades"), Minespades.getPlugin(Minespades.class));
        sidebar.addUpdatableLine(player -> Component.text("K/D/A: " + kills + "/" + deaths + "/" + assists));
        sidebar.addBlankLine();
        // Команда и лайфпул
        sidebar.addLine(Component.text("Команда ").append(team.getDisplayName()));
        sidebar.addUpdatableLine(player -> Component.text("Лайфпул: " + team.getLifepool()));
        // Пустая линия
        sidebar.addBlankLine();
        // Название карты
        sidebar.addLine(Component.text("Карта " + battleground.getBattlegroundName()));
        // Обновляем все линии каждые 10 тиков, то есть 2 раза в секунду
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

}