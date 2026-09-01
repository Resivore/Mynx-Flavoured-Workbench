package dev.resivore.matchaheart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class HeartUseFeedbackTest {
    private static final Path PROJECT = Path.of(System.getProperty("workbenchRoot"))
            .resolve("projects/matcha-heart-death-compat");

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void crystalSuccessSelectsHigherBeaconToneAndEndRodProfile() {
        int before = 20;
        int after = HeartRules.afterCrystal(before);

        assertEquals(22, after, "Crystal Heart state transition changed");
        HeartUseFeedback.Profile profile = HeartUseFeedback.profileForSuccessfulUse(
                HeartUseFeedback.Kind.CRYSTAL, before, after).orElseThrow();

        assertSame(HeartUseFeedback.CRYSTAL, profile);
        assertSame(SoundEvents.BEACON_POWER_SELECT, profile.sound());
        assertEquals(Identifier.withDefaultNamespace("block.beacon.power_select"),
                BuiltInRegistries.SOUND_EVENT.getKey(profile.sound()));
        assertEquals(0.70F, profile.volume());
        assertEquals(1.15F, profile.pitch());
        assertEquals(Identifier.withDefaultNamespace("end_rod"),
                BuiltInRegistries.PARTICLE_TYPE.getKey(profile.particle()));
        assertEquals(10, profile.particleCount());
        assertEquals(0.22D, profile.xSpread());
        assertEquals(0.30D, profile.ySpread());
        assertEquals(0.22D, profile.zSpread());
        assertEquals(0.015D, profile.speed());
    }

    @Test
    void reinforcedSuccessSelectsLowerBeaconToneAndGlowProfile() {
        int before = 18;
        int after = HeartRules.afterReinforced(before);

        assertEquals(20, after, "Reinforced Crystal Heart state transition changed");
        HeartUseFeedback.Profile profile = HeartUseFeedback.profileForSuccessfulUse(
                HeartUseFeedback.Kind.REINFORCED, before, after).orElseThrow();

        assertSame(HeartUseFeedback.REINFORCED, profile);
        assertSame(SoundEvents.BEACON_POWER_SELECT, profile.sound());
        assertEquals(Identifier.withDefaultNamespace("block.beacon.power_select"),
                BuiltInRegistries.SOUND_EVENT.getKey(profile.sound()));
        assertEquals(0.75F, profile.volume());
        assertEquals(0.90F, profile.pitch());
        assertEquals(Identifier.withDefaultNamespace("glow"),
                BuiltInRegistries.PARTICLE_TYPE.getKey(profile.particle()));
        assertEquals(14, profile.particleCount());
        assertEquals(0.30D, profile.xSpread());
        assertEquals(0.35D, profile.ySpread());
        assertEquals(0.30D, profile.zSpread());
        assertEquals(0.010D, profile.speed());
    }

    @Test
    void rejectedHeartTransitionsSelectNoSuccessFeedback() {
        assertTrue(HeartUseFeedback.profileForSuccessfulUse(
                HeartUseFeedback.Kind.CRYSTAL, 18, HeartRules.afterCrystal(18)).isEmpty());
        assertTrue(HeartUseFeedback.profileForSuccessfulUse(
                HeartUseFeedback.Kind.CRYSTAL, 60, HeartRules.afterCrystal(60)).isEmpty());
        assertTrue(HeartUseFeedback.profileForSuccessfulUse(
                HeartUseFeedback.Kind.REINFORCED, 20, HeartRules.afterReinforced(20)).isEmpty());
        assertTrue(HeartUseFeedback.profileForSuccessfulUse(
                HeartUseFeedback.Kind.REINFORCED, 24, HeartRules.afterReinforced(24)).isEmpty());
    }

    @Test
    void productionDispatchRemainsServerMainHandSuccessOnlyAndPostState() throws Exception {
        String initializer = Files.readString(PROJECT.resolve(
                "src/main/java/dev/resivore/matchaheart/MatchaHeartDeathCompat.java"),
                StandardCharsets.UTF_8);
        String feedback = Files.readString(PROJECT.resolve(
                "src/main/java/dev/resivore/matchaheart/HeartUseFeedback.java"),
                StandardCharsets.UTF_8);

        int serverMainHandGuard = initializer.indexOf(
                "level.isClientSide() || hand != InteractionHand.MAIN_HAND");
        int rejectionGate = initializer.indexOf("if (after == before)");
        int consume = initializer.indexOf("stack.shrink(1)");
        int stateChange = initializer.indexOf("setState(serverPlayer, after)");
        int feedbackDispatch = initializer.indexOf("HeartUseFeedback.emitSuccessful(");
        int success = initializer.indexOf("return InteractionResult.SUCCESS");

        assertTrue(serverMainHandGuard >= 0);
        assertTrue(serverMainHandGuard < rejectionGate);
        assertTrue(rejectionGate < consume);
        assertTrue(consume < stateChange);
        assertTrue(stateChange < feedbackDispatch,
                "Success feedback must dispatch only after the existing state change");
        assertTrue(feedbackDispatch < success);

        assertTrue(feedback.contains("ServerLevel level = player.level()"));
        assertTrue(feedback.contains("player.getY(CHEST_HEIGHT_FRACTION)"));
        assertTrue(feedback.contains("level.playSound("));
        assertTrue(feedback.contains("level.sendParticles("));
        assertFalse(feedback.contains("ParticleTypes.HEART"));
    }
}
