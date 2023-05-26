package me.nologic.minespades.bot.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.nologic.minespades.bot.BattlegroundBot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor @Getter
public class BotAnswerEvent extends Event {

    private final BattlegroundBot battlegroundBot;
    private final String answerData;

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}