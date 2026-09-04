package dev.resivore.ribbitsxaeroicons;

import java.util.Locale;
import java.util.Objects;

/** Exact whole-archive dependency policy for the Canary 1 binary seams. */
public final class CompatibilityActivation {
    public static final DependencyIdentity SUPPORTED_XAERO = new DependencyIdentity(
            "xaerominimap",
            "26.4.2",
            2_221_925L,
            "69284892d2eb853c9aefa85a4c9b74232c322da00207994c67ab8aeed8a64048");
    public static final DependencyIdentity SUPPORTED_XAEROLIB = new DependencyIdentity(
            "xaerolib",
            "1.7.1",
            621_485L,
            "7f4a78dd7e046fea0500fef83b1481d85317c8348d47e947035d7a07efe51065");
    public static final DependencyIdentity SUPPORTED_GECKOLIB = new DependencyIdentity(
            "geckolib",
            "5.5.1",
            703_096L,
            "4bf1c86b4b47aa2c5d84208255695f10d79d609b23d802c995711e64b45cfce0");
    public static final DependencyIdentity SUPPORTED_RIBBITS = new DependencyIdentity(
            "ribbits",
            "4.1.6+26.2-mynx-canary7",
            3_320_708L,
            "6b18658c5a68d66623b9a388cc644e2f7a1b864e490b6f8b35d57fcd73a5bf74");

    private CompatibilityActivation() {
    }

    public static Decision evaluate(
            DependencyIdentity xaero,
            DependencyIdentity xaeroLib,
            DependencyIdentity gecko,
            DependencyIdentity ribbits) {
        if (!SUPPORTED_XAERO.equals(xaero)) {
            return new Decision(false, mismatch("Xaero Minimap", SUPPORTED_XAERO, xaero));
        }
        if (!SUPPORTED_XAEROLIB.equals(xaeroLib)) {
            return new Decision(false, mismatch("XaeroLib", SUPPORTED_XAEROLIB, xaeroLib));
        }
        if (!SUPPORTED_GECKOLIB.equals(gecko)) {
            return new Decision(false, mismatch("GeckoLib", SUPPORTED_GECKOLIB, gecko));
        }
        if (!SUPPORTED_RIBBITS.equals(ribbits)) {
            return new Decision(false, mismatch("Ribbits", SUPPORTED_RIBBITS, ribbits));
        }

        return new Decision(true, "exact Canary 1 dependency contract matched");
    }

    private static String mismatch(
            String label, DependencyIdentity expected, DependencyIdentity actual) {
        return label + " exact binary mismatch: expected " + expected + ", found " + actual;
    }

    public record DependencyIdentity(String modId, String version, long size, String sha256) {
        public DependencyIdentity {
            Objects.requireNonNull(modId, "modId");
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(sha256, "sha256");
            sha256 = sha256.toLowerCase(Locale.ROOT);
        }
    }

    public record Decision(boolean active, String reason) {
        public Decision {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
