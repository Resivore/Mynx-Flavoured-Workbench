package tempeststudios.quickstacknearby.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.quickstacknearby.QuickStackButtonSlotBridge;
import tempeststudios.quickstacknearby.QuickStackClientNetworking;
import tempeststudios.quickstacknearby.ClientScreenCompat;
import tempeststudios.quickstacknearby.QuickStackIconButton;
import tempeststudios.quickstacknearby.QuickStackRulesScreen;
import tempeststudios.quickstacknearby.QuickStackRulesButtonScreen;
import tempeststudios.quickstacknearby.RecipeBookAwareButtonScreen;

@Mixin(AbstractContainerScreen.class)
public abstract class QuickStackInventoryScreenMixin implements RecipeBookAwareButtonScreen, QuickStackRulesButtonScreen {
    @Unique private static final String quickStackNearby$OWNER = "quick-stack-nearby";
    @Unique private static final String quickStackNearby$SLOT = "quick_stack_nearby";
    @Unique private static final String quickStackNearby$SEARCH_SLOT = "nearby_search";

    @Unique private Button quickStackNearby$button;
    @Unique private Button quickStackNearby$searchButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void quickStackNearby$onInit(CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (!(screen instanceof InventoryScreen)) {
            quickStackNearby$button = null;
            quickStackNearby$searchButton = null;
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            quickStackNearby$button = null;
            quickStackNearby$searchButton = null;
            return;
        }

        QuickStackButtonSlotBridge.releaseOwner(screen, quickStackNearby$OWNER);
        QuickStackButtonSlotBridge.SlotPlacement placement = QuickStackButtonSlotBridge.reservePlayerInventorySlot(
                screen,
                quickStackNearby$OWNER,
                quickStackNearby$SLOT
        );

        Button button = new QuickStackIconButton(
                placement.x(),
                placement.y(),
                Component.literal("Quick stack to nearby containers. Right-click for slot rules."),
                pressed -> {
                    QuickStackClientNetworking.sendQuickStackRequest();
                    quickStackNearby$clearFocus(client, screen, pressed);
                },
                pressed -> {
                    ClientScreenCompat.setScreen(client, new QuickStackRulesScreen(screen, client.player));
                    quickStackNearby$clearFocus(client, screen, pressed);
                }
        );
        ((ScreenAccessor) this).invokeAddRenderableWidget(button);
        quickStackNearby$button = button;
        QuickStackButtonSlotBridge.SlotPlacement searchPlacement = QuickStackButtonSlotBridge.reservePlayerInventorySlot(
                screen, quickStackNearby$OWNER, quickStackNearby$SEARCH_SLOT);
        Button search = new tempeststudios.quickstacknearby.NearbySearchIconButton(
                searchPlacement.x(), searchPlacement.y(), Component.literal("Search live nearby storage."),
                pressed -> {
                    ClientScreenCompat.setScreen(client, new tempeststudios.quickstacknearby.NearbySearchScreen(screen));
                    quickStackNearby$clearFocus(client, screen, pressed);
                });
        ((ScreenAccessor) this).invokeAddRenderableWidget(search);
        quickStackNearby$searchButton = search;
    }

    @Inject(method = {"render", "extractRenderState"}, at = @At("HEAD"), require = 0)
    private void quickStackNearby$onRender(CallbackInfo ci) {
        quickStackNearby$updateButtonPosition();
    }

    @Override
    public void quickstacknearby$updateButtonPositionsFromRecipeBookRender() {
        quickStackNearby$updateButtonPosition();
    }

    @Override
    public boolean quickstacknearby$openRulesFromButton(double mouseX, double mouseY, int button) {
        if (button != 1 || quickStackNearby$button == null || !quickStackNearby$button.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        ClientScreenCompat.setScreen(client, new QuickStackRulesScreen(screen, client.player));
        quickStackNearby$clearFocus(client, screen, quickStackNearby$button);
        return true;
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void quickStackNearby$onRemoved(CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        QuickStackButtonSlotBridge.releaseOwner(screen, quickStackNearby$OWNER);
        quickStackNearby$button = null;
        quickStackNearby$searchButton = null;
    }

    @Unique
    private void quickStackNearby$updateButtonPosition() {
        if (quickStackNearby$button == null) {
            return;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        QuickStackButtonSlotBridge.SlotPlacement placement = QuickStackButtonSlotBridge.reservePlayerInventorySlot(
                screen,
                quickStackNearby$OWNER,
                quickStackNearby$SLOT
        );
        quickStackNearby$button.setX(placement.x());
        quickStackNearby$button.setY(placement.y());
        if (quickStackNearby$searchButton != null) {
            QuickStackButtonSlotBridge.SlotPlacement searchPlacement = QuickStackButtonSlotBridge.reservePlayerInventorySlot(
                    screen, quickStackNearby$OWNER, quickStackNearby$SEARCH_SLOT);
            quickStackNearby$searchButton.setX(searchPlacement.x());
            quickStackNearby$searchButton.setY(searchPlacement.y());
        }
    }

    @Unique
    private static void quickStackNearby$clearFocus(Minecraft client, AbstractContainerScreen<?> screen, Button button) {
        client.execute(() -> {
            button.setFocused(false);
            screen.setFocused(null);
        });
    }

}
