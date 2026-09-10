package dev.resivore.carriedrouting.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.resivore.carriedrouting.RoutingLock;
import dev.resivore.carriedrouting.RoutingService;
import dev.resivore.carriedrouting.ToggleLockPayload;
import dev.resivore.carriedrouting.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

public final class CarriedContainerAutoRoutingClient implements ClientModInitializer {
    private static KeyMapping toggle;
    @Override public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("carried_container_auto_routing", "routing"));
        toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.carried_container_auto_routing.toggle_lock", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> { while (toggle.consumeClick()) sendTarget(client); });
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (RoutingService.isSupported(stack)) lines.add(Component.translatable(RoutingLock.isLocked(stack)
                    ? "text.carried_container_auto_routing.locked" : "text.carried_container_auto_routing.unlocked").withStyle(ChatFormatting.DARK_GRAY));
        });
        registerCsrHeaderBadgeWhenAvailable();
    }

    private static void registerCsrHeaderBadgeWhenAvailable() {
        if (!FabricLoader.getInstance().isModLoaded("container_slot_reservations")) return;
        try {
            // Keep every direct CSR API reference inside the isolated bridge class.
            Class.forName("dev.resivore.carriedrouting.client.CsrHeaderBadgeIntegration")
                    .getMethod("register").invoke(null);
        } catch (ReflectiveOperationException | LinkageError error) {
            LoggerFactory.getLogger("carried_container_auto_routing").warn(
                    "CSR header badge is unavailable; CCAR continues without it", error);
        }
    }
    public static boolean handleContainerKey(Minecraft client, KeyEvent event) {
        return toggle != null
                && client.gui.screen() instanceof AbstractContainerScreen<?>
                && toggle.matches(event)
                && sendTarget(client);
    }

    private static boolean sendTarget(Minecraft client) {
        if (client.player == null || !ClientPlayNetworking.canSend(ToggleLockPayload.TYPE)) return false;
        if (client.gui.screen() instanceof AbstractContainerScreen<?> screen) {
            Slot slot = ((AbstractContainerScreenAccessor) screen).carriedRouting$getHoveredSlot();
            int menuIndex = slot == null ? -1 : screen.getMenu().slots.indexOf(slot);
            if (slot != null
                    && slot.container == client.player.getInventory()
                    && menuIndex >= 0
                    && RoutingService.isSupported(slot.getItem())) {
                ClientPlayNetworking.send(new ToggleLockPayload(screen.getMenu().containerId, menuIndex, 0));
                return true;
            }
        }
        ItemStack main = client.player.getMainHandItem();
        ClientPlayNetworking.send(new ToggleLockPayload(-1, -1, RoutingService.isSupported(main) ? 1 : 2));
        return true;
    }
}
