package dev.resivore.dragonbound.client;

import dev.resivore.dragonbound.block.DragonboundWaystoneBlockEntity;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** World renderer wrapper preserving every C12 quad and UV extent while swapping only sprites. */
final class MaterializedWaystoneBlockStateModel extends WrapperBlockStateModel {
    MaterializedWaystoneBlockStateModel(BlockStateModel wrapped) {
        super(wrapped);
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<Direction> cullTest) {
        Optional<MaterializedWaystoneModels.DirectionalMaterial> material = materialAt(level, pos);
        if (material.isEmpty()) {
            wrapped.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        List<BlockStateModelPart> parts = new ArrayList<>();
        wrapped.collectParts(random, parts);
        for (BlockStateModelPart part : parts) {
            emit(part.getQuads(null), emitter, material.get());
            for (Direction direction : Direction.values()) {
                if (cullTest.test(direction)) {
                    emit(part.getQuads(direction), emitter, material.get());
                }
            }
        }
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        return materialAt(level, pos)
                .<Object>map(material -> List.of(wrapped.createGeometryKey(level, pos, state, random), material.blockId()))
                .orElseGet(() -> wrapped.createGeometryKey(level, pos, state, random));
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return materialAt(level, pos)
                .map(material -> new Material.Baked(material.face(Direction.UP).sprite(), false))
                .orElseGet(() -> wrapped.particleMaterial(level, pos, state));
    }

    private static Optional<MaterializedWaystoneModels.DirectionalMaterial> materialAt(
            BlockAndTintGetter level,
            BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof DragonboundWaystoneBlockEntity waystone
                ? MaterializedWaystoneModels.resolve(waystone.copyPlacedStack())
                : Optional.empty();
    }

    private static void emit(
            List<BakedQuad> quads,
            QuadEmitter emitter,
            MaterializedWaystoneModels.DirectionalMaterial material) {
        for (BakedQuad quad : quads) {
            emitter.fromBakedQuad(MaterializedWaystoneModels.retarget(quad, material)).emit();
        }
    }
}
