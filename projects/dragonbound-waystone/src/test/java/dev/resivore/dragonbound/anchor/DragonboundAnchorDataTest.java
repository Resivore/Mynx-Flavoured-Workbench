package dev.resivore.dragonbound.anchor;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DragonboundAnchorDataTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void placementAndRelocationUseFreshMonotonicGenerations() {
        DragonboundAnchorData data = new DragonboundAnchorData();

        AnchorBinding first = data.bind(Level.OVERWORLD, new BlockPos(4, 70, -8));
        AnchorBinding second = data.bind(Level.NETHER, new BlockPos(-11, 55, 19));

        assertEquals(1L, first.generation());
        assertEquals(2L, second.generation());
        assertEquals(second, data.active().orElseThrow());
        assertTrue(data.matches(second));
        assertFalse(data.matches(first));
        assertTrue(data.isDirty());
    }

    @Test
    void removalClearsOnlyTheAuthoritativePhysicalAnchor() {
        DragonboundAnchorData data = new DragonboundAnchorData();
        BlockPos oldPos = new BlockPos(1, 64, 1);
        BlockPos activePos = new BlockPos(2, 64, 2);

        AnchorBinding old = data.bind(Level.OVERWORLD, oldPos);
        AnchorBinding active = data.bind(Level.END, activePos);
        data.setDirty(false);

        assertFalse(data.clearIfMatching(Level.OVERWORLD, oldPos));
        assertFalse(data.clearIfMatching(old));
        assertTrue(data.matches(active));
        assertFalse(data.isDirty());

        assertTrue(data.clearIfMatching(Level.END, activePos));
        assertTrue(data.active().isEmpty());
        assertTrue(data.isDirty());
        assertEquals(2L, data.lastGeneration());
    }

    @Test
    void clearingAndReplacingAtSameCoordinatesStillInvalidatesOldGeneration() {
        DragonboundAnchorData data = new DragonboundAnchorData();
        BlockPos pos = new BlockPos(9, 80, 9);

        AnchorBinding first = data.bind(Level.OVERWORLD, pos);
        assertTrue(data.clearIfMatching(first));
        AnchorBinding replacement = data.bind(Level.OVERWORLD, pos);

        assertEquals(first.pos(), replacement.pos());
        assertEquals(first.dimension(), replacement.dimension());
        assertEquals(first.generation() + 1L, replacement.generation());
        assertFalse(data.matches(first));
        assertTrue(data.matches(replacement));
    }

    @Test
    void codecRoundTripPreservesCrossDimensionBindingAndNextGeneration() {
        DragonboundAnchorData original = new DragonboundAnchorData();
        original.bind(Level.OVERWORLD, new BlockPos(0, 64, 0));
        AnchorBinding expected = original.bind(Level.END, new BlockPos(42, 73, -17));

        JsonElement encoded = DragonboundAnchorData.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        DragonboundAnchorData restored = DragonboundAnchorData.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(expected, restored.active().orElseThrow());
        assertEquals(2L, restored.lastGeneration());

        AnchorBinding afterRestart = restored.bind(Level.NETHER, new BlockPos(-4, 45, 7));
        assertEquals(3L, afterRestart.generation());
    }
}
