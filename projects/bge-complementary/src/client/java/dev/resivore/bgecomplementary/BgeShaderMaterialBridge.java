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

    public static ShaderMaterialInheritance.Result inheritMaterialIds(
            Object2IntMap<BlockState> materialIds) {
        SpruceIrisMaterialTrace.MaterialSnapshot spruceBefore =
                SpruceIrisMaterialTrace.beforeMaterialInheritance(materialIds);
        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.Result.empty();
        for (Binding binding : BgeMaterialBindings.all()) {
            List<BlockState> physicalStates = binding.physicalBlock().getStateDefinition()
                    .getPossibleStates();
            result = result.plus(ShaderMaterialInheritance.inheritMissing(
                    materialIds, physicalStates, binding::canonicalState));
        }
        SpruceIrisMaterialTrace.afterMaterialInheritance(materialIds, spruceBefore);
        return result;
    }

    /**
     * Iris layer.* directives are block-wide. The same completed-map fallback keeps a pack's
     * explicit BGE block directive authoritative while every BGE-bound physical block follows
     * its authoritative canonical material's render-type override when one exists.
     */
    public static <T> ShaderMaterialInheritance.Result inheritLayerTypes(
            Map<Block, T> layerTypes) {
        SpruceIrisMaterialTrace.LayerSnapshot spruceBefore =
                SpruceIrisMaterialTrace.beforeLayerInheritance(layerTypes);
        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.Result.empty();
        for (Binding binding : BgeMaterialBindings.all()) {
            Block physical = binding.physicalBlock();
            result = result.plus(ShaderMaterialInheritance.inheritMissing(
                    layerTypes, List.of(physical),
                    ignored -> java.util.Optional.of(binding.canonicalMaterial())));
        }
        SpruceIrisMaterialTrace.afterLayerInheritance(layerTypes, spruceBefore);
        return result;
    }
}
