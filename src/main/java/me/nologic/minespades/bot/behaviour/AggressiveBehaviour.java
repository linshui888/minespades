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

    private final boolean coward;

    private final boolean sword;

    public AggressiveBehaviour(BattlegroundBot bot) {
        super(bot);
        this.coward = random.nextBoolean();
        this.sword = random.nextBoolean();
    }

    @Override
    public void behave(SituationKnowledge knowledge) {

        final BattlegroundFlag enemyFlag = knowledge.getEnemyFlag();

        if (knowledge.getEnemiesNear().size() <= 1) {
            if (bot.isFleeing()) bot.setFleeing(false);
            if (knowledge.getHealth() <= 13) {
                final int slot = super.getSlotForHealingPotion();
                if (slot != -1) {
                    bot.consume(slot);
                    return;
                }
            }
        }

        // Если бот является трусом, то при падении хп он попытается сбежать из боя, чтобы восстановить хп.
        if (coward && knowledge.getHealth() <= 10 && knowledge.getEnemiesNear().size() > 0 && (bot.getTarget() != null || !bot.isFleeing())) {
            bot.moveTo(bot.getBukkitPlayer().getLocation().add(bot.getTeam().getRandomRespawnLocation()).toCenterLocation().add(Math.random() * 5, 0, Math.random() * 5));
            battleground.broadcast(String.format("§3%s §rпытается убежать, чтобы выжить!", bot.getBukkitPlayer().getName()));
            bot.setFleeing(true);
            bot.setTarget(null);
            return;
        }

        if (bot.isFleeing()) return;
        super.heldWeapon(sword);


        // [#0] : Всегда проверяем, имеет ли бот флаг чужой команды.
        //        Если да, то приказываем ему идти на свою респу.
        if (bot.getBattlegroundPlayer().isCarryingFlag()) {
            bot.moveTo(bot.getTeam().getRandomRespawnLocation());
            return;
        }

        // [#1] : В первую очередь агрессивный бот ищет носителей флага среди врагов.
        if (knowledge.getEnemiesNear().size() > 0 && knowledge.getEnemiesNear().size() < 3) {
            final Player enemyFlagCarrier = (Player) knowledge.getEnemiesNear().stream().filter(e -> e instanceof Player player && BattlegroundPlayer.getBattlegroundPlayer(player) != null && BattlegroundPlayer.getBattlegroundPlayer(player).isCarryingFlag()).findAny().orElse(null);
            if (enemyFlagCarrier != null && !Objects.equals(bot.getTarget(), enemyFlagCarrier)) {
                bot.fight(enemyFlagCarrier);
                return;
            }
        }

        // [#2] : Агрессивный бот всегда нападает на ближайшего игрока. При этом цель может меняться.
        if (knowledge.getEnemiesNear().size() > 0) {
            final Player closestEnemy = (Player) knowledge.getEnemiesNear().stream().min(Comparator.comparingDouble(e -> e.getLocation().distance(bot.getBukkitPlayer().getLocation()))).orElse(knowledge.getEnemiesNear().get(0));
            if (!Objects.equals(closestEnemy, bot.getTarget())) {
                bot.fight(closestEnemy);
            }
            return;
        }

        // Если бот ничем не занят, то даём ему задание, связанное с передвижением
        if (!bot.isBusy()) {

            // [#0] : Проверяем наличие флага неподалёку.
            if (enemyFlag != null && enemyFlag.getPosition() != null && bot.getBukkitPlayer().getLocation().distance(enemyFlag.getPosition()) < 20) {
                bot.moveTo(enemyFlag.getPosition());
                return;
            }

            // [#1] : Агрессивные боты просто идут на вражескую респу.
            bot.moveTo(battleground.getTeams().get((int) (Math.random() * battleground.getTeams().size())).getRandomRespawnLocation());

        }

    }

}