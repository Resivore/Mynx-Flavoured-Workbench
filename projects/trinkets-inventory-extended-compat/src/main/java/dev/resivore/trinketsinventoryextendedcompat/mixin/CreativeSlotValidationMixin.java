package dev.resivore.trinketsinventoryextendedcompat.mixin;

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the dynamic Trinkets creative-slot upper bound exclusive.
 *
 * <p>Vanilla accepts menu slot IDs 1 through 45 for a 46-slot menu. Trinkets
 * replaces 45 with {@code inventoryMenu.slots.size()}, which admits the first
 * nonexistent index. Inventory Extended's compatibility build removes its
 * competing fixed 72 modifier. This guard preserves negative creative-drop
 * packets and rejects every nonnegative index outside the actual menu.</p>
 */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class CreativeSlotValidationMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handleSetCreativeModeSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private void trinketsInventoryExtendedCompat$rejectOutOfRangeSlot(
            ServerboundSetCreativeModeSlotPacket packet,
            CallbackInfo ci) {
        int slot = packet.slotNum();
        if (slot >= this.player.inventoryMenu.slots.size()) {
            ci.cancel();
        }
    }
}
