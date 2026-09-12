package dev.resivore.carriedrouting.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.resivore.carriedrouting.PickBlockRouting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;

    /** Vanilla's exact ordinary-inventory lookup always runs before CCAR's fallback. */
    @WrapOperation(
            method = "tryPickItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I"
            )
    )
    private int carriedRouting$pickFromUnlockedShulkerAfterVanillaMiss(
            Inventory inventory,
            ItemStack requested,
            Operation<Integer> original
    ) {
        int vanillaSlot = original.call(inventory, requested);
        if (vanillaSlot == Inventory.NOT_FOUND_INDEX && !player.hasInfiniteMaterials()) {
            PickBlockRouting.tryPickFromShulkers(player, requested);
        }
        return vanillaSlot;
    }
}
