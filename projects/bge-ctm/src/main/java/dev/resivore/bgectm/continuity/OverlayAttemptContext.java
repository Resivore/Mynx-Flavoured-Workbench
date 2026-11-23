package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.BgeCtmDiagnostics;
import dev.resivore.bgectm.SurfaceContactResolver.Decision;
import org.jetbrains.annotations.Nullable;

/** Thread-confined metadata used only to make a single overlay diagnostic explainable. */
public final class OverlayAttemptContext {
    private static final ThreadLocal<Attempt> CURRENT = BgeCtmDiagnostics.enabled() ? new ThreadLocal<>() : null;

    private OverlayAttemptContext() {}

    @Nullable public static Attempt end() {
        if (!BgeCtmDiagnostics.enabled()) return null;
        Attempt attempt = CURRENT.get();
        CURRENT.remove();
        return attempt;
    }

    @Nullable public static Attempt current() { return BgeCtmDiagnostics.enabled() ? CURRENT.get() : null; }

    /** Continuity 3.0.1 tests the collision gate before every other overlay decision. */
    public static void begin(boolean nativeFull, boolean promoted) {
        if (!BgeCtmDiagnostics.enabled()) return;
        Attempt attempt = new Attempt();
        attempt.nativeFull = nativeFull;
        attempt.promoted = promoted;
        if (!nativeFull) attempt.reason = promoted ? "PARTIAL_SOURCE_PROMOTED" : "PARTIAL_SOURCE_GATE_REJECT";
        CURRENT.set(attempt);
    }

    public static void connectBlocks(boolean result) {
        if (!BgeCtmDiagnostics.enabled()) return;
        Attempt attempt = CURRENT.get();
        if (attempt != null && !result) attempt.reason = "CONNECT_BLOCKS_REJECT";
    }

    public static void connectionSemantics(boolean nativeResult, boolean canonicalResult) {
        if (!BgeCtmDiagnostics.enabled()) return;
        Attempt attempt = CURRENT.get();
        if (attempt == null) return;
        attempt.nativeConnection = nativeResult;
        attempt.canonicalConnection = canonicalResult;
        if (nativeResult != canonicalResult) {
            attempt.reason = canonicalResult
                    ? "CANONICAL_CONNECTION_REJECTS_OVERLAY"
                    : "CARRIER_CONNECTION_REJECTION_REMOVED";
        }
    }

    public static void geometry(Decision decision) {
        Attempt attempt = current();
        if (attempt != null) attempt.geometry = decision;
    }

    public static final class Attempt {
        @Nullable public Decision geometry;
        public boolean nativeFull;
        public boolean promoted;
        public boolean nativeConnection;
        public boolean canonicalConnection;
        public String reason = "NATIVE_SEMANTIC_REJECT";
    }
}
