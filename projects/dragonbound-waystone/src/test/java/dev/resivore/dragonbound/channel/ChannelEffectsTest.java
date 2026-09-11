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

        assertEquals(10.42D, foreground.x, 1.0E-10D);
        assertEquals(63.85D, foreground.y, 1.0E-10D);
        assertEquals(-3.44D, foreground.z, 1.0E-10D);
        assertTrue(ChannelEffects.FOREGROUND_FORWARD_OFFSET >= 0.6D);
        assertTrue(ChannelEffects.FOREGROUND_FORWARD_OFFSET <= 0.8D);
        assertTrue(ChannelEffects.FOREGROUND_VERTICAL_OFFSET < 0.0D);
    }

    @Test
    void foregroundChannelDensityIsModestBoundedAndRampsWithTheBodyStream() {
        assertEquals(1, ChannelEffects.foregroundChannelParticleCount(0, 40));
        assertTrue(ChannelEffects.foregroundChannelParticleCount(39, 40)
                > ChannelEffects.foregroundChannelParticleCount(0, 40));
        assertEquals(2, ChannelEffects.foregroundChannelParticleCount(39, 40));
        assertTrue(ChannelEffects.FOREGROUND_SUCCESS_PARTICLE_COUNT > 0);
        assertTrue(ChannelEffects.FOREGROUND_SUCCESS_PARTICLE_COUNT < ChannelEffects.SUCCESS_PARTICLE_COUNT);

        for (long elapsed : new long[] {-20L, 0L, 20L, 39L, 60L}) {
            int count = ChannelEffects.foregroundChannelParticleCount(elapsed, 40);
            assertTrue(count >= ChannelEffects.FOREGROUND_CHANNEL_PARTICLES_AT_START);
            assertTrue(count <= ChannelEffects.FOREGROUND_CHANNEL_PARTICLES_AT_COMPLETION);
        }
    }

    @Test
    void existingBodyParticleCountsRemainFrozen() {
        assertEquals(30, ChannelEffects.SUCCESS_PARTICLE_COUNT);
        assertEquals(1, ChannelEffects.CHANNEL_PARTICLES_AT_START);
        assertEquals(4, ChannelEffects.CHANNEL_PARTICLES_AT_COMPLETION);
    }
}
