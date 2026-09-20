package dev.resivore.dragonbound.channel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Server-owned visual feedback for valid Dragonbound channels and confirmed arrivals. */
final class ChannelEffects {
    static final Identifier MIRROR_TELEPORT_SOUND_ID =
            Identifier.fromNamespaceAndPath("enderscape", "item.mirror.teleport");
    static final Identifier MIRROR_TELEPORT_IN_PARTICLE_ID =
            Identifier.fromNamespaceAndPath("enderscape", "mirror_teleport_in");
    static final int MIRROR_ARRIVAL_PARTICLE_COUNT = 50;
    static final double MIRROR_ARRIVAL_Y_OFFSET = 0.5D;
    static final double MIRROR_ARRIVAL_HORIZONTAL_SPREAD = 0.5D;
    static final double MIRROR_ARRIVAL_VERTICAL_SPREAD = 1.0D;
    static final double MIRROR_ARRIVAL_SPEED = 0.1D;
    static final float MIRROR_ARRIVAL_SOUND_VOLUME = 0.65F;
    static final float MIRROR_ARRIVAL_SOUND_PITCH = 1.0F;
    static final int CHANNEL_PARTICLES_AT_START = 1;
    static final int CHANNEL_PARTICLES_AT_COMPLETION = 4;
    static final int FOREGROUND_CHANNEL_PARTICLES_AT_START = 3;
    static final int FOREGROUND_CHANNEL_PARTICLES_AT_COMPLETION = 8;
    static final double FOREGROUND_GROUND_OFFSET = 0.05D;
    static final double FOREGROUND_CHANNEL_HORIZONTAL_RADIUS = 0.65D;
    static final double FOREGROUND_CHANNEL_VERTICAL_SPREAD = 0.05D;
    static final double FOREGROUND_MOTION_HORIZONTAL_MAX = 0.035D;
    static final double FOREGROUND_MOTION_VERTICAL_MIN = 0.06D;
    static final double FOREGROUND_MOTION_VERTICAL_MAX = 0.12D;

    private ChannelEffects() {
    }

    static void successfulTeleport(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 position = player.position();
        level.sendParticles(
                mirrorTeleportInParticle(),
                position.x,
                position.y + MIRROR_ARRIVAL_Y_OFFSET,
                position.z,
                MIRROR_ARRIVAL_PARTICLE_COUNT,
                MIRROR_ARRIVAL_HORIZONTAL_SPREAD,
                MIRROR_ARRIVAL_VERTICAL_SPREAD,
                MIRROR_ARRIVAL_HORIZONTAL_SPREAD,
                MIRROR_ARRIVAL_SPEED
        );
        level.playSound(
                null,
                position.x,
                position.y,
                position.z,
                mirrorTeleportSound(),
                SoundSource.PLAYERS,
                MIRROR_ARRIVAL_SOUND_VOLUME,
                MIRROR_ARRIVAL_SOUND_PITCH
        );
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

    static Vec3 foregroundReversePortalMotion(double horizontalX, double verticalY, double horizontalZ) {
        return new Vec3(horizontalX, verticalY, horizontalZ);
    }

    /**
     * Minecraft 26.2's {@code ReversePortalParticle} factory receives the count-zero packet's
     * motion values unchanged when packet speed is {@code 1.0}. Its live tick increments age and
     * adds {@code motionY * age / lifetime}; unlike {@code PortalParticle}, it does not add the
     * portal-specific vertical interpolation term or apply gravity. Positive packet Y therefore
     * produces a strictly upward step for every live tick. Its 60--61 tick lifetime yields a
     * cumulative vertical rise of roughly 1.77--3.60 blocks for this canary's 0.06--0.12 range.
     */
    static double reversePortalVerticalStep(double verticalMotion, int age, int lifetime) {
        return verticalMotion * age / lifetime;
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
        targetedRisingReversePortalParticles(
                player,
                level,
                foregroundChannelParticleCount(elapsedTicks, channelTicks),
                FOREGROUND_CHANNEL_HORIZONTAL_RADIUS,
                FOREGROUND_CHANNEL_VERTICAL_SPREAD
        );
    }

    private static SoundEvent mirrorTeleportSound() {
        return BuiltInRegistries.SOUND_EVENT.getOptional(MIRROR_TELEPORT_SOUND_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Required Enderscape Mirror teleport sound is unavailable: " + MIRROR_TELEPORT_SOUND_ID));
    }

    private static SimpleParticleType mirrorTeleportInParticle() {
        return BuiltInRegistries.PARTICLE_TYPE.getOptional(MIRROR_TELEPORT_IN_PARTICLE_ID)
                .filter(SimpleParticleType.class::isInstance)
                .map(SimpleParticleType.class::cast)
                .orElseThrow(() -> new IllegalStateException(
                        "Required Enderscape Mirror arrival particle is unavailable: "
                                + MIRROR_TELEPORT_IN_PARTICLE_ID));
    }

    /** Sends count-zero packets so each targeted reverse-portal particle has a deliberate upward launch. */
    private static void targetedRisingReversePortalParticles(
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
            Vec3 motion = foregroundReversePortalMotion(
                    centeredRandom(level, FOREGROUND_MOTION_HORIZONTAL_MAX),
                    FOREGROUND_MOTION_VERTICAL_MIN
                            + level.getRandom().nextDouble()
                            * (FOREGROUND_MOTION_VERTICAL_MAX - FOREGROUND_MOTION_VERTICAL_MIN),
                    centeredRandom(level, FOREGROUND_MOTION_HORIZONTAL_MAX));
            level.sendParticles(
                    player,
                    ParticleTypes.REVERSE_PORTAL,
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
