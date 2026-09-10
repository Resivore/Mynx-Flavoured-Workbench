package dev.resivore.dragonbound.channel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Server-owned arrival feedback for an already confirmed Dragonbound teleport. */
final class ChannelEffects {
    static final int SUCCESS_PARTICLE_COUNT = 30;

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
