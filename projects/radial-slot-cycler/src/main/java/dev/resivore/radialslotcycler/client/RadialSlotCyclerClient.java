package dev.resivore.radialslotcycler.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.resivore.radialslotcycler.RadialSlotCycler;
import dev.resivore.radialslotcycler.core.ColumnLayout;
import dev.resivore.radialslotcycler.core.OrdinaryInventorySnapshot;
import dev.resivore.radialslotcycler.network.SwapSlotPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class RadialSlotCyclerClient implements ClientModInitializer {
    private static final double DEAD_ZONE_RADIUS = 20.0D;
    private static final int LARGE_RING_RADIUS = 56;
    private static final int SMALL_RING_RADIUS = 48;

    private static final int COLOR_BACKDROP = 0xC0101010;
    private static final int COLOR_ENTRY = 0xD0303030;
    private static final int COLOR_ENTRY_EMPTY = 0xB0181818;
    private static final int COLOR_STORAGE = 0xFF9A9A9A;
    private static final int COLOR_ANCHOR = 0xFF55CFEF;
    private static final int COLOR_HIGHLIGHT = 0xFFFFD75A;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_MUTED_TEXT = 0xFFB8B8B8;

    private KeyMapping radialKey;
    private RadialInteractionController interaction;
    private RadialSession session;
    private int highlightedIndex = -1;
    private long sessionOpenedAtMillis;
    private Boolean privateVisualsAvailable;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                id("controls"));
        radialKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.radial_slot_cycler.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category));
        interaction = new RadialInteractionController(RadialClientConfig.loadMode());

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudElementRegistry.addLast(id("selector"), this::extractHud);
    }

    private void tick(Minecraft client) {
        boolean keyDown = radialKey.isDown();
        boolean keyClicked = radialKey.consumeClick();
        boolean canUse = canUse(client);

        if (interaction.isOpen()) {
            canUse = canUse && sessionIsStillValid(client);
            highlightedIndex = canUse ? pointerSelection(client) : -1;
        }

        RadialInteractionController.Action action = interaction.update(
                keyDown, keyClicked, canUse, highlightedIndex);
        switch (action) {
            case OPENED -> {
                if (!openSession(client)) {
                    interaction.forceClosed(keyDown);
                    closeSession(client);
                }
            }
            case CONFIRM -> {
                sendSelection();
                closeSession(client);
            }
            case CLOSED_WITHOUT_MUTATION, CANCELLED -> closeSession(client);
            case NONE -> { }
        }
    }

    private boolean canUse(Minecraft client) {
        if (client.player == null
                || client.level == null
                || client.getConnection() == null
                || client.gui.screen() != null
                || client.gui.hud.isHidden()
                || client.player.isSpectator()
                || !ClientPlayNetworking.canSend(SwapSlotPayload.TYPE)) {
            return false;
        }
        Inventory inventory = client.player.getInventory();
        return ColumnLayout.isHotbarSlot(inventory.getSelectedSlot())
                && ColumnLayout.isCompleteOrdinaryLayout(
                        inventory.getNonEquipmentItems().size());
    }

    private boolean openSession(Minecraft client) {
        if (!canUse(client)) {
            return false;
        }
        Inventory inventory = client.player.getInventory();
        int hotbarSlot = inventory.getSelectedSlot();
        int ordinarySize = inventory.getNonEquipmentItems().size();
        session = new RadialSession(
                hotbarSlot,
                ordinarySize,
                OrdinaryInventorySnapshot.capture(inventory.getNonEquipmentItems()),
                ColumnLayout.radialSlots(hotbarSlot, ordinarySize));
        sessionOpenedAtMillis = System.currentTimeMillis();
        highlightedIndex = -1;
        client.mouseHandler.releaseMouse();
        return true;
    }

    private boolean sessionIsStillValid(Minecraft client) {
        if (session == null || client.player == null) {
            return false;
        }
        Inventory inventory = client.player.getInventory();
        return inventory.getSelectedSlot() == session.hotbarSlot()
                && inventory.getNonEquipmentItems().size() == session.ordinarySize()
                && OrdinaryInventorySnapshot.matches(
                        session.contentsSnapshot(), inventory.getNonEquipmentItems());
    }

    private int pointerSelection(Minecraft client) {
        double mouseX = client.mouseHandler.getScaledXPos(client.getWindow());
        double mouseY = client.mouseHandler.getScaledYPos(client.getWindow());
        double centerX = client.getWindow().getGuiScaledWidth() / 2.0D;
        double centerY = client.getWindow().getGuiScaledHeight() / 2.0D;
        return RadialSelection.fromPointer(
                mouseX - centerX,
                mouseY - centerY,
                session.slots().size(),
                DEAD_ZONE_RADIUS);
    }

    private void sendSelection() {
        if (session == null
                || highlightedIndex <= 0
                || highlightedIndex >= session.slots().size()
                || !ClientPlayNetworking.canSend(SwapSlotPayload.TYPE)) {
            return;
        }
        ClientPlayNetworking.send(new SwapSlotPayload(
                session.hotbarSlot(),
                session.slots().get(highlightedIndex),
                session.ordinarySize()));
    }

    private void closeSession(Minecraft client) {
        session = null;
        sessionOpenedAtMillis = 0L;
        highlightedIndex = -1;
        if (client.player != null && client.gui.screen() == null) {
            client.mouseHandler.grabMouse();
        }
    }

    private void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (session == null
                || interaction == null
                || !interaction.isOpen()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!sessionIsStillValid(client)) {
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int entryCount = session.slots().size();
        List<ItemStack> ordinaryItems = client.player.getInventory().getNonEquipmentItems();

        if (privateVisualsAvailable(client)) {
            renderPrivateHud(graphics, client, ordinaryItems, centerX, centerY, entryCount);
        } else {
            renderCanary1Fallback(graphics, client, ordinaryItems, centerX, centerY, entryCount);
        }
    }

    private boolean privateVisualsAvailable(Minecraft client) {
        if (privateVisualsAvailable == null) {
            privateVisualsAvailable = client.getResourceManager()
                    .getResource(RadialVisualStyle.OVERLAY)
                    .isPresent();
            if (!privateVisualsAvailable) {
                RadialSlotCycler.LOGGER.warn(
                        "Private radial overlay is absent; using the Canary 1 project-owned fallback");
            }
        }
        return privateVisualsAvailable;
    }

    private void renderPrivateHud(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            List<ItemStack> ordinaryItems,
            int centerX,
            int centerY,
            int entryCount
    ) {
        float progress = RadialVisualStyle.easedOpenProgress(
                sessionOpenedAtMillis, System.currentTimeMillis());
        RadialVisualStyle.beginTransform(graphics, centerX, centerY, progress);
        try {
            RadialVisualStyle.drawOverlay(graphics, centerX, centerY, progress);
            for (int index = 0; index < entryCount; index++) {
                int itemX = RadialVisualStyle.itemX(centerX, index, entryCount);
                int itemY = RadialVisualStyle.itemY(centerY, index, entryCount);
                ItemStack stack = ordinaryItems.get(session.slots().get(index));
                if (index == highlightedIndex) {
                    RadialVisualStyle.drawHighlight(graphics, itemX, itemY);
                }
                if (!stack.isEmpty()) {
                    graphics.item(stack, itemX, itemY);
                    graphics.itemDecorations(client.font, stack, itemX, itemY);
                }
            }

            if (highlightedIndex >= 0) {
                graphics.centeredText(
                        client.font,
                        Component.translatable(
                                highlightedIndex == 0
                                        ? "hud.radial_slot_cycler.current_short"
                                        : "hud.radial_slot_cycler.swap_short"),
                        centerX,
                        centerY - 4,
                        COLOR_TEXT);
                showPrivateTooltip(graphics, client, ordinaryItems);
            }
        } finally {
            RadialVisualStyle.endTransform(graphics);
        }
    }

    private void showPrivateTooltip(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            List<ItemStack> ordinaryItems
    ) {
        ItemStack stack = ordinaryItems.get(session.slots().get(highlightedIndex));
        Component tooltip;
        if (!stack.isEmpty()) {
            tooltip = stack.getHoverName();
        } else if (highlightedIndex == 0) {
            tooltip = Component.translatable("hud.radial_slot_cycler.hotbar_empty");
        } else {
            tooltip = Component.translatable(
                    "hud.radial_slot_cycler.storage_empty_name", highlightedIndex);
        }
        graphics.setTooltipForNextFrame(
                client.font,
                tooltip,
                (int) client.mouseHandler.getScaledXPos(client.getWindow()),
                (int) client.mouseHandler.getScaledYPos(client.getWindow()));
    }

    private void renderCanary1Fallback(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            List<ItemStack> ordinaryItems,
            int centerX,
            int centerY,
            int entryCount
    ) {
        int ringRadius = entryCount > 4 ? LARGE_RING_RADIUS : SMALL_RING_RADIUS;

        graphics.fill(centerX - 17, centerY - 12, centerX + 18, centerY + 13, COLOR_BACKDROP);
        graphics.outline(centerX - 17, centerY - 12, 35, 25, COLOR_STORAGE);
        graphics.centeredText(
                client.font,
                Component.literal("C" + (session.hotbarSlot() + 1)),
                centerX,
                centerY - 5,
                COLOR_ANCHOR);
        graphics.centeredText(
                client.font,
                Component.translatable("hud.radial_slot_cycler.center"),
                centerX,
                centerY + 5,
                COLOR_MUTED_TEXT);

        for (int index = 0; index < entryCount; index++) {
            double angle = RadialSelection.entryAngle(index, entryCount);
            int itemX = (int) Math.round(centerX + Math.cos(angle) * ringRadius) - 8;
            int itemY = (int) Math.round(centerY + Math.sin(angle) * ringRadius) - 8;
            ItemStack stack = ordinaryItems.get(session.slots().get(index));
            renderEntry(graphics, client, stack, itemX, itemY, index);
        }

        Component description = descriptionForHighlighted(ordinaryItems);
        graphics.centeredText(
                client.font,
                description,
                centerX,
                centerY + ringRadius + 25,
                highlightedIndex > 0 ? COLOR_HIGHLIGHT : COLOR_MUTED_TEXT);
    }

    private void renderEntry(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            ItemStack stack,
            int itemX,
            int itemY,
            int index
    ) {
        boolean anchor = index == 0;
        boolean highlighted = index == highlightedIndex;
        int border = highlighted ? COLOR_HIGHLIGHT : (anchor ? COLOR_ANCHOR : COLOR_STORAGE);

        if (anchor) {
            graphics.outline(itemX - 4, itemY - 4, 24, 24, COLOR_ANCHOR);
        }
        graphics.fill(itemX - 3, itemY - 3, itemX + 19, itemY + 19, border);
        graphics.fill(
                itemX - 2,
                itemY - 2,
                itemX + 18,
                itemY + 18,
                stack.isEmpty() ? COLOR_ENTRY_EMPTY : COLOR_ENTRY);
        if (stack.isEmpty()) {
            graphics.outline(itemX, itemY, 16, 16, COLOR_STORAGE);
        } else {
            graphics.item(stack, itemX, itemY);
            graphics.itemDecorations(client.font, stack, itemX, itemY);
        }

        String label = anchor ? "H" : Integer.toString(index);
        graphics.centeredText(
                client.font,
                label,
                itemX + 8,
                itemY + 19,
                anchor ? COLOR_ANCHOR : COLOR_TEXT);
    }

    private Component descriptionForHighlighted(List<ItemStack> ordinaryItems) {
        if (highlightedIndex < 0) {
            return Component.translatable("hud.radial_slot_cycler.cancel");
        }
        if (highlightedIndex == 0) {
            return Component.translatable("hud.radial_slot_cycler.hotbar_anchor");
        }
        ItemStack stack = ordinaryItems.get(session.slots().get(highlightedIndex));
        return Component.translatable(
                stack.isEmpty()
                        ? "hud.radial_slot_cycler.storage_empty"
                        : "hud.radial_slot_cycler.storage",
                highlightedIndex);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(RadialSlotCycler.MOD_ID, path);
    }

    private record RadialSession(
            int hotbarSlot,
            int ordinarySize,
            List<ItemStack> contentsSnapshot,
            List<Integer> slots
    ) {
        private RadialSession {
            contentsSnapshot = List.copyOf(contentsSnapshot);
            slots = List.copyOf(slots);
        }
    }
}
