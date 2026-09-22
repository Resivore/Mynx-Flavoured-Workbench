package dev.resivore.enderscapeintegration.client;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Enderscape-specific compatibility decision made after Iris has resolved the active shader
 * pack's state map. The map's own vanilla oak-leaf entry, not a Complementary material number,
 * is the foliage reference.
 */
public final class VeiledLeavesShaderMaterialBridge {
    static final Identifier VEILED_LEAVES_ID = Identifier.fromNamespaceAndPath("enderscape", "veiled_leaves");

    private VeiledLeavesShaderMaterialBridge() {
    }

    public static VeiledLeavesShaderMaterialFallback.Result inheritMaterialIds(
            Object2IntMap<BlockState> materialIds) {
        return BuiltInRegistries.BLOCK.get(VEILED_LEAVES_ID)
                .map(veiledLeaves -> VeiledLeavesShaderMaterialFallback.inheritMissing(
                        materialIds,
                        veiledLeaves.value().getStateDefinition().getPossibleStates(),
                        Blocks.OAK_LEAVES.defaultBlockState()))
                .orElseGet(() -> new VeiledLeavesShaderMaterialFallback.Result(0, 0, 0));
    }
}
