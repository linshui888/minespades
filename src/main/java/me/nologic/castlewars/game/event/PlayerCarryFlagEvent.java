package me.nologic.castlewars.game.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.nologic.castlewars.battleground.Battleground;
import me.nologic.castlewars.battleground.BattlegroundPlayer;
import me.nologic.castlewars.game.object.base.BattlegroundFlag;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor @Getter
public class PlayerCarryFlagEvent extends Event {

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