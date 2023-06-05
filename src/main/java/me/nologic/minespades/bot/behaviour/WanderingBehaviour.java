package me.nologic.minespades.bot.behaviour;

import me.nologic.minespades.battleground.BattlegroundPlayer;
import me.nologic.minespades.bot.BattlegroundBot;
import me.nologic.minespades.bot.SituationKnowledge;
import me.nologic.minespades.game.flag.BattlegroundFlag;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Агрессивный паттерн поведения, представляющий агрессивного бота.
 * */
public class WanderingBehaviour extends Behaviour {

    private final int range;
    private final boolean sword;

    public WanderingBehaviour(BattlegroundBot bot) {
        super(bot);
        this.range = 40 + random.nextInt(20);
        this.sword = random.nextBoolean();
    }

    @Override
    public void behave(SituationKnowledge knowledge) {

        final BattlegroundFlag enemyFlag = knowledge.getEnemyFlag();

        if (bot.isConsuming()) return;

        // Блуждающий бот хилится когда рядом никого нет!
        if (knowledge.getEnemiesNear().isEmpty()) {
            if (knowledge.getHealth() <= 14) {
                final int slot = super.getSlotForHealingPotion();
                if (slot != -1) {
                    bot.consume(slot);
                    return;
                }
            }
        }

        // [#0] : Всегда проверяем, имеет ли бот флаг чужой команды.
        //        Если да, то приказываем ему идти на свою респу.
        if (bot.getBattlegroundPlayer().isCarryingFlag()) {
            bot.moveTo(bot.getTeam().getRandomRespawnLocation());
            return;
        }

        super.heldWeapon(sword);

        // [#0] : Проверяем наличие флага неподалёку.
        if (enemyFlag != null && enemyFlag.getPosition() != null && bot.getBukkitPlayer().getLocation().distance(enemyFlag.getPosition()) < 25) {
            if (bot.getDestination() != enemyFlag.getPosition()) {
                bot.moveTo(enemyFlag.getPosition());
                return;
            }
        }

        // [#2] : Блуждающий бот нападает на врага, только если врагов меньше чем союзников, или силы равны.
        if (!knowledge.getEnemiesNear().isEmpty() && knowledge.getEnemiesNear().size() <= knowledge.getAlliesNear().size()) {
            final Player enemy = (Player) knowledge.getEnemiesNear().stream().min(Comparator.comparingDouble(entity -> ((LivingEntity) entity).getHealth())).orElse(knowledge.getEnemiesNear().get(0));
            bot.fight(enemy);
            return;
        }

        // [#1] : Если у бота нет направления (или в заданной точке больше одного противника), то выбираем случайное
        if (bot.getDestination() == null || bot.getBukkitPlayer().getLocation().distance(bot.getDestination()) <= 3 || bot.getDestination().getNearbyEntities(20, 20, 20).stream().filter(entity -> entity instanceof Player player && (BattlegroundPlayer.getBattlegroundPlayer(player) != null) && !BattlegroundPlayer.getBattlegroundPlayer(player).getTeam().equals(bot.getTeam())).count() > 1) {
            bot.moveTo(pickRandomLocation(range));
        }
    }

    private Location pickRandomLocation(final int range) {

        @NotNull Location random;

        do {
            battleground.broadcast(String.format(String.format("§3%s §fпытается найти рандомную безопасную локацию!", bot.getBukkitPlayer().getName())));
            final Location min = battleground.getBoundingBox().getMin().toLocation(battleground.getWorld());
            final Location max = battleground.getBoundingBox().getMax().toLocation(battleground.getWorld());
            random = new Location(battleground.getWorld(), min.getX() + 1 + plugin.getRandom().nextInt((int) max.getX()), 0, min.getZ() - 1 + plugin.getRandom().nextInt((int) max.getZ()));
            random.setY(1 + battleground.getWorld().getHighestBlockYAt(random, HeightMap.MOTION_BLOCKING));
        } while (!random.getBlock().getType().isAir() && random.distance(bot.getBukkitPlayer().getLocation()) > range && random.distance(battleground.getCenter().toHighestLocation(HeightMap.MOTION_BLOCKING)) < 15 && random.getNearbyEntities(20, 20, 20).stream().anyMatch(entity -> entity instanceof Player player && (BattlegroundPlayer.getBattlegroundPlayer(player) != null) && !BattlegroundPlayer.getBattlegroundPlayer(player).getTeam().equals(bot.getTeam())));

        battleground.broadcast(String.format("§3%s §fнашёл безопасную локацию без вражеских игроков: §36%s", bot.getBukkitPlayer().getName(), random));
        return random;
    }

}