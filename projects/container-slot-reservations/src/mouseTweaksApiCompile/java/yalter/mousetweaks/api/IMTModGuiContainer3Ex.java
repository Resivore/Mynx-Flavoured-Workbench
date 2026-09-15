package yalter.mousetweaks.api;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Compile-only ABI declaration for Mouse Tweaks 2.31's public custom-container
 * API. The class is intentionally excluded from the CSR release artifact.
 */
public interface IMTModGuiContainer3Ex {
    boolean MT_isMouseTweaksDisabled();
    boolean MT_isWheelTweakDisabled();
    List<Slot> MT_getSlots();
    Slot MT_getSlotUnderMouse(double mouseX, double mouseY);
    boolean MT_isCraftingOutput(Slot slot);
    boolean MT_isIgnored(Slot slot);
    boolean MT_disableRMBDraggingFunctionality();
    void MT_clickSlot(Slot slot, int button, ContainerInput input);
}
