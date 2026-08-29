package dev.aero.shulkertrowel.mixin.client;

import dev.aero.shulkertrowel.client.TrowelShapeSwitcherOverlay;
import dev.tazer.clutternomore.client.ShapeSwitcherOverlay;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ShapeSwitcherOverlay.class, remap = false)
abstract class ShapeSwitcherOverlayRenderMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/tazer/clutternomore/common/shape_map/ShapeMap;transferStack(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;",
                    remap = false
            ),
            remap = false
    )
    private ItemStack shulkerTrowel$renderModeIcon(ItemStack held, int geometryId) {
        if ((Object) this instanceof TrowelShapeSwitcherOverlay overlay) {
            return overlay.iconStack(geometryId);
        }
        return ShapeMap.transferStack(held, geometryId);
    }
}
