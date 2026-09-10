package dev.resivore.quickstacknearbycompat.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.quickstacknearby.QuickStackIconButton;

/** Keeps QSN's upstream primary/right-click actions while giving its icon the shared utility size. */
@Mixin(value = QuickStackIconButton.class, remap = false)
public abstract class QuickStackIconButtonMixin {
    @Inject(
            method = "<init>(IILnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;Lnet/minecraft/client/gui/components/Button$OnPress;)V",
            at = @At("RETURN"),
            require = 1,
            remap = false
    )
    private void quickStackNearbyCompat$useStandardUtilityBounds(
            int x,
            int y,
            Component tooltip,
            Button.OnPress onPress,
            Button.OnPress secondaryOnPress,
            CallbackInfo callback
    ) {
        ((Button) (Object) this).setSize(18, 18);
    }
}
