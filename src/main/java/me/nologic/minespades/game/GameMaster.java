package me.nologic.minespades.game;

import me.nologic.minespades.battleground.Battleground;

import java.util.List;

public class GameMaster {

    /** Рабочие арены. */
    private List<Battleground> battlegrounds;

    /** Запуск арены. */
    public void launch(Battleground battleground) {
        this.battlegrounds.add(battleground);
    }

}
