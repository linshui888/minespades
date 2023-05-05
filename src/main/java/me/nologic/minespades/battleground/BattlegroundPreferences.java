package me.nologic.minespades.battleground;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.HashMap;

@RequiredArgsConstructor
public class BattlegroundPreferences implements Listener {

    private final Battleground battleground;
    private final HashMap<Preference, Boolean> preferences = new HashMap<>();

    public void apply(Preference preference, boolean state) {
        this.preferences.put(preference, state);
    }

    public boolean get(Preference preference) {
        return this.preferences.get(preference);
    }

    @EventHandler
    private void onPotionConsume(PlayerItemConsumeEvent event) {
        if (preferences.get(Preference.DELETE_EMPTY_BOTTLES)) {
            if (event.getReplacement() != null && event.getReplacement().getType() == Material.GLASS_BOTTLE) {
                event.setReplacement(null);
            }
        }
    }

    @EventHandler
    private void onPortalCreate(PortalCreateEvent event) {
        if (preferences.get(Preference.DISABLE_PORTALS)) {
            if (event.getWorld().equals(battleground.getWorld())) {
                event.setCancelled(true);
            }
        }
    }

    public enum Preference {

        AUTO_ASSIGN,
        FRIENDLY_FIRE,
        KEEP_INVENTORY,
        DELETE_EMPTY_BOTTLES,
        COLORFUL_ENDING,
        FLAG_PARTICLES,
        FLAG_STEALER_TRAILS,
        DISABLE_PORTALS

    }


}