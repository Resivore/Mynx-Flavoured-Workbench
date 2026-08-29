package com.fizzware.dramaticdoors.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.fizzware.dramaticdoors.tags.DDItemTags;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(MultiPlayerGameMode.class)
public class CPIMMixin
{

	@Inject(method = "performUseItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At(value = "RETURN"), cancellable = true)
	private void injectUse(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> ci) {
		boolean blWithKey = player.getMainHandItem() != null && player.getMainHandItem().is(DDItemTags.KEY);
		if (blWithKey && ci.isCancellable()) {
			ci.setReturnValue(player.level().getBlockState(hitResult.getBlockPos()).useWithoutItem(player.level(), player, hitResult));
		}
	}

}
