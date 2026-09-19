package dev.resivore.bgebushyleaves.client;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/** Models are captured per reload, after the active resource-pack stack has baked them. */
final class CanonicalLeafModels {
    private static final Map<BlockState, BlockStateModel> MODELS = new IdentityHashMap<>();
    private CanonicalLeafModels() {}
    static void clearForReload() { MODELS.clear(); }
    static void capture(BlockState state, BlockStateModel model) { MODELS.put(state, model); }
    static Optional<BlockStateModel> find(BlockState state) { return Optional.ofNullable(MODELS.get(state)); }
}
