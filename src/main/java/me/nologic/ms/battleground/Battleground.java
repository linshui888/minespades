package me.nologic.ms.battleground;

import org.bukkit.entity.Player;

import java.util.List;

// Короче, большой класс с кучей переменных
// Сам класс есть инстанция арены на которой могут играть игроки, то есть есть методы join/quit/restart ну и игровые параметры вроде лайфпула, команд, (возможно) времени до конца, топа игроков и т. п.
public class Battleground {

    // Settings содержит в себе название и прочую статическую информацию, которая хранится в конфигах и загружается при инициализации
    private BattlegroundSettings settings;

    // Volume содержит в себе блоки карты, которые хранятся в датабазе
    private BattlegroundVolume volume;

    private List<Team> teams;

    public void connect(Player player, Team target) {
        if (settings.automaticAssignation) { // Если включено автматическое распределение, то игрок попадёт в команду с наименьшим кол-вом игроков
            Team smallest = null;
            for (Team team : teams)
                if (smallest == null) smallest = team;
                else if (smallest.getPlayers().size() > team.getPlayers().size()) smallest = team;
            if (smallest != null)
                smallest.join(player);
        } else {
            target.join(player);
        }
    }


}
