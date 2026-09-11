package dev.resivore.carriedrouting;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class CarriedContainerAutoRouting implements ModInitializer {
    public static final String MOD_ID = "carried_container_auto_routing";
    @Override public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(ToggleLockPayload.TYPE, ToggleLockPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RoutedPickupSoundPayload.TYPE, RoutedPickupSoundPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToggleLockPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            ItemStack target = ItemStack.EMPTY;
            AbstractContainerMenu menu = context.player().containerMenu;
            if (payload.hand() == 0 && payload.menuId() == menu.containerId && payload.slotIndex() >= 0 && payload.slotIndex() < menu.slots.size()) {
                var targetSlot = menu.slots.get(payload.slotIndex());
                if (targetSlot.container == context.player().getInventory()) target = targetSlot.getItem();
            } else if (payload.hand() == 1) target = context.player().getItemInHand(InteractionHand.MAIN_HAND);
            else if (payload.hand() == 2) target = context.player().getItemInHand(InteractionHand.OFF_HAND);
            if (!RoutingService.isSupported(target)) return;
            boolean locked = !RoutingLock.isLocked(target);
            RoutingLock.setLocked(target, locked);
            menu.broadcastChanges();
            context.player().sendOverlayMessage(Component.translatable(locked
                    ? "text.carried_container_auto_routing.locked" : "text.carried_container_auto_routing.unlocked"));
        }));
    }
}
