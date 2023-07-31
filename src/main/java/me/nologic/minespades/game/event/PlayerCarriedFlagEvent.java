package me.nologic.minespades.game.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.game.object.BattlegroundFlag;
import me.nologic.minespades.game.object.TeamBattlegroundFlag;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor @Getter
public class PlayerCarriedFlagEvent extends Event {

    private final Battleground battleground;
    private final BattlegroundPlayer player;
    private final BattlegroundFlag flag;

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}