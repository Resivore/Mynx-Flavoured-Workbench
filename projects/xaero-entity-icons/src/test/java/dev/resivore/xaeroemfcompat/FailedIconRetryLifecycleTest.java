package dev.resivore.xaeroemfcompat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C7's retry is a cache-result policy, not a render-success claim.  Xaero may
 * query a FAILED value on ordinary map draws before it has granted the one
 * prerender opportunity that can replace the value.
 */
class FailedIconRetryLifecycleTest {

    @Test
    void failedEntryIsNotConsumedBeforeXaeroCanRecreateIt() {
        IconDiagnostics.reload();
        assertFalse(IconDiagnostics.retryFailedOnceAtPrerender(
                "minecraft:sheep", "adult", true, false));
        assertTrue(IconDiagnostics.retryFailedOnceAtPrerender(
                "minecraft:sheep", "adult", true, true));
        assertFalse(IconDiagnostics.retryFailedOnceAtPrerender(
                "minecraft:sheep", "adult", true, true));
    }

    @Test
    void oneRetryIsIndependentPerVariantAndResourceGeneration() {
        IconDiagnostics.reload();
        assertTrue(IconDiagnostics.retryFailedOnceAtPrerender(
                "minecraft:sheep", "adult", true, true));
        assertTrue(IconDiagnostics.retryFailedOnceAtPrerender(
                "minecraft:sheep", "baby", true, true));
        IconDiagnostics.reload();
        assertTrue(IconDiagnostics.retryFailedOnceAtPrerender(
                "minecraft:sheep", "adult", true, true));
    }
}
