package tempeststudios.quickstacknearby.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import tempeststudios.quickstacknearby.QuickStackInventoryControls;
import tempeststudios.quickstacknearby.QuickStackRulesButtonScreen;

@Mixin(AbstractContainerScreen.class)
public abstract class QuickStackInventoryScreenMixin implements QuickStackRulesButtonScreen {

    @Override
    public boolean quickstacknearby$openRulesFromButton(double mouseX, double mouseY, int button) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        return QuickStackInventoryControls.openRulesFromButton(screen, mouseX, mouseY, button);
    }
}
