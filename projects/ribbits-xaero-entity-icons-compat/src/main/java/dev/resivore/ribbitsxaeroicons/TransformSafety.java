package dev.resivore.ribbitsxaeroicons;

import org.joml.Matrix4fc;

/** Finite, nonsingular affine-transform checks used before icon framing or drawing. */
public final class TransformSafety {
    private static final double MIN_ABSOLUTE_DETERMINANT = 1.0e-9;

    private TransformSafety() {
    }

    public static boolean isFiniteAndInvertible(double[] matrix) {
        if (matrix == null || matrix.length != 16) {
            return false;
        }
        for (double value : matrix) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }

        double determinant =
                matrix[0] * (matrix[5] * matrix[10] - matrix[6] * matrix[9])
                        - matrix[4] * (matrix[1] * matrix[10] - matrix[2] * matrix[9])
                        + matrix[8] * (matrix[1] * matrix[6] - matrix[2] * matrix[5]);
        return Double.isFinite(determinant)
                && Math.abs(determinant) > MIN_ABSOLUTE_DETERMINANT;
    }

    public static boolean isFiniteAndInvertibleMatrix(Matrix4fc matrix) {
        if (matrix == null) {
            return false;
        }
        return isFiniteAndInvertible(new double[] {
                matrix.m00(), matrix.m01(), matrix.m02(), matrix.m03(),
                matrix.m10(), matrix.m11(), matrix.m12(), matrix.m13(),
                matrix.m20(), matrix.m21(), matrix.m22(), matrix.m23(),
                matrix.m30(), matrix.m31(), matrix.m32(), matrix.m33()
        });
    }
}
