package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CompatibilityActivationTest {
    private static final CompatibilityActivation.DependencyIdentity XAERO = identity(
            "xaerominimap", "26.4.2", 2_221_925L,
            "69284892d2eb853c9aefa85a4c9b74232c322da00207994c67ab8aeed8a64048");
    private static final CompatibilityActivation.DependencyIdentity XAEROLIB = identity(
            "xaerolib", "1.7.1", 621_485L,
            "7f4a78dd7e046fea0500fef83b1481d85317c8348d47e947035d7a07efe51065");
    private static final CompatibilityActivation.DependencyIdentity GECKOLIB = identity(
            "geckolib", "5.5.1", 703_096L,
            "4bf1c86b4b47aa2c5d84208255695f10d79d609b23d802c995711e64b45cfce0");
    private static final CompatibilityActivation.DependencyIdentity RIBBITS = identity(
            "ribbits", "4.1.6+26.2-mynx-canary7", 3_320_708L,
            "6b18658c5a68d66623b9a388cc644e2f7a1b864e490b6f8b35d57fcd73a5bf74");

    @Test
    void exactAuditedFourArchiveSetIsTheOnlyActiveContract() {
        CompatibilityActivation.Decision decision =
                CompatibilityActivation.evaluate(XAERO, XAEROLIB, GECKOLIB, RIBBITS);

        assertTrue(decision.active(), decision.reason());
        assertFalse(decision.reason().isBlank());
    }

    @Test
    void anyXaeroIdentityDriftFailsClosed() {
        assertInactive(identity("xaerominimap", "26.4.3", XAERO.size(), XAERO.sha256()),
                XAEROLIB, GECKOLIB, RIBBITS);
        assertInactive(identity("xaerominimap", XAERO.version(), XAERO.size() + 1L,
                XAERO.sha256()), XAEROLIB, GECKOLIB, RIBBITS);
        assertInactive(identity("xaerominimap", XAERO.version(), XAERO.size(),
                flip(XAERO.sha256())), XAEROLIB, GECKOLIB, RIBBITS);
        assertInactive(identity("xaeroworldmap", XAERO.version(), XAERO.size(), XAERO.sha256()),
                XAEROLIB, GECKOLIB, RIBBITS);
    }

    @Test
    void anyNestedXaeroLibIdentityDriftFailsClosed() {
        assertInactive(XAERO, identity("xaerolib", "1.7.2", XAEROLIB.size(),
                XAEROLIB.sha256()), GECKOLIB, RIBBITS);
        assertInactive(XAERO, identity("xaerolib", XAEROLIB.version(),
                XAEROLIB.size() + 1L, XAEROLIB.sha256()), GECKOLIB, RIBBITS);
        assertInactive(XAERO, identity("xaerolib", XAEROLIB.version(), XAEROLIB.size(),
                flip(XAEROLIB.sha256())), GECKOLIB, RIBBITS);
        assertInactive(XAERO, identity("embedded-library", XAEROLIB.version(), XAEROLIB.size(),
                XAEROLIB.sha256()), GECKOLIB, RIBBITS);
    }

    @Test
    void anyGeckoLibIdentityDriftFailsClosed() {
        assertInactive(XAERO, XAEROLIB,
                identity("geckolib", "5.5.2", GECKOLIB.size(), GECKOLIB.sha256()), RIBBITS);
        assertInactive(XAERO, XAEROLIB,
                identity("geckolib", GECKOLIB.version(), GECKOLIB.size() + 1L,
                        GECKOLIB.sha256()), RIBBITS);
        assertInactive(XAERO, XAEROLIB,
                identity("geckolib", GECKOLIB.version(), GECKOLIB.size(),
                        flip(GECKOLIB.sha256())), RIBBITS);
        assertInactive(XAERO, XAEROLIB,
                identity("other-gecko-provider", GECKOLIB.version(), GECKOLIB.size(),
                        GECKOLIB.sha256()), RIBBITS);
    }

    @Test
    void anyRibbitsIdentityDriftFailsClosed() {
        assertInactive(XAERO, XAEROLIB, GECKOLIB,
                identity("ribbits", "4.1.6+26.2-mynx-canary6",
                        RIBBITS.size(), RIBBITS.sha256()));
        assertInactive(XAERO, XAEROLIB, GECKOLIB, identity("ribbits", RIBBITS.version(),
                RIBBITS.size() + 1L, RIBBITS.sha256()));
        assertInactive(XAERO, XAEROLIB, GECKOLIB, identity("ribbits", RIBBITS.version(),
                RIBBITS.size(), flip(RIBBITS.sha256())));
        assertInactive(XAERO, XAEROLIB, GECKOLIB,
                identity("other-ribbit-mod", RIBBITS.version(),
                        RIBBITS.size(), RIBBITS.sha256()));
    }

    @Test
    void missingIdentityFailsClosedWithADiagnosticDecision() {
        assertInactive(null, XAEROLIB, GECKOLIB, RIBBITS);
        assertInactive(XAERO, null, GECKOLIB, RIBBITS);
        assertInactive(XAERO, XAEROLIB, null, RIBBITS);
        assertInactive(XAERO, XAEROLIB, GECKOLIB, null);
    }

    private static void assertInactive(
            CompatibilityActivation.DependencyIdentity xaero,
            CompatibilityActivation.DependencyIdentity xaeroLib,
            CompatibilityActivation.DependencyIdentity geckolib,
            CompatibilityActivation.DependencyIdentity ribbits) {
        CompatibilityActivation.Decision decision =
                CompatibilityActivation.evaluate(xaero, xaeroLib, geckolib, ribbits);
        assertFalse(decision.active());
        assertFalse(decision.reason().isBlank());
    }

    private static CompatibilityActivation.DependencyIdentity identity(
            String modId, String version, long size, String sha256) {
        return new CompatibilityActivation.DependencyIdentity(modId, version, size, sha256);
    }

    private static String flip(String hash) {
        return (hash.charAt(0) == '0' ? "1" : "0") + hash.substring(1);
    }
}
