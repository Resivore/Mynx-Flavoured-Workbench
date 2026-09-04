package com.yungnickyoung.minecraft.ribbits.chute;

import net.minecraft.world.phys.Vec3;

/** The complete Chute movement effect: a narrow terminal-descent cap and nothing else. */
public final class ChutePhysics {
    public static final double TERMINAL_DESCENT = -0.10D;

    private ChutePhysics() {
    }

    public static double capVerticalVelocity(double velocityY) {
        return velocityY < TERMINAL_DESCENT ? TERMINAL_DESCENT : velocityY;
    }

    public static Vec3 capDescent(Vec3 velocity) {
        double cappedY = capVerticalVelocity(velocity.y);
        return cappedY == velocity.y ? velocity : new Vec3(velocity.x, cappedY, velocity.z);
    }
}
