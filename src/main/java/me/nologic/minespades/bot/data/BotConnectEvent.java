package me.nologic.minespades.bot.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.net.Socket;

@RequiredArgsConstructor @Getter
public class BotConnectEvent extends Event {

    private final Socket socket;

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}