package dev.aero.cnmterraincompat;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Block item implementing funded partial growth and normal-debit canonical completion. */
public final class BgeBlockItem extends BlockItem {
    public BgeBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockState existing = level.getBlockState(context.getClickedPos());
        BlockState expansion = null;

        if (existing.is(getBlock()) && getBlock() instanceof BlockspaceFundedGeometry compound) {
            expansion = compound.expandedState(existing, context);
        }
        boolean canonicalCompletion = expansion != null
                && FullOccupancyNormalizer.normalize(expansion).getBlock() != expansion.getBlock();

        ItemStack stack = context.getItemInHand();
        int before = stack.getCount();
        InteractionResult result = super.place(context);

        if (expansion != null && !canonicalCompletion
                && result.consumesAction() && stack.getCount() < before) {
            stack.grow(before - stack.getCount());
        }
        return result;
    }
}
