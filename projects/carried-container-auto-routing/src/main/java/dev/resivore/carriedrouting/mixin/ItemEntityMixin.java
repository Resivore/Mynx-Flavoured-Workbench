package dev.resivore.carriedrouting.mixin;

import dev.resivore.carriedrouting.RoutingContext;
import dev.resivore.carriedrouting.RoutedPickupAudioDecision;
import dev.resivore.carriedrouting.RoutedPickupSoundFallbackPayload;
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
    @Unique private int carriedRouting$customItemsMoved;
    @Unique private boolean carriedRouting$routedToCarriedContainer;
    @Unique private boolean carriedRouting$vanillaTakeReached;
    @Unique private double carriedRouting$pickupX;
    @Unique private double carriedRouting$pickupY;
    @Unique private double carriedRouting$pickupZ;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void carriedRouting$resetPickupAudioState(Player player, CallbackInfo ci) {
        carriedRouting$customItemsMoved = 0;
        carriedRouting$routedToCarriedContainer = false;
        carriedRouting$vanillaTakeReached = false;
        carriedRouting$pickupX = 0.0D;
        carriedRouting$pickupY = 0.0D;
        carriedRouting$pickupZ = 0.0D;
    }

    @Inject(
            method = "playerTouch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getInventory()Lnet/minecraft/world/entity/player/Inventory;",
                    shift = At.Shift.BEFORE
            )
    )
    private void carriedRouting$routeBeforeInventoryAdd(Player player, CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        ItemStack incoming = itemEntity.getItem();
        int beforeRouting = incoming.getCount();
        RoutingService.RoutingResult result = RoutingService.routeIncomingStackWithResult(
                player, incoming, RoutingContext.WORLD_PICKUP, -1, false
        );
        carriedRouting$customItemsMoved = beforeRouting - incoming.getCount();
        carriedRouting$routedToCarriedContainer = result.routedToCarriedContainer();
        carriedRouting$pickupX = itemEntity.getX();
        carriedRouting$pickupY = itemEntity.getY();
        carriedRouting$pickupZ = itemEntity.getZ();
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
        carriedRouting$vanillaTakeReached = true;
        if (carriedRouting$routedToCarriedContainer
                && player instanceof ServerPlayer serverPlayer
                && serverPlayer.connection != null) {
            ServerPlayNetworking.send(serverPlayer, new RoutedPickupSoundPayload(((ItemEntity) (Object) this).getId()));
        }
    }

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void carriedRouting$sendFallbackPickupSound(Player player, CallbackInfo ci) {
        RoutedPickupAudioDecision.FallbackCue cue = RoutedPickupAudioDecision.fallbackCue(
                carriedRouting$customItemsMoved,
                carriedRouting$routedToCarriedContainer,
                carriedRouting$vanillaTakeReached
        );
        if (cue != RoutedPickupAudioDecision.FallbackCue.NONE
                && player instanceof ServerPlayer serverPlayer
                && serverPlayer.connection != null) {
            ServerPlayNetworking.send(serverPlayer, new RoutedPickupSoundFallbackPayload(
                    carriedRouting$pickupX,
                    carriedRouting$pickupY,
                    carriedRouting$pickupZ,
                    cue == RoutedPickupAudioDecision.FallbackCue.LOWER_PITCH
            ));
        }
    }
}
