package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.game.event.PlayerEnterBattlegroundEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class Multiground {

    private final Minespades plugin = Minespades.getPlugin(Minespades.class);
    private final List<String> battlegrounds = new ArrayList<>();

    @Getter
    private final String name;

    @Getter
    private Battleground battleground;

    public void connect(Player player, @NotNull BattlegroundTeam team) {
        Bukkit.getServer().getPluginManager().callEvent(new PlayerEnterBattlegroundEvent(battleground, team, player));
    }

    public void add(String battlegroundName) {
        this.battlegrounds.add(battlegroundName);
    }

    // Запуск следующей арены. Если индекс текущей арены == размер - 1, то значит арена последняя в списке.
    public void launchNextInOrder() {
        int index = battlegrounds.indexOf(battleground.getBattlegroundName());
        if (index < battlegrounds.size() - 1) this.battleground = plugin.getBattlegrounder().enable(battlegrounds.get(index + 1), this);
        else this.battleground = plugin.getBattlegrounder().enable(battlegrounds.get(0), this);
    }

    public void launchRandomly() {
        this.battleground = plugin.getBattlegrounder().enable(battlegrounds.get((int) (Math.random() * battlegrounds.size())), this);
    }

}