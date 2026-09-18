package dev.resivore.slotreservations.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.resivore.slotreservations.ContainerSlotReservations;
import dev.resivore.slotreservations.network.ReservationActionPayload;
import dev.resivore.slotreservations.network.ReservationSnapshotPayload;
import dev.resivore.slotreservations.network.ReservationSnapshotRequestPayload;
import dev.resivore.slotreservations.network.ShulkerPanelSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.Event;
import dev.resivore.slotreservations.mixin.client.ContainerScreenMouseAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

public final class ContainerSlotReservationsClient implements ClientModInitializer {
    private static final Identifier CARRIED_SHULKER_INPUT_PHASE = Identifier.fromNamespaceAndPath(
            ContainerSlotReservations.MOD_ID, "carried_shulker_input");
    private static final Identifier PUZZLES_BEFORE_PHASE = Identifier.fromNamespaceAndPath(
            "puzzleslib", "before");
    private static KeyMapping reservationKey;
    private static AbstractContainerScreen<?> requestedScreen;

    @Override
    public void onInitializeClient() {
        GhostItemRenderPipeline.initialize();
        registerCarriedShulkerGestureLifecycle();
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.fromNamespaceAndPath(ContainerSlotReservations.MOD_ID,
                                "shulker_panel_texture_layout");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager resources) {
                        ShulkerPanelTextureLayout.reload(resources);
                    }
                });

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(
                ContainerSlotReservations.MOD_ID, "controls"));
        reservationKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.container_slot_reservations.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category));

        ClientPlayNetworking.registerGlobalReceiver(ReservationSnapshotPayload.TYPE, (payload, context) ->
                context.client().execute(() -> acceptSnapshot(context.client(), payload)));
        ClientPlayNetworking.registerGlobalReceiver(ShulkerPanelSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ShulkerPanel.acceptSync(payload)));

        ClientTickEvents.END_CLIENT_TICK.register(ContainerSlotReservationsClient::requestSnapshotForNewScreen);
        ClientTickEvents.END_CLIENT_TICK.register(CarriedShulkerRmbCollector::maintain);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                CarriedShulkerRmbCollector.reset());
    }

    /**
     * Owns inbound RMB before Item Interactions' puzzleslib:before handler can
     * interrupt the Screen method. Empty origins pass through unchanged.
     */
    private static void registerCarriedShulkerGestureLifecycle() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

            Event<ScreenMouseEvents.AllowMouseClick> click = ScreenMouseEvents.allowMouseClick(screen);
            orderBeforeForeignInput(click);
            click.register(CARRIED_SHULKER_INPUT_PHASE, (_screen, event) -> {
                CarriedShulkerRmbCollector.observePhysicalPress(containerScreen, event.x(), event.y(),
                        event.button(), event.hasShiftDown(), event.hasControlDown(), event.hasAltDown());
                if (event.button() != 1 || event.hasShiftDown()
                        || event.hasControlDown() || event.hasAltDown()
                        || ShulkerPanel.containsPanel(event.x(), event.y())) return true;
                Slot target = ((ContainerScreenMouseAccess) containerScreen)
                        .containerSlotReservations$slotAt(event.x(), event.y());
                return !CarriedShulkerRmbCollector.begin(containerScreen, target);
            });

            Event<ScreenMouseEvents.AllowMouseDrag> drag = ScreenMouseEvents.allowMouseDrag(screen);
            orderBeforeForeignInput(drag);
            drag.register(CARRIED_SHULKER_INPUT_PHASE, (_screen, event, dragX, dragY) ->
                    event.button() != 1 || !CarriedShulkerRmbCollector.drag(
                            containerScreen, event.x(), event.y()));

            Event<ScreenMouseEvents.AllowMouseRelease> release = ScreenMouseEvents.allowMouseRelease(screen);
            orderBeforeForeignInput(release);
            release.register(CARRIED_SHULKER_INPUT_PHASE, (_screen, event) -> {
                if (event.button() != 1) return true;
                return !CarriedShulkerRmbCollector.release();
            });
        });
    }

    private static void orderBeforeForeignInput(Event<?> event) {
        event.addPhaseOrdering(CARRIED_SHULKER_INPUT_PHASE, PUZZLES_BEFORE_PHASE);
        event.addPhaseOrdering(CARRIED_SHULKER_INPUT_PHASE, Event.DEFAULT_PHASE);
    }

    public static boolean handleContainerKey(Minecraft client, KeyEvent event) {
        if (reservationKey == null
                || !reservationKey.matches(event)
                || client.player == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)
                || !ClientPlayNetworking.canSend(ReservationActionPayload.TYPE)) {
            return false;
        }

        Boolean panel = ShulkerPanel.reservationKey();
        if (panel != null) return panel;
        Slot slot = ((ReservationScreenAccess) screen).containerSlotReservations$getHoveredSlot();
        if (slot == null || !ClientReservationState.isEligible(screen.getMenu(), slot)) return false;
        int menuSlotIndex = screen.getMenu().slots.indexOf(slot);
        if (menuSlotIndex < 0) return false;

        ReservationActionPayload.Source source;
        if (!slot.getItem().isEmpty()) {
            source = ReservationActionPayload.Source.SLOT_STACK;
        } else if (!screen.getMenu().getCarried().isEmpty()) {
            source = ReservationActionPayload.Source.CARRIED_STACK;
        } else {
            source = ReservationActionPayload.Source.CLEAR_EMPTY;
        }

        ClientPlayNetworking.send(new ReservationActionPayload(
                screen.getMenu().containerId, menuSlotIndex, source));
        return true;
    }

    private static void requestSnapshotForNewScreen(Minecraft client) {
        if (client.gui.screen() instanceof AbstractContainerScreen<?> screen) {
            if (requestedScreen == screen) return;
            requestedScreen = screen;
            ClientReservationState.clear();
            if (client.player != null && ClientPlayNetworking.canSend(ReservationSnapshotRequestPayload.TYPE)) {
                ClientPlayNetworking.send(new ReservationSnapshotRequestPayload(screen.getMenu().containerId));
            }
        } else {
            requestedScreen = null;
            ClientReservationState.clear();
        }
    }

    private static void acceptSnapshot(Minecraft client, ReservationSnapshotPayload snapshot) {
        if (client.gui.screen() instanceof AbstractContainerScreen<?> screen
                && screen.getMenu().containerId == snapshot.menuId()) {
            ClientReservationState.accept(screen.getMenu(), snapshot);
        }
    }
}
