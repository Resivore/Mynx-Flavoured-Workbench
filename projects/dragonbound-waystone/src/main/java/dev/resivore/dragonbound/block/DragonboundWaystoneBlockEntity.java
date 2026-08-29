package dev.resivore.dragonbound.block;

import dev.resivore.dragonbound.DragonboundContent;
import dev.resivore.dragonbound.anchor.DragonboundAnchors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Retains the exact placed item stack so a survival relocation preserves components.
 * Destination authority remains exclusively in {@code DragonboundAnchorData}.
 */
public final class DragonboundWaystoneBlockEntity extends BlockEntity {
    private static final String PLACED_STACK_KEY = "placed_stack";

    private ItemStack placedStack = ItemStack.EMPTY;

    public DragonboundWaystoneBlockEntity(BlockPos pos, BlockState state) {
        super(DragonboundContent.WAYSTONE_BLOCK_ENTITY, pos, state);
    }

    public void setPlacedStack(ItemStack stack) {
        placedStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    public ItemStack copyPlacedStack() {
        return placedStack.isEmpty() ? ItemStack.EMPTY : placedStack.copyWithCount(1);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            DragonboundAnchors.clearIfMatching(serverLevel, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        placedStack = input.read(PLACED_STACK_KEY, ItemStack.CODEC)
                .filter(stack -> !stack.isEmpty())
                .map(stack -> stack.copyWithCount(1))
                .orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!placedStack.isEmpty()) {
            output.store(PLACED_STACK_KEY, ItemStack.CODEC, placedStack.copyWithCount(1));
        }
    }
}
