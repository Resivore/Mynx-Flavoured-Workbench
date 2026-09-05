package dev.resivore.mynxtrees;

import java.util.List;
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

/** Selects one baked cube, preserving its cull faces, UVs, lighting and material flags. */
final class SilverBirchBaseModel implements BlockStateModel, FabricBlockStateModel {
    private final BlockStateModel ordinary;
    private final BlockStateModel base;

    SilverBirchBaseModel(BlockStateModel ordinary, BlockStateModel base) {
        this.ordinary = ordinary;
        this.base = base;
    }

    private boolean grounded(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return level != null && level != BlockAndTintGetter.EMPTY
                && GroundContact.matches(state, pos, level::getBlockState,
                        SilverBirchBaseModels::uprightLog, below -> below.is(SilverBirchBaseModels.SOILS));
    }

    private FabricBlockStateModel delegate(boolean grounded) {
        return (FabricBlockStateModel) (grounded ? base : ordinary);
    }

    @Override public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
                                    BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        delegate(grounded(level, pos, state)).emitQuads(emitter, level, pos, state, random, cullTest);
    }

    @Override public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos,
                                               BlockState state, RandomSource random) {
        boolean grounded = grounded(level, pos, state);
        return GroundContact.geometryKey(this, grounded,
                delegate(grounded).createGeometryKey(level, pos, state, random));
    }

    @Override public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return delegate(grounded(level, pos, state)).particleMaterial(level, pos, state);
    }

    @Override public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return delegate(grounded(level, pos, state)).materialFlags(level, pos, state, random);
    }

    // Vanilla callers with no world context, including fallback rendering, retain ordinary bark.
    @Override public void collectParts(RandomSource random, List<BlockStateModelPart> output) { ordinary.collectParts(random, output); }
    @Override public Material.Baked particleMaterial() { return ordinary.particleMaterial(); }
    @Override public int materialFlags() { return ordinary.materialFlags() | base.materialFlags(); }
}
