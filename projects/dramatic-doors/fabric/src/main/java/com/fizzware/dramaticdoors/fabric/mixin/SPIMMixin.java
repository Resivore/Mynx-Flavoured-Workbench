package com.fizzware.dramaticdoors.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.fizzware.dramaticdoors.tags.DDItemTags;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(ServerPlayerGameMode.class)
public class SPIMMixin
{

	@Inject(method = "useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At(value = "RETURN"), cancellable = true)
	private void injectUse(ServerPlayer player, Level world, ItemStack stack, InteractionHand interactionHand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> ci) {
		boolean blWithKey = player.getMainHandItem() != null && player.getMainHandItem().is(DDItemTags.KEY);
		if (blWithKey) {
			ci.setReturnValue(world.getBlockState(hitResult.getBlockPos()).useWithoutItem(world, player, hitResult));
		}
	}
}
