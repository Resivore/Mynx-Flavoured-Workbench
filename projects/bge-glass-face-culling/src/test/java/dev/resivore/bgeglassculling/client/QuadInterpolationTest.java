package dev.resivore.bgeglassculling.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class QuadInterpolationTest {
    @Test
    void bilinearCropPreservesOriginalUvOrientation() {
        assertEquals(4.0F, QuadInterpolation.bilinear(0, 8, 0, 8, 0.5F, 0.25F));
        assertEquals(6.0F, QuadInterpolation.bilinear(8, 0, 8, 0, 0.25F, 0.75F));
        assertEquals(12.0F, QuadInterpolation.bilinear(0, 0, 16, 16, 0.4F, 0.75F));
    }

    @Test
    void vertexColorChannelsAreCroppedIndependently() {
        int value = QuadInterpolation.packedBytes(0x00000000, 0x000000ff,
                0x0000ff00, 0x00ffff00, 0.5F, 0.5F);
        assertEquals(0x00408040, value);
    }

    @Test
    void packedLightCoordinatesRemainProportional() {
        int value = QuadInterpolation.packedShorts(0x00000000, 0x00000010,
                0x00100000, 0x00100010, 0.25F, 0.75F);
        assertEquals(0x000c0004, value);
    }
}
