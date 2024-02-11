package me.nologic.minespades.battleground.builder;

import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.BattlegroundPreferences;
import me.nologic.minespades.battleground.Multiground;
import org.jetbrains.annotations.Nullable;

public class BattlegroundBuilder {

    private final Minespades plugin = Minespades.getInstance();

    /* Создаём новый Battleground. */
    @Nullable
    public Battleground build(String battlegroundName, @Nullable Multiground multiground) {
        Battleground battleground = new Battleground(battlegroundName);

        if (battleground.getPreference(BattlegroundPreferences.Preference.IS_MULTIGROUND).getAsBoolean() && multiground == null) {
            return null;
        }

        new LoadBattlegroundTask(battleground).runTaskAsynchronously(plugin);
        return battleground;
    }

}