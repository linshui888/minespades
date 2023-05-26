package me.nologic.minespades.bot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.nologic.minespades.game.flag.BattlegroundFlag;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@RequiredArgsConstructor @Getter
public class SituationKnowledge {

    private final double health;
    private final @Nullable BattlegroundFlag ownFlag, enemyFlag;
    private final List<Entity> alliesNear, enemiesNear;

    private final double distanceToCenter;

}