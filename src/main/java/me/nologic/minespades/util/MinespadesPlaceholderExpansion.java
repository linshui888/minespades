package me.nologic.minespades.util;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class MinespadesPlaceholderExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "minespades";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NoLogic";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        BattlegroundPlayer player = BattlegroundPlayer.getBattlegroundPlayer(offlinePlayer.getPlayer());

        // Если игрок не играет на арене, то будет возвращён null
        if (player != null) {
            return switch (params.toLowerCase()) {
                case "player_current_kill_score" -> "" + player.getKills();
                case "player_current_death_score" -> "" + player.getDeaths();
                case "player_current_assist_score" -> "" + player.getAssists();
                case "player_current_lifepool" -> "" + player.getTeam().getLifepool();
                case "player_current_team" -> player.getTeam().getDisplayName();
                case "player_current_map" -> player.getBattleground().getBattlegroundName();
                default -> null;
            };
        }

        return null;
    }

}