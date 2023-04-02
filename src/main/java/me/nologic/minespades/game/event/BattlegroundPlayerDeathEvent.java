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
public class BattlegroundPlayerDeathEvent extends Event {

    private final Battleground battleground;
    private final Player       player, killer;
    private final Team         team;

    private final boolean      keepInventory;
    private final RespawnMethod respawnMethod;

    public BattlegroundPlayerDeathEvent(Battleground battleground, Player player, Team team, boolean keepInventory, RespawnMethod respawnMethod) {
        this.battleground = battleground;
        this.player = player;
        this.killer = null;
        this.team = team;
        this.keepInventory = keepInventory;
        this.respawnMethod = respawnMethod;
    }

    public enum RespawnMethod {
        NORMAL, AOS, QUICK;
    }

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}