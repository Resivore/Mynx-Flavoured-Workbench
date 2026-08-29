package dev.resivore.slabdecorations.mixin;

import dev.resivore.slabdecorations.SubstrateBonemeal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {
    @Inject(method = "growCrop", at = @At("HEAD"), cancellable = true)
    private static void slabDecorations$delegateCanonicalSubstrate(
            ItemStack stack,
            Level level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        Optional<Boolean> result = SubstrateBonemeal.growCrop(stack, level, pos);
        result.ifPresent(cir::setReturnValue);
    }

    /** Makes level event 1505 use the canonical NEIGHBOR_SPREADER particle contract. */
    @Redirect(
            method = "addGrowthParticles",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"))
    private static Block slabDecorations$canonicalParticleOwner(BlockState state) {
        return SubstrateBonemeal.target(state)
                .map(target -> target.family().canonicalBlock())
                .orElseGet(state::getBlock);
    }
}
