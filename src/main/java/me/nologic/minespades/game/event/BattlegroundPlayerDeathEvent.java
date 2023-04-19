package me.nologic.minespades.game.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.battleground.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class BattlegroundPlayerDeathEvent extends Event {

    private final Battleground battleground;
    private final Player       player;
    private final Team         team;

    private BattlegroundPlayer victim, killer;

    private final boolean      keepInventory;
    private final RespawnMethod respawnMethod;

    // игрока убил другой игрок
    public BattlegroundPlayerDeathEvent(Battleground battleground, Player player, Player killer, boolean keepInventory, RespawnMethod respawnMethod) {
        this.battleground = battleground;
        this.player = player;
        this.team = battleground.getPlayerTeam(player);
        this.keepInventory = keepInventory;
        this.respawnMethod = respawnMethod;
        this.victim = new BattlegroundPlayer(battleground, battleground.getPlayerTeam(player), player); // FIXME
        this.killer = new BattlegroundPlayer(battleground, battleground.getPlayerTeam(killer), killer); // FIXME
    }

    // игрок умер не от другого игрока
    public BattlegroundPlayerDeathEvent(Battleground battleground, Player player, Team team, boolean b, RespawnMethod quick) {
        this.battleground = battleground;
        this.player = player;
        this.team = team;
        this.keepInventory = b;
        this.respawnMethod = quick;
        this.victim = new BattlegroundPlayer(battleground, battleground.getPlayerTeam(player), player); // FIXME
        this.killer = null;
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