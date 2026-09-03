package dev.resivore.quickstacknearbycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.resivore.quickstacknearbycompat.core.CsrQuickStackIntegration;
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
import java.util.Set;

@Mixin(value = QuickStackService.class, remap = false)
public abstract class QuickStackServiceMixin {
    @WrapMethod(
            method = "quickStack(Lnet/minecraft/server/level/ServerPlayer;Ltempeststudios/quickstacknearby/QuickStackMoveEngine$SourceRules;)Ltempeststudios/quickstacknearby/QuickStackMoveEngine$Result;"
    )
    private static QuickStackMoveEngine.Result quickStackNearbyCompat$scopeReservationDiscovery(
            ServerPlayer player,
            QuickStackMoveEngine.SourceRules sourceRules,
            Operation<QuickStackMoveEngine.Result> original) {
        Inventory inventory = player.getInventory();
        PlayerStorageSlots.Window window = PlayerStorageSlots.liveWindow(inventory);
        QuickStackMoveEngine.SourceRules effectiveRules = sourceRules == null
                ? QuickStackMoveEngine.SourceRules.EMPTY
                : new QuickStackMoveEngine.SourceRules(
                        PlayerStorageSlots.filterRuleMap(sourceRules.slotRules(), window)
                );
        return CsrQuickStackIntegration.withDiscoverySource(
                inventory,
                window.firstInclusive(),
                window.endExclusive(),
                effectiveRules,
                () -> original.call(player, sourceRules)
        );
    }

    @WrapOperation(
            method = "scanContainer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ltempeststudios/quickstacknearby/QuickStackService$ScannedContainer;",
            at = @At(
                    value = "INVOKE",
                    target = "Ltempeststudios/quickstacknearby/QuickStackMoveEngine;acceptedTypes(Lnet/minecraft/world/Container;)Ljava/util/Set;",
                    remap = false
            ),
            require = 1,
            remap = false
    )
    private static Set<QuickStackMoveEngine.StackKey> quickStackNearbyCompat$includeReservationAffinity(
            Container target,
            Operation<Set<QuickStackMoveEngine.StackKey>> original) {
        return CsrQuickStackIntegration.augmentDiscoveredAcceptedTypes(
                target,
                original.call(target)
        );
    }

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
