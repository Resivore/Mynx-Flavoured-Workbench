package dev.resivore.woolsoundproofchests;

import java.util.List;
import java.util.function.Function;

public final class SoundproofingEvaluator {
    public enum Face {
        NORTH,
        SOUTH,
        EAST,
        WEST,
        DOWN
    }

    public record Seal(boolean fullBlock, boolean wool) {
        public static final Seal OPEN = new Seal(false, false);
    }

    private static final List<Face> TESTED_FACES = List.of(Face.values());

    private SoundproofingEvaluator() {
    }

    public static boolean qualifies(Function<Face, Seal> sealAt) {
        int sealedFaces = 0;
        int woolFaces = 0;

        for (Face face : TESTED_FACES) {
            Seal seal = sealAt.apply(face);
            if (seal.fullBlock()) {
                sealedFaces++;
                if (seal.wool()) {
                    woolFaces++;
                }
            }
        }

        return sealedFaces >= 4 && woolFaces >= 2;
    }
}
