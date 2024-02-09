package me.nologic.minespades.game;

import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.BattlegroundPlayer;
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
@Configurable(file = "killfeed.yml", path = "killfeed-output", comment = { "Here you can change killfeed settings and death symbols.", "If you are playing on 1.20+, you can use all existing emojis!" })
public class PlayerKDAHandler implements MinorityFeature {

    @ConfigurationKey(name = "DEFAULT", value = "⚔")
    private String basic;

    @ConfigurationKey(name = "PROJECTILE", value = "➸")
    private String projectile;

    @ConfigurationKey(name = "LAVA, FIRE, FIRE_TICK", value = "🔥")
    private String burning;

    @ConfigurationKey(name = "MAGIC", value = "⚡")
    private String magic;

    @ConfigurationKey(name = "ENTITY_EXPLOSION", value = "✴", comment = "Works only with tnt which was primed by a player")
    private String tnt;

    @ConfigurationKey(name = "death-without-killer-message", value = "&f☠ %s &f☠", comment = "This message will be displayed if the player died due to his own fault.")
    private String deathWithoutKiller;

    private final EventDrivenGameMaster gameMaster;

    public PlayerKDAHandler(final EventDrivenGameMaster gameMaster) {
        this.init(this, this.getClass(), Minespades.getInstance());
        this.gameMaster = gameMaster;
    }

    public void handlePlayerDeath(final BattlegroundPlayerDeathEvent event) {

        final Player             victim = event.getVictim().getBukkitPlayer();
        final BattlegroundPlayer killer = BattlegroundPlayer.getBattlegroundPlayer(this.gameMaster.getLastAttacker(victim));

        event.getVictim().setDeaths(event.getVictim().getDeaths() + 1);

        String deathMessage;
        if (killer != null) {
            String symbol = this.getDeathSymbol(event);
            deathMessage = String.format(killer.getDisplayName() + " §f%s " + event.getVictim().getDisplayName(), symbol);
            killer.setKills(killer.getKills() + 1);

            // Обновляем счётчик киллов для убийцы в таблисте
            Scoreboard scoreboard = event.getBattleground().getScoreboard();
            Objective objective = scoreboard.getObjective("kill_counter");
            if (objective != null) {
                objective.getScore(killer.getBukkitPlayer().getName()).setScore(killer.getKills());
            }

        } else {
            deathMessage = String.format(this.deathWithoutKiller, event.getVictim().getDisplayName());
        }

        // Sending message
        event.getBattleground().getBattlegroundPlayers().forEach(battlegroundPlayer -> battlegroundPlayer.getBukkitPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(deathMessage)));
        this.gameMaster.resetAttacker(victim);
    }
    
    private String getDeathSymbol(final BattlegroundPlayerDeathEvent event) {

        switch (event.getDamageCause()) {
            case PROJECTILE: return projectile;
            case LAVA, FIRE, FIRE_TICK: return burning;
            case MAGIC: return magic;
            case ENTITY_EXPLOSION: return tnt;
        }

        // little easter egg :D
        final BattlegroundPlayer killer = BattlegroundPlayer.getBattlegroundPlayer(this.gameMaster.getLastAttacker(event.getVictim().getBukkitPlayer()));
        if (killer != null) {
            String itemName = killer.getBukkitPlayer().getInventory().getItemInMainHand().getType().toString().toLowerCase();
            if (itemName.contains("pickaxe")) {
                return "⛏";
            }
            if (itemName.equals("air")) {
                return "ツ";
            }
        }

        return basic;
    }

}