package tempeststudios.quickstacknearby;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class QuickStackButtonSlotBridge {
    private static final ExternalInventoryPlusSlots EXTERNAL_SLOTS = ExternalInventoryPlusSlots.create();

    private QuickStackButtonSlotBridge() {
    }

    public static SlotPlacement reservePlayerInventorySlot(AbstractContainerScreen<?> screen, String ownerId, String slotId) {
        if (EXTERNAL_SLOTS != null) {
            SlotPlacement placement = EXTERNAL_SLOTS.reservePlayerInventorySlot(screen, ownerId, slotId);
            if (placement != null) {
                return placement;
            }
        }

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
        if (EXTERNAL_SLOTS != null) {
            EXTERNAL_SLOTS.releaseOwner(screen, ownerId);
        }
        QuickStackScreenButtonSlots.releaseOwner(screen, ownerId);
    }

    public record SlotPlacement(int x, int y) {
    }

    private static final class ExternalInventoryPlusSlots {
        private static final String API_CLASS_NAME = "tempeststudios.inventorysort.api.InventoryScreenButtonSlots";
        private static final String GROUP_CLASS_NAME =
                "tempeststudios.inventorysort.api.InventoryScreenButtonSlots$RightSlotGroup";

        private final Method reserveRightSlot;
        private final Method releaseOwner;
        private final Method x;
        private final Method y;
        private final Object playerInventoryGroup;
        private final int priority;

        private ExternalInventoryPlusSlots(
                Method reserveRightSlot,
                Method releaseOwner,
                Method x,
                Method y,
                Object playerInventoryGroup,
                int priority) {
            this.reserveRightSlot = reserveRightSlot;
            this.releaseOwner = releaseOwner;
            this.x = x;
            this.y = y;
            this.playerInventoryGroup = playerInventoryGroup;
            this.priority = priority;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        static ExternalInventoryPlusSlots create() {
            try {
                Class<?> apiClass = Class.forName(API_CLASS_NAME);
                Class<? extends Enum> groupClass = (Class<? extends Enum>) Class.forName(GROUP_CLASS_NAME).asSubclass(Enum.class);
                Object playerInventoryGroup = Enum.valueOf(groupClass, "PLAYER_INVENTORY");
                Method reserveRightSlot = apiClass.getMethod(
                        "reserveRightSlot",
                        AbstractContainerScreen.class,
                        groupClass,
                        String.class,
                        String.class,
                        int.class,
                        int.class
                );
                Method releaseOwner = apiClass.getMethod(
                        "releaseOwner",
                        AbstractContainerScreen.class,
                        String.class
                );
                Method x = reserveRightSlot.getReturnType().getMethod("x");
                Method y = reserveRightSlot.getReturnType().getMethod("y");
                Field priorityField = apiClass.getField("THIRD_PARTY_DEFAULT_PRIORITY");
                return new ExternalInventoryPlusSlots(
                        reserveRightSlot,
                        releaseOwner,
                        x,
                        y,
                        playerInventoryGroup,
                        priorityField.getInt(null)
                );
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                return null;
            }
        }

        SlotPlacement reservePlayerInventorySlot(AbstractContainerScreen<?> screen, String ownerId, String slotId) {
            try {
                Object placement = reserveRightSlot.invoke(
                        null,
                        screen,
                        playerInventoryGroup,
                        ownerId,
                        slotId,
                        priority,
                        QuickStackScreenButtonSlots.DEFAULT_BUTTON_SIZE
                );
                return new SlotPlacement(((Number) x.invoke(placement)).intValue(), ((Number) y.invoke(placement)).intValue());
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }

        boolean releaseOwner(AbstractContainerScreen<?> screen, String ownerId) {
            try {
                releaseOwner.invoke(null, screen, ownerId);
                return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
    }
}
