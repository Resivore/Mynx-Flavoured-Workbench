package dev.resivore.xaeroemfcompat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.ModelPartUtil;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelPartRenderTrace;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Builds an icon-only bridge between Xaero's canonical head frame and the
 * relocated EMF subtree which actually supplied the traced head geometry.
 *
 * <p>Xaero deliberately treats the rendered subtree and the part used for
 * centering as separate identities. EMF keeps the original vanilla root, so
 * the bridge can retain Xaero's direct-cuboid head frame while rendering only
 * the traced Fresh Animations head through copies of its live ancestor
 * transforms. The live EMF tree is never reparented or otherwise changed.</p>
 */
public final class EmfIconPartResolver {
    static final String EMF_ROOT_CLASS =
            "traben.entity_model_features.models.parts.EMFModelPartRoot";
    private static final String VANILLA_ROOT_FIELD = "vanillaRoot";
    private static final float CUBE_DIMENSION_TOLERANCE = 0.02F;
    private static final float MINIMUM_FRAME_DETERMINANT = 1.0E-6F;
    private static final Map<ModelPart, AdapterMetadata> ADAPTERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ModelPart, ModelPart> CANONICAL_FRAMES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private EmfIconPartResolver() {
    }

    public static Optional<Resolution> resolve(
            ModelPart root,
            ModelPart failedMainPart,
            ModelRenderTrace trace,
            boolean resetHeadRotation
    ) {
        if (!isExactEmfRoot(root)) {
            return Optional.empty();
        }

        ModelPart canonicalHead = unwrapCanonicalPart(failedMainPart);
        ModelPart vanillaRoot = retainedVanillaRoot(root).orElse(null);
        if (canonicalHead == null || vanillaRoot == null) {
            return Optional.empty();
        }
        return resolveRelocatedHead(
                root, canonicalHead, vanillaRoot, trace, resetHeadRotation);
    }

    static Optional<Resolution> resolveRelocatedHead(
            ModelPart root,
            ModelPart canonicalHead,
            ModelPart vanillaRoot,
            ModelRenderTrace trace,
            boolean resetHeadRotation
    ) {
        List<PathNode> canonicalPath = findIdentityPath(root, canonicalHead);
        if (canonicalPath == null || !isDirectCanonicalHeadPath(canonicalPath)
                || ModelPartUtil.hasCubes(canonicalHead)) {
            return Optional.empty();
        }

        ModelPart vanillaCanonicalHead = followPath(vanillaRoot, canonicalPath);
        if (vanillaCanonicalHead == null
                || !ModelPartUtil.hasDirectCubes(vanillaCanonicalHead)) {
            return Optional.empty();
        }

        List<Node> candidates = new ArrayList<>();
        collect(root, "root", List.of(), canonicalHead, trace, candidates);
        Optional<Node> selected = candidates.stream()
                .max(Comparator.comparingInt(Node::score)
                        .thenComparingInt(node -> -node.path().size())
                        .thenComparing(Node::pathText));
        if (selected.isEmpty()) {
            return Optional.empty();
        }

        Node node = selected.orElseThrow();
        ModelPart.Cube canonicalCuboid =
                ModelPartUtil.getBiggestCuboid(vanillaCanonicalHead);
        GeometrySelection geometry = selectCanonicalGeometry(
                node.path(), canonicalCuboid).orElse(null);
        if (geometry == null) {
            return Optional.empty();
        }

        ModelPart adapter = buildAdapter(
                node.part(),
                geometry,
                canonicalHead,
                canonicalCuboid,
                resetHeadRotation
        );
        if (adapter == null) {
            return Optional.empty();
        }
        ModelPart centeringPart = canonicalFrame(
                canonicalHead, ModelPartUtil.getCubes(vanillaCanonicalHead));
        return Optional.of(new Resolution(
                canonicalHead,
                node.part(),
                geometry.part(),
                adapter,
                centeringPart,
                node.color(),
                pathText(canonicalPath),
                node.pathText(),
                pathText(geometry.path())
        ));
    }

