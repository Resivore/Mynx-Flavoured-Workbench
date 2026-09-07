package tempeststudios.quickstacknearby.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.quickstacknearby.RecipeBookAwareButtonScreen;

@Pseudo
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen")
public abstract class AbstractRecipeBookScreenMixin {
    @Inject(method = {"render", "extractRenderState"}, at = @At("HEAD"), require = 0)
    private void quickStackNearby$updateButtonsOnRecipeBookRender(CallbackInfo ci) {
        if (this instanceof RecipeBookAwareButtonScreen buttonScreen) {
            buttonScreen.quickstacknearby$updateButtonPositionsFromRecipeBookRender();
        }
    }
}
