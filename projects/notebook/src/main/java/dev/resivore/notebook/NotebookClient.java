package dev.resivore.notebook;

import com.mojang.blaze3d.platform.InputConstants;
import dev.resivore.notebook.client.NotebookScreen;
import dev.resivore.notebook.mixin.client.ContainerScreenAccess;
import dev.resivore.notebook.storage.NotebookStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Client-only entry point for Notebook access and storage ownership. */
public final class NotebookClient implements ClientModInitializer {
    public static final String MOD_ID = "notebook";
    private static final int INVENTORY_BUTTON_SIZE = 18;
    private static final int INVENTORY_BUTTON_GAP = 4;

    private KeyMapping openKey;
    private NotebookStore store;
    private final Map<Screen, WeakReference<AbstractWidget>> inventoryButtons = new WeakHashMap<>();

    @Override
    public void onInitializeClient() {
        Path notebookRoot = FabricLoader.getInstance().getConfigDir().resolve("notebook");
        store = new NotebookStore(notebookRoot);

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "controls"));
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.notebook.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(this::handleOpenKey);
        ScreenEvents.AFTER_INIT.register(this::addInventoryButton);
    }

    private void handleOpenKey(Minecraft client) {
        while (openKey.consumeClick()) {
            Screen current = client.gui.screen();
            if (current instanceof NotebookScreen) {
                return;
            }
            if (current == null) {
                open(client, current);
            }
        }
    }

    private void addInventoryButton(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
        if (!(screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen)
                || !(screen instanceof ContainerScreenAccess bounds)) {
            return;
        }

        List<AbstractWidget> widgets = Screens.getWidgets(screen);
        WeakReference<AbstractWidget> previousReference = inventoryButtons.remove(screen);
        AbstractWidget previous = previousReference == null ? null : previousReference.get();
        if (previous != null) {
            widgets.remove(previous);
        }
        int preferredX = bounds.notebook$getLeftPos()
                + bounds.notebook$getImageWidth()
                + INVENTORY_BUTTON_GAP;
        int preferredY = bounds.notebook$getTopPos() + INVENTORY_BUTTON_GAP;
        Position position = findFreeUtilityPosition(
                widgets,
                preferredX,
                preferredY,
                bounds.notebook$getLeftPos() - INVENTORY_BUTTON_SIZE - INVENTORY_BUTTON_GAP,
                scaledWidth,
                scaledHeight);

        Button button = Button.builder(Component.literal("N"), ignored -> open(client, screen))
                .bounds(position.x(), position.y(), INVENTORY_BUTTON_SIZE, INVENTORY_BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("button.notebook.open")))
                .build();
        widgets.add(button);
        inventoryButtons.put(screen, new WeakReference<>(button));
    }

    private static Position findFreeUtilityPosition(
            List<AbstractWidget> widgets,
            int rightX,
            int firstY,
            int leftX,
            int screenWidth,
            int screenHeight
    ) {
        int[] columns = {rightX, leftX};
        for (int x : columns) {
            if (x < 2 || x + INVENTORY_BUTTON_SIZE > screenWidth - 2) {
                continue;
            }
            for (int row = 0; row < 9; row++) {
                int y = firstY + row * (INVENTORY_BUTTON_SIZE + 2);
                if (y < 2 || y + INVENTORY_BUTTON_SIZE > screenHeight - 2) {
                    break;
                }
                if (widgets.stream().noneMatch(widget -> overlaps(widget, x, y))) {
                    return new Position(x, y);
                }
            }
        }
        return new Position(
                Math.max(2, Math.min(screenWidth - INVENTORY_BUTTON_SIZE - 2, rightX)),
                Math.max(2, Math.min(screenHeight - INVENTORY_BUTTON_SIZE - 2, firstY)));
    }

    private static boolean overlaps(AbstractWidget widget, int x, int y) {
        return widget.visible
                && x < widget.getRight() + 2
                && x + INVENTORY_BUTTON_SIZE + 2 > widget.getX()
                && y < widget.getBottom() + 2
                && y + INVENTORY_BUTTON_SIZE + 2 > widget.getY();
    }

    private void open(Minecraft client, Screen parent) {
        client.setScreenAndShow(new NotebookScreen(parent, store));
    }

    private record Position(int x, int y) {
    }
}
