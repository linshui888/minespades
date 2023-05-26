package me.nologic.minespades.bot.behaviour;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.bot.BattlegroundBot;
import me.nologic.minespades.bot.SituationKnowledge;

public abstract class AbstractBehaviour {

    protected final BattlegroundBot bot;
    protected final Battleground    battleground;

    public AbstractBehaviour(BattlegroundBot bot) {
        this.bot = bot;
        this.battleground = bot.getBattleground();
    }

    public abstract void behave(SituationKnowledge knowledge);

    @SneakyThrows
    public static AbstractBehaviour getRandomBehaviour(BattlegroundBot bot) {
        return Type.AGGRESSIVE.getClazz().getConstructor(BattlegroundBot.class).newInstance(bot);
    }

    private enum Type {

        AGGRESSIVE(AggressiveBehaviour.class);

        @Getter
        private final Class<? extends AbstractBehaviour> clazz;

        Type(Class<? extends AbstractBehaviour> clazz) {
            this.clazz = clazz;
        }
    }

}