package dev.resivore.mynxtrees;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MossyOakGenerationTest {
    @Test void independentlySelectsPositionsWithTheFeatureProbability() {
        for (int denominator : new int[] {3, 10}) {
            int count = 0, adjacent = 0, changedSeed = 0;
            for (int i = 0; i < 32768; i++) {
                var p = new BlockPos(i % 128 - 64, i % 19 + 50, i / 128 - 128);
                boolean selected = MossyOakGeneration.selected(12345L, p, denominator);
                assertEquals(selected, MossyOakGeneration.selected(12345L, p, denominator));
                if (selected) {
                    count++;
                    if (MossyOakGeneration.selected(12345L, p.above(), denominator)) adjacent++;
                }
                if (selected != MossyOakGeneration.selected(-42L, p, denominator)) changedSeed++;
            }
            assertEquals(32768.0 / denominator, count, 150);
            assertEquals(32768.0 / (denominator * denominator), adjacent, 150);
            // Pin the C9 sample as well as its distribution across neighboring positions.
            assertEquals(denominator == 3 ? 10982 : 3326, count);
            assertEquals(denominator == 3 ? 3563 : 337, adjacent);
            assertTrue(changedSeed > 4500);
        }
    }
}
