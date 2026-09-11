package dev.resivore.carriedrouting.mixin;

import dev.resivore.carriedrouting.RoutingContext;
import dev.resivore.carriedrouting.RoutedPickupSoundPayload;
import dev.resivore.carriedrouting.RoutingService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
    @Unique private boolean carriedRouting$routedToCarriedContainer;

    @Inject(
            method = "playerTouch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getInventory()Lnet/minecraft/world/entity/player/Inventory;",
                    shift = At.Shift.BEFORE
            )
    )
    private void carriedRouting$routeBeforeInventoryAdd(Player player, CallbackInfo ci) {
        ItemStack incoming = ((ItemEntity) (Object) this).getItem();
        carriedRouting$routedToCarriedContainer = RoutingService.routeIncomingStackWithResult(
                player, incoming, RoutingContext.WORLD_PICKUP, -1, false
        ).routedToCarriedContainer();
    }

    @Inject(
            method = "playerTouch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void carriedRouting$markRoutedPickupSound(Player player, CallbackInfo ci) {
        if (carriedRouting$routedToCarriedContainer && player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new RoutedPickupSoundPayload(((ItemEntity) (Object) this).getId()));
        }
    }
}
