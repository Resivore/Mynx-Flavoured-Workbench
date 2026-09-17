package dev.resivore.slabdecorations;

import dev.resivore.slabdecorations.mixin.GrowingPlantBlockAccessor;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.BigDripleafBlock;
import net.minecraft.world.level.block.BigDripleafStemBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CactusFlowerBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.SmallDripleafBlock;
import net.minecraft.world.level.block.SporeBlossomBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Classifies slab-compatible foliage by Minecraft behavior and attachment contracts.
 *
 * <p>This class intentionally owns no registry-ID permission table. A family only describes how
 * to find a physical root and how to align its representation; the block's own projected vanilla
 * survival result remains the permission decision.</p>
 */
public final class PlantFamilyEligibility {
    private PlantFamilyEligibility() {
    }

    public static Optional<Family> family(BlockState state) {
        var block = state.getBlock();

        // The exclusion boundary is deliberately structural and small. Everything below this
        // block is classified only by how its vertical root/anchor can be resolved; vanilla
        // canonical-parent survival remains the actual permission decision.
        if (block instanceof StemBlock
                || block instanceof StemBlock
                || block instanceof AttachedStemBlock
                || block instanceof ChorusPlantBlock
                || block instanceof ChorusFlowerBlock
                || block instanceof LilyPadBlock
                || block instanceof VineBlock
                || block instanceof MultifaceBlock
                || block instanceof CocoaBlock
                || block instanceof BaseCoralWallFanBlock) {
            return Optional.empty();
        }

        // A planted propagule is a sapling; hanging propagules deliberately retain vanilla's
        // leaf-attached lifecycle and must never acquire a floor attachment.
        if (block instanceof MangrovePropaguleBlock
                && state.hasProperty(MangrovePropaguleBlock.HANGING)
                && state.getValue(MangrovePropaguleBlock.HANGING)) {
            return Optional.empty();
        }
        if (block instanceof SaplingBlock) return Optional.of(Family.UPWARD_VEGETATION);

        // Minecraft 26.2 has no shared hanging-foliage superclass for these two single-block
        // decorations. Their concrete classes are still behavior contracts, not registry IDs.
        if (block instanceof HangingRootsBlock || block instanceof SporeBlossomBlock) {
            return Optional.of(Family.CEILING_FOLIAGE);
        }

        // Pale hanging moss is its own downward-growing, same-block column implementation.
        if (block instanceof HangingMossBlock) {
            return Optional.of(Family.HANGING_MOSS_COLUMN);
        }

        // Generic head/body columns cover upward, downward, terrestrial, aquatic, and compatible
        // modded implementations. Direction determines the physical root/anchor and offset.
        if (block instanceof GrowingPlantBlock) {
            if (block instanceof GrowingPlantBlockAccessor accessor) {
                Direction direction = accessor.slabDecorations$getGrowthDirection();
                if (direction == Direction.UP) {
                    return Optional.of(Family.UPWARD_GROWING_COLUMN);
                }
                if (direction == Direction.DOWN) {
                    return Optional.of(Family.DOWNWARD_GROWING_COLUMN);
                }
            }
            return Optional.empty();
        }

        if (block instanceof SugarCaneBlock) {
            return Optional.of(Family.SUGAR_CANE_COLUMN);
        }

        if (block instanceof BambooSaplingBlock || block instanceof BambooStalkBlock) {
            return Optional.of(Family.BAMBOO_COLUMN);
        }

        if (block instanceof CactusBlock || block instanceof CactusFlowerBlock) {
            return Optional.of(Family.CACTUS_COLUMN);
        }

        // These two dripleaf implementations do not extend VegetationBlock, but share one rooted
        // upward-supported column contract.
        if (block instanceof BigDripleafBlock || block instanceof BigDripleafStemBlock) {
            return Optional.of(Family.DRIPLEAF_COLUMN);
        }

        if (block instanceof SmallDripleafBlock || block instanceof DoublePlantBlock) {
            return Optional.of(Family.DOUBLE_HEIGHT_VEGETATION);
        }

        if (block instanceof MossyCarpetBlock
                || block instanceof CarpetBlock && !(block instanceof WoolCarpetBlock)) {
            return Optional.of(Family.SURFACE_FOLIAGE);
        }

        // Floor coral plants/fans are not VegetationBlock subclasses, but their vanilla contract
        // is still one upward-facing substrate. Wall fans were excluded above.
        if (block instanceof BaseCoralPlantTypeBlock) {
            return Optional.of(Family.UPWARD_VEGETATION);
        }

        // These are behaviour contracts shared by all vanilla wood variants. LanternBlock also
        // covers BBB's WoodenLanternBlock and Aurora's AmethystLanternBlock without linking either
        // optional mod. Ribbits is deliberately handled by its confirmed HANGING state contract.
        if (block instanceof LanternBlock || OptionalSurfaceAdapters.isRibbitsSwampLantern(state)) {
            return Optional.of(Family.LANTERN);
        }
        if (block instanceof StandingSignBlock) return Optional.of(Family.STANDING_SIGN);
        if (block instanceof CeilingHangingSignBlock) return Optional.of(Family.CEILING_HANGING_SIGN);
        if (block instanceof TorchBlock && !(block instanceof RedstoneTorchBlock)) {
            return Optional.of(Family.FLOOR_TORCH);
        }

        return block instanceof VegetationBlock
                ? Optional.of(Family.UPWARD_VEGETATION)
                : Optional.empty();
    }

    public static boolean isEligible(BlockState state) {
        return family(state).isPresent();
    }

    public enum Family {
        UPWARD_VEGETATION,
        DOUBLE_HEIGHT_VEGETATION,
        DRIPLEAF_COLUMN,
        SURFACE_FOLIAGE,
        CEILING_FOLIAGE,
        UPWARD_GROWING_COLUMN,
        DOWNWARD_GROWING_COLUMN,
        SUGAR_CANE_COLUMN,
        BAMBOO_COLUMN,
        CACTUS_COLUMN,
        HANGING_MOSS_COLUMN,
        LANTERN,
        STANDING_SIGN,
        CEILING_HANGING_SIGN,
        FLOOR_TORCH
    }
}
