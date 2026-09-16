package dev.aero.shulkertrowel.compat;

/**
 * Narrow binary contract for the independently audited Quick Right-Click JAR.
 * A different upstream binary intentionally receives no compatibility mixin.
 */
public final class QuickRightClickCompatibility {
    public static final String MOD_ID = "quickrightclick";
    public static final String AUDITED_VERSION = "1.9";

    private QuickRightClickCompatibility() {}

    public static boolean supports(String modId, String version) {
        return MOD_ID.equals(modId) && AUDITED_VERSION.equals(version);
    }
}
