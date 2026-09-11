package dev.resivore.dragonbound.channel;

import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChannelEffectsTest {
    @Test
    void channelParticleDensityRampsFromSparseToDenseForTheDefaultDuration() {
        assertEquals(1, ChannelEffects.channelParticleCount(0, 40));
        assertTrue(ChannelEffects.channelParticleCount(20, 40)
                > ChannelEffects.channelParticleCount(0, 40));
        assertEquals(4, ChannelEffects.channelParticleCount(39, 40));
    }

    @Test
    void channelParticleDensityUsesAnyPositiveConfiguredDurationAndStaysBounded() {
        for (int duration : new int[] {1, 2, 7, 40, 137}) {
            int first = ChannelEffects.channelParticleCount(0, duration);
            int last = ChannelEffects.channelParticleCount(duration - 1L, duration);
            assertEquals(1, first);
            assertTrue(last >= first);
            for (long elapsed : new long[] {-20, 0, duration / 2L, duration - 1L, duration + 20L}) {
                int count = ChannelEffects.channelParticleCount(elapsed, duration);
                assertTrue(count >= ChannelEffects.CHANNEL_PARTICLES_AT_START);
                assertTrue(count <= ChannelEffects.CHANNEL_PARTICLES_AT_COMPLETION);
            }
        }
    }

    @Test
    void foregroundOriginUsesNormalizedLookDirectionAheadOfAndBelowTheEye() {
        Vec3 eye = new Vec3(10.0D, 64.0D, -4.0D);
        Vec3 foreground = ChannelEffects.foregroundOrigin(eye, new Vec3(3.0D, 0.0D, 4.0D));

        assertEquals(10.33D, foreground.x, 1.0E-10D);
        assertEquals(63.85D, foreground.y, 1.0E-10D);
        assertEquals(-3.56D, foreground.z, 1.0E-10D);
        assertEquals(0.55D, ChannelEffects.FOREGROUND_FORWARD_OFFSET);
        assertEquals(-0.15D, ChannelEffects.FOREGROUND_VERTICAL_OFFSET);
    }

    @Test
    void foregroundChannelDensityIsStrongBoundedAndRampsWithTheBodyStream() {
        assertEquals(3, ChannelEffects.foregroundChannelParticleCount(0, 40));
        assertTrue(ChannelEffects.foregroundChannelParticleCount(39, 40)
                > ChannelEffects.foregroundChannelParticleCount(0, 40));
        assertEquals(8, ChannelEffects.foregroundChannelParticleCount(39, 40));
        assertEquals(24, ChannelEffects.FOREGROUND_SUCCESS_PARTICLE_COUNT);

        for (int duration : new int[] {2, 7, 40, 137}) {
            assertEquals(3, ChannelEffects.foregroundChannelParticleCount(0, duration));
            assertEquals(8, ChannelEffects.foregroundChannelParticleCount(duration - 1L, duration));
            for (long elapsed : new long[] {-20L, 0L, duration / 2L, duration - 1L, duration + 20L}) {
                int count = ChannelEffects.foregroundChannelParticleCount(elapsed, duration);
                assertTrue(count >= ChannelEffects.FOREGROUND_CHANNEL_PARTICLES_AT_START);
                assertTrue(count <= ChannelEffects.FOREGROUND_CHANNEL_PARTICLES_AT_COMPLETION);
            }
        }
        assertEquals(3, ChannelEffects.foregroundChannelParticleCount(0, 1));
    }

    @Test
    void existingBodyParticleCountsRemainFrozen() {
        assertEquals(30, ChannelEffects.SUCCESS_PARTICLE_COUNT);
        assertEquals(1, ChannelEffects.CHANNEL_PARTICLES_AT_START);
        assertEquals(4, ChannelEffects.CHANNEL_PARTICLES_AT_COMPLETION);
    }
}
