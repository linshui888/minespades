package me.nologic.minespades.battleground;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import java.util.HashMap;
import java.util.prefs.Preferences;

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

    public enum Preference {

        AUTO_ASSIGN,
        FRIENDLY_FIRE,
        KEEP_INVENTORY,
        DELETE_EMPTY_BOTTLES,
        COLORFUL_ENDING,
        FLAG_PARTICLES,
        FLAG_STEALER_TRAILS

    }


}