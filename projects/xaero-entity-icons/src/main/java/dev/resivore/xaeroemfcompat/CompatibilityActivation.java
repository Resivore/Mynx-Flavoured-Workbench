package dev.resivore.xaeroemfcompat;

import java.util.Optional;

/** Exact current-binary activation policy for Canary 3. */
public final class CompatibilityActivation {
    public static final String SUPPORTED_XAERO_VERSION = "26.4.2";
    public static final String SUPPORTED_EMF_VERSION = "3.2.6";

    private CompatibilityActivation() {
    }

    public static boolean shouldApply(Optional<String> xaeroVersion, Optional<String> emfVersion) {
        if (xaeroVersion.isEmpty() || emfVersion.isEmpty()) {
            return false;
        }
        requireSupportedVersions(xaeroVersion.orElseThrow(), emfVersion.orElseThrow());
        return true;
    }

    public static void requireSupportedVersions(String xaeroVersion, String emfVersion) {
        if (!SUPPORTED_XAERO_VERSION.equals(xaeroVersion)
                || !SUPPORTED_EMF_VERSION.equals(emfVersion)) {
            throw new IllegalStateException(
                    "Xaero/EMF entity-icon compatibility Canary 3 supports Xaero's Minimap "
                            + SUPPORTED_XAERO_VERSION + " and EMF " + SUPPORTED_EMF_VERSION
                            + "; found Xaero " + xaeroVersion + " and EMF " + emfVersion);
        }
    }
}
