package dev.resivore.carriedrouting.client;

import dev.resivore.carriedrouting.RoutingLock;
import dev.resivore.carriedrouting.RoutingService;
import dev.resivore.carriedrouting.ToggleLockPayload;
import dev.resivore.slotreservations.api.client.ShulkerPanelHeaderDecorations;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.Optional;

/** Direct CSR API linkage lives only in this class, loaded after Fabric confirms CSR is present. */
public final class CsrHeaderBadgeIntegration {
    static final int LOCKED_WIDTH = 7, LOCKED_HEIGHT = 9;
    static final int UNLOCKED_WIDTH = 10, UNLOCKED_HEIGHT = 9;
    static final Identifier LOCKED_SPRITE = Identifier.fromNamespaceAndPath(
            "carried_container_auto_routing", "textures/gui/lock_locked.png");
    static final Identifier UNLOCKED_SPRITE = Identifier.fromNamespaceAndPath(
            "carried_container_auto_routing", "textures/gui/lock_unlocked.png");

    private CsrHeaderBadgeIntegration() {}

    public static void register() {
        ShulkerPanelHeaderDecorations.register(new ShulkerPanelHeaderDecorations.Provider() {
            @Override public Optional<ShulkerPanelHeaderDecorations.Decoration> decoration(ItemStack stack) {
                return CsrHeaderBadgeIntegration.decoration(stack);
            }

            @Override public Optional<ShulkerPanelHeaderDecorations.Interaction> interaction(ItemStack stack) {
                if (decoration(stack).isEmpty()) return Optional.empty();
                return Optional.of(new ShulkerPanelHeaderDecorations.Interaction(context -> {
                    if (!ClientPlayNetworking.canSend(ToggleLockPayload.TYPE)) return false;
                    ClientPlayNetworking.send(new ToggleLockPayload(context.menuId(), context.menuSlot(), 0));
                    return true;
                }));
            }
        });
    }

    static Optional<ShulkerPanelHeaderDecorations.Decoration> decoration(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof ShulkerBoxBlock)
                || !RoutingService.isSupported(stack)) {
            return Optional.empty();
        }
        boolean locked = RoutingLock.isLocked(stack);
        int width = locked ? LOCKED_WIDTH : UNLOCKED_WIDTH;
        int height = locked ? LOCKED_HEIGHT : UNLOCKED_HEIGHT;
        Identifier sprite = locked ? LOCKED_SPRITE : UNLOCKED_SPRITE;
        return Optional.of(new ShulkerPanelHeaderDecorations.Decoration(width, height,
                (graphics, x, y) -> graphics.blit(RenderPipelines.GUI_TEXTURED, sprite, x, y + 1,
                        0, 0, width, height, width, height)));
    }
}
