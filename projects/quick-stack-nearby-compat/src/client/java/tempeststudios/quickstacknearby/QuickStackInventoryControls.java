package tempeststudios.quickstacknearby;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import tempeststudios.quickstacknearby.mixin.AbstractContainerScreenAccessor;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Installs QSN controls after every player inventory has finished its own initialization. */
public final class QuickStackInventoryControls {
    private static final int SIZE = QuickStackIconButtonRenderer.SIZE;
    private static final int GAP = 1;
    private static final Map<Screen, Controls> CONTROLS = new WeakHashMap<>();

    private QuickStackInventoryControls() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register(QuickStackInventoryControls::install);
    }

    private static void install(Minecraft client, Screen screen, int width, int height) {
        // Inventory Extended augments this same vanilla screen, rather than replacing its
        // lifecycle.  AFTER_INIT intentionally observes its final player-inventory layout.
        if (!(screen instanceof InventoryScreen) || !(screen instanceof AbstractContainerScreen<?> container)) {
            return;
        }
        List<AbstractWidget> widgets = Screens.getWidgets(screen);
        removePrevious(screen, widgets);
        AbstractContainerScreenAccessor bounds = (AbstractContainerScreenAccessor) container;
        Placement placement = findOpenPair(widgets, bounds, width, height);
        Button quickStack = new QuickStackIconButton(placement.quickX(), placement.quickY(),
                Component.literal("Quick stack to nearby containers. Right-click for slot rules."), pressed -> {
                    QuickStackClientNetworking.sendQuickStackRequest();
                    clearFocus(client, container, pressed);
                }, pressed -> openRules(client, container, pressed));
        Button search = new NearbySearchIconButton(placement.searchX(), placement.searchY(),
                Component.literal("Search live nearby storage."), pressed -> {
                    ClientScreenCompat.setScreen(client, new NearbySearchScreen(screen));
                    clearFocus(client, container, pressed);
                });
        widgets.add(quickStack);
        widgets.add(search);
        CONTROLS.put(screen, new Controls(new WeakReference<>(quickStack), new WeakReference<>(search)));
        System.out.println("[QuickStackNearby] Installed Quick Stack and Nearby Search controls on player inventory.");
    }

    public static boolean openRulesFromButton(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        Controls controls = CONTROLS.get(screen);
        Button quickStack = controls == null ? null : controls.quickStack().get();
        if (button != 1 || quickStack == null || !quickStack.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        openRules(client, screen, quickStack);
        return true;
    }

    static boolean hasControls(Screen screen) {
        Controls controls = CONTROLS.get(screen);
        return controls != null && controls.quickStack().get() != null && controls.search().get() != null;
    }

    static boolean openNearbySearchForSmoke(Minecraft client, Screen screen) {
        Controls controls = CONTROLS.get(screen);
        Button search = controls == null ? null : controls.search().get();
        if (search == null || !search.visible || !search.active) {
            return false;
        }
        ClientScreenCompat.setScreen(client, new NearbySearchScreen(screen));
        clearFocus(client, (AbstractContainerScreen<?>) screen, search);
        return true;
    }

    private static void openRules(Minecraft client, AbstractContainerScreen<?> screen, Button button) {
        ClientScreenCompat.setScreen(client, new QuickStackRulesScreen(screen, client.player));
        clearFocus(client, screen, button);
    }

    private static void clearFocus(Minecraft client, AbstractContainerScreen<?> screen, Button button) {
        client.execute(() -> {
            button.setFocused(false);
            screen.setFocused(null);
        });
    }

    private static void removePrevious(Screen screen, List<AbstractWidget> widgets) {
        Controls previous = CONTROLS.remove(screen);
        if (previous == null) {
            return;
        }
        widgets.remove(previous.quickStack().get());
        widgets.remove(previous.search().get());
    }

    private static Placement findOpenPair(
            List<AbstractWidget> widgets,
            AbstractContainerScreenAccessor bounds,
            int screenWidth,
            int screenHeight
    ) {
        int preferredY = bounds.getTopPos() + bounds.getImageHeight() - 83;
        int[] columns = {
                bounds.getLeftPos() + bounds.getImageWidth() + GAP,
                bounds.getLeftPos() - SIZE - GAP
        };
        for (int x : columns) {
            if (x < 2 || x + SIZE > screenWidth - 2) {
                continue;
            }
            for (int row = 0; row < 8; row++) {
                int quickY = preferredY + row * (SIZE + GAP);
                int searchY = quickY + SIZE + GAP;
                if (quickY < 2 || searchY + SIZE > screenHeight - 2) {
                    break;
                }
                if (open(widgets, x, quickY) && open(widgets, x, searchY)) {
                    return new Placement(x, quickY, x, searchY);
                }
            }
        }
        // The ordinary inventory always has a free utility column.  Keep a bounded fallback
        // for exotic resource-pack dimensions instead of placing controls off-screen.
        int x = Math.max(2, Math.min(screenWidth - SIZE - 2, columns[0]));
        int quickY = Math.max(2, Math.min(screenHeight - SIZE * 2 - GAP - 2, preferredY));
        return new Placement(x, quickY, x, quickY + SIZE + GAP);
    }

    private static boolean open(List<AbstractWidget> widgets, int x, int y) {
        return widgets.stream().noneMatch(widget -> widget.visible
                && x < widget.getRight() + GAP && x + SIZE + GAP > widget.getX()
                && y < widget.getBottom() + GAP && y + SIZE + GAP > widget.getY());
    }

    private record Controls(WeakReference<Button> quickStack, WeakReference<Button> search) {
    }

    private record Placement(int quickX, int quickY, int searchX, int searchY) {
    }
}
