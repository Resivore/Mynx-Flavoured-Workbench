package dev.resivore.bgebushyleaves;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BgeLeafEligibilityTest {
    @Test void canonicalLeafSemanticsAcceptVanillaLeafBindingOutput() {
        assertTrue(BgeLeafEligibility.hasLeafSemantics(true, Optional.empty()));
    }

    @Test void nonLeafCanonicalMaterialIsRejected() {
        assertFalse(BgeLeafEligibility.hasLeafSemantics(false, Optional.empty()));
    }
}
