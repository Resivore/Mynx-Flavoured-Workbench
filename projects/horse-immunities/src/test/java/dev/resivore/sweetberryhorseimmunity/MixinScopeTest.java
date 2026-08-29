package dev.resivore.sweetberryhorseimmunity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinScopeTest {
    private static final String MIXIN_SOURCE =
        "src/main/java/dev/resivore/sweetberryhorseimmunity/mixin/SweetBerryBushBlockMixin.java";
    private static final String POWDER_SNOW_MIXIN_SOURCE =
        "src/main/java/dev/resivore/sweetberryhorseimmunity/mixin/PowderSnowBlockMixin.java";
    private static final String LIVING_ENTITY_MIXIN_SOURCE =
        "src/main/java/dev/resivore/sweetberryhorseimmunity/mixin/LivingEntityMixin.java";
    private static final String POLICY_SOURCE =
        "src/main/java/dev/resivore/sweetberryhorseimmunity/SweetBerryHorseImmunity.java";

    @Test
    void interceptionIsLimitedToTheBerryBushOverride() throws Exception {
        String mixin = read(MIXIN_SOURCE);

        assertAll(
            () -> assertTrue(mixin.contains("@Mixin(SweetBerryBushBlock.class)")),
            () -> assertTrue(mixin.contains("entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V")),
            () -> assertTrue(mixin.contains("@At(\"HEAD\")")),
            () -> assertTrue(mixin.contains("cancellable = true")),
            () -> assertTrue(mixin.contains("require = 1")),
            () -> assertTrue(mixin.contains("Entity vehicle = entity.getVehicle()")),
            () -> assertEquals(1, occurrences(mixin, "entity.getVehicle()")),
            () -> assertTrue(mixin.contains("SweetBerryHorseImmunity.shouldBypassSweetBerryEffects(")),
            () -> assertTrue(mixin.contains("entity instanceof Player")),
            () -> assertTrue(mixin.contains("vehicle == null ? null : vehicle.getType()")),
            () -> assertTrue(mixin.contains("entity.resetFallDistance()")),
            () -> assertTrue(mixin.indexOf("entity.resetFallDistance()") < mixin.indexOf("callbackInfo.cancel()")),
            () -> assertTrue(mixin.contains("callbackInfo.cancel()"))
        );

        assertAll(
            () -> assertFalse(mixin.contains("@Redirect")),
            () -> assertFalse(mixin.contains("@Overwrite")),
            () -> assertFalse(mixin.contains("@Mixin(Player.class)")),
            () -> assertFalse(mixin.contains("@Mixin(Entity.class)")),
            () -> assertFalse(mixin.contains("makeStuckInBlock")),
            () -> assertFalse(mixin.contains("hurtServer")),
            () -> assertFalse(mixin.contains("DamageSource")),
            () -> assertFalse(mixin.contains("void move(")),
            () -> assertFalse(mixin.contains("randomTick")),
            () -> assertFalse(mixin.contains("useItemOn")),
            () -> assertFalse(mixin.contains("useWithoutItem")),
            () -> assertFalse(mixin.contains("getPassengers"))
        );
    }

    @Test
    void powderSnowInterceptionIsLimitedToWalkabilityAndAboveBlockCollision() throws Exception {
        String mixin = read(POWDER_SNOW_MIXIN_SOURCE);

        assertAll(
            () -> assertTrue(mixin.contains("@Mixin(PowderSnowBlock.class)")),
            () -> assertTrue(mixin.contains("canEntityWalkOnPowderSnow(Lnet/minecraft/world/entity/Entity;)Z")),
            () -> assertTrue(mixin.contains("getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;")),
            () -> assertEquals(2, occurrences(mixin, "@Inject(")),
            () -> assertEquals(2, occurrences(mixin, "@At(\"HEAD\")")),
            () -> assertEquals(2, occurrences(mixin, "cancellable = true")),
            () -> assertEquals(2, occurrences(mixin, "require = 1")),
            () -> assertTrue(mixin.contains("SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(entity)")),
            () -> assertTrue(mixin.contains("context.isPlacement()")),
            () -> assertTrue(mixin.contains("context instanceof EntityCollisionContext")),
            () -> assertTrue(mixin.contains("context.isAbove(Shapes.block(), pos, false)")),
            () -> assertTrue(mixin.contains("callbackInfo.setReturnValue(Shapes.block())"))
        );

        assertAll(
            () -> assertFalse(mixin.contains("entityInside")),
            () -> assertFalse(mixin.contains("context.isDescending()")),
            () -> assertFalse(mixin.contains("makeStuckInBlock")),
            () -> assertFalse(mixin.contains("setTicksFrozen")),
            () -> assertFalse(mixin.contains("hurtServer")),
            () -> assertFalse(mixin.contains("@Redirect")),
            () -> assertFalse(mixin.contains("@Overwrite"))
        );
    }

    @Test
    void freezingInterceptionIsLimitedToLivingEntityCanFreeze() throws Exception {
        String mixin = read(LIVING_ENTITY_MIXIN_SOURCE);

        assertAll(
            () -> assertTrue(mixin.contains("@Mixin(LivingEntity.class)")),
            () -> assertTrue(mixin.contains("canFreeze()Z")),
            () -> assertEquals(1, occurrences(mixin, "@Inject(")),
            () -> assertTrue(mixin.contains("@At(\"HEAD\")")),
            () -> assertTrue(mixin.contains("cancellable = true")),
            () -> assertTrue(mixin.contains("require = 1")),
            () -> assertTrue(mixin.contains("SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(entity)")),
            () -> assertTrue(mixin.contains("callbackInfo.setReturnValue(false)"))
        );

        assertAll(
            () -> assertFalse(mixin.contains("PowderSnowBlock")),
            () -> assertFalse(mixin.contains("setTicksFrozen")),
            () -> assertFalse(mixin.contains("clearFreeze")),
            () -> assertFalse(mixin.contains("hurtServer")),
            () -> assertFalse(mixin.contains("@Redirect")),
            () -> assertFalse(mixin.contains("@Overwrite"))
        );
    }

    @Test
    void canary1HorsePolicyRemainsSourceEquivalent() throws Exception {
        String policy = read(POLICY_SOURCE).replaceAll("\\s+", "");

        assertTrue(policy.contains(
            "publicstaticbooleanshouldBypassSweetBerryEffects(EntityType<?>entityType)"
                + "{returnentityType==EntityTypes.HORSE;}"
        ));
    }

    @Test
    void mixinConfigurationContainsOnlyTheThreeScopedMixins() throws Exception {
        String config = read("src/main/resources/sweet_berry_horse_immunity.mixins.json")
            .replaceAll("\\s+", "");

        assertEquals(
            "{\"required\":true,\"package\":\"dev.resivore.sweetberryhorseimmunity.mixin\","
                + "\"compatibilityLevel\":\"JAVA_25\",\"mixins\":[\"LivingEntityMixin\","
                + "\"PowderSnowBlockMixin\",\"SweetBerryBushBlockMixin\"],"
                + "\"injectors\":{\"defaultRequire\":1}}",
            config
        );
    }

    private static String read(String relativePath) throws Exception {
        Path projectRoot = Path.of(System.getProperty("projectRoot"));
        return Files.readString(projectRoot.resolve(relativePath));
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
