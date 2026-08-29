package dev.resivore.sweetberryhorseimmunity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowderSnowVanillaContractTest {
    @Test
    void minecraft26_2KeepsPowderSnowAtTheTargetedCollisionAndFreezeSeams() throws Exception {
        Method walkability = PowderSnowBlock.class.getDeclaredMethod(
            "canEntityWalkOnPowderSnow",
            Entity.class
        );
        Method collision = PowderSnowBlock.class.getDeclaredMethod(
            "getCollisionShape",
            BlockState.class,
            BlockGetter.class,
            BlockPos.class,
            CollisionContext.class
        );
        Method inside = PowderSnowBlock.class.getDeclaredMethod(
            "entityInside",
            BlockState.class,
            Level.class,
            BlockPos.class,
            Entity.class,
            InsideBlockEffectApplier.class,
            boolean.class
        );
        Method canFreeze = LivingEntity.class.getDeclaredMethod("canFreeze");
        Method horseCanFreeze = Horse.class.getMethod("canFreeze");
        Method playerCanFreeze = Player.class.getMethod("canFreeze");
        Method rideTick = Player.class.getDeclaredMethod("rideTick");

        assertAll(
            () -> assertEquals(boolean.class, walkability.getReturnType()),
            () -> assertTrue(Modifier.isStatic(walkability.getModifiers())),
            () -> assertEquals(VoxelShape.class, collision.getReturnType()),
            () -> assertEquals(void.class, inside.getReturnType()),
            () -> assertEquals(boolean.class, canFreeze.getReturnType()),
            () -> assertEquals(LivingEntity.class, horseCanFreeze.getDeclaringClass()),
            () -> assertEquals(LivingEntity.class, playerCanFreeze.getDeclaringClass()),
            () -> assertEquals(void.class, rideTick.getReturnType())
        );

        String powderSnowContract = classContract(PowderSnowBlock.class);
        assertAll(
            () -> assertTrue(powderSnowContract.contains("canEntityWalkOnPowderSnow")),
            () -> assertTrue(powderSnowContract.contains("FALLING_COLLISION_SHAPE")),
            () -> assertTrue(powderSnowContract.contains("isAbove")),
            () -> assertTrue(powderSnowContract.contains("isDescending")),
            () -> assertTrue(powderSnowContract.contains("makeStuckInBlock")),
            () -> assertTrue(powderSnowContract.contains("FREEZE"))
        );

        String freezeEffectContract = classContract(InsideBlockEffectType.class);
        assertAll(
            () -> assertTrue(freezeEffectContract.contains("setIsInPowderSnow")),
            () -> assertTrue(freezeEffectContract.contains("canFreeze")),
            () -> assertTrue(freezeEffectContract.contains("getTicksRequiredToFreeze")),
            () -> assertTrue(freezeEffectContract.contains("setTicksFrozen"))
        );

        String livingEntityContract = classContract(LivingEntity.class);
        assertAll(
            () -> assertTrue(livingEntityContract.contains("isInPowderSnow")),
            () -> assertTrue(livingEntityContract.contains("canFreeze")),
            () -> assertTrue(livingEntityContract.contains("setTicksFrozen")),
            () -> assertTrue(livingEntityContract.contains("isFullyFrozen")),
            () -> assertTrue(livingEntityContract.contains("freeze")),
            () -> assertTrue(livingEntityContract.contains("hurtServer"))
        );

        String playerContract = classContract(Player.class);
        assertAll(
            () -> assertTrue(playerContract.contains("rideTick")),
            () -> assertTrue(playerContract.contains("stopRiding")),
            () -> assertTrue(playerContract.contains("getVehicle"))
        );
    }

    private static String classContract(Class<?> type) throws Exception {
        try (InputStream input = type.getResourceAsStream(type.getSimpleName() + ".class")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
