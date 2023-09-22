package me.nologic.minespades.game;

import me.nologic.minespades.Minespades;
import me.nologic.minespades.game.event.BattlegroundPlayerDeathEvent;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Configurable;
import me.nologic.minority.annotations.ConfigurationKey;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Специализированный класс, который крайне подробно обрабатывает событие смерти игрока.
 * Учёт KDA, киллфид (отправление сообщений об убийстве и пр.) — всё это происходит тут.
 */
@Configurable(file = "killfeed.yml", path = "killfeed-output", comment = { "Here you can change the killfeed settings and symbols.", "If you are playing on 1.20+ then you can use emoji." })
public class PlayerKDAHandler implements MinorityFeature {

    @ConfigurationKey(name = "DEFAULT", value = "⚔")
    private String basic;

    @ConfigurationKey(name = "PROJECTILE", value = "➸")
    private String projectile;

    @ConfigurationKey(name = "LAVA, FIRE, FIRE_TICK", value = "🔥")
    private String burning;

    @ConfigurationKey(name = "MAGIC", value = "⚡")
    private String magic;

    @ConfigurationKey(name = "death-without-killer-message", value = "&f☠ %s &f☠", comment = "This message will be displayed if the player died due to his own fault.")
    private String deathWithoutKiller;

    public PlayerKDAHandler() {
        this.init(this, this.getClass(), Minespades.getInstance());
    }

    public void handlePlayerDeath(BattlegroundPlayerDeathEvent event) {

        Player killer = null;
        if (event.getKiller() != null) killer = event.getKiller().getBukkitPlayer();
        event.getVictim().setDeaths(event.getVictim().getDeaths() + 1);

        String deathMessage;
        if (killer != null) {
            String symbol = this.getDeathSymbol(event);
            deathMessage = String.format(event.getKiller().getDisplayName() + " &f%s " + event.getVictim().getDisplayName(), symbol);
            event.getKiller().setKills(event.getKiller().getKills() + 1);

            // Обновляем счётчик киллов для убийцы в таблисте
            Scoreboard scoreboard = event.getBattleground().getScoreboard();
            Objective objective = scoreboard.getObjective("kill_counter");
            if (objective != null) {
                objective.getScore(killer.getName()).setScore(event.getKiller().getKills());
            }

        } else {
            deathMessage = String.format(this.deathWithoutKiller, event.getVictim().getDisplayName());
        }

        // Sending message
        event.getBattleground().getPlayers().forEach(battlegroundPlayer -> battlegroundPlayer.getBukkitPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(deathMessage)));
    }
    
    private String getDeathSymbol(BattlegroundPlayerDeathEvent event) {

        if (event.getKiller() != null) {
            String itemName = event.getKiller().getBukkitPlayer().getInventory().getItemInMainHand().getType().toString().toLowerCase();
            if (itemName.contains("pickaxe")) {
                return "⛏";
            }
            if (itemName.equals("air")) {
                return "ツ";
            }
        }

        return switch (event.getDamageCause()) {
            case PROJECTILE -> projectile;
            case LAVA, FIRE, FIRE_TICK -> burning;
            case MAGIC -> magic;
            default -> basic;
        };
    }

}