package dev.resivore.dragonbound.channel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Server-owned audiovisual feedback for the shared Dragonbound channel lifecycle. */
final class ChannelEffects {
    static final int AMBIENT_PARTICLE_INTERVAL_TICKS = 4;
    static final int AMBIENT_PARTICLE_COUNT = 3;
    static final int SUCCESS_PARTICLE_COUNT = 30;

    private ChannelEffects() {
    }

    static void channelStarted(ServerPlayer player) {
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    static boolean shouldEmitAmbientParticles(long gameTime) {
        return gameTime % AMBIENT_PARTICLE_INTERVAL_TICKS == 0L;
    }

    static void channelTick(ServerPlayer player, long gameTime) {
        if (!shouldEmitAmbientParticles(gameTime)) {
            return;
        }
        ((ServerLevel) player.level()).sendParticles(
                ParticleTypes.PORTAL,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.5D,
                player.getZ(),
                AMBIENT_PARTICLE_COUNT,
                0.35D,
                0.70D,
                0.35D,
                0.02D
        );
    }

    static void successfulTeleport(
            ServerLevel departureLevel,
            Vec3 departurePosition,
            ServerLevel arrivalLevel,
            Vec3 arrivalPosition
    ) {
        portalBurst(departureLevel, departurePosition);
        portalBurst(arrivalLevel, arrivalPosition);
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
