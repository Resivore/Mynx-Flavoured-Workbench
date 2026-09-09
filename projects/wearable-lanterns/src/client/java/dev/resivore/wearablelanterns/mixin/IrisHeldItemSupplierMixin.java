package dev.resivore.wearablelanterns.mixin;

import dev.resivore.wearablelanterns.WearableLanternsIrisBridge;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Iris 1.11.2 obtains heldItemId2, heldBlockLightValue2, and heldBlockLightColor2 from this one
 * supplier read. The redirect is intentionally confined to that shader-uniform evaluation; game
 * code still sees the real offhand.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.IdMapUniforms$HeldItemSupplier", remap = false)
public abstract class IrisHeldItemSupplierMixin {
    @Redirect(
            method = "update()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"),
            require = 1,
            remap = false)
    private ItemStack wearableLanterns$secondaryHandLightOnly(
            LocalPlayer player, InteractionHand hand) {
        ItemStack realOffhand = player.getItemInHand(hand);
        return hand == InteractionHand.OFF_HAND
                ? WearableLanternsIrisBridge.selectSecondaryHandLightStack(player, realOffhand)
                : realOffhand;
    }
}
