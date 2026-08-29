package dev.resivore.quickstacknearbycompat.mixin.client;

import dev.resivore.quickstacknearbycompat.core.PlayerStorageSlots;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import tempeststudios.quickstacknearby.QuickStackRulesScreen;

@Mixin(value = QuickStackRulesScreen.class, remap = false)
public abstract class QuickStackRulesScreenMixin {
    @Shadow(remap = false)
    @Final
    private Player player;

    @ModifyConstant(
            method = "isTargetSlot(I)Z",
            constant = @Constant(intValue = 36),
            require = 1,
            expect = 1,
            remap = false
    )
    private int quickStackNearbyCompat$useLiveRuleBoundary(int originalExclusiveEnd) {
        return PlayerStorageSlots.liveWindow(this.player.getInventory()).endExclusive();
    }

    @ModifyConstant(
            method = "computeLayout()V",
            constant = @Constant(intValue = 3),
            require = 1,
            expect = 1,
            remap = false
    )
    private int quickStackNearbyCompat$useLiveStorageRows(int originalRows) {
        return PlayerStorageSlots.liveWindow(this.player.getInventory()).rowCount();
    }
}
