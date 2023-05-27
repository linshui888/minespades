package me.nologic.minespades.bot.behaviour;

import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.bot.BattlegroundBot;
import me.nologic.minespades.bot.SituationKnowledge;
import me.nologic.minespades.game.flag.BattlegroundFlag;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Objects;

/**
 * Агрессивный паттерн поведения, представляющий агрессивного бота.
 * */
public class AggressiveBehaviour extends Behaviour {

    public AggressiveBehaviour(BattlegroundBot bot) {
        super(bot);
    }

    @Override
    public void behave(SituationKnowledge knowledge) {

        final BattlegroundFlag enemyFlag = knowledge.getEnemyFlag();

        // [#0] : В первую очередь агрессивный бот ищет носителей флага среди врагов.
        if (knowledge.getEnemiesNear().size() > 0 && knowledge.getEnemiesNear().size() < 3) {
            final Player enemyFlagCarrier = (Player) knowledge.getEnemiesNear().stream().filter(e -> e instanceof Player player && BattlegroundPlayer.getBattlegroundPlayer(player) != null && BattlegroundPlayer.getBattlegroundPlayer(player).isCarryingFlag()).findAny().orElse(null);
            if (enemyFlagCarrier != null && !Objects.equals(bot.getTarget(), enemyFlagCarrier)) { // FIXME: сравнение нуликов даёт true
                bot.fight(enemyFlagCarrier);
                return;
            }
        }

        // [#0] : Агрессивный бот всегда нападает на вражеских игроков!
        if (knowledge.getEnemiesNear().size() > 0 && bot.getTarget() == null) {
            final Player closestEnemy = (Player) knowledge.getEnemiesNear().stream().min(Comparator.comparingDouble(e -> e.getLocation().distance(bot.getBukkitPlayer().getLocation()))).orElse(knowledge.getEnemiesNear().get(0));
            bot.fight(closestEnemy);
            return;
        }

        // Если бот ничем не занят, то даём ему задание, связанное с передвижением
        if (!bot.isBusy()) {

            // [#0] : Всегда проверяем, имеет ли бот флаг чужой команды.
            //        Если да, то приказываем ему идти на свою респу.
            if (bot.getBattlegroundPlayer().isCarryingFlag()) {
                bot.moveTo(bot.getTeam().getRandomRespawnLocation());
                return;
            }

            // [#0] : Проверяем наличие флага неподалёку.
            if (enemyFlag != null && enemyFlag.getPosition() != null && bot.getBukkitPlayer().getLocation().distance(enemyFlag.getPosition()) < 20) {
                bot.moveTo(enemyFlag.getPosition());
                return;
            }

            // [#1] : Агрессивный бот почти всегда хочет занять центр.
            if (knowledge.getDistanceToCenter() > 20) {
                bot.moveTo(battleground.getCenter().toHighestLocation().add(0, 1, 0));
                return;
            } else if (knowledge.getAlliesNear().size() > 0 && enemyFlag != null && enemyFlag.getPosition() != null) {
                // [#1.1] : Если бот в центре и рядом есть хотя бы один союзник, то бот идёт захватывать вражеский флаг
                bot.moveTo(enemyFlag.getPosition());
            }

        }

    }

}