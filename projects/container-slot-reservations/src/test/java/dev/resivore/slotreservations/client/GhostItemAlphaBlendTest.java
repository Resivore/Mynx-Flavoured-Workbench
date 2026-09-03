package dev.resivore.slotreservations.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pixel-level proof of the final shader and blend contract; this is not runtime visual proof. */
final class GhostItemAlphaBlendTest {
    private static final int MATCHA_SLOT_BACKGROUND = 0x43362A;
    private static final int DIAGNOSTIC_BACKGROUND = 0xC9E8FF;
    private static final int LIGHT_ITEM = 0xF2D66B;
    private static final int DARK_ITEM = 0x263149;
    private static final double OPACITY = 0x59 / 255.0;
    private static final double EPSILON = 1.0E-9;

    @Test
    void packedBoundaryCarriesOnlyAlphaAndNeutralWhiteRgb() {
        int packed = GhostItemRenderScope.alphaOnlyWhite(0x59);

        assertEquals(0x59FFFFFF, packed);
        assertEquals(0x59, packed >>> 24);
        assertEquals(0xFFFFFF, packed & 0xFFFFFF);
        assertNotEquals(0x5943362A, packed,
                "#43362A is a slot background, never an item multiplier");
        assertNotEquals(0x59595959, packed,
                "Alpha must not be copied into the RGB multiplier channels");
    }

    @Test
    void lightAndDarkSpriteRgbIsPreservedOverMatchaAndContrastingBackgrounds() {
        for (int item : new int[]{LIGHT_ITEM, DARK_ITEM}) {
            for (int background : new int[]{MATCHA_SLOT_BACKGROUND, DIAGNOSTIC_BACKGROUND}) {
                double[] actual = composite(item, 1.0, background);
                double[] expected = straightComposite(item, OPACITY, background);
                assertArrayEquals(expected, actual, EPSILON);

                double[] recoveredSource = recoverSource(actual, background, OPACITY);
                assertArrayEquals(rgb(item), recoveredSource, EPSILON,
                        "Uniform opacity must preserve the source hue and RGB");
            }
        }
    }

    @Test
    void transparentPixelsRemainTransparentAndBackgroundShowsThroughWithoutAMatte() {
        for (int background : new int[]{MATCHA_SLOT_BACKGROUND, DIAGNOSTIC_BACKGROUND}) {
            assertArrayEquals(rgb(background), composite(LIGHT_ITEM, 0.0, background), EPSILON,
                    "A zero-alpha sprite texel must contribute no color");
        }

        double[] onMatcha = composite(LIGHT_ITEM, 1.0, MATCHA_SLOT_BACKGROUND);
        double[] onDiagnostic = composite(LIGHT_ITEM, 1.0, DIAGNOSTIC_BACKGROUND);
        double[] expectedDifference = scale(
                subtract(rgb(DIAGNOSTIC_BACKGROUND), rgb(MATCHA_SLOT_BACKGROUND)),
                1.0 - OPACITY
        );
        assertArrayEquals(expectedDifference, subtract(onDiagnostic, onMatcha), EPSILON,
                "Changing only the background must pass through by one-minus-alpha");
    }

    @Test
    void translucentEdgesHaveNoBlackFringeOrDoubleAttenuation() {
        double spriteAlpha = 0.31;
        double effectiveAlpha = spriteAlpha * OPACITY;

        for (int item : new int[]{LIGHT_ITEM, DARK_ITEM}) {
            for (int background : new int[]{MATCHA_SLOT_BACKGROUND, DIAGNOSTIC_BACKGROUND}) {
                double[] actual = composite(item, spriteAlpha, background);
                assertArrayEquals(
                        straightComposite(item, effectiveAlpha, background),
                        actual,
                        EPSILON,
                        "Premultiplied cached RGB must be attenuated exactly once"
                );

                double[] invalidStraightPipeline = invalidStraightBlendOfPremultipliedCache(
                        item,
                        spriteAlpha,
                        background
                );
                assertTrue(maxDistance(actual, invalidStraightPipeline) > 0.01,
                        "A plain straight-alpha pipeline would darken premultiplied edge texels");

                double[] doubleAttenuated = invalidDoubleAttenuation(
                        item,
                        spriteAlpha,
                        background
                );
                assertTrue(maxDistance(actual, doubleAttenuated) > 0.01,
                        "RGB must not be reduced in the shader and again by straight blending");
            }
        }
    }

    private static double[] composite(int item, double spriteAlpha, int background) {
        double[] straight = rgb(item);
        double[] cachedPremultiplied = scale(straight, spriteAlpha);
        double[] shaderPremultiplied = scale(cachedPremultiplied, OPACITY);
        double outputAlpha = spriteAlpha * OPACITY;
        return add(shaderPremultiplied, scale(rgb(background), 1.0 - outputAlpha));
    }

    private static double[] straightComposite(int item, double alpha, int background) {
        return add(scale(rgb(item), alpha), scale(rgb(background), 1.0 - alpha));
    }

    private static double[] invalidStraightBlendOfPremultipliedCache(
            int item,
            double spriteAlpha,
            int background
    ) {
        double outputAlpha = spriteAlpha * OPACITY;
        return add(
                scale(rgb(item), spriteAlpha * outputAlpha),
                scale(rgb(background), 1.0 - outputAlpha)
        );
    }

    private static double[] invalidDoubleAttenuation(
            int item,
            double spriteAlpha,
            int background
    ) {
        double outputAlpha = spriteAlpha * OPACITY;
        return add(
                scale(rgb(item), spriteAlpha * OPACITY * outputAlpha),
                scale(rgb(background), 1.0 - outputAlpha)
        );
    }

    private static double[] recoverSource(double[] composite, int background, double alpha) {
        return scale(subtract(composite, scale(rgb(background), 1.0 - alpha)), 1.0 / alpha);
    }

    private static double[] rgb(int packed) {
        return new double[]{
                (packed >>> 16 & 0xFF) / 255.0,
                (packed >>> 8 & 0xFF) / 255.0,
                (packed & 0xFF) / 255.0
        };
    }

    private static double[] add(double[] left, double[] right) {
        return new double[]{left[0] + right[0], left[1] + right[1], left[2] + right[2]};
    }

    private static double[] subtract(double[] left, double[] right) {
        return new double[]{left[0] - right[0], left[1] - right[1], left[2] - right[2]};
    }

    private static double[] scale(double[] value, double scale) {
        return new double[]{value[0] * scale, value[1] * scale, value[2] * scale};
    }

    private static double maxDistance(double[] left, double[] right) {
        return Math.max(
                Math.abs(left[0] - right[0]),
                Math.max(Math.abs(left[1] - right[1]), Math.abs(left[2] - right[2]))
        );
    }
}
