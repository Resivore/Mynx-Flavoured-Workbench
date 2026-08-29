package dev.resivore.blockfamilies.mixin;

import dev.resivore.blockfamilies.cnm.runtime.AuditedDoorBreakContext;
import dev.resivore.blockfamilies.cnm.runtime.AuditedShapeRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Owns exactly one item drop for one successful alternate-door player break. */
@Mixin(ServerPlayerGameMode.class)
abstract class AuditedDoorPlayerBreakMixin {
    @Shadow
    private ServerLevel level;

    @Shadow
    @Final
    private ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), require = 1)
    private void interchangeableBlockFamilies$beginAuditedDoorBreak(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        AuditedDoorBreakContext.clear();
        Item sourceItem = level.getBlockState(pos).getBlock().asItem();
        if (!player.preventsBlockDrops()
                && AuditedShapeRuntime.isAuditedAlternateDoor(sourceItem)) {
            AuditedDoorBreakContext.begin(level, pos, sourceItem);
        }
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"), require = 1)
    private void interchangeableBlockFamilies$finishAuditedDoorBreak(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        AuditedDoorBreakContext.finish(cir.getReturnValueZ());
    }
}
