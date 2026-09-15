package dev.resivore.villagerwork.mixin;

import dev.resivore.villagerwork.LivestockGateBlocker;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {
    /** Make only VWR-owned open gates classify exactly like vanilla closed gates for livestock. */
    @Inject(
            method = "getPathType(Lnet/minecraft/world/level/pathfinder/PathfindingContext;III)Lnet/minecraft/world/level/pathfinder/PathType;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void villagerWork$blockOwnedGate(PathfindingContext context, int x, int y, int z,
                                              CallbackInfoReturnable<PathType> callback) {
        if (LivestockGateBlocker.blocks(this.mob, context, x, y, z))
            callback.setReturnValue(PathType.FENCE);
    }
}
