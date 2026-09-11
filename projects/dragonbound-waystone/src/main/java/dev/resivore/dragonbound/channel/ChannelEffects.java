package dev.resivore.dragonbound.channel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Server-owned visual feedback for valid Dragonbound channels and confirmed arrivals. */
final class ChannelEffects {
    static final int SUCCESS_PARTICLE_COUNT = 30;
    static final int FOREGROUND_SUCCESS_PARTICLE_COUNT = 8;
    static final int CHANNEL_PARTICLES_AT_START = 1;
    static final int CHANNEL_PARTICLES_AT_COMPLETION = 4;
    static final int FOREGROUND_CHANNEL_PARTICLES_AT_START = 1;
    static final int FOREGROUND_CHANNEL_PARTICLES_AT_COMPLETION = 2;
    static final double FOREGROUND_FORWARD_OFFSET = 0.70D;
    static final double FOREGROUND_VERTICAL_OFFSET = -0.15D;

    private ChannelEffects() {
    }

    static void successfulTeleport(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 position = player.position();
        level.playSound(
                null,
                position.x,
                position.y,
                position.z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        portalBurst(level, position);
        foregroundPortalBurst(player, level);
    }

    static void channelStarted(ServerPlayer player, int channelTicks) {
        channelParticles(player, 0L, channelTicks);
    }

    static void channelTick(ServerPlayer player, long elapsedTicks, int channelTicks) {
        channelParticles(player, elapsedTicks, channelTicks);
    }

    /**
     * Returns a bounded particle count based on actual configured channel duration. The last
     * valid tick before completion reaches the dense end of the ramp without assuming 40 ticks.
     */
    static int channelParticleCount(long elapsedTicks, int channelTicks) {
        return rampedParticleCount(
                elapsedTicks,
                channelTicks,
                CHANNEL_PARTICLES_AT_START,
                CHANNEL_PARTICLES_AT_COMPLETION);
    }

    static int foregroundChannelParticleCount(long elapsedTicks, int channelTicks) {
        return rampedParticleCount(
                elapsedTicks,
                channelTicks,
                FOREGROUND_CHANNEL_PARTICLES_AT_START,
                FOREGROUND_CHANNEL_PARTICLES_AT_COMPLETION);
    }

    static Vec3 foregroundOrigin(Vec3 eyePosition, Vec3 lookVector) {
        return eyePosition.add(lookVector.normalize().scale(FOREGROUND_FORWARD_OFFSET))
                .add(0.0D, FOREGROUND_VERTICAL_OFFSET, 0.0D);
    }

    private static int rampedParticleCount(long elapsedTicks, int channelTicks, int start, int completion) {
        if (channelTicks <= 1) {
            return start;
        }
        long clampedElapsed = Math.max(0L, Math.min(elapsedTicks, channelTicks - 1L));
        long range = completion - start;
        return start + (int) (clampedElapsed * range / (channelTicks - 1L));
    }

    private static void channelParticles(ServerPlayer player, long elapsedTicks, int channelTicks) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 position = player.position();
        level.sendParticles(
                ParticleTypes.PORTAL,
                position.x,
                position.y + 0.9D,
                position.z,
                channelParticleCount(elapsedTicks, channelTicks),
                0.35D,
                0.60D,
                0.35D,
                0.04D
        );
        Vec3 foreground = foregroundOrigin(player.getEyePosition(), player.getLookAngle());
        level.sendParticles(
                player,
                ParticleTypes.PORTAL,
                false,
                false,
                foreground.x,
                foreground.y,
                foreground.z,
                foregroundChannelParticleCount(elapsedTicks, channelTicks),
                0.22D,
                0.14D,
                0.22D,
                0.04D
        );
    }

    private static void portalBurst(ServerLevel level, Vec3 position) {
        level.sendParticles(
                ParticleTypes.PORTAL,
                position.x,
                position.y + 0.9D,
                position.z,
                SUCCESS_PARTICLE_COUNT,
                0.50D,
                0.90D,
                0.50D,
                0.08D
        );
    }

    private static void foregroundPortalBurst(ServerPlayer player, ServerLevel level) {
        Vec3 foreground = foregroundOrigin(player.getEyePosition(), player.getLookAngle());
        level.sendParticles(
                player,
                ParticleTypes.PORTAL,
                false,
                false,
                foreground.x,
                foreground.y,
                foreground.z,
                FOREGROUND_SUCCESS_PARTICLE_COUNT,
                0.24D,
                0.16D,
                0.24D,
                0.08D
        );
    }
}
