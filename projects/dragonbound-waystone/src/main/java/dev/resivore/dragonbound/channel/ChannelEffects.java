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
    static final int FOREGROUND_SUCCESS_PARTICLE_COUNT = 24;
    static final int CHANNEL_PARTICLES_AT_START = 1;
    static final int CHANNEL_PARTICLES_AT_COMPLETION = 4;
    static final int FOREGROUND_CHANNEL_PARTICLES_AT_START = 3;
    static final int FOREGROUND_CHANNEL_PARTICLES_AT_COMPLETION = 8;
    static final double FOREGROUND_GROUND_OFFSET = 0.05D;
    static final double FOREGROUND_CHANNEL_HORIZONTAL_RADIUS = 0.65D;
    static final double FOREGROUND_SUCCESS_HORIZONTAL_RADIUS = 0.75D;
    static final double FOREGROUND_CHANNEL_VERTICAL_SPREAD = 0.05D;
    static final double FOREGROUND_SUCCESS_VERTICAL_SPREAD = 0.05D;
    static final double FOREGROUND_MOTION_HORIZONTAL_MAX = 0.035D;
    static final double FOREGROUND_MOTION_VERTICAL_MIN = 0.08D;
    static final double FOREGROUND_MOTION_VERTICAL_MAX = 0.18D;

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

    static Vec3 foregroundGroundOrigin(Vec3 playerPosition) {
        return playerPosition.add(0.0D, FOREGROUND_GROUND_OFFSET, 0.0D);
    }

    static Vec3 foregroundParticleStart(Vec3 groundOrigin, double horizontalX, double verticalOffset, double horizontalZ) {
        return groundOrigin.add(horizontalX, verticalOffset, horizontalZ);
    }

    static Vec3 foregroundPortalMotion(double horizontalX, double verticalY, double horizontalZ) {
        return new Vec3(horizontalX, verticalY, horizontalZ);
    }

    /**
     * The 26.2 {@code PortalParticle} evaluates its first client tick as
     * {@code yStart + velocityY * (1 - age / lifetime)^2 + (1 - age / lifetime)}.
     * A positive explicit Y vector therefore launches a particle upward from its low packet
     * origin; normal count packets cannot guarantee that because they Gaussian-randomize all
     * velocity axes. This helper keeps the exact packet-vector behavior unit-testable.
     */
    static double portalFirstTickVerticalRise(double verticalMotion, int lifetime) {
        double progress = 1.0D / lifetime;
        double remaining = 1.0D - progress;
        return verticalMotion * remaining * remaining + remaining;
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
        targetedRisingPortalParticles(
                player,
                level,
                foregroundChannelParticleCount(elapsedTicks, channelTicks),
                FOREGROUND_CHANNEL_HORIZONTAL_RADIUS,
                FOREGROUND_CHANNEL_VERTICAL_SPREAD
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
        targetedRisingPortalParticles(
                player,
                level,
                FOREGROUND_SUCCESS_PARTICLE_COUNT,
                FOREGROUND_SUCCESS_HORIZONTAL_RADIUS,
                FOREGROUND_SUCCESS_VERTICAL_SPREAD
        );
    }

    /** Sends count-zero packets so each targeted Portal particle has a deliberate upward launch. */
    private static void targetedRisingPortalParticles(
            ServerPlayer player,
            ServerLevel level,
            int count,
            double horizontalRadius,
            double verticalSpread) {
        Vec3 groundOrigin = foregroundGroundOrigin(player.position());
        for (int index = 0; index < count; index++) {
            double angle = level.getRandom().nextDouble() * (2.0D * Math.PI);
            double radius = Math.sqrt(level.getRandom().nextDouble()) * horizontalRadius;
            Vec3 start = foregroundParticleStart(
                    groundOrigin,
                    Math.cos(angle) * radius,
                    centeredRandom(level, verticalSpread),
                    Math.sin(angle) * radius);
            Vec3 motion = foregroundPortalMotion(
                    centeredRandom(level, FOREGROUND_MOTION_HORIZONTAL_MAX),
                    FOREGROUND_MOTION_VERTICAL_MIN
                            + level.getRandom().nextDouble()
                            * (FOREGROUND_MOTION_VERTICAL_MAX - FOREGROUND_MOTION_VERTICAL_MIN),
                    centeredRandom(level, FOREGROUND_MOTION_HORIZONTAL_MAX));
            level.sendParticles(
                    player,
                    ParticleTypes.PORTAL,
                    false,
                    false,
                    start.x,
                    start.y,
                    start.z,
                    0,
                    motion.x,
                    motion.y,
                    motion.z,
                    1.0D
            );
        }
    }

    private static double centeredRandom(ServerLevel level, double maximumMagnitude) {
        return (level.getRandom().nextDouble() * 2.0D - 1.0D) * maximumMagnitude;
    }
}
