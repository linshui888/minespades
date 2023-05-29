package me.nologic.minespades.bot.behaviour;

import lombok.Getter;
import lombok.SneakyThrows;
import me.nologic.minespades.Minespades;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.bot.BattlegroundBot;
import me.nologic.minespades.bot.SituationKnowledge;

import java.util.Random;

public abstract class Behaviour {

    protected final Minespades plugin;
    protected final Random     random;

    protected final BattlegroundBot bot;
    protected final Battleground    battleground;

    public Behaviour(BattlegroundBot bot) {
        this.plugin = Minespades.getInstance();
        this.random = plugin.getRandom();
        this.bot = bot;
        this.battleground = bot.getBattleground();
    }

    public abstract void behave(SituationKnowledge knowledge);

    @SneakyThrows
    public static Behaviour getRandomBehaviour(BattlegroundBot bot) {
        return Type.AGGRESSIVE.getClazz().getConstructor(BattlegroundBot.class).newInstance(bot);
    }

    private enum Type {

        AGGRESSIVE(AggressiveBehaviour.class);

        @Getter
        private final Class<? extends Behaviour> clazz;

        Type(Class<? extends Behaviour> clazz) {
            this.clazz = clazz;
        }
    }

}