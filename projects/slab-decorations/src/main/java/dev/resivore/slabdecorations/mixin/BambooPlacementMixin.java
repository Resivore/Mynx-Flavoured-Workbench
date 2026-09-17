package dev.resivore.slabdecorations.mixin;

import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets bamboo's initial sapling use the same projected substrate test as its grown column. */
@Mixin(BambooStalkBlock.class)
public abstract class BambooPlacementMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void slabDecorations$useCanonicalParentForInitialSapling(
            BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null) return;

        BlockPos pos = context.getClickedPos();
        if (!context.getLevel().getFluidState(pos).isEmpty()) return;

        BlockState sapling = Blocks.BAMBOO_SAPLING.defaultBlockState();
        if (NibaruHorizontalSurface.supporting(sapling, context.getLevel(), pos).isPresent()) {
            cir.setReturnValue(sapling);
        }
    }
}
