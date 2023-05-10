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
                case "player_kda" -> player.getKills() + "/" + player.getDeaths() + "/" + player.getAssists();
                case "player_team_lifepool" -> "" + player.getTeam().getLifepool();
                case "player_team_scores" -> "" + player.getTeam().getScores();
                default -> null;
            };
        }

        return null; // Placeholder is unknown by the Expansion
    }

}