package dev.aero.shulkertrowel.palette;

import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Optional;

public interface CandidateSelector {
    Optional<PaletteCandidate> select(List<PaletteCandidate> candidates, RandomSource random);
}
