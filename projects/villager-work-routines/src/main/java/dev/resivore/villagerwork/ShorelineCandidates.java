package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure standing-position qualification for an already verified open-water block. */
final class ShorelineCandidates {
    private static final Direction[] SIDES = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private ShorelineCandidates() {}

    interface Geometry {
        boolean loaded(BlockPos pos);
        boolean clear(BlockPos pos);
        boolean supports(BlockPos pos);
    }

    record Candidate(BlockPos feet, BlockPos water) {}

    static List<Candidate> adjacentTo(BlockPos water, Geometry geometry) {
        List<Candidate> candidates = new ArrayList<>(8);
        for (Direction side : SIDES) {
            BlockPos besideWater = water.relative(side);
            // A level bank stands beside the water; an ordinary solid shore stands one block higher.
            for (int rise = 0; rise <= 1; rise++) {
                BlockPos feet = besideWater.above(rise);
                BlockPos head = feet.above();
                BlockPos support = feet.below();
                if (geometry.loaded(feet) && geometry.loaded(head) && geometry.loaded(support)
                        && geometry.clear(feet) && geometry.clear(head) && geometry.supports(support))
                    candidates.add(new Candidate(feet, water));
            }
        }
        return candidates;
    }

    static Comparator<Candidate> nearestTo(BlockPos villager, BlockPos site) {
        return Comparator.comparingDouble((Candidate candidate) -> candidate.feet.distSqr(villager))
                .thenComparingDouble(candidate -> candidate.water.distSqr(site))
                .thenComparingInt(candidate -> candidate.feet.getX())
                .thenComparingInt(candidate -> candidate.feet.getY())
                .thenComparingInt(candidate -> candidate.feet.getZ())
                .thenComparingInt(candidate -> candidate.water.getX())
                .thenComparingInt(candidate -> candidate.water.getY())
                .thenComparingInt(candidate -> candidate.water.getZ());
    }
}
