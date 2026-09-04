package com.yungnickyoung.minecraft.ribbits.mixin.mixins.chute;

import com.yungnickyoung.minecraft.ribbits.chute.ChuteEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rejects inventory-click and quick-move Elytra insertion without consuming either item. */
@Mixin(ArmorSlot.class)
public abstract class ArmorSlotChuteMixin {
    @Shadow
    @Final
    private LivingEntity owner;

    @Shadow
    @Final
    private EquipmentSlot slot;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void ribbits$rejectChestGliderBesideChute(ItemStack incoming, CallbackInfoReturnable<Boolean> cir) {
        if (this.slot == EquipmentSlot.CHEST
                && this.owner instanceof Player player
                && ChuteEquipment.hasActiveChute(player)
                && ChuteEquipment.isVanillaChestGlider(incoming)) {
            cir.setReturnValue(false);
        }
    }
}
