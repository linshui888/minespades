package me.nologic.ms.battleground;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class Battleground {

    private List<Team> teams;

    private boolean launched = false;

    private boolean autoAssign, allowFriendlyFire, maxTeamSize, keepInventory, useCorpses, skipDeathScreen, colorfulEnding, airStrike;

    private World world;

    /** Подготовка арены к использованию. Инициализация настроек, команд, загрузка энтитей (пока не реализоавноо). */
    public void prepare(BattlegroundData data) throws SQLException {
        this.init(data.getData(BattlegroundData.Table.PREFERENCES));
        this.construct(data.getData(BattlegroundData.Table.VOLUME));
    }

    private void init(ResultSet settings) throws SQLException {
        this.world = Bukkit.getWorld(settings.getString("world"));
    }

    private void construct(ResultSet blocks) throws SQLException {
        while(blocks.next()) {
            int x = blocks.getInt("x"), y = blocks.getInt("y"), z = blocks.getInt("z");
            Material material = Material.valueOf(blocks.getString("material"));
            this.world.setType(x, y, z, material);
        }
    }

    private void addTeams(ResultSet teams) throws SQLException {
        while (teams.next()) {
            Team team = new Team(teams.getString("name"), teams.getInt("lifepool"), teams.getString("color"));



        }
    }

    public String getDescription() {
        return "ДАЙ КАКУЛЮ";
    }

    // TODO: сохранение инвентаря игрока перед подключением, обязательно в дб, дабы игроки не проёбывали вещи
    public void connect(Player player, Team target) {
        if (!launched) return;
        if (autoAssign) {
            Team smallestTeam = null;
            for (Team team : teams)
                if (smallestTeam == null) smallestTeam = team;
                else if (smallestTeam.getPlayers().size() > team.getPlayers().size()) smallestTeam = team;
            if (smallestTeam != null)
                smallestTeam.connect(player);
        } else {
            target.connect(player);
        }
    }

    public void disconnect(Player player) {

    }


}