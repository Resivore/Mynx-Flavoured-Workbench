package dev.resivore.xaeroemfcompat;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityActivationTest {
    @Test
    void bridgeIsActiveOnlyWithXaeroAndEmf() {
        assertTrue(CompatibilityActivation.shouldApply(
                Optional.of("26.4.2"), Optional.of("3.2.6")));
        assertFalse(CompatibilityActivation.shouldApply(
                Optional.empty(), Optional.of("3.2.6")));
        assertFalse(CompatibilityActivation.shouldApply(
                Optional.of("26.4.2"), Optional.empty()));
        assertFalse(CompatibilityActivation.shouldApply(
                Optional.empty(), Optional.empty()));
    }

    @Test
    void exactAuditedVersionsAreAccepted() {
        assertDoesNotThrow(() -> CompatibilityActivation.requireSupportedVersions("26.4.2", "3.2.6"));
    }

    @Test
    void xaeroVersionDriftFailsClearly() {
        assertThrows(IllegalStateException.class,
                () -> CompatibilityActivation.shouldApply(
                        Optional.of("26.4.3"), Optional.of("3.2.6")));
    }

    @Test
    void emfVersionDriftFailsClearly() {
        assertThrows(IllegalStateException.class,
                () -> CompatibilityActivation.shouldApply(
                        Optional.of("26.4.2"), Optional.of("3.2.7")));
    }
}
