package me.nologic.minespades.game;

import me.nologic.minespades.battleground.Battleground;

import java.util.List;

public class GameMaster {

    private List<Battleground> workingBattlegrounds;

    public void addBattleground(Battleground battleground) {
        this.workingBattlegrounds.add(battleground);
        battleground.setLaunched(true);
    }

}