package dev.resivore.blockfamilies.mixin;

import dev.resivore.blockfamilies.cnm.runtime.AuditedDoorBreakContext;
import dev.resivore.blockfamilies.cnm.runtime.AuditedShapeRuntime;
import dev.tazer.clutternomore.common.CHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * CNM normally replaces every shape-segment result with drops from the
 * canonical parent's default (lower) state. During one audited alternate-door
 * player break, suppress those physical-segment evaluations; the surrounding
 * player-break context emits the exact source design once after success. For
 * every other destruction path, retain the provider result so the provider's
 * lower/base state remains the sole drop-owning segment.
 */
@Mixin(value = CHooks.class, remap = false)
abstract class CnmAuditedDoorLootMixin {
    @Inject(method = "getDrops", at = @At("HEAD"), cancellable = true, require = 1)
    private static void interchangeableBlockFamilies$preserveNativeDoorDrops(
            List<ItemStack> providerDrops,
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity breaker,
            ItemInstance tool,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        Item sourceItem = state.getBlock().asItem();
        if (AuditedDoorBreakContext.suppressesSegmentDrop(sourceItem)) {
            cir.setReturnValue(List.of());
        } else if (AuditedShapeRuntime.isAuditedAlternateDoor(sourceItem)) {
            cir.setReturnValue(providerDrops);
        }
    }
}
