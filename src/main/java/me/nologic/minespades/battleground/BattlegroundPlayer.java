package me.nologic.minespades.battleground;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class BattlegroundPlayer {

    private @Getter final Battleground battleground;
    private @Getter final Team         team;
    private @Getter final Player       player;

    private @Setter @Getter int kills, deaths, assists;

}