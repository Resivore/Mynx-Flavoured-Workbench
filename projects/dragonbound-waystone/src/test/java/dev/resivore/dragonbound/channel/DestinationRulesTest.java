package dev.resivore.dragonbound.channel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DestinationRulesTest {
    @Test
    void arrivalIsExactlyAtTheWaystoneTopCenter() {
        Vec3 destination = DestinationRules.exactTopCenter(new BlockPos(-13, 72, 8));

        assertEquals(-12.5D, destination.x);
        assertEquals(72.0D + 3.0D / 16.0D, destination.y);
        assertEquals(8.5D, destination.z);
    }

    @Test
    void destinationMustPassEveryExactLocationCheck() {
        assertTrue(DestinationRules.exactDestinationIsSafe(true, true, true, true, true));

        for (int failedCheck = 0; failedCheck < 5; failedCheck++) {
            boolean[] checks = {true, true, true, true, true};
            checks[failedCheck] = false;
            assertFalse(DestinationRules.exactDestinationIsSafe(
                    checks[0], checks[1], checks[2], checks[3], checks[4]));
        }
    }

    @Test
    void safetyRuleReturnsOnlyARefusalAndCannotSubstituteNearbyCoordinates() throws NoSuchMethodException {
        var rule = DestinationRules.class.getMethod(
                "exactDestinationIsSafe",
                boolean.class, boolean.class, boolean.class, boolean.class, boolean.class);

        assertEquals(boolean.class, rule.getReturnType());
        assertEquals(5, rule.getParameterCount());
    }
}
