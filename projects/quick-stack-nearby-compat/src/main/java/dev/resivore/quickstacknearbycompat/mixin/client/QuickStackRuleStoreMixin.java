package dev.resivore.quickstacknearbycompat.mixin.client;

import dev.resivore.quickstacknearbycompat.core.PlayerStorageSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tempeststudios.quickstacknearby.QuickStackRequestPayload;
import tempeststudios.quickstacknearby.QuickStackRuleStore;

import java.util.List;

@Mixin(value = QuickStackRuleStore.class, remap = false)
public abstract class QuickStackRuleStoreMixin {
    @Inject(
            method = "payloadRules()Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void quickStackNearbyCompat$boundRulesToLiveStorage(
            CallbackInfoReturnable<List<QuickStackRequestPayload.SlotRule>> callback
    ) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            callback.setReturnValue(List.of());
            return;
        }
        callback.setReturnValue(PlayerStorageSlots.filterRules(
                callback.getReturnValue(),
                QuickStackRequestPayload.SlotRule::slotIndex,
                PlayerStorageSlots.liveWindow(player.getInventory())
        ));
    }
}
