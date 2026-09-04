package com.yungnickyoung.minecraft.ribbits.chute;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class ChuteInputAndPhysicsTest {
    @Test
    void heldGroundJumpNeedsReleaseAndFreshAirbornePress() {
        ChuteInputState input = new ChuteInputState();

        assertFalse(input.sample(true, false).isPresent(), "initial ground jump is never sent");
        assertFalse(input.sample(true, true).isPresent(), "walking airborne while held invents no edge");
        assertFalse(input.sample(false, true).isPresent(), "release only rearms");
        assertEquals(OptionalLong.of(1L), input.sample(true, true));
        assertFalse(input.sample(true, true).isPresent(), "held press sends once");
        input.sample(false, true);
        assertEquals(OptionalLong.of(2L), input.sample(true, true));
    }

    @Test
    void disallowedScreenOrFocusStateStillConsumesPhysicalEdge() {
        ChuteInputState input = new ChuteInputState();

        assertFalse(input.sample(true, false).isPresent());
        assertFalse(input.sample(true, true).isPresent());
        input.sample(false, false);
        assertEquals(OptionalLong.of(1L), input.sample(true, true));
    }

    @Test
    void movementCapPreservesHorizontalUpwardAndSlowDescentComponents() {
        Vec3 fast = ChutePhysics.capDescent(new Vec3(0.375D, -1.75D, -0.625D));
        assertEquals(0.375D, fast.x);
        assertEquals(-0.10D, fast.y);
        assertEquals(-0.625D, fast.z);

        Vec3 slow = new Vec3(2.0D, -0.05D, 3.0D);
        Vec3 upward = new Vec3(-2.0D, 0.4D, -3.0D);
        assertSame(slow, ChutePhysics.capDescent(slow));
        assertSame(upward, ChutePhysics.capDescent(upward));
    }
}
