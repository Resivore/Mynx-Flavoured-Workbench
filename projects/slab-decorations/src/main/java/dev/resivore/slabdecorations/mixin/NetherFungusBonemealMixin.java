package dev.resivore.slabdecorations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.resivore.slabdecorations.StructureGrowthTransaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves vanilla huge-fungus selection while transactionally projecting its nylium parent. */
@Mixin(NetherFungusBlock.class)
public abstract class NetherFungusBonemealMixin {
    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"), cancellable = true)
    private void slabDecorations$allowCanonicalNyliumTarget(
            net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state,
            CallbackInfoReturnable<Boolean> cir) {
        if (dev.resivore.slabdecorations.CanonicalSurvivalProjection.evaluate(state, level, pos)
                .orElse(false)) cir.setReturnValue(true);
    }

    @WrapMethod(method = "performBonemeal")
    private void slabDecorations$growAgainstCanonicalParent(
            ServerLevel level, RandomSource random, BlockPos pos, BlockState state,
            Operation<Void> original) {
        StructureGrowthTransaction.run(level, pos, state, () -> {
            original.call(level, random, pos, state);
            return !level.getBlockState(pos).is(state.getBlock());
        });
    }
}
