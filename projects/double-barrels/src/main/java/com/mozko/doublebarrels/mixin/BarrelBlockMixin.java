package com.mozko.doublebarrels.mixin;

import com.mozko.doublebarrels.DoubleBarrelProperties;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BarrelBlock.class)
public abstract class BarrelBlockMixin {
    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void doublebarrels$appendProperties(
            StateDefinition.Builder<Block, BlockState> builder,
            CallbackInfo ci) {
        builder.add(DoubleBarrelProperties.DOUBLE);
    }
}
