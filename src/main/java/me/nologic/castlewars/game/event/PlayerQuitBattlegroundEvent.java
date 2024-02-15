package me.nologic.castlewars.game.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.nologic.castlewars.battleground.Battleground;
import me.nologic.castlewars.battleground.BattlegroundTeam;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor @Getter
public class PlayerQuitBattlegroundEvent extends Event {

    private final Battleground battleground;
    private final BattlegroundTeam team;
    private final Player player;

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}