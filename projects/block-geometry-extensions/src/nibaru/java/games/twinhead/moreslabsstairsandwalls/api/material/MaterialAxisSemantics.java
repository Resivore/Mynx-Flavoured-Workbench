package games.twinhead.moreslabsstairsandwalls.api.material;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Canonical-parent authority for independent material orientation on native Nibaru geometry. */
public final class MaterialAxisSemantics {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    private MaterialAxisSemantics() {}

    /** Applicability comes only from the canonical parent's actual vanilla state definition. */
    public static boolean applies(ModBlocks family) {
        return applies(family.parentBlock);
    }

    public static boolean applies(NibaruMaterialProfile profile) {
        return applies(profile.canonicalParent());
    }

    public static boolean applies(Block canonicalParent) {
        return canonicalParent.defaultBlockState().hasProperty(AXIS);
    }

    /** Matches pillar placement while retaining the existing material axis during slab merging. */
    public static Direction.Axis placementAxis(BlockState existing, Block block, Direction clickedFace) {
        return existing.is(block) && existing.hasProperty(AXIS)
                ? existing.getValue(AXIS)
                : clickedFace.getAxis();
    }
}
