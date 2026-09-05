package dev.resivore.mynxtrees;

import java.util.*;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SilverBirchBaseModelTest {
    private static final class Model implements BlockStateModel, FabricBlockStateModel {
        int emits, parts;
        final int flags;
        Predicate<Direction> lastCull;
        Model(int flags) { this.flags = flags; }
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) { parts++; }
        public Material.Baked particleMaterial() { return null; }
        public int materialFlags() { return flags; }
        public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state,
                              RandomSource random, Predicate<Direction> cull) { emits++; lastCull = cull; }
        public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) { return this; }
        public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) { return null; }
        public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) { return flags; }
    }

    @Test void noWorldFallbackUsesOnlyOrdinaryGeometryAndPassesThroughCulling() {
        var ordinary = new Model(1); var base = new Model(2);
        var model = new SilverBirchBaseModel(ordinary, base);
        Predicate<Direction> cull = d -> d == Direction.DOWN;
        model.emitQuads(null, null, null, null, null, cull);
        model.collectParts(null, new ArrayList<>());
        assertEquals(1, ordinary.emits); assertEquals(1, ordinary.parts);
        assertEquals(0, base.emits); assertEquals(0, base.parts);
        assertSame(cull, ordinary.lastCull);
        assertEquals(1, model.materialFlags(null, null, null, null));
        assertEquals(3, model.materialFlags());
        assertEquals(GroundContact.geometryKey(model, false, ordinary), model.createGeometryKey(null, null, null, null));
    }
}
