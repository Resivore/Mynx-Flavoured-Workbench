package dev.resivore.matchafrost;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
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

    private static String classContract(Class<?> type) throws Exception {
        try (InputStream input = type.getResourceAsStream(type.getSimpleName() + ".class")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
