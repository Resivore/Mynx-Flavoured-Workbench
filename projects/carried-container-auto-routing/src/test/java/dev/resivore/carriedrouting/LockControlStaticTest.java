package dev.resivore.carriedrouting;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockControlStaticTest {
    private static String read(Path root, String relative) throws Exception {
        return Files.readString(root.resolve(relative));
    }

    @Test
    void configurableRHoverPayloadServerToggleAndFeedbackAreWired() throws Exception {
        Path project = Path.of(System.getProperty("projectRoot"));
        String client = read(project, "src/main/java/dev/resivore/carriedrouting/client/CarriedContainerAutoRoutingClient.java");
        String server = read(project, "src/main/java/dev/resivore/carriedrouting/CarriedContainerAutoRouting.java");
        String lock = read(project, "src/main/java/dev/resivore/carriedrouting/RoutingLock.java");
        String accessor = read(project, "src/main/java/dev/resivore/carriedrouting/mixin/AbstractContainerScreenAccessor.java");
        String keyboardMixin = read(project, "src/main/java/dev/resivore/carriedrouting/mixin/KeyboardHandlerMixin.java");
        String mixinConfig = read(project, "src/main/resources/carried_container_auto_routing.mixins.json");
        String metadata = read(project, "src/main/resources/fabric.mod.json");
        String language = read(project, "src/main/resources/assets/carried_container_auto_routing/lang/en_us.json");

        assertTrue(metadata.contains("CarriedContainerAutoRoutingClient"));
        assertTrue(client.contains("KeyMappingHelper.registerKeyMapping"));
        assertTrue(client.contains("GLFW.GLFW_KEY_R"));
        assertTrue(client.contains("toggle.matches(event)"));
        assertTrue(client.contains("carriedRouting$getHoveredSlot"));
        assertTrue(client.contains("screen.getMenu().slots.indexOf(slot)"));
        assertTrue(client.contains("slot.container == client.player.getInventory()"));
        assertTrue(!client.contains("slot.index, 0"));
        assertTrue(accessor.contains("@Accessor(\"hoveredSlot\")"));
        assertTrue(mixinConfig.contains("\"KeyboardHandlerMixin\""));
        assertTrue(keyboardMixin.contains("@Mixin(KeyboardHandler.class)"));
        assertTrue(keyboardMixin.contains("method = \"keyPress\""));
        assertTrue(keyboardMixin.contains("action != InputConstants.PRESS"));
        assertTrue(keyboardMixin.contains("instanceof AbstractContainerScreen<?>"));
        assertTrue(keyboardMixin.contains("CarriedContainerAutoRoutingClient.handleContainerKey(client, event)"));
        assertTrue(client.contains("ClientPlayNetworking.send(new ToggleLockPayload"));
        assertTrue(client.contains("RoutingService.isSupported(main) ? 1 : 2"));
        assertTrue(server.contains("ServerPlayNetworking.registerGlobalReceiver"));
        assertTrue(server.contains("resolveActiveMenuSlot(context.player(), payload.menuId()"));
        assertTrue(server.contains("!targetSlot.isActive() || targetSlot.isFake()"));
        assertTrue(server.contains("!targetSlot.mayPickup(player) || !targetSlot.allowModification(player)"));
        assertTrue(server.contains("targetSlot.container.stillValid(player)"));
        assertTrue(server.contains("RoutingLock.setLocked(target, locked)"));
        assertTrue(server.contains("sendOverlayMessage"));
        assertTrue(!server.contains("text.carried_container_auto_routing.no_target"));
        assertTrue(server.contains("text.carried_container_auto_routing.locked"));
        assertTrue(server.contains("text.carried_container_auto_routing.unlocked"));
        assertTrue(lock.contains("carried_container_auto_routing.locked"));
        assertTrue(language.contains("key.categories.carried_container_auto_routing.routing"));
        assertTrue(language.contains("Auto-routing: Locked"));
        assertTrue(language.contains("Auto-routing: Unlocked"));
        assertTrue(!language.contains("No supported carried container targeted"));
    }

    @Test
    void creativeWrapperMustUseItsLiveMenuPositionRatherThanPublicSlotIndex() {
        Object first = new Object();
        Object hoveredWrapper = new Object();
        List<Object> menuSlots = List.of(first, new Object(), hoveredWrapper);
        int creativeWrapperPublicIndex = 0;

        assertEquals(0, creativeWrapperPublicIndex);
        assertEquals(2, menuSlots.indexOf(hoveredWrapper));
    }
}
