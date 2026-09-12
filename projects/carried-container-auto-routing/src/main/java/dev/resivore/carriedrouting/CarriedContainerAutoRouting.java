package dev.resivore.carriedrouting;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class CarriedContainerAutoRouting implements ModInitializer {
    public static final String MOD_ID = "carried_container_auto_routing";
    @Override public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(ToggleLockPayload.TYPE, ToggleLockPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RoutedPickupSoundPayload.TYPE, RoutedPickupSoundPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                RoutedPickupSoundFallbackPayload.TYPE,
                RoutedPickupSoundFallbackPayload.CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(ToggleLockPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            ItemStack target = ItemStack.EMPTY;
            AbstractContainerMenu menu = context.player().containerMenu;
            if (payload.hand() == 0) target = resolveActiveMenuSlot(context.player(), payload.menuId(),
                    payload.slotIndex()).map(Slot::getItem).orElse(ItemStack.EMPTY);
            else if (payload.hand() == 1) target = context.player().getItemInHand(InteractionHand.MAIN_HAND);
            else if (payload.hand() == 2) target = context.player().getItemInHand(InteractionHand.OFF_HAND);
            if (!RoutingService.isSupported(target)) return;
            boolean locked = !RoutingLock.isLocked(target);
            RoutingLock.setLocked(target, locked);
            menu.broadcastChanges();
            context.player().sendOverlayMessage(Component.translatable(locked
                    ? "text.carried_container_auto_routing.locked" : "text.carried_container_auto_routing.unlocked"));
        }));
    }

    /** Exact live menu-slot validation shared by the packet receiver and focused server tests. */
    static Optional<Slot> resolveActiveMenuSlot(net.minecraft.server.level.ServerPlayer player,
                                                int menuId, int slotIndex) {
        if (player == null || !player.isAlive() || player.isSpectator()) return Optional.empty();
        AbstractContainerMenu menu = player.containerMenu;
        if (menuId != menu.containerId || !menu.stillValid(player)
                || slotIndex < 0 || slotIndex >= menu.slots.size()) return Optional.empty();
        Slot targetSlot = menu.slots.get(slotIndex);
        if (!targetSlot.isActive() || targetSlot.isFake() || !targetSlot.container.stillValid(player)
                || !targetSlot.mayPickup(player) || !targetSlot.allowModification(player)) return Optional.empty();
        return Optional.of(targetSlot);
    }
}