    private static void collect(
            ModelPart part,
            String name,
            List<PathNode> parentPath,
            ModelPart excludedSubtree,
            ModelRenderTrace trace,
            List<Node> candidates
    ) {
        if (part == excludedSubtree) {
            return;
        }

        List<PathNode> path = new ArrayList<>(parentPath.size() + 1);
        path.addAll(parentPath);
        path.add(new PathNode(name, part));

        int score = semanticHeadScore(name);
        ModelPartRenderTrace renderInfo = trace.getModelPartRenderInfo(part);
        if (score > 0 && renderInfo != null && ModelPartUtil.hasCubes(part)) {
            candidates.add(new Node(
                    part, renderInfo.color, List.copyOf(path), score, pathText(path)));
        }

        Map<String, ModelPart> children = ModelPartUtil.getChildren(part);
        if (children == null || children.isEmpty()) {
            return;
        }
        children.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> collect(
                        entry.getValue(), entry.getKey(), path,
                        excludedSubtree, trace, candidates));
    }

    private static Optional<GeometrySelection> selectCanonicalGeometry(
            List<PathNode> semanticHeadPath,
            ModelPart.Cube canonicalCuboid
    ) {
        if (canonicalCuboid == null) {
            return Optional.empty();
        }
        for (int index = semanticHeadPath.size() - 1; index >= 1; index--) {
            ModelPart candidate = semanticHeadPath.get(index).part();
            if (index < semanticHeadPath.size() - 1
                    && ModelPartUtil.hasDirectCubes(candidate)) {
                break;
            }
            List<CubeMatch> matches = findMatchingCubes(candidate, canonicalCuboid);
            if (matches.size() > 1) {
                return Optional.empty();
            }
            if (matches.size() == 1) {
                return Optional.of(new GeometrySelection(
                        candidate,
                        List.copyOf(semanticHeadPath.subList(0, index + 1)),
                        matches.getFirst()
                ));
            }
        }
        return Optional.empty();
    }

    private static List<CubeMatch> findMatchingCubes(
            ModelPart part,
            ModelPart.Cube canonicalCuboid
    ) {
        List<ModelPart> path = new ArrayList<>();
        List<CubeMatch> matches = new ArrayList<>();
        collectMatchingCubes(part, canonicalCuboid, path, matches);
        return List.copyOf(matches);
    }

    private static void collectMatchingCubes(
            ModelPart part,
            ModelPart.Cube canonicalCuboid,
            List<ModelPart> path,
            List<CubeMatch> matches
    ) {
        path.add(part);
        List<ModelPart.Cube> cubes = ModelPartUtil.getCubes(part);
        if (cubes != null) {
            for (ModelPart.Cube cube : cubes) {
                if (sameDimensions(cube, canonicalCuboid)) {
                    matches.add(new CubeMatch(cube, List.copyOf(path)));
                }
            }
        }
        Map<String, ModelPart> children = ModelPartUtil.getChildren(part);
        if (children != null) {
            for (Map.Entry<String, ModelPart> entry : children.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).toList()) {
                collectMatchingCubes(
                        entry.getValue(), canonicalCuboid, path, matches);
            }
        }
        path.removeLast();
    }

    private static boolean sameDimensions(ModelPart.Cube left, ModelPart.Cube right) {
        return close(cubeSizeX(left), cubeSizeX(right))
                && close(cubeSizeY(left), cubeSizeY(right))
                && close(cubeSizeZ(left), cubeSizeZ(right));
    }

    private static boolean close(float left, float right) {
        return Math.abs(left - right) <= CUBE_DIMENSION_TOLERANCE;
    }

    private static float cubeSizeX(ModelPart.Cube cube) {
        return Math.abs(cube.maxX - cube.minX);
    }

    private static float cubeSizeY(ModelPart.Cube cube) {
        return Math.abs(cube.maxY - cube.minY);
    }

    private static float cubeSizeZ(ModelPart.Cube cube) {
        return Math.abs(cube.maxZ - cube.minZ);
    }

    private static ModelPart buildAdapter(
            ModelPart tracedHead,
            GeometrySelection geometry,
            ModelPart canonicalHead,
            ModelPart.Cube canonicalCuboid,
            boolean resetHeadRotation
    ) {
        List<PathNode> path = geometry.path();
        ModelPart branch = geometry.part();

        for (int index = path.size() - 2; index >= 1; index--) {
            PathNode ancestor = path.get(index);
            String childName = path.get(index + 1).name();
            branch = transformOnlyCopy(ancestor.part(), childName, branch);
        }

        String firstName = path.get(1).name();
        ModelPart topBranch = branch;
        ModelPart adapter = new ModelPart(List.of(), Map.of(firstName, topBranch));
        adapter.setPos(canonicalHead.x, canonicalHead.y, canonicalHead.z);
        adapter.setInitialPose(adapter.storePose());
        Matrix4f correction = canonicalCorrection(
                geometry,
                canonicalHead,
                canonicalCuboid,
                resetHeadRotation
        );
        if (correction == null) {
            return null;
        }
        ADAPTERS.put(adapter, new AdapterMetadata(tracedHead, topBranch, correction));
        return adapter;
    }

    private static Matrix4f canonicalCorrection(
            GeometrySelection geometry,
            ModelPart canonicalHead,
            ModelPart.Cube canonicalCuboid,
            boolean resetHeadRotation
    ) {
        PoseStack canonicalPose = new PoseStack();
        ModelPart canonicalTransform = new ModelPart(List.of(), Map.of());
        PartPose initialPose = canonicalHead.getInitialPose();
        canonicalTransform.setRotation(
                resetHeadRotation ? initialPose.xRot() : canonicalHead.xRot,
                resetHeadRotation ? initialPose.yRot() : canonicalHead.yRot,
                resetHeadRotation ? initialPose.zRot() : canonicalHead.zRot
        );
        canonicalTransform.xScale = canonicalHead.xScale;
        canonicalTransform.yScale = canonicalHead.yScale;
        canonicalTransform.zScale = canonicalHead.zScale;
        canonicalTransform.translateAndRotate(canonicalPose);
        translateToCubeCenter(canonicalPose, canonicalCuboid);
        Matrix4f canonicalFrame = new Matrix4f(canonicalPose.last().pose());

        PoseStack emfPose = new PoseStack();
        matchingTransformPath(geometry)
                .forEach(part -> part.translateAndRotate(emfPose));
        translateToCubeCenter(emfPose, geometry.match().cube());
        Matrix4f emfFrame = new Matrix4f(emfPose.last().pose());
        if (!isInvertibleFrame(canonicalFrame) || !isInvertibleFrame(emfFrame)) {
            return null;
        }

        Matrix4f correction;
        if (resetHeadRotation) {
            correction = canonicalFrame.mul(emfFrame.invert());
        } else {
            Vector3f canonicalCenter = canonicalFrame.transformPosition(new Vector3f());
            Vector3f emfCenter = emfFrame.transformPosition(new Vector3f());
            correction = new Matrix4f().translation(canonicalCenter.sub(emfCenter));
        }
        return correction.isFinite() ? correction : null;
    }

    private static boolean isInvertibleFrame(Matrix4f frame) {
        float determinant = frame.determinant();
        return frame.isFinite()
                && Float.isFinite(determinant)
                && Math.abs(determinant) >= MINIMUM_FRAME_DETERMINANT;
    }

    private static List<ModelPart> matchingTransformPath(GeometrySelection geometry) {
        List<ModelPart> result = new ArrayList<>();
        for (int index = 1; index < geometry.path().size(); index++) {
            result.add(geometry.path().get(index).part());
        }
        List<ModelPart> ownerPath = geometry.match().ownerPath();
        for (int index = 1; index < ownerPath.size(); index++) {
            result.add(ownerPath.get(index));
        }
        return List.copyOf(result);
    }

    private static void translateToCubeCenter(PoseStack pose, ModelPart.Cube cube) {
        Vector3f center = cubeCenter(cube).div(16.0F);
        pose.translate(center.x, center.y, center.z);
    }

    private static Vector3f cubeCenter(ModelPart.Cube cube) {
        return new Vector3f(
                (cube.minX + cube.maxX) / 2.0F,
                (cube.minY + cube.maxY) / 2.0F,
                (cube.minZ + cube.maxZ) / 2.0F
        );
    }

    private static ModelPart transformOnlyCopy(
            ModelPart source,
            String childName,
            ModelPart child
    ) {
        ModelPart copy = new ModelPart(List.of(), Map.of(childName, child));
        copyCurrentTransform(source, copy);
        copy.visible = source.visible;
        copy.skipDraw = source.skipDraw;
        copy.setInitialPose(copy.storePose());
        return copy;
    }

    private static void copyCurrentTransform(ModelPart source, ModelPart destination) {
        destination.x = source.x;
        destination.y = source.y;
        destination.z = source.z;
        destination.xRot = source.xRot;
        destination.yRot = source.yRot;
        destination.zRot = source.zRot;
        destination.xScale = source.xScale;
        destination.yScale = source.yScale;
        destination.zScale = source.zScale;
    }

    private static List<PathNode> findIdentityPath(ModelPart root, ModelPart target) {
        List<PathNode> result = new ArrayList<>();
        if (findIdentityPath(root, "root", target, result)) {
            return List.copyOf(result);
        }
        return null;
    }

    private static boolean findIdentityPath(
            ModelPart current,
            String name,
            ModelPart target,
            List<PathNode> result
    ) {
        result.add(new PathNode(name, current));
        if (current == target) {
            return true;
        }

        Map<String, ModelPart> children = ModelPartUtil.getChildren(current);
        if (children != null) {
            for (Map.Entry<String, ModelPart> entry : children.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).toList()) {
                if (findIdentityPath(entry.getValue(), entry.getKey(), target, result)) {
                    return true;
                }
            }
        }
        result.removeLast();
        return false;
    }

    private static ModelPart followPath(ModelPart vanillaRoot, List<PathNode> path) {
        ModelPart current = vanillaRoot;
        for (int index = 1; index < path.size(); index++) {
            Map<String, ModelPart> children = ModelPartUtil.getChildren(current);
            if (children == null) {
                return null;
            }
            current = children.get(path.get(index).name());
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static boolean isDirectCanonicalHeadPath(List<PathNode> path) {
        if (path.size() != 2) {
            return false;
        }
        String name = path.getLast().name();
        return name.equals("head") || name.equals("head_parts");
    }

    private static Optional<ModelPart> retainedVanillaRoot(ModelPart root) {
        try {
            Field field = root.getClass().getField(VANILLA_ROOT_FIELD);
            Object value = field.get(root);
            return value instanceof ModelPart modelPart
                    ? Optional.of(modelPart)
                    : Optional.empty();
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    private static ModelPart unwrapCanonicalPart(ModelPart part) {
        ModelPart canonicalPart = CANONICAL_FRAMES.get(part);
        return canonicalPart == null ? part : canonicalPart;
    }

    public static AdapterMetadata adapterMetadata(ModelPart part) {
        return ADAPTERS.get(part);
    }

    /**
     * Renders an adapter through the same canonical-frame correction used by
     * the controlled bounds fixture. Returns false for ordinary model parts.
     */
    public static boolean renderAdapter(
            ModelPart part,
            PoseStack pose,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int color
    ) {
        AdapterMetadata metadata = ADAPTERS.get(part);
        if (metadata == null) {
            return false;
        }
        pose.pushPose();
        try {
            prepareAdapterPose(part, metadata, pose);
            metadata.topBranch().render(
                    pose, vertexConsumer, packedLight, packedOverlay, color);
        } finally {
            pose.popPose();
        }
        return true;
    }

    static boolean getAdapterExtentsForGui(
            ModelPart part,
            PoseStack pose,
            Consumer<Vector3fc> output
    ) {
        AdapterMetadata metadata = ADAPTERS.get(part);
        if (metadata == null) {
            return false;
        }
        pose.pushPose();
        try {
            prepareAdapterPose(part, metadata, pose);
            metadata.topBranch().getExtentsForGui(pose, output);
        } finally {
            pose.popPose();
        }
        return true;
    }

    private static void prepareAdapterPose(
            ModelPart part,
            AdapterMetadata metadata,
            PoseStack pose
    ) {
        part.translateAndRotate(pose);
        pose.mulPose(metadata.canonicalCorrection());
    }

    private static ModelPart canonicalFrame(
            ModelPart canonicalPart,
            List<ModelPart.Cube> vanillaCubes
    ) {
        ModelPart frame = new ModelPart(List.copyOf(vanillaCubes), Map.of());
        copyCurrentTransform(canonicalPart, frame);
        frame.visible = canonicalPart.visible;
        frame.skipDraw = canonicalPart.skipDraw;
        frame.setInitialPose(canonicalPart.getInitialPose());
        CANONICAL_FRAMES.put(frame, canonicalPart);
        return frame;
    }

    private static boolean isExactEmfRoot(ModelPart root) {
        return root != null && EMF_ROOT_CLASS.equals(root.getClass().getName());
    }

    private static int semanticHeadScore(String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        if (name.startsWith("emf_")) {
            name = name.substring(4);
        }
        if (name.equals("head")) {
            return 1_000;
        }
        if (name.matches("head\\d+")) {
            return 990;
        }
        if (name.startsWith("head_")) {
            return 900;
        }
        if (name.endsWith("_head")) {
            return 850;
        }
        return name.contains("head") ? 800 : 0;
    }

    private static String pathText(List<PathNode> path) {
        return path.stream()
                .map(PathNode::name)
                .reduce((left, right) -> left + "/" + right)
                .orElse("root");
    }

    public record Resolution(
            ModelPart canonicalHead,
            ModelPart tracedHead,
            ModelPart geometryRoot,
            ModelPart renderAdapter,
            ModelPart centeringPart,
            int color,
            String canonicalPath,
            String tracedPath,
            String geometryPath
    ) {
    }

    public record AdapterMetadata(
            ModelPart tracedHead,
            ModelPart topBranch,
            Matrix4f canonicalCorrection
    ) {
    }

    private record PathNode(String name, ModelPart part) {
    }

    private record Node(
            ModelPart part,
            int color,
            List<PathNode> path,
            int score,
            String pathText
    ) {
    }

    private record GeometrySelection(
            ModelPart part,
            List<PathNode> path,
            CubeMatch match
    ) {
    }

    private record CubeMatch(ModelPart.Cube cube, List<ModelPart> ownerPath) {
    }
}
