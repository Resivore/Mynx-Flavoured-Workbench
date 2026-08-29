package dev.resivore.radialslotcycler;

import dev.resivore.radialslotcycler.core.ExactPairwiseSwap;
import dev.resivore.radialslotcycler.core.SwapRequestValidator;
import dev.resivore.radialslotcycler.network.SwapSlotPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RadialSlotCycler implements ModInitializer {
    public static final String MOD_ID = "radial_slot_cycler";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(SwapSlotPayload.TYPE, SwapSlotPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SwapSlotPayload.TYPE, (payload, context) ->
                context.server().execute(() -> applySwap(context.player(), payload)));
    }

    private static void applySwap(ServerPlayer player, SwapSlotPayload payload) {
        if (!player.isAlive()) {
            return;
        }
        Inventory inventory = player.getInventory();
        int liveOrdinarySize = inventory.getNonEquipmentItems().size();
        SwapRequestValidator.Result result = SwapRequestValidator.validate(
                payload.selectedHotbarSlot(),
                payload.storageSlot(),
                payload.ordinarySize(),
                inventory.getSelectedSlot(),
                liveOrdinarySize,
                player.containerMenu == player.inventoryMenu,
                player.isSpectator());
        if (result != SwapRequestValidator.Result.ACCEPTED) {
            return;
        }

        ExactPairwiseSwap.exchange(
                inventory.getNonEquipmentItems(),
                payload.selectedHotbarSlot(),
                payload.storageSlot());
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
    }
}
