package dev.resivore.bgeglassculling.client;

import dev.resivore.bgeglassculling.SurfaceOverlapResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/** Last-phase wrapper that clips the complete upstream model's final emitted quads. */
final class GlassCullingBlockStateModel extends WrapperBlockStateModel {
    GlassCullingBlockStateModel(BlockStateModel wrapped) {
        super(wrapped);
    }

    @Override
    public void emitQuads(QuadEmitter output, BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        QuadEmitter upstream = Renderer.get().quadEmitter(quad ->
                GlassQuadClipper.emit(quad, output, level, pos, state));
        Predicate<Direction> coherentCullTest = coherentCullTest(cullTest, level, pos, state);
        ((FabricBlockStateModel) wrapped).emitQuads(upstream, level, pos, state,
                random, coherentCullTest);
    }

    /**
     * Extends ordinary directional culling only when the renderer supplied a real neighbor
     * direction. A null direction has no adjacent boundary, so its upstream meaning is preserved
     * exactly without reading the level or evaluating BGE geometry.
     */
    static Predicate<Direction> coherentCullTest(Predicate<Direction> cullTest,
            BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return direction -> cullDecision(cullTest, direction, evaluatedDirection ->
                SurfaceOverlapResolver.canEvaluateBoundary(state,
                        level.getBlockState(pos.relative(evaluatedDirection)),
                        evaluatedDirection));
    }

    /**
     * Reserves compatible, BGE-evaluable directional faces for exact final-quad clipping. The
     * renderer's predicate can only discard a whole face, so it must not run for a boundary whose
     * overlap is owned by {@link GlassQuadClipper}. A null direction has no boundary and retains
     * the upstream predicate's exact meaning.
     */
    static boolean cullDecision(Predicate<Direction> cullTest, Direction direction,
            Predicate<Direction> canEvaluateBoundary) {
        if (direction == null) return cullTest.test(null);
        if (canEvaluateBoundary.test(direction)) return false;
        return cullTest.test(direction);
    }
}
