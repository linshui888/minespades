package me.nologic.ms.session;

import me.nologic.ms.battleground.Battleground;
import org.bukkit.entity.Player;

public abstract class AbstractSession {

    protected String chatInfoMessage;
    protected String actionBarName;

    protected Player player;
    protected Battleground battleground;

    protected AbstractSession() {

    }

    protected abstract void pulse();

}
