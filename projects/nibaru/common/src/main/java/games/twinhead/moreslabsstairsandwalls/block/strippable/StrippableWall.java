package games.twinhead.moreslabsstairsandwalls.block.strippable;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseWall;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
@SuppressWarnings("deprecation")
public class StrippableWall extends BaseWall {

    private final ModBlocks strippedBlock;

    public StrippableWall(ModBlocks block,ModBlocks strippedBlock, Properties settings) {
        super(block,settings);
        this.strippedBlock = strippedBlock;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return StrippingSemantics.strip(stack, state,
                strippedBlock.getBlock(ModBlocks.BlockType.WALL).defaultBlockState(), world, pos, player, hand);
    }
}
