package games.twinhead.moreslabsstairsandwalls.block.axis;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.strippable.StrippingSemantics;
import games.twinhead.moreslabsstairsandwalls.block.strippable.StrippableGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Axis-aware native slab retaining Nibaru's exact stripping transition. */
@SuppressWarnings("deprecation")
public final class AxisStrippableSlab extends AxisSlab implements StrippableGeometry {
    private final ModBlocks strippedBlock;

    public AxisStrippableSlab(ModBlocks block, ModBlocks strippedBlock, Properties settings) {
        super(block, settings);
        this.strippedBlock = strippedBlock;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return StrippingSemantics.strip(stack, state,
                strippedBlock.getBlock(ModBlocks.BlockType.SLAB).defaultBlockState(), world, pos, player, hand);
    }
}
