package me.nologic.minespades.bot.behaviour;

import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.bot.BattlegroundBot;
import me.nologic.minespades.bot.SituationKnowledge;
import me.nologic.minespades.game.flag.BattlegroundFlag;
import org.bukkit.entity.Player;

import java.util.Comparator;

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

        if (bot.isConsuming()) return;

        if (bot.getTarget() != null) {
            super.heldWeapon(sword);
        }

        if (bot.isFleeing() && (knowledge.getHealth() >= 14 || knowledge.getAlliesNear().size() > 1)) bot.setFleeing(false);

        if (knowledge.getEnemiesNear().size() == 0) {
            if (knowledge.getHealth() <= 14 && knowledge.getEnemiesNear().isEmpty()) {
                final int slot = super.getSlotForHealingPotion();
                if (slot != -1) {
                    bot.consume(slot);
                    return;
                }
            }
        }

        // Если бот является трусом, то при падении хп он попытается сбежать из забива (когда энимей больше 1), чтобы восстановить хп.
        if (coward && knowledge.getHealth() <= 11 && knowledge.getEnemiesNear().size() > 1 && (bot.getTarget() != null || !bot.isFleeing())) {
            bot.moveTo(bot.getBukkitPlayer().getLocation().add(bot.getTeam().getRandomRespawnLocation()).toCenterLocation().add(Math.random() * 5, 0, Math.random() * 5));
            bot.setFleeing(true);
            bot.setTarget(null);
            return;
        }

        if (bot.isFleeing()) return;

        // [#0] : Всегда проверяем, имеет ли бот флаг чужой команды.
        //        Если да, то приказываем ему идти на свою респу.
        if (bot.getBattlegroundPlayer().isCarryingFlag()) {
            bot.moveTo(bot.getTeam().getRandomRespawnLocation());
            return;
        }

        // [#1] : В первую очередь агрессивный бот ищет носителей флага среди врагов.
        if (knowledge.getEnemiesNear().size() > 0) {
            final Player enemyFlagCarrier = (Player) knowledge.getEnemiesNear().stream().filter(e -> e instanceof Player player && BattlegroundPlayer.getBattlegroundPlayer(player) != null && BattlegroundPlayer.getBattlegroundPlayer(player).isCarryingFlag()).findAny().orElse(null);
            if (enemyFlagCarrier != null) {
                bot.fight(enemyFlagCarrier);
                return;
            }
        }

        // [#2] : Агрессивный бот всегда нападает на ближайшего игрока. При этом цель может меняться.
        if (knowledge.getEnemiesNear().size() > 0) {
            final Player closestEnemy = (Player) knowledge.getEnemiesNear().stream().min(Comparator.comparingDouble(e -> e.getLocation().distance(bot.getBukkitPlayer().getLocation()))).orElse(knowledge.getEnemiesNear().get(0));
            bot.fight(closestEnemy);
            bot.moveTo(closestEnemy.getLocation(), 3);
            return;
        }

        // [#0] : Проверяем наличие флага неподалёку.
        if (enemyFlag != null && enemyFlag.getPosition() != null && bot.getBukkitPlayer().getLocation().distance(enemyFlag.getPosition()) < 25) {
            bot.moveTo(enemyFlag.getPosition());
        }

        // [#1] : В случае отсутствия задачи, агрессивные боты идут на случайную вражескую респу.
        if (bot.getDestination() == null) {
            // Получаем случайную команду (исключая нашу), получаем случайную респу этой команды.
            battleground.broadcast("%s выдвигается на вражескую базу!".formatted(bot.getBukkitPlayer().getName()));
            bot.moveTo(battleground.getTeams().stream().filter(team -> !team.equals(bot.getTeam())).toList().get((int) (Math.random() * (battleground.getTeams().size() - 1))).getRandomRespawnLocation());
        }

    }

}