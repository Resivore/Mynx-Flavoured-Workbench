package games.twinhead.moreslabsstairsandwalls.mixin;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.world.level.block.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneBlockMixin extends Block implements Fallable, SimpleWaterloggedBlock {

    public PointedDripstoneBlockMixin(Properties settings) {
        super(settings);
    }


    @Inject(method = "canGrow", at = @At("HEAD"), cancellable = true)
    private void moreSlabsStairsAndWalls$allowGeometrySupport(
            LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState supportState = level.getBlockState(pos.above());
        if (!isDripstoneWaterloggable(supportState)) {
            return;
        }

        FluidState sourceFluid = level.getFluidState(pos.above(2));
        cir.setReturnValue(isWaterlogged(supportState)
                || sourceFluid.is(FluidTags.WATER) && sourceFluid.isSource());
    }

    private static boolean isWaterlogged(BlockState blockState){
        for (ModBlocks.BlockType type: ModBlocks.BlockType.values()) {
            if(blockState.is(ModBlocks.DRIPSTONE_BLOCK.getBlock(type))) return blockState.getFluidState().is(FluidTags.WATER);
        }
        return false;
    }

    private static boolean isDripstoneWaterloggable(BlockState blockState){
        return blockState.is(ModBlocks.DRIPSTONE_BLOCK.getBlock(ModBlocks.BlockType.SLAB)) || blockState.is(ModBlocks.DRIPSTONE_BLOCK.getBlock(ModBlocks.BlockType.STAIRS)) || blockState.is(ModBlocks.DRIPSTONE_BLOCK.getBlock(ModBlocks.BlockType.WALL));
    }

}
