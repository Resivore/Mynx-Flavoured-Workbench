package dev.resivore.villagerwork;

import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;

import java.util.Objects;

/** Pure scope/decision rules for the temporary livestock gate pathfinding overlay. */
final class LivestockGateRules {
    private LivestockGateRules() {}

    static boolean isContainedLivestock(Class<?> entityClass) {
        Objects.requireNonNull(entityClass, "entityClass");
        return Sheep.class.isAssignableFrom(entityClass)
                || Cow.class.isAssignableFrom(entityClass)
                || Pig.class.isAssignableFrom(entityClass)
                || Chicken.class.isAssignableFrom(entityClass)
                || Rabbit.class.isAssignableFrom(entityClass);
    }

    static boolean shouldTreatAsFence(Class<?> entityClass, boolean registered,
                                      boolean sameOpenFenceGate) {
        return registered && sameOpenFenceGate && isContainedLivestock(entityClass);
    }
}
