package dev.resivore.dragonbound.channel;

import org.junit.jupiter.api.Test;

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
}
