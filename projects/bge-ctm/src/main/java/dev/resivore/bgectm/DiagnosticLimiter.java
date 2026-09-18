package dev.resivore.bgectm;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Bounded, signature-based admission policy for render-path diagnostics. */
public final class DiagnosticLimiter {
    private final int cap;
    private final Set<String> signatures = new HashSet<>();
    private boolean suppressionReported;

    public DiagnosticLimiter(int cap) {
        if (cap < 1) throw new IllegalArgumentException("cap must be positive");
        this.cap = cap;
    }

    public synchronized Admission admit(String signature) {
        Objects.requireNonNull(signature, "signature");
        if (signatures.contains(signature)) return Admission.DUPLICATE;
        if (signatures.size() < cap) {
            signatures.add(signature);
            return Admission.ACCEPTED;
        }
        if (!suppressionReported) {
            suppressionReported = true;
            return Admission.SUPPRESS_NOTICE;
        }
        return Admission.SUPPRESSED;
    }

    public enum Admission {
        ACCEPTED,
        DUPLICATE,
        SUPPRESS_NOTICE,
        SUPPRESSED
    }
}
