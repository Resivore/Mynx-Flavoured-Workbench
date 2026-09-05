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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientAndNetworkContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Pattern MENU_TYPE = Pattern.compile("type == ([A-Za-z]+Menu)\\.class");

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

        assertTrue(client.contains("GLFW.GLFW_KEY_UNKNOWN"), "The reservation key must ship unbound");
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

        assertTrue(payload.contains(
                "record ReservationActionPayload(int menuId, int menuSlotIndex, Source source)"));
        assertTrue(payload.contains("SLOT_STACK"));
        assertTrue(payload.contains("CARRIED_STACK"));
        assertTrue(payload.contains("CLEAR_EMPTY"));
        assertFalse(payload.contains("ItemStack"),
                "The client must not authoritatively send a reservation stack");
        assertFalse(payload.contains("ItemStackTemplate"),
                "The client must not authoritatively send a reservation template");
    }

    @Test
    void panelPayloadsCarryOnlyExactAuthorityAndNeverClientComputedStacks() throws IOException {
        String content = source("network/ShulkerPanelContentActionPayload.java");
        String reservation = source("network/ShulkerPanelReservationActionPayload.java");
        String selection = source("network/ShulkerSelectionPayload.java");
        String resolver = source("ShulkerHostResolver.java");
        String actions = source("ShulkerPanelActions.java");
        String tracker = source("ShulkerSelectionTracker.java");

        for (String payload : List.of(content, reservation, selection)) {
            assertTrue(payload.contains("int menuId"));
            assertTrue(payload.contains("ShulkerHostLocator host"));
            assertTrue(payload.contains("int internalSlot"));
            assertTrue(payload.contains("String hostFingerprint"));
            assertFalse(payload.contains("ItemStack"));
            assertFalse(payload.contains("ItemStackTemplate"));
        }
        assertTrue(content.contains("Click click"));
        assertTrue(reservation.contains("ReservationActionPayload.Source source"));
        assertTrue(resolver.contains("menu.slots.get(menuSlot) != slot"));
        assertTrue(resolver.contains("stack.getCount() != 1"));
        assertTrue(resolver.contains("!slot.isActive() || slot.isFake()"));
        assertTrue(resolver.contains("!slot.mayPickup(player) || !slot.mayPlace(stack)"));
        assertTrue(actions.contains("ShulkerTransferPlanner.planExactInsertion("));
        assertTrue(actions.contains("ShulkerTransferPlanner.planExtraction("));
        assertTrue(actions.contains("host.menu().broadcastChanges()"));
        assertTrue(actions.contains("candidate.owner() == changed.owner()"),
                "Shared compound-container viewers must synchronize by physical owner identity");
        assertTrue(tracker.contains("private static final Map<Player, Selection> SELECTIONS"));
        assertFalse(tracker.contains("DataComponents"));
        assertFalse(tracker.contains("ReservationStore.set"));
    }

    @Test
    void panelInputOwnsCoveredCoordinatesAndRejectsUnsupportedGestures() throws IOException {
        String screen = source("mixin/client/AbstractContainerScreenMixin.java");
        String panel = source("client/ShulkerPanel.java");
        assertTrue(screen.contains("!doubleClick && !event.hasShiftDown()"));
        assertTrue(screen.contains("!event.hasControlDown() && !event.hasAltDown()"));
        assertTrue(screen.contains("checkHotbarKeyPressed"));
        assertTrue(screen.contains("ShulkerPanel.ownsHoveredCell()"));
        assertTrue(panel.contains("STATE.capturePointer()"));
        assertTrue(panel.contains("STATE.ownsDrag("));
        assertTrue(panel.contains("STATE.releasePointer("));
        assertTrue(panel.contains("if (standardClick && slot >= 0"));
        assertTrue(panel.contains("return true;"), "Owned panel bounds must consume no-op input");
    }

    @Test
    void serverUsesAClosedExactMenuAllowlistRatherThanGenericMenus() throws IOException {
        String networking = source("ReservationNetworking.java");
        String supportedMenus = between(
                networking,
                "private static boolean isSupportedMenu(AbstractContainerMenu menu)",
                "record ValidatedTarget("
        );
        Set<String> expected = Set.of(
                "ChestMenu",
                "ShulkerBoxMenu",
                "DispenserMenu",
                "HopperMenu",
                "FurnaceMenu",
                "BlastFurnaceMenu",
                "SmokerMenu",
                "BrewingStandMenu",
                "CrafterMenu"
        );
        Set<String> actual = new HashSet<>();
        Matcher matcher = MENU_TYPE.matcher(supportedMenus);
        int matches = 0;
        while (matcher.find()) {
            matches++;
            assertTrue(actual.add(matcher.group(1)), "The exact menu allowlist must not contain duplicates");
        }

        assertEquals(expected, actual,
                "Snapshots and actions must be limited to the nine requested exact menu classes");
        assertEquals(expected.size(), matches);
        assertTrue(supportedMenus.contains("Class<?> type = menu.getClass()"));
        assertFalse(supportedMenus.contains("instanceof"),
                "Subclass and generic menu admission would silently expand the support contract");
        assertFalse(supportedMenus.contains("isAssignableFrom"));
        assertFalse(supportedMenus.contains("AbstractContainerMenu.class"));
    }

    @Test
    void serverValidatesAuthorityAndReadsTheResolvedPhysicalOwnerStack() throws IOException {
        String networking = source("ReservationNetworking.java");

        assertTrue(networking.contains("ItemStack physical = target.resolvedSlot().physicalStack()"));
        assertFalse(networking.contains("target.slot().getItem()"),
                "An ephemeral combined view must not author the physical slot state");
        assertTrue(networking.contains("ItemStack carried = target.menu().getCarried()"));
        assertTrue(networking.contains("case SLOT_STACK"));
        assertTrue(networking.contains("if (physical.isEmpty()) return false"));
        assertTrue(networking.contains("case CARRIED_STACK"));
        assertTrue(networking.contains("if (!physical.isEmpty() || carried.isEmpty()) return false"));
        assertTrue(networking.contains("SupportedContainerResolver.isShulkerOwner(target.resolvedSlot())"));
        assertTrue(networking.contains("!carried.getItem().canFitInsideContainerItems()"),
                "Server must reject impossible nested-container reservation templates for shulkers");
        assertTrue(networking.contains("case CLEAR_EMPTY"));
        assertTrue(networking.contains("ReservationStore.setOwnerData(target.resolvedSlot().owner()"),
                "Mutation must bind to the stable physical or player-owned reservation owner");

        assertTrue(networking.contains("player.containerMenu"));
        assertTrue(networking.contains("player.isAlive()"));
        assertTrue(networking.contains("!player.isSpectator()"));
        assertTrue(networking.contains("menu.containerId == menuId"));
        assertTrue(networking.contains("menu.stillValid(player)"));
        assertTrue(networking.contains("menuSlotIndex >= menu.slots.size()"));
        assertTrue(networking.contains("slot.index != menuSlotIndex || !slot.isActive() || slot.isFake()"));
        assertTrue(networking.contains(
                "SupportedContainerResolver.resolve(slot.container, slot.getContainerSlot())"));
        assertFalse(networking.contains("payload.template()"));
        assertFalse(networking.contains("payload.stack()"));
    }

    @Test
    void snapshotsContainOnlyResolvedLiveOwnerSlotsAndSynchronizeEveryViewer() throws IOException {
        String networking = source("ReservationNetworking.java");

        assertTrue(networking.contains("if (!isSupportedMenu(menu)) return List.of()"));
        assertTrue(networking.contains(
                "if (slot.index != menuIndex || !slot.isActive() || slot.isFake()) continue"));
        assertTrue(networking.contains(
                "SupportedContainerResolver.resolve(slot.container, slot.getContainerSlot())"));
        assertTrue(networking.contains("new ReservationSnapshotPayload.Entry(menuIndex, template)"));
        assertTrue(networking.contains("ReservationSnapshotPayload.MAX_ENTRIES"));
        assertTrue(networking.contains("target.menu().broadcastChanges()"));
        assertTrue(networking.contains("syncAllOpenSupportedMenus(player.level().getServer())"));
        assertTrue(networking.contains("for (ServerPlayer viewer : server.getPlayerList().getPlayers())"));
        assertTrue(networking.contains("sendSnapshot(viewer, viewer.containerMenu)"));
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

        ReservationSnapshotPayload.Entry normalized =
                new ReservationSnapshotPayload.Entry(4, Optional.of(many));
        assertEquals(1, normalized.template().orElseThrow().count());
        assertEquals(1, normalized.template().orElseThrow().create().getCount());
        assertTrue(ItemStack.isSameItemSameComponents(
                logicalItem,
                normalized.template().orElseThrow().create()
        ));

        assertThrows(IllegalArgumentException.class,
                () -> new ReservationSnapshotPayload.Entry(-1, Optional.empty()));
        List<ReservationSnapshotPayload.Entry> oversized = new ArrayList<>();
        for (int index = 0; index <= ReservationSnapshotPayload.MAX_ENTRIES; index++) {
            oversized.add(new ReservationSnapshotPayload.Entry(index, Optional.empty()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new ReservationSnapshotPayload(1, oversized));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(ROOT.resolve("src/main/java/dev/resivore/slotreservations").resolve(relative));
    }

    private static String between(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertTrue(start >= 0, "Missing source-contract marker: " + startMarker);
        int end = text.indexOf(endMarker, start + startMarker.length());
        assertTrue(end >= 0, "Missing source-contract marker: " + endMarker);
        return text.substring(start, end);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(needle, index)) >= 0; index += needle.length()) {
            count++;
        }
        return count;
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }
}
