package dev.resivore.villagerpoireachability.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Keeps the real job-site POI as AcquirePoi's target, but lets a villager prove access through an
 * adjacent standing cell when that target is exactly one block above that cell.  This is deliberately
 * limited to the job-site candidate list and never changes global Path.canReach semantics.
 */
@Mixin(AcquirePoi.class)
abstract class AcquirePoiMixin {
    @Inject(method = "findPathToPois", at = @At("RETURN"), cancellable = true, require = 1)
    private static void villagerPoiReachability$allowRaisedJobSiteInteraction(
            Mob mob,
            Set<Pair<Holder<PoiType>, BlockPos>> candidates,
            CallbackInfoReturnable<Path> callback
    ) {
        Path vanillaPath = callback.getReturnValue();
        if (!(mob instanceof Villager) || (vanillaPath != null && vanillaPath.canReach())) return;

        for (Pair<Holder<PoiType>, BlockPos> candidate : candidates) {
            if (!candidate.getFirst().is(PoiTypeTags.ACQUIRABLE_JOB_SITE)) continue;
            BlockPos poi = candidate.getSecond();
            if (!mob.level().getBlockState(poi.below()).isSolidRender()) continue;

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos standing = poi.below().relative(direction);
                if (!hasClearInteractionLine(mob, standing, poi)) continue;

                // Range zero requires the navigation result to end at this exact standing cell.
                Path interactionPath = mob.getNavigation().createPath(standing, 0);
                if (interactionPath == null || !interactionPath.canReach()
                        || !interactionPath.getTarget().equals(standing)) continue;

                callback.setReturnValue(pathWithRealPoiTarget(interactionPath, poi));
                return;
            }
        }
    }

    private static boolean hasClearInteractionLine(Mob mob, BlockPos standing, BlockPos poi) {
        Vec3 eye = new Vec3(standing.getX() + 0.5, standing.getY() + mob.getEyeHeight(), standing.getZ() + 0.5);
        BlockHitResult hit = mob.level().clip(new ClipContext(eye, Vec3.atCenterOf(poi),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(poi);
    }

    private static Path pathWithRealPoiTarget(Path interactionPath, BlockPos poi) {
        List<Node> nodes = new ArrayList<>(interactionPath.getNodeCount());
        for (int index = 0; index < interactionPath.getNodeCount(); index++) nodes.add(interactionPath.getNode(index));
        return new Path(nodes, poi, true);
    }
}
