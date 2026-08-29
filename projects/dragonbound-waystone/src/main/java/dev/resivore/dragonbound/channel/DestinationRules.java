package dev.resivore.dragonbound.channel;

import dev.resivore.dragonbound.block.DragonboundWaystoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class DestinationRules {
    private DestinationRules() {
    }

    public static Vec3 exactTopCenter(BlockPos waystonePos) {
        return new Vec3(
                waystonePos.getX() + 0.5D,
                waystonePos.getY() + DragonboundWaystoneBlock.HEIGHT_BLOCKS,
                waystonePos.getZ() + 0.5D
        );
    }

    public static boolean exactDestinationIsSafe(
            boolean bindingMatches,
            boolean blockMatches,
            boolean insideBuildHeight,
            boolean insideWorldBorder,
            boolean collisionFree
    ) {
        return bindingMatches && blockMatches && insideBuildHeight && insideWorldBorder && collisionFree;
    }
}
