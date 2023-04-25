package me.nologic.minespades.command;

import me.nologic.minespades.BattlegroundManager;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.BattlegroundTeam;
import me.nologic.minespades.battleground.editor.loadout.Loadout;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * CommandCompletions является утилитарным классом, который содержит все необходимые методы для удобного
 * автоматического завершения команд.
 */
public class CommandCompletions {

    private final Minespades plugin = Minespades.getPlugin(Minespades.class);
    private final BattlegroundManager battlegrounder = plugin.getBattlegrounder();

    // Возвращает список названий запущенных арен.
    public List<String> getEnabledBattlegroundNames() {
        List<String> battlegroundNames = new ArrayList<>();
        battlegrounder.getEnabledBattlegrounds().forEach(b -> battlegroundNames.add(b.getBattlegroundName()));
        return battlegroundNames;
    }

    // Возвращает список лоадаутов команды, редактируемой игроком в данный момент.
    public List<String> getTargetTeamLoadoutNames(Player player) {
        List<String> loadoutNames = new ArrayList<>();

        String targetTeamName = battlegrounder.getEditor().getTargetTeam(player);
        if (targetTeamName == null) return null;

        BattlegroundTeam team = battlegrounder.getBattlegroundByName(battlegrounder.getEditor().getTargetBattleground(player)).getTeamByName(targetTeamName);
        if (team == null) return null;

        for (Loadout loadout : team.getLoadouts()) {
            loadoutNames.add(loadout.getName());
        }

        return loadoutNames;
    }
}
