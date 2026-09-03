package dev.resivore.slotreservations.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

/** Shared, presentation-only rendering for a reservation attached to one physical slot. */
public final class ReservationVisualRenderer {
    public static final float GHOST_ALPHA = 0.35F;
    public static final int GHOST_ALPHA_8 = 0x59;
    public static final int GHOST_ALPHA_ONLY_COLOR = 0x59FFFFFF;
    public static final int RESERVATION_MARKER = 0xFF24C7B8;

    private static final String EMPTY_COUNT = "0";

    private ReservationVisualRenderer() {
    }

    public static SlotVisualState state(ItemStack physical, Optional<ItemStack> reservation) {
        Objects.requireNonNull(physical, "physical");
        Objects.requireNonNull(reservation, "reservation");
        if (reservation.isEmpty()) {
            return SlotVisualState.UNRESERVED;
        }
        return physical.isEmpty()
                ? SlotVisualState.EMPTY_RESERVED
                : SlotVisualState.OCCUPIED_RESERVED;
    }

    public static void extract(
            GuiGraphicsExtractor graphics,
            Font font,
            ItemStack physical,
            Optional<ItemStack> reservation,
            int itemX,
            int itemY,
            int seed
    ) {
        switch (state(physical, reservation)) {
            case UNRESERVED -> {
            }
            case EMPTY_RESERVED -> {
                ItemStack ghost = reservation.orElseThrow().copyWithCount(1);
                GhostItemRenderScope.extract(graphics, ghost, itemX, itemY, seed);
                graphics.text(
                        font,
                        EMPTY_COUNT,
                        literalZeroX(itemX, font.width(EMPTY_COUNT)),
                        literalZeroY(itemY),
                        -1,
                        true
                );
            }
            case OCCUPIED_RESERVED -> graphics.fill(
                    itemX + 13,
                    itemY,
                    itemX + 16,
                    itemY + 3,
                    RESERVATION_MARKER
            );
        }
    }

    /** Matches vanilla's item-count x calculation: x + 19 - 2 - glyph width. */
    public static int literalZeroX(int itemX, int glyphWidth) {
        return itemX + 17 - glyphWidth;
    }

    /** Matches vanilla's item-count baseline: y + 6 + 3. */
    public static int literalZeroY(int itemY) {
        return itemY + 9;
    }

    public enum SlotVisualState {
        UNRESERVED,
        EMPTY_RESERVED,
        OCCUPIED_RESERVED
    }
}
