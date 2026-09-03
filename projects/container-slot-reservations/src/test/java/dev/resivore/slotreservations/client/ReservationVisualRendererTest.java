package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ReservationData;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReservationVisualRendererTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.POISONOUS_POTATO);
        bindTestComponents(Items.STONE);
        bindTestComponents(Blocks.SHULKER_BOX.asItem());
    }

    @Test
    void emptyReservationSelectsGhostPlusLiteralZeroAtActualThirtyFivePercentAlpha() {
        ItemStack template = identity(Items.POISONOUS_POTATO, 37, "green_curry", "Green Curry");

        assertEquals(
                ReservationVisualRenderer.SlotVisualState.EMPTY_RESERVED,
                ReservationVisualRenderer.state(ItemStack.EMPTY, Optional.of(template))
        );
        assertEquals(0x59, ReservationVisualRenderer.GHOST_ALPHA_8);
        assertEquals(0x59FFFFFF, ReservationVisualRenderer.GHOST_ALPHA_ONLY_COLOR);
        assertEquals(
                ReservationVisualRenderer.GHOST_ALPHA_ONLY_COLOR,
                GhostItemRenderScope.alphaOnlyWhite(ReservationVisualRenderer.GHOST_ALPHA_8)
        );
        assertEquals(-1, GhostItemRenderScope.alphaOnlyWhite(0xFF));
        assertEquals(
                ReservationVisualRenderer.GHOST_ALPHA,
                ReservationVisualRenderer.GHOST_ALPHA_8 / 255.0F,
                0.002F
        );
        assertEquals(21, ReservationVisualRenderer.literalZeroX(10, 6));
        assertEquals(19, ReservationVisualRenderer.literalZeroY(10));
    }

    @Test
    void occupiedReservedAndUnreservedSlotsRemainDistinct() {
        ItemStack physical = new ItemStack(Items.STONE, 17);
        ItemStack reservation = new ItemStack(Items.STONE);

        assertEquals(
                ReservationVisualRenderer.SlotVisualState.OCCUPIED_RESERVED,
                ReservationVisualRenderer.state(physical, Optional.of(reservation))
        );
        assertEquals(
                ReservationVisualRenderer.SlotVisualState.UNRESERVED,
                ReservationVisualRenderer.state(physical, Optional.empty())
        );
        assertEquals(
                ReservationVisualRenderer.SlotVisualState.UNRESERVED,
                ReservationVisualRenderer.state(ItemStack.EMPTY, Optional.empty())
        );
    }

    @Test
    void tooltipPlansEveryPhysicalIndexZeroThroughTwentySixWithoutMutatingInputs() {
        ReservationData reservations = ReservationData.EMPTY;
        NonNullList<ItemStack> physical = NonNullList.withSize(27, ItemStack.EMPTY);
        for (int slot = 0; slot < 27; slot++) {
            reservations = reservations.with(slot, identity(
                    Items.POISONOUS_POTATO,
                    slot + 2,
                    "slot_" + slot,
                    "Slot " + slot
            ));
        }
        physical.set(13, identity(Items.POISONOUS_POTATO, 8, "slot_13", "Slot 13"));
        ReservationData beforeReservations = reservations;
        List<ItemStack> beforePhysical = physical.stream().map(ItemStack::copy).toList();
        List<ShulkerTooltipOverlay.SlotOverlay> plan =
                ShulkerTooltipOverlay.plan(reservations, physical);

        assertEquals(27, plan.size());
        assertEquals(
                java.util.stream.IntStream.range(0, 27).boxed().toList(),
                plan.stream().map(ShulkerTooltipOverlay.SlotOverlay::slot).toList()
        );
        assertEquals(
                ReservationVisualRenderer.SlotVisualState.EMPTY_RESERVED,
                plan.getFirst().state()
        );
        assertEquals(
                ReservationVisualRenderer.SlotVisualState.OCCUPIED_RESERVED,
                plan.get(13).state()
        );
        assertTrue(ItemStack.isSameItemSameComponents(
                reservations.get(26).orElseThrow(),
                plan.getLast().template()
        ));
        assertEquals(beforeReservations, reservations);
        for (int slot = 0; slot < 27; slot++) {
            assertTrue(ItemStack.matches(beforePhysical.get(slot), physical.get(slot)));
        }

        ItemStack exposed = plan.getFirst().template();
        assertNotSame(exposed, plan.getFirst().template());
        exposed.set(DataComponents.ITEM_NAME, Component.literal("caller mutation"));
        assertFalse(ItemStack.isSameItemSameComponents(exposed, plan.getFirst().template()));
    }

    @Test
    void tooltipLeavesUnreservedCasesAndReservationFreeShulkersCompletelyUnplanned() {
        NonNullList<ItemStack> physical = NonNullList.withSize(27, ItemStack.EMPTY);
        physical.set(4, new ItemStack(Items.STONE, 12));

        assertTrue(ShulkerTooltipOverlay.plan(ReservationData.EMPTY, physical).isEmpty());

        ReservationData reservations =
                ReservationData.EMPTY.with(2, new ItemStack(Items.POISONOUS_POTATO));
        List<ShulkerTooltipOverlay.SlotOverlay> plan =
                ShulkerTooltipOverlay.plan(reservations, physical);
        assertEquals(1, plan.size());
        assertEquals(2, plan.getFirst().slot());
        assertTrue(ShulkerTooltipOverlay.at(reservations, physical, 4).isEmpty(),
                "A physically occupied but unreserved slot must remain native");
        assertTrue(ShulkerTooltipOverlay.at(reservations, physical, 3).isEmpty(),
                "An empty unreserved slot must remain blank");
        assertTrue(ShulkerTooltipOverlay.plan(reservations, physical.subList(0, 26)).isEmpty(),
                "Only the exact vanilla 27-slot shulker grid is eligible");
    }

    @Test
    void ghostExtractionAlphaScopeRestoresAfterNestingAndFailure() {
        assertEquals(GhostItemRenderScope.OPAQUE_ALPHA, GhostItemRenderScope.activeAlpha());
        GhostItemRenderScope.withAlpha(0x59, () -> {
            assertEquals(0x59, GhostItemRenderScope.activeAlpha());
            GhostItemRenderScope.withAlpha(0x40,
                    () -> assertEquals(0x40, GhostItemRenderScope.activeAlpha()));
            assertEquals(0x59, GhostItemRenderScope.activeAlpha());
        });
        assertEquals(GhostItemRenderScope.OPAQUE_ALPHA, GhostItemRenderScope.activeAlpha());

        assertThrows(IllegalStateException.class, () -> GhostItemRenderScope.withAlpha(0x59, () -> {
            throw new IllegalStateException("fixture");
        }));
        assertEquals(GhostItemRenderScope.OPAQUE_ALPHA, GhostItemRenderScope.activeAlpha());
        assertThrows(IllegalArgumentException.class,
                () -> GhostItemRenderScope.alphaOnlyWhite(-1));
        assertThrows(IllegalArgumentException.class,
                () -> GhostItemRenderScope.alphaOnlyWhite(0x100));
    }

    @Test
    void tooltipSourceCaptureIsNoOpWithoutReservationsAndDefensivelyCopiesWhenNeeded() {
        ItemStack source = new ItemStack(Blocks.SHULKER_BOX.asItem());
        ItemStack sourceBefore = source.copy();
        RecordingTooltipComponent tooltip = new RecordingTooltipComponent();

        EasyShulkerTooltipCompat.captureSource(
                source,
                ReservationData.EMPTY,
                Optional.of(tooltip)
        );

        assertEquals(0, tooltip.captureCount);
        assertTrue(tooltip.source.isEmpty());
        assertTrue(ItemStack.matches(sourceBefore, source));

        ReservationData reservations = ReservationData.EMPTY.with(
                7,
                identity(Items.POISONOUS_POTATO, 19, "green_curry", "Green Curry")
        );
        EasyShulkerTooltipCompat.captureSource(source, reservations, Optional.of(tooltip));

        assertEquals(1, tooltip.captureCount);
        assertNotSame(source, tooltip.source);
        assertTrue(ItemStack.matches(source, tooltip.source));
        assertTrue(ItemStack.matches(sourceBefore, source));
    }

    private static ItemStack identity(Item item, int count, String model, String name) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("container_slot_reservations_fixture", model));
        stack.set(DataComponents.ITEM_NAME, Component.literal(name));
        return stack;
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }

    private static final class RecordingTooltipComponent
            implements TooltipComponent, TooltipSourceAccess {
        private ItemStack source = ItemStack.EMPTY;
        private int captureCount;

        @Override
        public ItemStack containerSlotReservations$getSourceStack() {
            return source;
        }

        @Override
        public void containerSlotReservations$setSourceStack(ItemStack sourceStack) {
            source = sourceStack.copy();
            captureCount++;
        }
    }
}
