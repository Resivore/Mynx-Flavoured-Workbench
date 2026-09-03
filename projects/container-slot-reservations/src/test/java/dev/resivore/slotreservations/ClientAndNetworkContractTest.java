package dev.resivore.slotreservations;

import dev.resivore.slotreservations.network.ReservationSnapshotPayload;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientAndNetworkContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.POISONOUS_POTATO);
    }

    @Test
    void reservationKeyIsRemappableUnboundAndHandledOncePerPhysicalPress() throws IOException {
        String client = source("client/ContainerSlotReservationsClient.java");
        String keyboard = source("mixin/client/KeyboardHandlerMixin.java");

        assertTrue(client.contains("GLFW.GLFW_KEY_UNKNOWN"), "Canary 1 must ship the key unbound");
        assertTrue(client.contains("key.container_slot_reservations.toggle"));
        assertFalse(client.contains("consumeClick"), "Screen input must not add an end-tick click loop");
        assertEquals(1, occurrences(client, "ClientPlayNetworking.send(new ReservationActionPayload("),
                "One accepted key press must send one mutation request");
        assertTrue(keyboard.contains("action != InputConstants.PRESS"));
        assertTrue(keyboard.contains("@Inject(method = \"keyPress\", at = @At(\"HEAD\"), cancellable = true)"));
        assertTrue(keyboard.contains("handleContainerKey(client, event)"));
    }

    @Test
    void mutationPayloadCarriesContextAndSourceModeButNoClientTemplate() throws IOException {
        String payload = source("network/ReservationActionPayload.java");

        assertTrue(payload.contains("record ReservationActionPayload(int menuId, int menuSlotIndex, Source source)"));
        assertTrue(payload.contains("SLOT_STACK"));
        assertTrue(payload.contains("CARRIED_STACK"));
        assertTrue(payload.contains("CLEAR_EMPTY"));
        assertFalse(payload.contains("ItemStack"), "The client must not authoritatively send a reservation stack");
        assertFalse(payload.contains("ItemStackTemplate"),
                "The client must not authoritatively send a reservation template");
    }

    @Test
    void ghostIsEmptyOnlyAlphaTintedZeroLabeledAndAnchoredToTheLiveSlot() throws IOException {
        String screen = source("mixin/client/AbstractContainerScreenMixin.java");
        String renderer = source("client/ReservationVisualRenderer.java");

        assertTrue(screen.contains("@Inject(method = \"extractSlot\", at = @At(\"TAIL\"))"));
        assertTrue(screen.contains("ReservationVisualRenderer.extract("));
        assertTrue(screen.contains("slot.x,"));
        assertTrue(screen.contains("slot.y,"));
        assertTrue(renderer.contains("copyWithCount(1)"));
        assertTrue(renderer.contains("GhostItemRenderScope.extract("));
        assertTrue(renderer.contains("EMPTY_COUNT = \"0\""));
        assertFalse(renderer.contains("copyWithCount(0)"));
        assertFalse(renderer.contains("graphics.outline("));
        assertFalse(renderer.contains("GHOST_WASH"));
        assertTrue(screen.contains("tooltip.container_slot_reservations.reserved"));
        assertTrue(screen.contains("tooltip.container_slot_reservations.empty"));
        assertFalse(renderer.contains("itemDecorations"),
                "The literal zero must be text, not decoration on a fake stack");
        assertFalse(renderer.contains("getCount()"),
                "Ghost rendering must not derive the literal zero from a stack count");
    }

    @Test
    void snapshotsNormalizeTemplatesToOneAndRejectOversizedOrMalformedMaps() {
        ItemStack logicalItem = new ItemStack(Items.POISONOUS_POTATO, 37);
        logicalItem.set(DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("matcha_fixture", "green_curry"));
        ItemStackTemplate many = ItemStackTemplate.fromNonEmptyStack(logicalItem);

        ReservationSnapshotPayload.Entry normalized = new ReservationSnapshotPayload.Entry(4, Optional.of(many));
        assertEquals(1, normalized.template().orElseThrow().count());
        assertEquals(1, normalized.template().orElseThrow().create().getCount());
        assertTrue(ItemStack.isSameItemSameComponents(logicalItem,
                normalized.template().orElseThrow().create()));

        assertThrows(IllegalArgumentException.class,
                () -> new ReservationSnapshotPayload.Entry(-1, Optional.empty()));
        List<ReservationSnapshotPayload.Entry> oversized = new ArrayList<>();
        for (int index = 0; index <= ReservationSnapshotPayload.MAX_ENTRIES; index++) {
            oversized.add(new ReservationSnapshotPayload.Entry(index, Optional.empty()));
        }
        assertThrows(IllegalArgumentException.class, () -> new ReservationSnapshotPayload(1, oversized));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(ROOT.resolve("src/main/java/dev/resivore/slotreservations").resolve(relative));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(needle, index)) >= 0; index += needle.length()) count++;
        return count;
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }
}
