package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TransformSafetyTest {
    @Test
    void finiteInvertibleColumnMajorTransformsAreAccepted() {
        assertTrue(TransformSafety.isFiniteAndInvertible(identity()));

        double[] translatedRotatedScaled = {
                0.0, 2.0, 0.0, 0.0,
                -3.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 4.0, 0.0,
                12.5, -7.25, 3.0, 1.0
        };
        assertTrue(TransformSafety.isFiniteAndInvertible(translatedRotatedScaled));
    }

    @Test
    void everyNonFiniteMatrixPositionFailsClosed() {
        for (int index = 0; index < 16; index++) {
            double[] nan = identity();
            nan[index] = Double.NaN;
            assertFalse(TransformSafety.isFiniteAndInvertible(nan), "NaN index " + index);

            double[] positiveInfinity = identity();
            positiveInfinity[index] = Double.POSITIVE_INFINITY;
            assertFalse(TransformSafety.isFiniteAndInvertible(positiveInfinity),
                    "+infinity index " + index);

            double[] negativeInfinity = identity();
            negativeInfinity[index] = Double.NEGATIVE_INFINITY;
            assertFalse(TransformSafety.isFiniteAndInvertible(negativeInfinity),
                    "-infinity index " + index);
        }
    }

    @Test
    void singularAndNearSingularPoseTransformsFailClosed() {
        double[] singular = identity();
        singular[0] = 0.0;
        assertFalse(TransformSafety.isFiniteAndInvertible(singular));

        double[] threshold = identity();
        threshold[0] = 1.0e-9;
        assertFalse(TransformSafety.isFiniteAndInvertible(threshold));

        double[] safelyAboveThreshold = identity();
        safelyAboveThreshold[0] = 1.000_001e-9;
        assertTrue(TransformSafety.isFiniteAndInvertible(safelyAboveThreshold));
    }

    @Test
    void nullOrWrongSizedMatricesFailClosed() {
        assertFalse(TransformSafety.isFiniteAndInvertible(null));
        assertFalse(TransformSafety.isFiniteAndInvertible(new double[0]));
        assertFalse(TransformSafety.isFiniteAndInvertible(new double[15]));
        assertFalse(TransformSafety.isFiniteAndInvertible(new double[17]));
    }

    private static double[] identity() {
        double[] result = new double[16];
        Arrays.fill(result, 0.0);
        result[0] = 1.0;
        result[5] = 1.0;
        result[10] = 1.0;
        result[15] = 1.0;
        return result;
    }
}
