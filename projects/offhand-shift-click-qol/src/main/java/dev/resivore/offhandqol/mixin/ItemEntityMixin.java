package dev.resivore.offhandqol.mixin;

import dev.resivore.offhandqol.OffhandRoutingService;
import dev.resivore.offhandqol.OffhandPickupAudioDecision;
import dev.resivore.offhandqol.OffhandPickupSoundFallbackPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
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
    @Unique private int offhandQol$customItemsMoved;
    @Unique private boolean offhandQol$vanillaTakeReached;
    @Unique private boolean offhandQol$carriedRoutingPresent;
    @Unique private double offhandQol$pickupX;
    @Unique private double offhandQol$pickupY;
    @Unique private double offhandQol$pickupZ;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void offhandQol$resetPickupAudioState(Player player, CallbackInfo ci) {
        offhandQol$customItemsMoved = 0;
        offhandQol$vanillaTakeReached = false;
        offhandQol$carriedRoutingPresent = FabricLoader.getInstance().isModLoaded("carried_container_auto_routing");
        offhandQol$pickupX = 0.0D;
        offhandQol$pickupY = 0.0D;
        offhandQol$pickupZ = 0.0D;
    }

    @Inject(
            method = "playerTouch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getInventory()Lnet/minecraft/world/entity/player/Inventory;",
                    shift = At.Shift.BEFORE
            )
    )
    private void offhandQol$routeBeforeInventoryAdd(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()
                || offhandQol$carriedRoutingPresent) return;
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        ItemStack incoming = itemEntity.getItem();
        int beforeRouting = incoming.getCount();
        OffhandRoutingService.routeIncoming(player, incoming, -1, false);
        offhandQol$customItemsMoved = beforeRouting - incoming.getCount();
        offhandQol$pickupX = itemEntity.getX();
        offhandQol$pickupY = itemEntity.getY();
        offhandQol$pickupZ = itemEntity.getZ();
    }

    @Inject(
            method = "playerTouch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void offhandQol$markVanillaTakeFeedback(Player player, CallbackInfo ci) {
        offhandQol$vanillaTakeReached = true;
    }

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void offhandQol$sendFallbackPickupSound(Player player, CallbackInfo ci) {
        if (!OffhandPickupAudioDecision.shouldSendFallback(
                offhandQol$customItemsMoved,
                offhandQol$vanillaTakeReached,
                offhandQol$carriedRoutingPresent
        ) || !(player instanceof ServerPlayer serverPlayer) || serverPlayer.connection == null) return;
        ServerPlayNetworking.send(serverPlayer, new OffhandPickupSoundFallbackPayload(
                offhandQol$pickupX,
                offhandQol$pickupY,
                offhandQol$pickupZ
        ));
    }
}
