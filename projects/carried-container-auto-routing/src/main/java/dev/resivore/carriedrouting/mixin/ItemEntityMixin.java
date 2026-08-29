package dev.resivore.carriedrouting.mixin;

import dev.resivore.carriedrouting.RoutingContext;
import dev.resivore.carriedrouting.RoutingService;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
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
        RoutingService.routeIncomingStack(player, incoming, RoutingContext.WORLD_PICKUP);
    }
}
