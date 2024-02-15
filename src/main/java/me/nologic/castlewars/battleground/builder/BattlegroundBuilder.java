package me.nologic.castlewars.battleground.builder;

import me.nologic.castlewars.CastleWars;
import me.nologic.castlewars.battleground.Battleground;
import me.nologic.castlewars.battleground.BattlegroundPreferences;
import me.nologic.castlewars.battleground.Multiground;
import org.jetbrains.annotations.Nullable;

public class BattlegroundBuilder {

    private final CastleWars plugin = CastleWars.getInstance();

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