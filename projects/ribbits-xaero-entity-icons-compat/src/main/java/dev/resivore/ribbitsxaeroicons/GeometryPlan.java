package dev.resivore.ribbitsxaeroicons;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Immutable bind-pose selection and framing derived only from selected direct cubes. */
final class GeometryPlan {
    static final float FRAME_CENTER = 32.0F;
    static final float FRAME_DEPTH = -450.0F;
    static final float FRAME_SPAN = 58.0F;
    private static final double MIN_EXTENT = 1.0e-6;

    private final GeoBone main;
    private final CuboidGeoBone body;
    private final List<GeoCube> cubes;
    private final Bounds bounds;
    private final float fitScale;

    private GeometryPlan(
            GeoBone main,
            CuboidGeoBone body,
            List<GeoCube> cubes,
            Bounds bounds,
            float fitScale) {
        this.main = main;
        this.body = body;
        this.cubes = cubes;
        this.bounds = bounds;
        this.fitScale = fitScale;
    }

    static GeometryPlan create(GeoBone main, CuboidGeoBone body, GeoCube[] directCubes) {
        require(main != null && body != null, "selected bones are null");
        require(body.parent() == main, "body is not a direct child of main");
        require(directCubes != null && directCubes.length > 0, "body has no direct cubes");
        require(main.frameSnapshot == null && body.frameSnapshot == null,
                "selected bones contain transient animation snapshots");
        validateBone(main);
        validateBone(body);

        PoseStack pose = new PoseStack();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        applyBindTransform(pose, main);
        applyBindTransform(pose, body);
        require(TransformSafety.isFiniteAndInvertibleMatrix(pose.last().pose()),
                "main/body bind transform is non-finite or singular");

        MutableBounds bounds = new MutableBounds();
        for (GeoCube cube : directCubes) {
            require(cube != null, "body contains a null direct cube");
            validateCube(cube);
            pose.pushPose();
            try {
                cube.translateToPivotPoint(pose);
                cube.rotate(pose);
                cube.translateAwayFromPivotPoint(pose);
                Matrix4f matrix = pose.last().pose();
                require(TransformSafety.isFiniteAndInvertibleMatrix(matrix),
                        "cube transform is non-finite or singular");
                includeVertices(bounds, cube, matrix);
            } finally {
                pose.popPose();
            }
        }

        Bounds frozen = bounds.freeze();
        double width = frozen.maxX - frozen.minX;
        double height = frozen.maxY - frozen.minY;
        require(Double.isFinite(width) && Double.isFinite(height)
                        && width > MIN_EXTENT && height > MIN_EXTENT,
                "selected direct-cube bounds are empty or degenerate");
        double scale = FRAME_SPAN / Math.max(width, height);
        require(Double.isFinite(scale) && scale > 0.0 && scale <= Float.MAX_VALUE,
                "derived frame scale is invalid");

        return new GeometryPlan(
                main, body, List.copyOf(Arrays.asList(directCubes)), frozen, (float) scale);
    }

    void applyFraming(PoseStack pose, float xaeroScale) {
        require(Float.isFinite(xaeroScale) && xaeroScale > 0.0F,
                "Xaero form scale is invalid");
        float boundedXaeroScale = Math.min(1.0F, xaeroScale);
        float scale = fitScale * boundedXaeroScale;
        pose.translate(FRAME_CENTER, FRAME_CENTER, FRAME_DEPTH);
        pose.scale(scale, scale, -scale);
        pose.translate(-bounds.centerX(), -bounds.centerY(), -bounds.centerZ());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        applyBindTransform(pose, main);
        applyBindTransform(pose, body);
        require(TransformSafety.isFiniteAndInvertibleMatrix(pose.last().pose()),
                "framed main/body transform is non-finite or singular");
    }

    List<GeoCube> cubes() {
        return cubes;
    }

    private static void applyBindTransform(PoseStack pose, GeoBone bone) {
        require(bone.frameSnapshot == null, "animation snapshot appeared during icon capture");
        bone.translateToPivotPoint(pose);
        if (bone.baseRotZ() != 0.0F) {
            pose.mulPose(Axis.ZP.rotation(bone.baseRotZ()));
        }
        if (bone.baseRotY() != 0.0F) {
            pose.mulPose(Axis.YP.rotation(bone.baseRotY()));
        }
        if (bone.baseRotX() != 0.0F) {
            pose.mulPose(Axis.XP.rotation(bone.baseRotX()));
        }
        bone.translateAwayFromPivotPoint(pose);
    }

    private static void validateBone(GeoBone bone) {
        require(finite(
                        bone.pivotX(), bone.pivotY(), bone.pivotZ(),
                        bone.baseRotX(), bone.baseRotY(), bone.baseRotZ()),
                "bone transform contains a non-finite value");
    }

    private static void validateCube(GeoCube cube) {
        Vec3 pivot = cube.pivot();
        Vec3 rotation = cube.rotation();
        Vec3 size = cube.size();
        require(pivot != null && rotation != null && size != null,
                "cube transform metadata is absent");
        require(finite(
                        pivot.x, pivot.y, pivot.z,
                        rotation.x, rotation.y, rotation.z,
                        size.x, size.y, size.z),
                "cube metadata contains a non-finite value");
        require(cube.quads() != null && cube.quads().length > 0,
                "cube has no renderable quads");
    }

    private static void includeVertices(MutableBounds bounds, GeoCube cube, Matrix4f matrix) {
        boolean found = false;
        for (GeoQuad quad : cube.quads()) {
            require(quad != null, "cube contains a null quad");
            require(finite(quad.normalX(), quad.normalY(), quad.normalZ()),
                    "quad normal contains a non-finite value");
            GeoVertex[] vertices = quad.vertices();
            require(vertices != null && vertices.length > 0, "quad has no vertices");
            for (GeoVertex vertex : vertices) {
                require(vertex != null && finite(
                                vertex.posX(), vertex.posY(), vertex.posZ(),
                                vertex.texU(), vertex.texV()),
                        "vertex contains a missing or non-finite value");
                Vector4f transformed = matrix.transform(new Vector4f(
                        vertex.posX(), vertex.posY(), vertex.posZ(), 1.0F));
                require(finite(transformed.x, transformed.y, transformed.z, transformed.w)
                                && Math.abs(transformed.w) > MIN_EXTENT,
                        "transformed vertex is non-finite or projectively singular");
                bounds.include(
                        transformed.x / transformed.w,
                        transformed.y / transformed.w,
                        transformed.z / transformed.w);
                found = true;
            }
        }
        require(found, "cube has no renderable vertices");
    }

    private static boolean finite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private record Bounds(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        double centerX() {
            return (minX + maxX) * 0.5;
        }

        double centerY() {
            return (minY + maxY) * 0.5;
        }

        double centerZ() {
            return (minZ + maxZ) * 0.5;
        }
    }

    private static final class MutableBounds {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        void include(double x, double y, double z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        Bounds freeze() {
            require(finite(minX, minY, minZ, maxX, maxY, maxZ),
                    "selected direct-cube bounds are non-finite");
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
