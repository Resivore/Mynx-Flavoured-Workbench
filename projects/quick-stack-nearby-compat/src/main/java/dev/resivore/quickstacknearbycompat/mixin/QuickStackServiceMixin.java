package dev.resivore.quickstacknearbycompat.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.resivore.quickstacknearbycompat.core.CsrQuickStackIntegration;
import dev.resivore.quickstacknearbycompat.core.CarriedContainerSources;
import dev.resivore.quickstacknearbycompat.core.PlayerStorageSlots;
import dev.resivore.quickstacknearbycompat.core.PopulatedShulkerOuterProtection;
import dev.resivore.quickstacknearbycompat.core.QsnDestinationExclusions;
import dev.resivore.quickstacknearbycompat.core.ReservationOnlyOuterCarriers;
import dev.resivore.quickstacknearbycompat.core.ShapeMapTargetAffinity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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
        // Snapshot populated shulkers before native QSN discovers loose source affinity.  Pass
        // this overlay through the whole native action so an emptied carrier cannot become a
        // loose source until the next button press; CarriedContainerSources keeps sourceRules.
        QuickStackMoveEngine.SourceRules looseRules = PopulatedShulkerOuterProtection
                .snapshotLooseRules(inventory, sourceRules);
        QuickStackMoveEngine.SourceRules effectiveRules = new QuickStackMoveEngine.SourceRules(
                PlayerStorageSlots.filterRuleMap(looseRules.slotRules(), window));
        return ReservationOnlyOuterCarriers.scoped(inventory, window, sourceRules, () ->
                CarriedContainerSources.scoped(player, sourceRules, () ->
                        CsrQuickStackIntegration.withDiscoverySource(
                                inventory,
                                window.firstInclusive(),
                                window.endExclusive(),
                                effectiveRules,
                                () -> dev.resivore.quickstacknearbycompat.core.NestedShulkerDiscovery.scoped(
                                        () -> original.call(player, looseRules))
                        )));
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
            Operation<Set<QuickStackMoveEngine.StackKey>> original,
            net.minecraft.server.level.ServerLevel level, ServerPlayer player,
            net.minecraft.core.BlockPos origin, net.minecraft.core.BlockPos position,
            @com.llamalad7.mixinextras.sugar.Local(ordinal = 0) List<net.minecraft.core.BlockPos> positions) {
        return dev.resivore.quickstacknearbycompat.core.NestedShulkerDiscovery.discover(target,
                CsrQuickStackIntegration.augmentDiscoveredAcceptedTypes(target, original.call(target)), level, player, positions);
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
        List<QuickStackMoveEngine.Target> filtered = dev.resivore.quickstacknearbycompat.core.NestedShulkerDiscovery.expand(
                QsnDestinationExclusions.filterVanillaShelves(targets));
        if (filtered != targets) {
            callback.setReturnValue(filtered);
        }
    }

    @WrapOperation(
            method = "quickStack(Lnet/minecraft/server/level/ServerPlayer;Ltempeststudios/quickstacknearby/QuickStackMoveEngine$SourceRules;)Ltempeststudios/quickstacknearby/QuickStackMoveEngine$Result;",
            at = @At(
                    value = "INVOKE",
                    target = "Ltempeststudios/quickstacknearby/QuickStackMoveEngine;moveMatchingItems(Lnet/minecraft/world/Container;IILjava/util/List;Ltempeststudios/quickstacknearby/QuickStackMoveEngine$SourceRules;)Ltempeststudios/quickstacknearby/QuickStackMoveEngine$Result;",
                    remap = false
            ),
            require = 1,
            remap = false
    )
    private static QuickStackMoveEngine.Result quickStackNearbyCompat$useLiveStorageBoundary(
            Container source, int firstSourceSlot, int exclusiveLastSourceSlot,
            List<QuickStackMoveEngine.Target> targets, QuickStackMoveEngine.SourceRules sourceRules,
            Operation<QuickStackMoveEngine.Result> original) {
        if (!(source instanceof Inventory inventory)) {
            return original.call(source, firstSourceSlot, exclusiveLastSourceSlot, targets, sourceRules);
        }
        PlayerStorageSlots.Window window = PlayerStorageSlots.liveWindow(inventory);
        QuickStackMoveEngine.SourceRules effectiveRules = sourceRules == null
                ? QuickStackMoveEngine.SourceRules.EMPTY
                : new QuickStackMoveEngine.SourceRules(
                        PlayerStorageSlots.filterRuleMap(sourceRules.slotRules(), window));
        List<QuickStackMoveEngine.Target> augmented = ShapeMapTargetAffinity.augmentTargets(
                source, firstSourceSlot, window.endExclusive(), targets, effectiveRules);
        // C18 owns only this pre-loose, CSR-matching return phase. The original loose overlay
        // remains in force below, and returned carriers are explicitly retired from C16 drain.
        QuickStackMoveEngine.Result homes = ReservationOnlyOuterCarriers.returnMatchingHomes(augmented);
        QuickStackMoveEngine.Result loose = original.call(
                source, firstSourceSlot, window.endExclusive(), augmented, effectiveRules);
        QuickStackMoveEngine.Result carried = CarriedContainerSources.drain(augmented);
        int inheritedTargetTouches = loose.targetContainersTouched() + carried.targetContainersTouched();
        return new QuickStackMoveEngine.Result(
                homes.itemsMoved() + loose.itemsMoved() + carried.itemsMoved(),
                homes.sourceStacksTouched() + loose.sourceStacksTouched() + carried.sourceStacksTouched(),
                homes.itemsMoved() > 0
                        ? ReservationOnlyOuterCarriers.targetContainersTouchedAfterHomeReturn(
                                homes.targetContainersTouched() + inheritedTargetTouches)
                        : inheritedTargetTouches);
    }
}
