package dev.aero.shulkertrowel.palette;

import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Optional;

/** Provisional Canary behavior, matching the audited Shulker Palette 1.0.0 quantity weighting. */
public final class QuantityWeightedSelector implements CandidateSelector {
    @Override
    public Optional<PaletteCandidate> select(List<PaletteCandidate> candidates, RandomSource random) {
        int totalWeight = candidates.stream().mapToInt(PaletteCandidate::weight).sum();
        if (totalWeight <= 0) return Optional.empty();
        int roll = random.nextInt(totalWeight);
        for (PaletteCandidate candidate : candidates) {
            roll -= candidate.weight();
            if (roll < 0) return Optional.of(candidate);
        }
        return Optional.empty();
    }
}
