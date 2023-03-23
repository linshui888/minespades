package me.nologic.minespades.game.event;

import me.nologic.minespades.battleground.Battleground;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerEnterBattlegroundEvent extends Event {

    private Battleground battleground;
    private Player       player;

    public Battleground getBattleground() {
        return battleground;
    }

    public Player getPlayer() {
        return player;
    }

    /* Nothing to see here. */

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

}
