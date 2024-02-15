package me.nologic.castlewars.game.event;

import lombok.Getter;
import me.nologic.castlewars.CastleWars;
import me.nologic.castlewars.battleground.Battleground;
import me.nologic.castlewars.battleground.BattlegroundPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class BattlegroundPlayerDeathEvent extends Event {

    private final CastleWars plugin = CastleWars.getPlugin(CastleWars.class);

    private final Battleground       battleground;
    private final BattlegroundPlayer victim;

    private final boolean            keepInventory;
    private final RespawnStrategy    respawnStrategy;

    private final EntityDamageEvent.DamageCause damageCause;

    public BattlegroundPlayerDeathEvent(BattlegroundPlayer victim, EntityDamageEvent.DamageCause damageCause, boolean keepInventory, RespawnStrategy respawnMethod) {
        this.victim          = victim;
        this.damageCause     = damageCause;
        this.battleground    = victim.getBattleground();
        this.keepInventory   = keepInventory;
        this.respawnStrategy = respawnMethod;
    }

    public enum RespawnStrategy {
        NORMAL, AOS, QUICK
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