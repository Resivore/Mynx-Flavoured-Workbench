package dev.resivore.quickstacknearbycompat.mixin;

import dev.resivore.quickstacknearbycompat.core.PlayerStorageSlots;
import dev.resivore.quickstacknearbycompat.core.QsnDestinationExclusions;
import dev.resivore.quickstacknearbycompat.core.ShapeMapTargetAffinity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;
import tempeststudios.quickstacknearby.QuickStackService;

import java.util.List;

@Mixin(value = QuickStackService.class, remap = false)
public abstract class QuickStackServiceMixin {
    @Inject(
            method = "nearbyTargets(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void quickStackNearbyCompat$excludeVanillaShelves(
            ServerPlayer player,
            CallbackInfoReturnable<List<QuickStackMoveEngine.Target>> callback) {
        List<QuickStackMoveEngine.Target> targets = callback.getReturnValue();
        List<QuickStackMoveEngine.Target> filtered = QsnDestinationExclusions.filterVanillaShelves(targets);
        if (filtered != targets) {
            callback.setReturnValue(filtered);
        }
    }

    @ModifyArgs(
            method = "quickStack(Lnet/minecraft/server/level/ServerPlayer;Ltempeststudios/quickstacknearby/QuickStackMoveEngine$SourceRules;)Ltempeststudios/quickstacknearby/QuickStackMoveEngine$Result;",
            at = @At(
                    value = "INVOKE",
                    target = "Ltempeststudios/quickstacknearby/QuickStackMoveEngine;moveMatchingItems(Lnet/minecraft/world/Container;IILjava/util/List;Ltempeststudios/quickstacknearby/QuickStackMoveEngine$SourceRules;)Ltempeststudios/quickstacknearby/QuickStackMoveEngine$Result;",
                    remap = false
            ),
            require = 1,
            remap = false
    )
    private static void quickStackNearbyCompat$useLiveStorageBoundary(Args args) {
        Container source = args.get(0);
        if (!(source instanceof Inventory inventory)) {
            return;
        }

        PlayerStorageSlots.Window window = PlayerStorageSlots.liveWindow(inventory);
        args.set(2, window.endExclusive());

        QuickStackMoveEngine.SourceRules sourceRules = args.get(4);
        if (sourceRules != null) {
            args.set(4, new QuickStackMoveEngine.SourceRules(
                    PlayerStorageSlots.filterRuleMap(sourceRules.slotRules(), window)
            ));
        }

        int firstSourceSlot = args.get(1);
        int exclusiveLastSourceSlot = args.get(2);
        List<QuickStackMoveEngine.Target> targets = args.get(3);
        args.set(3, ShapeMapTargetAffinity.augmentTargets(
                source,
                firstSourceSlot,
                exclusiveLastSourceSlot,
                targets,
                args.get(4)
        ));
    }
}
