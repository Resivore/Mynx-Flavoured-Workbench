package dev.resivore.sweetberryhorseimmunity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SweetBerryBushVanillaContractTest {
    @Test
    void minecraft26_2KeepsBerryEffectsInTheTargetedOverride() throws Exception {
        Method method = SweetBerryBushBlock.class.getDeclaredMethod(
            "entityInside",
            BlockState.class,
            Level.class,
            BlockPos.class,
            Entity.class,
            InsideBlockEffectApplier.class,
            boolean.class
        );
        assertEquals(void.class, method.getReturnType());

        byte[] classBytes;
        try (InputStream input = SweetBerryBushBlock.class.getResourceAsStream("SweetBerryBushBlock.class")) {
            assertNotNull(input);
            classBytes = input.readAllBytes();
        }
        String classContract = new String(classBytes, StandardCharsets.ISO_8859_1);

        assertAll(
            () -> assertTrue(classContract.contains("LivingEntity")),
            () -> assertTrue(classContract.contains("FOX")),
            () -> assertTrue(classContract.contains("BEE")),
            () -> assertTrue(classContract.contains("makeStuckInBlock")),
            () -> assertTrue(classContract.contains("sweetBerryBush")),
            () -> assertTrue(classContract.contains("hurtServer"))
        );

        Method vehicleMethod = Entity.class.getDeclaredMethod("getVehicle");
        assertEquals(Entity.class, vehicleMethod.getReturnType());

        byte[] entityClassBytes;
        try (InputStream input = Entity.class.getResourceAsStream("Entity.class")) {
            assertNotNull(input);
            entityClassBytes = input.readAllBytes();
        }
        String entityContract = new String(entityClassBytes, StandardCharsets.ISO_8859_1);
        assertAll(
            () -> assertTrue(entityContract.contains("checkInsideBlocks")),
            () -> assertTrue(entityContract.contains("getEntityInsideCollisionShape")),
            () -> assertTrue(entityContract.contains("entityInside"))
        );
    }
}
