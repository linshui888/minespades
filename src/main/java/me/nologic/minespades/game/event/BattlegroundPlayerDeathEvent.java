package me.nologic.minespades.game.event;

import lombok.Getter;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class BattlegroundPlayerDeathEvent extends Event {

    private final Minespades         plugin = Minespades.getPlugin(Minespades.class);

    private final Battleground       battleground;
    private final BattlegroundPlayer victim, killer;
    private final boolean            keepInventory;
    private final RespawnMethod      respawnMethod;

    // игрока убил другой игрок
    public BattlegroundPlayerDeathEvent( Player player, Player killer, boolean keepInventory, RespawnMethod respawnMethod) {
        this.victim = plugin.getGameMaster().getPlayerManager().getBattlegroundPlayer(player);
        this.killer = plugin.getGameMaster().getPlayerManager().getBattlegroundPlayer(killer);
        this.battleground = victim.getBattleground();
        this.keepInventory = keepInventory;
        this.respawnMethod = respawnMethod;
    }

    // игрок умер не от другого игрока
    public BattlegroundPlayerDeathEvent(Player player, boolean keepInventory, RespawnMethod quick) {
        this.victim = plugin.getGameMaster().getPlayerManager().getBattlegroundPlayer(player);
        this.killer = null;
        this.battleground = victim.getBattleground();
        this.keepInventory = keepInventory;
        this.respawnMethod = quick;

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