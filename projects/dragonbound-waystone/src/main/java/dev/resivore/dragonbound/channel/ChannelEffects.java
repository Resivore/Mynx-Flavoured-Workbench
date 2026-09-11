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
    static final int CHANNEL_PARTICLES_AT_START = 1;
    static final int CHANNEL_PARTICLES_AT_COMPLETION = 4;

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
        if (channelTicks <= 1) {
            return CHANNEL_PARTICLES_AT_START;
        }
        long clampedElapsed = Math.max(0L, Math.min(elapsedTicks, channelTicks - 1L));
        long range = CHANNEL_PARTICLES_AT_COMPLETION - CHANNEL_PARTICLES_AT_START;
        return CHANNEL_PARTICLES_AT_START + (int) (clampedElapsed * range / (channelTicks - 1L));
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
}
