package dev.resivore.bgebushyleaves.client;

import dev.resivore.bgebushyleaves.BgeLeafEligibility;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/** Retains BGE's clean model, then emits only real exterior foliage from its canonical source. */
final class BushyLeafBlockStateModel extends WrapperBlockStateModel {
    BushyLeafBlockStateModel(BlockStateModel wrapped) { super(wrapped); }

    @Override public void emitQuads(QuadEmitter output, BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
        wrapped.emitQuads(output, level, pos, state, random, cullTest);
        BgeLeafEligibility.binding(state).flatMap(binding -> binding.canonicalState(state)
                .flatMap(canonical -> CanonicalLeafModels.find(canonical).map(model -> new Source(binding, canonical, model))))
                .ifPresent(source -> emitExteriorFoliage(output, level, pos, state, random, source));
    }

    private static void emitExteriorFoliage(QuadEmitter output, BlockAndTintGetter level, BlockPos pos,
            BlockState physical, RandomSource random, Source source) {
        MutableMesh canonicalQuads = Renderer.get().mutableMesh();
        source.model.emitQuads(canonicalQuads.emitter(), level, pos, source.canonical, random, ignored -> false);
        CanonicalFoliageProjection.emit(canonicalQuads, output, source.binding, physical, level, pos);
    }

    @Override public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random) {
        return BgeLeafEligibility.binding(state).isPresent() ? null : wrapped.createGeometryKey(level, pos, state, random);
    }
    private record Source(dev.aero.cnmterraincompat.BgeMaterialBindings.Binding binding,
            BlockState canonical, BlockStateModel model) {}
}
