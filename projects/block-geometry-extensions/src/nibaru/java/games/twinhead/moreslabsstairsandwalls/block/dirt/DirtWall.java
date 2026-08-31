package games.twinhead.moreslabsstairsandwalls.block.dirt;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseWall;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
@SuppressWarnings("deprecation")
public class DirtWall extends BaseWall {

    public DirtWall(ModBlocks modBlocks, Properties settings) {
        super(modBlocks, settings);
    }


    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return PathSemantics.flatten(stack, state,
                ModBlocks.DIRT_PATH.getBlock(ModBlocks.BlockType.WALL).defaultBlockState(), world, pos, player, hand);
    }
}
