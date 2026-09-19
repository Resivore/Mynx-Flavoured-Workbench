package dev.resivore.bgeglassculling.client;

/** Attribute interpolation used when a rectangular quad is geometrically cropped. */
final class QuadInterpolation {
    private QuadInterpolation() {}

    static float bilinear(float lowLow, float highLow, float lowHigh, float highHigh,
            float u, float v) {
        float low = lowLow + (highLow - lowLow) * u;
        float high = lowHigh + (highHigh - lowHigh) * u;
        return low + (high - low) * v;
    }

    static int packedBytes(int lowLow, int highLow, int lowHigh, int highHigh,
            float u, float v) {
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int channel = Math.round(bilinear((lowLow >>> shift) & 0xff,
                    (highLow >>> shift) & 0xff, (lowHigh >>> shift) & 0xff,
                    (highHigh >>> shift) & 0xff, u, v));
            result |= (channel & 0xff) << shift;
        }
        return result;
    }

    static int packedShorts(int lowLow, int highLow, int lowHigh, int highHigh,
            float u, float v) {
        int low = Math.round(bilinear(lowLow & 0xffff, highLow & 0xffff,
                lowHigh & 0xffff, highHigh & 0xffff, u, v));
        int high = Math.round(bilinear((lowLow >>> 16) & 0xffff,
                (highLow >>> 16) & 0xffff, (lowHigh >>> 16) & 0xffff,
                (highHigh >>> 16) & 0xffff, u, v));
        return (low & 0xffff) | (high & 0xffff) << 16;
    }
}
