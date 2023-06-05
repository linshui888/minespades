package me.nologic.minespades.bot;

import lombok.RequiredArgsConstructor;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.battleground.BattlegroundTeam;
import me.nologic.minespades.game.flag.BattlegroundFlag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

@RequiredArgsConstructor
public class DecideGenerator extends BukkitRunnable {

    private final BattlegroundBot bot;

    @Override
    public void run() {
        try {
            if (!bot.getBukkitPlayer().isOnline()) {
                Minespades.getInstance().getLogger().info(String.format("Бот %s ливнул, закрываем сокеты, кикаем бота.", bot.getBukkitPlayer().getName()));
                bot.getController().shutdown();
                this.cancel();
                return;
            }

            Player player = bot.getBukkitPlayer();
            final double health = player.getHealth();
            final BattlegroundFlag ownFlag = bot.getBattlegroundPlayer().getFlag();
            final BattlegroundFlag enemyFlag;

            final List<BattlegroundTeam> enemyTeams = bot.getBattleground().getTeams().stream().filter(team -> !bot.getBattlegroundPlayer().getTeam().equals(team)).toList();
            enemyFlag = enemyTeams.get((int) (Math.random() * enemyTeams.size())).getFlag();

            final List<Entity> allies = player.getNearbyEntities(15, 15, 15).stream().filter(e -> e instanceof Player p && BattlegroundPlayer.getBattlegroundPlayer(p) != null && BattlegroundPlayer.getBattlegroundPlayer(p).getTeam().equals(bot.getBattlegroundPlayer().getTeam())).toList();
            final List<Entity> enemies = player.getNearbyEntities(15, 15, 15).stream().filter(e -> e instanceof Player p && BattlegroundPlayer.getBattlegroundPlayer(p) != null && !BattlegroundPlayer.getBattlegroundPlayer(p).getTeam().equals(bot.getBattlegroundPlayer().getTeam())).toList();

            // TODO: Придётся добавлять марки для ботов. Или всё-таки придумать умный алгоритм детекта.
            final double distanceToCenter = player.getLocation().distance(bot.getBattleground().getCenter().toHighestLocation().add(0, 1, 0));

            bot.getBehaviour().behave(new SituationKnowledge(health, ownFlag, enemyFlag, allies, enemies, distanceToCenter));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}