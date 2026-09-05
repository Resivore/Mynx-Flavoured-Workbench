package dev.resivore.mynxregions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

final class MycotoxicDaisyParticles {
    private static final int SPAWN_CHANCE = 4;

    private MycotoxicDaisyParticles() {}

    static void trySpawn(Level level, BlockPos pos, RandomSource random, double minY, double maxY) {
        if (random.nextInt(SPAWN_CHANCE) != 0) return;

        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double y = pos.getY() + minY + random.nextDouble() * (maxY - minY);
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        double velocityX = (random.nextDouble() - 0.5) * 0.008;
        double velocityY = 0.002 + random.nextDouble() * 0.006;
        double velocityZ = (random.nextDouble() - 0.5) * 0.008;
        level.addParticle(ParticleTypes.END_ROD, x, y, z, velocityX, velocityY, velocityZ);
    }
}
