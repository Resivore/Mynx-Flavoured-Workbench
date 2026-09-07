package tempeststudios.quickstacknearby;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class QuickStackButtonSlotBridge {
    private QuickStackButtonSlotBridge() {
    }

    public static SlotPlacement reservePlayerInventorySlot(AbstractContainerScreen<?> screen, String ownerId, String slotId) {
        QuickStackScreenButtonSlots.SlotPlacement placement = QuickStackScreenButtonSlots.reservePlayerInventorySlot(
                screen,
                ownerId,
                slotId,
                QuickStackScreenButtonSlots.THIRD_PARTY_DEFAULT_PRIORITY,
                QuickStackScreenButtonSlots.DEFAULT_BUTTON_SIZE
        );
        return new SlotPlacement(placement.x(), placement.y());
    }

    public static void releaseOwner(AbstractContainerScreen<?> screen, String ownerId) {
        QuickStackScreenButtonSlots.releaseOwner(screen, ownerId);
    }

    public record SlotPlacement(int x, int y) {
    }
}
