package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Material orientation inherited from a canonical vanilla-style pillar parent. */
public final class MaterialAxisState {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    private MaterialAxisState() {}

    /** The canonical parent's state definition is authoritative, not its texture classification. */
    public static boolean applies(NibaruMaterialProfile profile) {
        return profile.canonicalParent().defaultBlockState().hasProperty(AXIS);
    }

    /** Matches vanilla pillar placement while retaining the material axis when two partial pieces combine. */
    public static Direction.Axis placementAxis(BlockState existing, Block block, Direction clickedFace) {
        return existing.is(block) && existing.hasProperty(AXIS)
                ? existing.getValue(AXIS)
                : clickedFace.getAxis();
    }

}
