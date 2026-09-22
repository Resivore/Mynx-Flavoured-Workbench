package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Exact physical-arrival rule shared by Fisherman and Shepherd deposits. */
public final class BarrelInteractionRules {
    public static final double MAX_BOUNDARY_GAP = 0.5D;
    public static final double MAX_BOUNDARY_GAP_SQUARED =
            MAX_BOUNDARY_GAP * MAX_BOUNDARY_GAP;
    public static final double MIN_HORIZONTAL_FACING_DOT = 0.8D;

    private BarrelInteractionRules() {
    }

    public static boolean withinReach(AABB villagerBody, BlockPos barrel) {
        return withinReach(villagerBody, new AABB(barrel));
    }

    public static boolean withinReach(AABB villagerBody, AABB barrelBody) {
        return villagerBody.distanceToSqr(barrelBody) <= MAX_BOUNDARY_GAP_SQUARED;
    }

    /** Predicts the same body placement used by the collision-clear standing-position check. */
    public static AABB bodyAtFeet(AABB currentBody, Vec3 currentPosition, BlockPos feet) {
        return currentBody.move(feet.getX() + 0.5D - currentPosition.x,
                feet.getY() - currentPosition.y, feet.getZ() + 0.5D - currentPosition.z);
    }

    /** Uses the visible horizontal look direction; pitch cannot hide a backwards-facing deposit. */
    public static boolean facing(Vec3 lookDirection, Vec3 eyePosition, BlockPos barrel) {
        Vec3 toBarrel = Vec3.atCenterOf(barrel).subtract(eyePosition);
        double lookLength = Math.hypot(lookDirection.x, lookDirection.z);
        double targetLength = Math.hypot(toBarrel.x, toBarrel.z);
        if (lookLength < 1.0E-6D || targetLength < 1.0E-6D) return false;
        double dot = (lookDirection.x * toBarrel.x + lookDirection.z * toBarrel.z)
                / (lookLength * targetLength);
        return dot >= MIN_HORIZONTAL_FACING_DOT;
    }
}
