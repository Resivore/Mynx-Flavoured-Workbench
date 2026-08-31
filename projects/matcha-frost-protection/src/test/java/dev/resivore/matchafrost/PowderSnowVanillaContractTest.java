package dev.resivore.matchafrost;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.Hud;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

class PowderSnowVanillaContractTest {
    @Test
    void minecraft26_2RetainsTheLeatherBootsTraversalDecision() throws Exception {
        Method walkability = PowderSnowBlock.class.getDeclaredMethod(
                "canEntityWalkOnPowderSnow", Entity.class);
        Method collision = PowderSnowBlock.class.getDeclaredMethod(
                "getCollisionShape",
                BlockState.class,
                BlockGetter.class,
                BlockPos.class,
                CollisionContext.class);

        assertAll(
                () -> assertEquals(boolean.class, walkability.getReturnType()),
                () -> assertTrue(Modifier.isStatic(walkability.getModifiers())),
                () -> assertEquals(VoxelShape.class, collision.getReturnType()));

        String contract = classContract(PowderSnowBlock.class);
        assertAll(
                () -> assertTrue(contract.contains("canEntityWalkOnPowderSnow")),
                () -> assertTrue(contract.contains("POWDER_SNOW_WALKABLE_MOBS")),
                () -> assertTrue(contract.contains("LEATHER_BOOTS")),
                () -> assertTrue(contract.contains("getItemBySlot")),
                () -> assertTrue(contract.contains("isAbove")),
                () -> assertTrue(contract.contains("isDescending")));
    }

    @Test
    void minecraft26_2RetainsLeatherWearablesUnderlyingFreezeStatePath() throws Exception {
        Method entityInside = PowderSnowBlock.class.getDeclaredMethod(
                "entityInside",
                BlockState.class,
                Level.class,
                BlockPos.class,
                Entity.class,
                InsideBlockEffectApplier.class,
                boolean.class);
        Method canFreeze = LivingEntity.class.getDeclaredMethod("canFreeze");

        assertAll(
                () -> assertEquals(void.class, entityInside.getReturnType()),
                () -> assertEquals(boolean.class, canFreeze.getReturnType()),
                () -> assertTrue(Modifier.isPublic(canFreeze.getModifiers())));

        String powderSnow = classContract(PowderSnowBlock.class);
        String freezeEffect = classContract(InsideBlockEffectType.class);
        String livingEntity = classContract(LivingEntity.class);
        assertAll(
                () -> assertTrue(powderSnow.contains("entityInside")),
                () -> assertTrue(powderSnow.contains("FREEZE")),
                () -> assertTrue(freezeEffect.contains("setIsInPowderSnow")),
                () -> assertTrue(freezeEffect.contains("canFreeze")),
                () -> assertTrue(freezeEffect.contains("getTicksRequiredToFreeze")),
                () -> assertTrue(freezeEffect.contains("getTicksFrozen")),
                () -> assertTrue(freezeEffect.contains("setTicksFrozen")),
                () -> assertTrue(livingEntity.contains("FREEZE_IMMUNE_WEARABLES")),
                () -> assertTrue(livingEntity.contains("isInPowderSnow")),
                () -> assertTrue(livingEntity.contains("isFullyFrozen")),
                () -> assertTrue(livingEntity.contains("freeze")),
                () -> assertTrue(livingEntity.contains("hurtServer")));
    }

    @Test
    void minecraft26_2LeatherBootsBaselinePreventsTheFreezeOverlayThroughState()
            throws Exception {
        String tagPath = "/data/minecraft/tags/item/freeze_immune_wearables.json";
        try (InputStream input = PowderSnowBlock.class.getResourceAsStream(tagPath)) {
            assertNotNull(input, () -> "Missing vanilla tag: " + tagPath);
            var values = JsonParser.parseReader(
                            new java.io.InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    .getAsJsonArray("values")
                    .asList()
                    .stream()
                    .map(element -> element.getAsString())
                    .collect(Collectors.toUnmodifiableSet());
            assertEquals(Set.of(
                    "minecraft:leather_boots",
                    "minecraft:leather_leggings",
                    "minecraft:leather_chestplate",
                    "minecraft:leather_helmet",
                    "minecraft:leather_horse_armor"), values);
        }

        String hud = classContract(Hud.class);
        assertAll(
                () -> assertTrue(hud.contains("textures/misc/powder_snow_outline.png")),
                () -> assertTrue(hud.contains("getTicksFrozen")),
                () -> assertTrue(hud.contains("getPercentFrozen")));
    }

    private static String classContract(Class<?> type) throws Exception {
        try (InputStream input = type.getResourceAsStream(type.getSimpleName() + ".class")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
