package dev.resivore.offhandqol.mixin;

import dev.resivore.offhandqol.OffhandRoutingService;
import net.fabricmc.loader.api.FabricLoader;
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
    private void offhandQol$routeBeforeInventoryAdd(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()
                || FabricLoader.getInstance().isModLoaded("carried_container_auto_routing")) return;
        ItemStack incoming = ((ItemEntity) (Object) this).getItem();
        OffhandRoutingService.routeIncoming(player, incoming, -1, false);
    }
}
