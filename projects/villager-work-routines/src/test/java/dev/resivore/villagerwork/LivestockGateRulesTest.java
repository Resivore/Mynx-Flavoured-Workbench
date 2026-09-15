package dev.resivore.villagerwork;

import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivestockGateRulesTest {
    @Test void coversExactlyTheRequestedVanillaLivestockClasses() {
        assertTrue(LivestockGateRules.isContainedLivestock(Sheep.class));
        assertTrue(LivestockGateRules.isContainedLivestock(Cow.class));
        assertTrue(LivestockGateRules.isContainedLivestock(Pig.class));
        assertTrue(LivestockGateRules.isContainedLivestock(Chicken.class));
        assertTrue(LivestockGateRules.isContainedLivestock(Rabbit.class));

        assertFalse(LivestockGateRules.isContainedLivestock(Goat.class));
        assertFalse(LivestockGateRules.isContainedLivestock(MushroomCow.class));
        assertFalse(LivestockGateRules.isContainedLivestock(Villager.class));
        assertFalse(LivestockGateRules.isContainedLivestock(Player.class));
    }

    @Test void registeredSameOpenGateIsRequired() {
        assertTrue(LivestockGateRules.shouldTreatAsFence(Sheep.class, true, true));
        assertTrue(LivestockGateRules.shouldTreatAsFence(Cow.class, true, true));
        assertTrue(LivestockGateRules.shouldTreatAsFence(Pig.class, true, true));
        assertTrue(LivestockGateRules.shouldTreatAsFence(Chicken.class, true, true));
        assertTrue(LivestockGateRules.shouldTreatAsFence(Rabbit.class, true, true));
        assertFalse(LivestockGateRules.shouldTreatAsFence(Sheep.class, false, true),
                "an ordinary player-opened gate has no VWR registration");
        assertFalse(LivestockGateRules.shouldTreatAsFence(Sheep.class, true, false),
                "a closed, replaced, or changed gate is not a live virtual blocker");
        assertFalse(LivestockGateRules.shouldTreatAsFence(Goat.class, true, true));
        assertFalse(LivestockGateRules.shouldTreatAsFence(Villager.class, true, true));
        assertFalse(LivestockGateRules.shouldTreatAsFence(Player.class, true, true));
    }
}
