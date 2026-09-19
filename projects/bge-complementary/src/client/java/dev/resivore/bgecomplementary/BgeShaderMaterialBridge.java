package dev.resivore.bgecomplementary;

import dev.aero.cnmterraincompat.BgeMaterialBindings;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Workbench-owned fallback over the maps Iris has already constructed from the active shader pack.
 * This class never parses BGE registry names or shader-pack files.
 */
public final class BgeShaderMaterialBridge {
    private static final Set<Block> CANARY_ONE_PARENTS = Set.of(
            Blocks.GLASS,
            Blocks.STAINED_GLASS.pick(DyeColor.WHITE), Blocks.STAINED_GLASS.pick(DyeColor.ORANGE),
            Blocks.STAINED_GLASS.pick(DyeColor.MAGENTA), Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_BLUE),
            Blocks.STAINED_GLASS.pick(DyeColor.YELLOW), Blocks.STAINED_GLASS.pick(DyeColor.LIME),
            Blocks.STAINED_GLASS.pick(DyeColor.PINK), Blocks.STAINED_GLASS.pick(DyeColor.GRAY),
            Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_GRAY), Blocks.STAINED_GLASS.pick(DyeColor.CYAN),
            Blocks.STAINED_GLASS.pick(DyeColor.PURPLE), Blocks.STAINED_GLASS.pick(DyeColor.BLUE),
            Blocks.STAINED_GLASS.pick(DyeColor.BROWN), Blocks.STAINED_GLASS.pick(DyeColor.GREEN),
            Blocks.STAINED_GLASS.pick(DyeColor.RED), Blocks.STAINED_GLASS.pick(DyeColor.BLACK),
            Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK,
            Blocks.GLOWSTONE, Blocks.SEA_LANTERN);

    private BgeShaderMaterialBridge() {}

    public static ShaderMaterialInheritance.Result inheritMaterialIds(
            Object2IntMap<BlockState> materialIds) {
        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.Result.empty();
        for (Binding binding : BgeMaterialBindings.all()) {
            List<BlockState> physicalStates = binding.physicalBlock().getStateDefinition()
                    .getPossibleStates();
            result = result.plus(ShaderMaterialInheritance.inheritMissing(
                    materialIds, physicalStates, binding::canonicalState,
                    state -> CANARY_ONE_PARENTS.contains(state.getBlock())));
        }
        return result;
    }

    /**
     * Iris layer.* directives are block-wide. The same completed-map fallback keeps a pack's
     * explicit BGE block directive authoritative, while allowing an eligible BGE block to follow
     * its canonical parent's render-type override when one exists.
     */
    public static <T> ShaderMaterialInheritance.Result inheritLayerTypes(
            Map<Block, T> layerTypes) {
        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.Result.empty();
        for (Binding binding : BgeMaterialBindings.all()) {
            Block physical = binding.physicalBlock();
            result = result.plus(ShaderMaterialInheritance.inheritMissing(
                    layerTypes, List.of(physical),
                    ignored -> java.util.Optional.of(binding.canonicalMaterial()),
                    CANARY_ONE_PARENTS::contains));
        }
        return result;
    }
}
