package tempeststudios.quickstacknearby.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tempeststudios.quickstacknearby.QuickStackRulesButtonScreen;

@Mixin(AbstractContainerScreen.class)
public abstract class QuickStackInventoryMouseClickedMixin {
    @Inject(
            method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void quickStackNearby$onMouseClicked(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if ((Object) this instanceof QuickStackRulesButtonScreen screen
                && screen.quickstacknearby$openRulesFromButton(event.x(), event.y(), event.button())) {
            cir.setReturnValue(true);
        }
    }
}
