package com.yungnickyoung.minecraft.ribbits.mixin.mixins.chute;

import com.yungnickyoung.minecraft.ribbits.chute.ChuteEquipment;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rejects right-click and equip-on-target chest-glider swaps before either stack is split. */
@Mixin(Equippable.class)
public abstract class EquippableChuteMixin {
    @Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private void ribbits$rejectChestGliderSwap(
            ItemStack incoming,
            Player player,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (ChuteEquipment.isVanillaChestGlider(incoming) && ChuteEquipment.hasActiveChute(player)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "equipOnTarget", at = @At("HEAD"), cancellable = true)
    private void ribbits$rejectChestGliderTargetEquip(
            Player actor,
            LivingEntity target,
            ItemStack incoming,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (target instanceof Player player
                && ChuteEquipment.isVanillaChestGlider(incoming)
                && ChuteEquipment.hasActiveChute(player)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
