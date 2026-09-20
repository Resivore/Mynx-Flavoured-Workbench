package dev.resivore.bgebushyleaves.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AppearanceSelectionTest {
    private record Attributes(String sprite, int tint, String material, String layer, boolean emissive) {}

    @Test void syntheticNonCullDecorativeQuadsWinAndPreserveAllAppearanceAttributes() {
        Attributes ordinary = new Attributes("test:leaf_base", 0, "cutout", "cutout_mipped", false);
        Attributes decorative = new Attributes("test:modded_canopy", 3, "translucent", "translucent", true);
        List<Attributes> selected = AppearanceSelection.preferDecorative(List.of(
                new AppearanceSelection.Candidate<>(ordinary, false),
                new AppearanceSelection.Candidate<>(decorative, true)));
        assertEquals(List.of(decorative), selected);
    }

    @Test void ordinaryCanonicalLeafFaceIsTheProviderNeutralFallback() {
        Attributes north = new Attributes("test:any_leaf", 1, "cutout", "cutout", false);
        Attributes south = new Attributes("test:any_leaf_bottom", 1, "cutout", "cutout", false);
        assertEquals(List.of(north, south), AppearanceSelection.preferDecorative(List.of(
                new AppearanceSelection.Candidate<>(north, false), new AppearanceSelection.Candidate<>(south, false))));
    }

    @Test void selectionHasNoBushyPathOrRegistryFamilyConvention() {
        Attributes arbitrary = new Attributes("third_party:autumn_canopy_outer", 7, "custom", "custom_layer", true);
        assertEquals(List.of(arbitrary), AppearanceSelection.preferDecorative(List.of(
                new AppearanceSelection.Candidate<>(arbitrary, true))));
    }
}
