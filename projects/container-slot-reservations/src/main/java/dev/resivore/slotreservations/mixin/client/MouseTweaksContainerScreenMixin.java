package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.MouseTweaksTrace;
import dev.resivore.slotreservations.client.ShulkerPanel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import yalter.mousetweaks.api.IMTModGuiContainer3Ex;

import java.util.List;

/**
 * Mouse Tweaks 2.31's supported custom-container API wired to CSR's transient
 * client-side panel cells. The mixin plugin applies it only with Mouse Tweaks;
 * the ABI declaration used for compilation is never packaged by CSR.
 */
@Pseudo
@Mixin(AbstractContainerScreen.class)
abstract class MouseTweaksContainerScreenMixin implements IMTModGuiContainer3Ex {
    @Shadow private boolean isQuickCrafting;
    @Shadow private int quickCraftingButton;
    @Shadow private boolean skipNextRelease;

    @Override
    public boolean MT_isMouseTweaksDisabled() {
        MouseTweaksTrace.event(2, "Mouse Tweaks extended provider selected",
                "IMTModGuiContainer3Ex is active for this screen");
        return false;
    }

    @Override
    public boolean MT_isWheelTweakDisabled() {
        return false;
    }

    @Override
    public List<Slot> MT_getSlots() {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        return ShulkerPanel.mouseTweaksSlots(screen.getMenu().slots);
    }

    @Override
    public Slot MT_getSlotUnderMouse(double mouseX, double mouseY) {
        MouseTweaksTrace.event(6, "Mouse Tweaks requested slot under pointer",
                "x=" + mouseX + ", y=" + mouseY);
        Slot panelSlot = ShulkerPanel.mouseTweaksSlotAt(mouseX, mouseY);
        Slot resolved = panelSlot != null ? panelSlot
                : ((ContainerScreenMouseAccess) this).containerSlotReservations$slotAt(mouseX, mouseY);
        MouseTweaksTrace.event(7, "Provider slot resolved",
                "kind=" + (panelSlot != null ? "csr-virtual" : resolved == null ? "none" : "native")
                        + ", cell=" + ShulkerPanel.mouseTweaksPanelCell(resolved));
        return resolved;
    }

    @Override
    public boolean MT_isCraftingOutput(Slot slot) {
        return slot instanceof ResultSlot || slot instanceof FurnaceResultSlot || slot instanceof MerchantResultSlot;
    }

    @Override
    public boolean MT_isIgnored(Slot slot) {
        return false;
    }

    @Override
    public boolean MT_disableRMBDraggingFunctionality() {
        skipNextRelease = true;
        if (isQuickCrafting && quickCraftingButton == 1) {
            isQuickCrafting = false;
            return true;
        }
        return false;
    }

    @Override
    public void MT_clickSlot(Slot slot, int button, ContainerInput input) {
        MouseTweaksTrace.event(9, "Mouse Tweaks invoked slot action",
                "kind=" + (ShulkerPanel.mouseTweaksPanelCell(slot) >= 0 ? "csr-virtual" : "native")
                        + ", cell=" + ShulkerPanel.mouseTweaksPanelCell(slot)
                        + ", button=" + button + ", input=" + input);
        if (ShulkerPanel.mouseTweaksClick(slot, button, input)) return;
        if (input == ContainerInput.QUICK_MOVE && ShulkerPanel.mouseTweaksQuickMoveFromMenuSlot(slot)) return;
        ShulkerPanel.NativeClickPlan plan = ShulkerPanel.beforeMouseTweaksNativeClick(slot, button, input);
        if (!plan.invoke()) return;
        boolean completed = false;
        try {
            ((ContainerScreenMouseAccess) this).containerSlotReservations$clickSlot(
                    slot, slot.index, button, input);
            completed = true;
        } finally {
            ShulkerPanel.afterMouseTweaksNativeClick(plan, completed);
        }
    }
}
