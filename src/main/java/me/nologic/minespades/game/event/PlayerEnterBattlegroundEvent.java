package me.nologic.minespades.game.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor @Getter
public class PlayerEnterBattlegroundEvent extends Event {

    private final Battleground battleground;
    private final Team         team;
    private final Player       player;

    /* Nothing to see here. */

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
