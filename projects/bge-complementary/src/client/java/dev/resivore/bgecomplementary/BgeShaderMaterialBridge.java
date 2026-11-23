package dev.resivore.bgecomplementary;

import dev.aero.cnmterraincompat.BgeMaterialBindings;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Workbench-owned universal canonical-parent fallback over the maps Iris has already constructed
 * from the active shader pack. This class never parses BGE registry names or shader-pack files.
 */
public final class BgeShaderMaterialBridge {
    private BgeShaderMaterialBridge() {}

    public static void inheritMaterialIds(
            Object2IntMap<BlockState> materialIds) {
        for (Binding binding : BgeMaterialBindings.all()) {
            List<BlockState> physicalStates = binding.physicalBlock().getStateDefinition()
                    .getPossibleStates();
            ShaderMaterialInheritance.inheritMissing(
                    materialIds, physicalStates, binding::canonicalState);
        }
    }

    /**
     * Iris layer.* directives are block-wide. The same completed-map fallback keeps a pack's
     * explicit BGE block directive authoritative while every BGE-bound physical block follows
     * its authoritative canonical material's render-type override when one exists.
     */
    public static <T> void inheritLayerTypes(
            Map<Block, T> layerTypes) {
        for (Binding binding : BgeMaterialBindings.all()) {
            Block physical = binding.physicalBlock();
            ShaderMaterialInheritance.inheritMissing(
                    layerTypes, List.of(physical),
                    ignored -> java.util.Optional.of(binding.canonicalMaterial()));
        }
    }
}
