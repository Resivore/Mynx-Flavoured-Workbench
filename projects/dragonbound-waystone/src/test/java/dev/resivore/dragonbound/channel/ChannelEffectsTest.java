package dev.resivore.dragonbound.channel;

import org.junit.jupiter.api.Test;
import net.minecraft.resources.Identifier;
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
    void foregroundGroundOriginUsesOnlyPlayerFeetPosition() {
        Vec3 playerPosition = new Vec3(10.0D, 64.0D, -4.0D);
        Vec3 foreground = ChannelEffects.foregroundGroundOrigin(playerPosition);

        assertEquals(10.0D, foreground.x, 1.0E-10D);
        assertEquals(64.05D, foreground.y, 1.0E-10D);
        assertEquals(-4.0D, foreground.z, 1.0E-10D);
        assertEquals(0.05D, ChannelEffects.FOREGROUND_GROUND_OFFSET);
    }

    @Test
    void foregroundChannelDensityIsStrongBoundedAndRampsWithTheBodyStream() {
        assertEquals(3, ChannelEffects.foregroundChannelParticleCount(0, 40));
        assertTrue(ChannelEffects.foregroundChannelParticleCount(39, 40)
                > ChannelEffects.foregroundChannelParticleCount(0, 40));
        assertEquals(8, ChannelEffects.foregroundChannelParticleCount(39, 40));
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
    void targetedForegroundStartsStayNearGroundAndWithinTheirConfiguredRadius() {
        Vec3 ground = ChannelEffects.foregroundGroundOrigin(new Vec3(10.0D, 64.0D, -4.0D));
        Vec3 channelStart = ChannelEffects.foregroundParticleStart(ground, 0.65D, -0.05D, -0.65D);

        assertEquals(10.65D, channelStart.x, 1.0E-10D);
        assertEquals(64.0D, channelStart.y, 1.0E-10D);
        assertEquals(-4.65D, channelStart.z, 1.0E-10D);
        assertEquals(0.65D, ChannelEffects.FOREGROUND_CHANNEL_HORIZONTAL_RADIUS);
        assertEquals(0.05D, ChannelEffects.FOREGROUND_CHANNEL_VERTICAL_SPREAD);
    }

    @Test
    void explicitReversePortalMotionIsBoundedAndRisesThroughoutItsExactLifetime() {
        Vec3 motion = ChannelEffects.foregroundReversePortalMotion(0.02D, 0.06D, -0.02D);

        assertEquals(0.02D, motion.x, 1.0E-10D);
        assertEquals(0.06D, motion.y, 1.0E-10D);
        assertEquals(-0.02D, motion.z, 1.0E-10D);
        assertEquals(0.035D, ChannelEffects.FOREGROUND_MOTION_HORIZONTAL_MAX);
        assertEquals(0.06D, ChannelEffects.FOREGROUND_MOTION_VERTICAL_MIN);
        assertEquals(0.12D, ChannelEffects.FOREGROUND_MOTION_VERTICAL_MAX);
        for (int lifetime = 60; lifetime <= 61; lifetime++) {
            for (int age = 1; age < lifetime; age++) {
                assertTrue(ChannelEffects.reversePortalVerticalStep(
                        ChannelEffects.FOREGROUND_MOTION_VERTICAL_MIN, age, lifetime) > 0.0D);
                assertTrue(ChannelEffects.reversePortalVerticalStep(
                        ChannelEffects.FOREGROUND_MOTION_VERTICAL_MAX, age, lifetime) > 0.0D);
            }
        }
    }

    @Test
    void channelBuildupCountsRemainFrozen() {
        assertEquals(1, ChannelEffects.CHANNEL_PARTICLES_AT_START);
        assertEquals(4, ChannelEffects.CHANNEL_PARTICLES_AT_COMPLETION);
    }

    @Test
    void mirrorArrivalUsesTheAuditedEnderscapePresentationParameters() {
        assertEquals(Identifier.fromNamespaceAndPath("enderscape", "item.mirror.teleport"),
                ChannelEffects.MIRROR_TELEPORT_SOUND_ID);
        assertEquals(Identifier.fromNamespaceAndPath("enderscape", "mirror_teleport_in"),
                ChannelEffects.MIRROR_TELEPORT_IN_PARTICLE_ID);
        assertEquals(50, ChannelEffects.MIRROR_ARRIVAL_PARTICLE_COUNT);
        assertEquals(0.5D, ChannelEffects.MIRROR_ARRIVAL_Y_OFFSET);
        assertEquals(0.5D, ChannelEffects.MIRROR_ARRIVAL_HORIZONTAL_SPREAD);
        assertEquals(1.0D, ChannelEffects.MIRROR_ARRIVAL_VERTICAL_SPREAD);
        assertEquals(0.1D, ChannelEffects.MIRROR_ARRIVAL_SPEED);
        assertEquals(0.65F, ChannelEffects.MIRROR_ARRIVAL_SOUND_VOLUME);
        assertEquals(1.0F, ChannelEffects.MIRROR_ARRIVAL_SOUND_PITCH);
    }
}
