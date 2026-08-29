package dev.resivore.strippingtogglefallingtreecompat;

import fr.rakambda.fallingtree.common.config.enums.SneakMode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToggleAuthorityTest {
    private static boolean permitsWholeTree(
            boolean strippingToggleEnabled,
            boolean crouching,
            boolean normalNonToggleEligible) {
        Set<String> nativeTags = new HashSet<>();
        if (ToggleAuthority.shouldHaveDisabledTag(strippingToggleEnabled)) {
            nativeTags.add(ToggleAuthority.LEGACY_DISABLED_TAG);
        }
        return ToggleAuthority.isEnabled(nativeTags)
                && SneakMode.IGNORE.test(crouching)
                && normalNonToggleEligible;
    }

    @Test
    void ignoreModeMatrixUsesStrippingToggleAsTheOnlyActivationGate() {
        assertFalse(permitsWholeTree(false, false, true));
        assertFalse(permitsWholeTree(false, true, true));
        assertTrue(permitsWholeTree(true, false, true));
        assertTrue(permitsWholeTree(true, true, true));
    }

    @Test
    void offAlwaysDisarmsAndOnDelegatesToNormalNonToggleEligibilityForEitherPosture() {
        for (boolean crouching : new boolean[] {false, true}) {
            for (boolean normalEligibility : new boolean[] {false, true}) {
                assertFalse(permitsWholeTree(false, crouching, normalEligibility));
                assertEquals(
                        normalEligibility,
                        permitsWholeTree(true, crouching, normalEligibility));
            }
        }
    }

    @Test
    void nativeDisabledTagIsTheInverseMirrorOfTheMasterToggle() {
        assertTrue(ToggleAuthority.shouldHaveDisabledTag(false));
        assertFalse(ToggleAuthority.shouldHaveDisabledTag(true));
        assertFalse(ToggleAuthority.isEnabled(Set.of(ToggleAuthority.LEGACY_DISABLED_TAG)));
        assertTrue(ToggleAuthority.isEnabled(Set.of()));
    }

    @Test
    void exactAuditedIgnoreModeAcceptsBothPostures() {
        assertTrue(SneakMode.IGNORE.test(false));
        assertTrue(SneakMode.IGNORE.test(true));
    }

    @Test
    void statusFeedbackDescribesTheMasterState() {
        assertTrue(ToggleAuthority.statusTranslationKey(true).endsWith(".enabled"));
        assertTrue(ToggleAuthority.statusTranslationKey(false).endsWith(".disabled"));
    }
}
