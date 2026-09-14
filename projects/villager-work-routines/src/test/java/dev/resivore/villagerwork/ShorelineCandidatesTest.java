package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShorelineCandidatesTest {
    @Test void ordinaryShoreUsesAirAboveSolidBlockBesideWater() {
        BlockPos water = new BlockPos(0, 64, 0);
        BlockPos shore = new BlockPos(1, 64, 0);
        List<ShorelineCandidates.Candidate> candidates = ShorelineCandidates.adjacentTo(
                water, geometry(Set.of(shore), Set.of(), Set.of()));
        assertEquals(List.of(new ShorelineCandidates.Candidate(shore.above(), water)), candidates);
    }

    @Test void lowerBankCanStandAtWaterHeight() {
        BlockPos water = new BlockPos(0, 64, 0);
        BlockPos support = new BlockPos(0, 63, -1);
        List<ShorelineCandidates.Candidate> candidates = ShorelineCandidates.adjacentTo(
                water, geometry(Set.of(support), Set.of(), Set.of()));
        assertEquals(List.of(new ShorelineCandidates.Candidate(support.above(), water)), candidates);
    }

    @Test void blockedHeadOrUnloadedSupportRejectsStandingPosition() {
        BlockPos water = new BlockPos(0, 64, 0);
        BlockPos support = new BlockPos(1, 64, 0);
        BlockPos feet = support.above();
        assertEquals(List.of(), ShorelineCandidates.adjacentTo(
                water, geometry(Set.of(support, feet.above()), Set.of(), Set.of())));
        assertEquals(List.of(), ShorelineCandidates.adjacentTo(
                water, geometry(Set.of(support), Set.of(support), Set.of())));
    }

    @Test void nearestOrderingBreaksAllTiesByCoordinates() {
        BlockPos villager = new BlockPos(0, 65, 0);
        BlockPos site = new BlockPos(0, 64, 0);
        ShorelineCandidates.Candidate east = new ShorelineCandidates.Candidate(
                new BlockPos(1, 65, 0), new BlockPos(0, 64, 0));
        ShorelineCandidates.Candidate west = new ShorelineCandidates.Candidate(
                new BlockPos(-1, 65, 0), new BlockPos(0, 64, 0));
        ShorelineCandidates.Candidate fartherWater = new ShorelineCandidates.Candidate(
                new BlockPos(0, 65, 1), new BlockPos(0, 64, 2));
        List<ShorelineCandidates.Candidate> candidates = new ArrayList<>(List.of(east, fartherWater, west));
        candidates.sort(ShorelineCandidates.nearestTo(villager, site));
        assertEquals(List.of(west, east, fartherWater), candidates);
    }

    private static ShorelineCandidates.Geometry geometry(
            Set<BlockPos> solid, Set<BlockPos> unloaded, Set<BlockPos> obstructed) {
        return new ShorelineCandidates.Geometry() {
            @Override public boolean loaded(BlockPos pos) { return !unloaded.contains(pos); }
            @Override public boolean clear(BlockPos pos) {
                return !solid.contains(pos) && !obstructed.contains(pos);
            }
            @Override public boolean supports(BlockPos pos) { return solid.contains(pos); }
        };
    }
}
