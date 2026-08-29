package dev.resivore.radialslotcycler;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticCoexistenceTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void serverOwnsAThreeIntegerRequestAndValidatesBackingIdentity() throws IOException {
        String common = read("src/main/java/dev/resivore/radialslotcycler/RadialSlotCycler.java");
        String payload = read("src/main/java/dev/resivore/radialslotcycler/network/SwapSlotPayload.java");
        String validator = read("src/main/java/dev/resivore/radialslotcycler/core/SwapRequestValidator.java");

        assertTrue(common.contains("PayloadTypeRegistry.serverboundPlay().register"));
        assertTrue(common.contains("ServerPlayNetworking.registerGlobalReceiver"));
        assertTrue(common.contains("!player.isAlive()"));
        assertTrue(common.contains("player.containerMenu == player.inventoryMenu"));
        assertTrue(common.contains("inventory.getNonEquipmentItems()"));
        assertTrue(common.contains("player.inventoryMenu.broadcastChanges()"));
        assertTrue(validator.contains("STALE_SELECTED_HOTBAR_SLOT"));
        assertTrue(validator.contains("STALE_ORDINARY_SIZE"));
        assertTrue(payload.contains("int selectedHotbarSlot"));
        assertTrue(payload.contains("int storageSlot"));
        assertTrue(payload.contains("int ordinarySize"));
        assertFalse(payload.contains("ItemStack"));
    }

    @Test
    void clientUsesAnUnboundSeparateKeyAndSuppressesGuiSpectatorAndStaleState() throws IOException {
        String client = read("src/main/java/dev/resivore/radialslotcycler/client/RadialSlotCyclerClient.java");

        assertTrue(client.contains("GLFW.GLFW_KEY_UNKNOWN"));
        assertTrue(client.contains("client.gui.screen() != null"));
        assertTrue(client.contains("client.gui.hud.isHidden()"));
        assertTrue(client.contains("client.player.isSpectator()"));
        assertTrue(client.contains("inventory.getSelectedSlot() == session.hotbarSlot()"));
        assertTrue(client.contains("OrdinaryInventorySnapshot.matches"));
        assertTrue(client.contains("client.mouseHandler.releaseMouse()"));
        assertTrue(client.contains("client.mouseHandler.grabMouse()"));
        assertTrue(client.contains("DEAD_ZONE_RADIUS = 20.0D"));
    }

    @Test
    void implementationDoesNotEnterOtherWorkbenchMutationSystems() throws IOException {
        String allMain = readTree("src/main/java");

        assertFalse(allMain.contains("Inventory.INVENTORY_SIZE"));
        assertFalse(allMain.contains("quickMoveStack"));
        assertFalse(allMain.contains("moveItemStackTo"));
        assertFalse(allMain.contains("ContainerInput.QUICK_MOVE"));
        assertFalse(allMain.contains("ItemEntity"));
        assertFalse(allMain.contains("travelertoolbelt"));
        assertFalse(allMain.contains("fuzs.hotbarslotcycling"));
        assertFalse(allMain.contains("fuzs.slotcycler"));
        assertFalse(allMain.contains("carriedrouting"));
        assertFalse(allMain.contains("quickstacknearby"));
        assertFalse(allMain.contains("trinkets"));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/radial_slot_cycler.mixins.json")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/resources/assets/radial_slot_cycler/textures/gui/belt_overlay.png")));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    private static String readTree(String relative) throws IOException {
        StringBuilder result = new StringBuilder();
        try (var paths = Files.walk(ROOT.resolve(relative))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                result.append(Files.readString(path));
            }
        }
        return result.toString();
    }
}
