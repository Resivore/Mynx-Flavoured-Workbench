package dev.resivore.enderscapepruning;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Current Matcha one-heart unit: Regeneration III for 24 ticks, with no hunger restoration. */
public final class MatchaHealing {
    public static final int REGENERATION_III = 2;
    public static final int ONE_HEART_TICKS = 24;
    public static final int TWO_HEART_TICKS = 48;

    private MatchaHealing() {
    }

    public static void grantOneHeart(ServerLevel level, Player player) {
        grant(level, player, ONE_HEART_TICKS);
    }

    public static void grantTwoHearts(ServerLevel level, Player player) {
        grant(level, player, TWO_HEART_TICKS);
    }

    private static void grant(ServerLevel level, Player player, int duration) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, REGENERATION_III, false, false, false));
    }
}
