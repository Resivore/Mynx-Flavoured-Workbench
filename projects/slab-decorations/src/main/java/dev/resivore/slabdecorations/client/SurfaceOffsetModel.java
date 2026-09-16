package dev.resivore.slabdecorations.client;

import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import dev.resivore.slabdecorations.mixin.client.RenderSectionRegionAccessor;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/** FRAPI model wrapper so renderer implementations see the same derived Y offset as selection. */
public final class SurfaceOffsetModel implements BlockStateModel {
    private final BlockStateModel wrapped;

    public SurfaceOffsetModel(BlockStateModel wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void emitQuads(
            QuadEmitter output,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        double offset;
        if (level instanceof RenderSectionRegionAccessor snapshot) {
            offset = NibaruHorizontalSurface.visibleOffset(
                    state, level, snapshot.slabDecorations$getLevel(), pos);
        } else if (!(level instanceof LevelReader)) {
            offset = 0.0D;
        } else {
            offset = NibaruHorizontalSurface.visibleOffset(state, level, pos);
        }
        if (offset == 0.0D) {
            wrapped.emitQuads(output, level, pos, state, random, cullTest);
            return;
        }

        MutableMesh mesh = Renderer.get().mutableMesh();
        wrapped.emitQuads(mesh.emitter(), level, pos, state, random, ignored -> false);
        mesh.forEach(quad -> {
            output.copyFrom(quad);
            for (int vertex = 0; vertex < 4; vertex++) {
                output.pos(vertex, quad.x(vertex), quad.y(vertex) + (float) offset, quad.z(vertex));
            }
            output.cullFace(null);
            output.emit();
        });
    }

    @Override
    public @Nullable Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        // Geometry depends on the resolved support/anchor column, so do not share a
        // context-free cached key.
        return null;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        wrapped.collectParts(random, output);
    }

    @Override
    public Material.Baked particleMaterial() {
        return wrapped.particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state) {
        return wrapped.particleMaterial(level, pos, state);
    }

    @Override
    public int materialFlags() {
        return wrapped.materialFlags();
    }

    @Override
    public int materialFlags(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        return wrapped.materialFlags(level, pos, state, random);
    }
}
