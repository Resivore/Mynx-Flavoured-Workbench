package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.GrassFamilyBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpreadingSnowyBlock.class)
abstract class SpreadingSnowyBlockMixin {
    @Inject(method = "randomTick", at = @At("TAIL"))
    private void cnmTerrainCompat$spreadToCustomDirt(BlockState state, ServerLevel level,
                                                     BlockPos pos, RandomSource random,
                                                     CallbackInfo ci) {
        if (state.is(Blocks.GRASS_BLOCK) && level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
            GrassFamilyBehavior.spreadFrom(state, level, pos, random, false);
        }
    }
}
