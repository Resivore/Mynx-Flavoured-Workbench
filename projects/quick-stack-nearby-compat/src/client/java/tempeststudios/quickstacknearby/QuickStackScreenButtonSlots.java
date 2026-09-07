package tempeststudios.quickstacknearby;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import tempeststudios.quickstacknearby.mixin.AbstractContainerScreenAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

final class QuickStackScreenButtonSlots {
    static final int DEFAULT_BUTTON_SIZE = 12;
    static final int DEFAULT_BUTTON_GAP = 1;
    static final int THIRD_PARTY_DEFAULT_PRIORITY = 1000;

    private static final int EDGE_OVERLAP = 3;
    private static final Map<AbstractContainerScreen<?>, ScreenReservations> SCREENS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private QuickStackScreenButtonSlots() {
    }

    static SlotPlacement reservePlayerInventorySlot(
            AbstractContainerScreen<?> screen,
            String ownerId,
            String slotId,
            int priority,
            int buttonSize) {
        Objects.requireNonNull(screen, "screen");
        validateButtonSize(buttonSize);

        ReservationKey key = key(ownerId, slotId);
        synchronized (SCREENS) {
            ScreenReservations reservations = SCREENS.computeIfAbsent(screen, ignored -> new ScreenReservations());
            reservations.reserve(key, priority);
            int slotIndex = reservations.slotIndex(key);
            Coordinates coordinates = coordinatesFor(screen, slotIndex, buttonSize);
            return new SlotPlacement(coordinates.x(), coordinates.y());
        }
    }

    static void releaseOwner(AbstractContainerScreen<?> screen, String ownerId) {
        if (screen == null || ownerId == null || ownerId.isBlank()) {
            return;
        }

        synchronized (SCREENS) {
            ScreenReservations reservations = SCREENS.get(screen);
            if (reservations == null) {
                return;
            }
            reservations.releaseOwner(ownerId);
            if (reservations.isEmpty()) {
                SCREENS.remove(screen);
            }
        }
    }

    private static Coordinates coordinatesFor(AbstractContainerScreen<?> screen, int slotIndex, int buttonSize) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int x = rightColumnX(accessor.getLeftPos(), accessor.getImageWidth(), screen.width, buttonSize);
        int y = accessor.getTopPos()
                + accessor.getImageHeight()
                - 83
                + slotIndex * (buttonSize + DEFAULT_BUTTON_GAP);
        return new Coordinates(x, y);
    }

    private static int rightColumnX(int leftPos, int imageWidth, int screenWidth, int buttonSize) {
        int preferredRightX = leftPos + imageWidth - EDGE_OVERLAP;
        if (preferredRightX + buttonSize <= screenWidth) {
            return Math.max(0, preferredRightX);
        }

        int leftFallbackX = leftPos - buttonSize + EDGE_OVERLAP;
        if (leftFallbackX >= 0) {
            return leftFallbackX;
        }

        return Math.max(0, Math.min(preferredRightX, Math.max(0, screenWidth - buttonSize)));
    }

    private static ReservationKey key(String ownerId, String slotId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        if (slotId == null || slotId.isBlank()) {
            throw new IllegalArgumentException("slotId must not be blank");
        }
        return new ReservationKey(ownerId, slotId);
    }

    private static void validateButtonSize(int buttonSize) {
        if (buttonSize < 1) {
            throw new IllegalArgumentException("buttonSize must be positive");
        }
    }

    record SlotPlacement(int x, int y) {
    }

    private record ReservationKey(String ownerId, String slotId) {
    }

    private record Reservation(ReservationKey key, int priority, long sequence) {
    }

    private record Coordinates(int x, int y) {
    }

    private static final class ScreenReservations {
        private final LinkedHashMap<ReservationKey, Reservation> reservations = new LinkedHashMap<>();
        private long nextSequence;

        private void reserve(ReservationKey key, int priority) {
            Reservation previous = reservations.get(key);
            long sequence = previous == null ? nextSequence++ : previous.sequence();
            reservations.put(key, new Reservation(key, priority, sequence));
        }

        private void releaseOwner(String ownerId) {
            reservations.entrySet().removeIf(candidate -> ownerId.equals(candidate.getKey().ownerId()));
        }

        private int slotIndex(ReservationKey key) {
            List<Reservation> sorted = sortedReservations();
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).key().equals(key)) {
                    return i;
                }
            }
            return sorted.size();
        }

        private boolean isEmpty() {
            return reservations.isEmpty();
        }

        private List<Reservation> sortedReservations() {
            if (reservations.isEmpty()) {
                return List.of();
            }
            List<Reservation> sorted = new ArrayList<>(reservations.values());
            sorted.sort((left, right) -> {
                int priority = Integer.compare(left.priority(), right.priority());
                if (priority != 0) {
                    return priority;
                }
                return Long.compare(left.sequence(), right.sequence());
            });
            return sorted;
        }
    }
}
