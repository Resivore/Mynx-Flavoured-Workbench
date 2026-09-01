package dev.resivore.matchaheart;

import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Server-authoritative audiovisual feedback for successful semantic Heart use. */
final class HeartUseFeedback {
    private static final double CHEST_HEIGHT_FRACTION = 0.65D;

    static final Profile CRYSTAL = new Profile(
            SoundEvents.BEACON_POWER_SELECT,
            0.70F,
            1.15F,
            ParticleTypes.END_ROD,
            10,
            0.22D,
            0.30D,
            0.22D,
            0.015D);

    static final Profile REINFORCED = new Profile(
            SoundEvents.BEACON_POWER_SELECT,
            0.75F,
            0.90F,
            ParticleTypes.GLOW,
            14,
            0.30D,
            0.35D,
            0.30D,
            0.010D);

    private HeartUseFeedback() {}

    static Optional<Profile> profileForSuccessfulUse(Kind kind, int before, int after) {
        if (after == before) {
            return Optional.empty();
        }
        return Optional.of(kind == Kind.CRYSTAL ? CRYSTAL : REINFORCED);
    }

    static void emitSuccessful(ServerPlayer player, Kind kind, int before, int after) {
        profileForSuccessfulUse(kind, before, after).ifPresent(profile -> emit(player, profile));
    }

    private static void emit(ServerPlayer player, Profile profile) {
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY(CHEST_HEIGHT_FRACTION);
        double z = player.getZ();

        level.playSound(
                null,
                x,
                y,
                z,
                profile.sound(),
                SoundSource.PLAYERS,
                profile.volume(),
                profile.pitch());
        level.sendParticles(
                profile.particle(),
                x,
                y,
                z,
                profile.particleCount(),
                profile.xSpread(),
                profile.ySpread(),
                profile.zSpread(),
                profile.speed());
    }

    enum Kind {
        CRYSTAL,
        REINFORCED
    }

    record Profile(
            SoundEvent sound,
            float volume,
            float pitch,
            SimpleParticleType particle,
            int particleCount,
            double xSpread,
            double ySpread,
            double zSpread,
            double speed) {}
}
