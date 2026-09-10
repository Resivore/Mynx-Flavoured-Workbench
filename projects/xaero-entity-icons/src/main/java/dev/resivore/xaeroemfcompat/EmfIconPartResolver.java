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
        return resolve(root, failedMainPart, trace, resetHeadRotation, IconTargetPolicy.Selection.NONE);
    }

    /**
     * Runs C11's exact, fail-closed model contracts before the unchanged C9
     * selector.  A non-target never reaches this branch.
     */
    public static Optional<Resolution> resolve(
            ModelPart root,
            ModelPart failedMainPart,
            ModelRenderTrace trace,
            boolean resetHeadRotation,
            IconTargetPolicy.Selection target
    ) {
        if (!isSupportedEmfRoot(root)) {
            return reject("NON_EMF_ROOT", root == null ? "null" : root.getClass().getName());
        }

        ModelPart canonicalHead = unwrapCanonicalPart(failedMainPart);
        // EMFModelPartVanilla is itself the retained vanilla tree.  Some CEM
        // variants use it as the model root rather than wrapping it in
        // EMFModelPartRoot, so requiring the wrapper incorrectly excluded
        // frog, allay, and vex before the selector could even run.
        ModelPart vanillaRoot = retainedVanillaRoot(root).orElse(root);
        if (canonicalHead == null) return reject("MISSING_CANONICAL_PART", "root");
        if (vanillaRoot == null) return reject("MISSING_RETAINED_VANILLA_ROOT", "root");
        if (target != null && target.isCompositionTarget()) {
            Optional<Resolution> targeted = resolveTargeted(
                    root, canonicalHead, vanillaRoot, trace, resetHeadRotation, target);
            if (targeted.isPresent()) return targeted;
            IconDiagnostics.event("TARGETED_CONTRACT_FALLBACK_C9", target.composition().name());
        }
        return resolveRelocatedHead(
                root, canonicalHead, vanillaRoot, trace, resetHeadRotation);
    }

    /** C11 plans name only the verified effective EMF/Fresh Animations contracts. */
    private static Optional<Resolution> resolveTargeted(
            ModelPart root,
            ModelPart canonicalHead,
            ModelPart vanillaRoot,
            ModelRenderTrace trace,
            boolean resetHeadRotation,
            IconTargetPolicy.Selection target
    ) {
        TargetedPlan plan = switch (target.composition()) {
            case FOX -> foxPlan(root);
            case GOAT -> goatPlan(root);
            case BOGGED -> boggedPlan(root);
            case FROG -> frogPlan(root);
            case WITCH -> witchPlan(root);
            case VILLAGER -> villagerPlan(root, target.expectedHat());
            case NONE -> null;
        };
        if (plan == null || plan.anchor() == null) {
            return reject("TARGETED_EXPECTED_STRUCTURE_ABSENT", target.composition().name());
        }
        ModelPartRenderTrace renderInfo = trace.getModelPartRenderInfo(plan.tracedPart());
        if (renderInfo == null) {
            return reject("TARGETED_SUBTREE_UNTRACED", target.composition().name());
        }
        List<PathNode> canonicalPath = findIdentityPath(root, canonicalHead);
        if (canonicalPath == null) return reject("TARGETED_CANONICAL_PATH_ABSENT", target.composition().name());
        ModelPart vanillaCanonical = followPath(vanillaRoot, canonicalPath);
        ModelPart vanillaGeometry = vanillaCanonical == null ? null : uniqueGeometryOwner(vanillaCanonical);
        ModelPart.Cube canonicalCube = vanillaGeometry == null
                ? plan.anchor().cube() : ModelPartUtil.getBiggestCuboid(vanillaGeometry);
        if (canonicalCube == null) return reject("TARGETED_MISSING_FRAME", target.composition().name());

        GeometrySelection geometry = new GeometrySelection(
                plan.source(), plan.path(), new CubeMatch(plan.anchor().cube(), plan.anchor().ownerPath()));
        ModelPart adapter = buildTargetedAdapter(plan.detached(), plan.tracedPart(), geometry,
                canonicalHead, canonicalCube, resetHeadRotation);
        if (adapter == null) return reject("TARGETED_INVALID_OR_SINGULAR_TRANSFORM", target.composition().name());
        List<ModelPart.Cube> frameCubes = vanillaGeometry == null
                ? ModelPartUtil.getCubes(plan.anchor().ownerPath().getLast())
                : ModelPartUtil.getCubes(vanillaGeometry);
        ModelPart centeringPart = canonicalFrame(canonicalHead, frameCubes);
        IconDiagnostics.event("TARGETED_RESOLVED_" + target.composition().name(),
                plan.pathText() + " anchor=" + plan.anchorPathText());
        return Optional.of(new Resolution(canonicalHead, plan.tracedPart(), plan.source(), adapter,
                centeringPart, renderInfo.color, "targeted", plan.anchorPathText(), plan.pathText()));
    }

    /** Package-visible synthetic-fixture seam; production enters through {@link #resolve}. */
    static Optional<Resolution> resolveTargetedForFixture(
            ModelPart root, ModelPart canonicalHead, ModelPart vanillaRoot, ModelRenderTrace trace,
            boolean resetHeadRotation, IconTargetPolicy.Selection target) {
        return resolveTargeted(root, canonicalHead, vanillaRoot, trace, resetHeadRotation, target);
    }

    private static TargetedPlan foxPlan(ModelPart root) {
        List<PathNode> path = namedPath(root, "body", "body", "head2");
        if (path == null) return null;
        ModelPart source = path.getLast().part();
        CubeAnchor anchor = directCubeAnchor(source, List.of(source));
        return anchor == null ? null : new TargetedPlan(
                path, source, detachAll(source), source, anchor, pathText(path));
    }

    private static TargetedPlan goatPlan(ModelPart root) {
        List<PathNode> path = namedPath(root, "body", "body", "head2");
        if (path == null) return null;
        ModelPart source = path.getLast().part();
        NamedPart snout = namedChildEntry(source, "snout");
        if (snout == null) return null;
        CubeAnchor anchor = directCubeAnchor(snout.part(), List.of(source, snout.part()));
        return anchor == null ? null : new TargetedPlan(
                path, source, detachAll(source), snout.part(), anchor,
                pathText(path) + "/" + snout.name());
    }

    private static TargetedPlan boggedPlan(ModelPart root) {
        List<PathNode> path = namedPath(root, "hat", "headwear");
        if (path == null) return null;
        ModelPart source = path.getLast().part();
        CubeAnchor anchor = directCubeAnchor(source, List.of(source));
        return anchor == null ? null : new TargetedPlan(
                path, source, detachAll(source), source, anchor, pathText(path));
    }

    private static TargetedPlan frogPlan(ModelPart root) {
        List<PathNode> path = namedPath(root, "body", "body", "body2");
        if (path == null) return null;
        ModelPart body2 = path.getLast().part();
        NamedPart head2 = namedChildEntry(body2, "head2");
        if (head2 == null) return null;
        ModelPart detached = detach(body2, true, Map.of(head2.name(), detachAll(head2.part())));
        CubeAnchor anchor = directCubeAnchor(head2.part(), List.of(body2, head2.part()));
        return anchor == null ? null : new TargetedPlan(
                path, body2, detached, head2.part(), anchor,
                pathText(path) + "/" + head2.name());
    }

    private static TargetedPlan witchPlan(ModelPart root) {
        List<PathNode> path = namedPath(root, "body", "body");
        if (path == null) return null;
        ModelPart body = path.getLast().part();
        NamedPart face = namedChildEntry(body, "head2");
        NamedPart hat = namedChildEntry(body, "hat");
        if (face == null || hat == null) return null;
        ModelPart detached = detach(body, false, Map.of(
                face.name(), detachAll(face.part()), hat.name(), detachAll(hat.part())));
        CubeAnchor faceAnchor = directCubeAnchor(face.part(), List.of(body, face.part()));
        if (faceAnchor == null) return null;
        return new TargetedPlan(path, body, detached, face.part(), faceAnchor,
                pathText(path) + "/" + face.name());
    }

    private static TargetedPlan villagerPlan(ModelPart root, String expectedHat) {
        if (expectedHat == null) return null;
        List<PathNode> path = namedPath(root, "head", "nose", "nose");
        if (path == null) return null;
        ModelPart nose = path.getLast().part();
        NamedPart face = namedChildEntry(nose, "face");
        NamedPart eyes = namedChildEntry(nose, "frog_eyes");
        NamedPart hat = namedChildEntry(nose, expectedHat);
        if (face == null || eyes == null || hat == null) return null;
        ModelPart detached = detach(nose, true, Map.of(
                face.name(), detachAll(face.part()),
                eyes.name(), detachAll(eyes.part()),
                hat.name(), detachAll(hat.part())));
        CubeAnchor anchor = directCubeAnchor(nose, List.of(nose));
        return anchor == null ? null : new TargetedPlan(
                path, nose, detached, nose, anchor, pathText(path));
    }

    private static ModelPart buildTargetedAdapter(
            ModelPart detached,
            ModelPart tracedPart,
            GeometrySelection geometry,
            ModelPart canonicalHead,
            ModelPart.Cube canonicalCuboid,
            boolean resetHeadRotation
    ) {
        List<PathNode> path = geometry.path();
        ModelPart branch = detached;
        for (int index = path.size() - 2; index >= 1; index--) {
            PathNode ancestor = path.get(index);
            branch = transformOnlyCopy(ancestor.part(), path.get(index + 1).name(), branch);
        }
        ModelPart topBranch = branch;
        ModelPart adapter = new ModelPart(List.of(), Map.of(path.get(1).name(), topBranch));
        adapter.setPos(canonicalHead.x, canonicalHead.y, canonicalHead.z);
        adapter.setInitialPose(adapter.storePose());
        Matrix4f correction = canonicalCorrection(geometry, canonicalHead, canonicalCuboid, resetHeadRotation);
        if (correction == null) return null;
        ADAPTERS.put(adapter, new AdapterMetadata(tracedPart, topBranch, correction));
        return adapter;
    }

    private static List<PathNode> namedPath(ModelPart root, String... names) {
        List<PathNode> path = new ArrayList<>();
        path.add(new PathNode("root", root));
        ModelPart current = root;
        for (String name : names) {
            NamedPart child = namedChildEntry(current, name);
            if (child == null) return null;
            current = child.part();
            path.add(new PathNode(child.name(), current));
        }
        return List.copyOf(path);
    }

    /** Exact contract name, accepting EMF's implementation prefix only. */
    private static ModelPart namedChild(ModelPart parent, String name) {
        NamedPart child = namedChildEntry(parent, name);
        return child == null ? null : child.part();
    }

    private static NamedPart namedChildEntry(ModelPart parent, String name) {
        Map<String, ModelPart> children = ModelPartUtil.getChildren(parent);
        if (children == null) return null;
        ModelPart exact = children.get(name);
        if (exact != null) return new NamedPart(name, exact);
        String emfName = "EMF_" + name;
        ModelPart emf = children.get(emfName);
        return emf == null ? null : new NamedPart(emfName, emf);
    }

    private static ModelPart detachAll(ModelPart source) {
        Map<String, ModelPart> children = new java.util.LinkedHashMap<>();
        Map<String, ModelPart> sourceChildren = ModelPartUtil.getChildren(source);
        if (sourceChildren != null) sourceChildren.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> children.put(entry.getKey(), detachAll(entry.getValue())));
        return detach(source, true, children);
    }

    private static ModelPart detach(
            ModelPart source, boolean includeDirectCubes, Map<String, ModelPart> children) {
        List<ModelPart.Cube> cubes = includeDirectCubes
                ? List.copyOf(ModelPartUtil.getCubes(source)) : List.of();
        ModelPart copy = new ModelPart(cubes, children);
        copyCurrentTransform(source, copy);
        copy.visible = source.visible;
        copy.skipDraw = false;
        copy.setInitialPose(source.getInitialPose());
        return copy;
    }

    private static CubeAnchor directCubeAnchor(ModelPart owner, List<ModelPart> ownerPath) {
        ModelPart.Cube cube = ModelPartUtil.getBiggestCuboid(owner);
        return cube == null ? null : new CubeAnchor(cube, List.copyOf(ownerPath));
    }

    static Optional<Resolution> resolveRelocatedHead(
            ModelPart root,
            ModelPart canonicalHead,
            ModelPart vanillaRoot,
            ModelRenderTrace trace,
            boolean resetHeadRotation
    ) {
        List<PathNode> canonicalPath = findIdentityPath(root, canonicalHead);
        if (canonicalPath == null) return reject("MISSING_CANONICAL_PATH", "root");
        if (canonicalPath.size() < 2 || canonicalPath.size() > 16)
            return reject("UNSUPPORTED_CANONICAL_PATH", pathText(canonicalPath));
        String canonicalName = canonicalPath.getLast().name();
        if (!canonicalName.equals("head") && !canonicalName.equals("head_parts"))
            return reject("UNSUPPORTED_CANONICAL_NAME", pathText(canonicalPath));
        ModelPart vanillaCanonicalHead = followPath(vanillaRoot, canonicalPath);
        if (vanillaCanonicalHead == null) {
            // Non-attached JEM replacements can relocate the live semantic
            // head without preserving the corresponding canonical path in
            // EMF's retained vanilla tree.  Do not accept that fact by
            // itself: it only permits the already-bounded traced-head
            // selector below to prove one safe replacement.
            IconDiagnostics.event("RETAINED_CANONICAL_PATH_ABSENT", pathText(canonicalPath));
        } else {
            IconDiagnostics.event("RETAINED_CANONICAL_PATH_FOUND", pathText(canonicalPath));
        }
        // A transform-only canonical head may own its geometry through one child path.
        ModelPart vanillaGeometry = vanillaCanonicalHead == null
                ? null
                : uniqueGeometryOwner(vanillaCanonicalHead);

        // Xaero's ordinary path can still leave the destination empty when a
        // canonical head is an EMF part with direct cubes.  Render a detached
        // vanilla ModelPart copy of just that semantic head (and explicitly
        // named headwear branches) rather than asking Xaero to render the live
        // EMF implementation again.  This is deliberately not a recursive
        // descendant fallback: body, arms, accessories, and sibling parts are
        // never copied.
        if (vanillaGeometry != null && hasCanonicalHeadGeometry(canonicalHead)) {
            return resolveCanonicalGeometry(
                    canonicalHead, vanillaGeometry, trace, pathText(canonicalPath));
        }

        List<Node> candidates = new ArrayList<>();
        collect(root, "root", List.of(), canonicalHead, trace, candidates);
        Optional<Node> selected = candidates.stream()
                .max(Comparator.comparingInt(Node::score)
                        .thenComparingInt(node -> -node.path().size())
                        .thenComparing(Node::pathText));
        if (selected.isEmpty()) return reject("NO_TRACED_HEAD_CANDIDATE", pathText(canonicalPath));
        int bestScore = selected.orElseThrow().score();
        if (candidates.stream().filter(n -> n.score() == bestScore).count() != 1)
            return reject("MULTIPLE_AMBIGUOUS_CANDIDATES", pathText(canonicalPath));

        Node node = selected.orElseThrow();
        if (vanillaGeometry == null) {
            // This is not a broader geometry search: use only the already
            // selected direct semantic head as the reference frame.  C8
            // covered an existing retained path whose geometry was absent;
            // C9 also covers a retained path that cannot be followed at all.
            return resolveWithoutRetainedVanillaGeometry(
                    node,
                    canonicalHead,
                    resetHeadRotation,
                    canonicalPath,
                    vanillaCanonicalHead == null
                            ? "TRACED_HEAD_NO_RETAINED_PATH_FALLBACK"
                            : "TRACED_HEAD_FRAME_FALLBACK"
            );
        }
        ModelPart.Cube canonicalCuboid =
                ModelPartUtil.getBiggestCuboid(vanillaGeometry);
        GeometrySelection geometry = selectCanonicalGeometry(
                node.path(), canonicalCuboid).orElse(null);
        if (geometry == null) return Optional.empty();

        ModelPart adapter = buildAdapter(
                node.part(),
                geometry,
                canonicalHead,
                canonicalCuboid,
                resetHeadRotation
        );
        if (adapter == null) return reject("INVALID_OR_SINGULAR_TRANSFORM", node.pathText());
        ModelPart centeringPart = canonicalFrame(
                canonicalHead, ModelPartUtil.getCubes(vanillaGeometry));
        IconDiagnostics.event("RESOLVED", pathText(canonicalPath) + " -> " + pathText(geometry.path()));
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

    private static Optional<Resolution> resolveCanonicalGeometry(
            ModelPart canonicalHead,
            ModelPart vanillaGeometry,
            ModelRenderTrace trace,
            String canonicalPath
    ) {
        ModelPartRenderTrace renderInfo = trace.getModelPartRenderInfo(canonicalHead);
        if (renderInfo == null) {
            return reject("UNTRACED_CANONICAL_HEAD", canonicalPath);
        }
        ModelPart detachedHead = detachedCanonicalHead(canonicalHead);
        if (detachedHead == null || !ModelPartUtil.hasCubes(detachedHead)) {
            return reject("EMPTY_CANONICAL_HEAD_COPY", canonicalPath);
        }
        ModelPart adapter = new ModelPart(List.of(), Map.of("head", detachedHead));
        adapter.setInitialPose(adapter.storePose());
        ADAPTERS.put(adapter, new AdapterMetadata(
                canonicalHead, detachedHead, new Matrix4f().identity()));
        ModelPart centeringPart = canonicalFrame(
                canonicalHead, ModelPartUtil.getCubes(vanillaGeometry));
        IconDiagnostics.event("RESOLVED_CANONICAL_GEOMETRY", canonicalPath);
        return Optional.of(new Resolution(
                canonicalHead,
                canonicalHead,
                canonicalHead,
                adapter,
                centeringPart,
                renderInfo.color,
                canonicalPath,
                canonicalPath,
                canonicalPath
        ));
    }

    private static Optional<Resolution> resolveWithoutRetainedVanillaGeometry(
            Node node,
            ModelPart canonicalHead,
            boolean resetHeadRotation,
            List<PathNode> canonicalPath,
            String fallbackStage
    ) {
        ModelPart.Cube headCube = ModelPartUtil.getBiggestCuboid(node.part());
        if (headCube == null) {
            return reject("EMPTY_TRACED_HEAD_GEOMETRY", node.pathText());
        }
        GeometrySelection geometry = new GeometrySelection(
                node.part(), node.path(), new CubeMatch(headCube, List.of(node.part())));
        ModelPart adapter = buildAdapter(
                node.part(), geometry, canonicalHead, headCube, resetHeadRotation);
        if (adapter == null) {
            return reject("INVALID_OR_SINGULAR_TRANSFORM", node.pathText());
        }
        ModelPart centeringPart = canonicalFrame(
                canonicalHead, ModelPartUtil.getCubes(node.part()));
        IconDiagnostics.event(fallbackStage,
                pathText(canonicalPath) + " -> " + node.pathText());
        return Optional.of(new Resolution(
                canonicalHead,
                node.part(),
                node.part(),
                adapter,
                centeringPart,
                node.color(),
                pathText(canonicalPath),
                node.pathText(),
                node.pathText()
        ));
    }

    private static ModelPart detachedCanonicalHead(ModelPart source) {
        Map<String, ModelPart> children = new java.util.LinkedHashMap<>();
        Map<String, ModelPart> sourceChildren = ModelPartUtil.getChildren(source);
        if (sourceChildren != null) {
            sourceChildren.entrySet().stream()
                    .filter(entry -> isHeadAttachedAnchor(entry.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> children.put(entry.getKey(),
                            detachedHeadwearBranch(entry.getValue())));
        }
        ModelPart copy = new ModelPart(List.copyOf(ModelPartUtil.getCubes(source)), children);
        copyCurrentTransform(source, copy);
        // This copy is handed to Xaero as the selected, traced head.  EMF
        // commonly leaves the canonical part as a traversal container and
        // sets skipDraw while its own renderer compiles the cubes.  Retaining
        // that flag on a plain ModelPart makes Xaero accept the adapter but
        // submit no head vertices.  The original part is not changed; named
        // headwear descendants still keep their individual visibility state.
        copy.visible = true;
        copy.skipDraw = false;
        copy.setInitialPose(source.getInitialPose());
        return copy;
    }

    private static ModelPart detachedHeadwearBranch(ModelPart source) {
        Map<String, ModelPart> children = new java.util.LinkedHashMap<>();
        Map<String, ModelPart> sourceChildren = ModelPartUtil.getChildren(source);
        if (sourceChildren != null) {
            sourceChildren.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> children.put(entry.getKey(),
                            detachedHeadwearBranch(entry.getValue())));
        }
        ModelPart copy = new ModelPart(List.copyOf(ModelPartUtil.getCubes(source)), children);
        copyCurrentTransform(source, copy);
        copy.visible = source.visible;
        // EMF's skipDraw marks a traversal implementation boundary, not a
        // request to hide semantic headwear.  A plain detached ModelPart must
        // render through an empty transformed owner to reach its nested cubes.
        // Visibility remains the authoritative semantic visibility state.
        copy.skipDraw = false;
        copy.setInitialPose(source.getInitialPose());
        return copy;
    }

    private static boolean isHeadAttachedAnchor(String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        if (name.startsWith("emf_")) {
            name = name.substring(4);
        }
        return name.equals("hat") || name.equals("headwear")
                || name.startsWith("hat_") || name.startsWith("headwear_")
                // Vanilla's facial attachment points are part of the head
                // boundary.  The active hatted-villager layouts attach their
                // hat below nose, not below a generic headwear sibling.
                || name.equals("nose") || name.equals("snout")
                || name.equals("beak") || name.equals("muzzle")
                || name.equals("ear") || name.equals("ears")
                || name.startsWith("ear_") || name.startsWith("ear")
                || name.equals("horn") || name.equals("horns")
                || name.startsWith("horn_");
    }

    private static boolean hasCanonicalHeadGeometry(ModelPart canonicalHead) {
        if (ModelPartUtil.hasDirectCubes(canonicalHead)) {
            return true;
        }
        Map<String, ModelPart> children = ModelPartUtil.getChildren(canonicalHead);
        return children != null && children.entrySet().stream()
                .filter(entry -> isHeadAttachedAnchor(entry.getKey()))
                .anyMatch(entry -> ModelPartUtil.hasCubes(entry.getValue()));
    }

    private static <T> Optional<T> reject(String reason, String path) {
        IconDiagnostics.event(reason, path);
        return Optional.empty();
    }

    private static ModelPart uniqueGeometryOwner(ModelPart root) {
        if (ModelPartUtil.hasDirectCubes(root)) return root;
        Map<String, ModelPart> children = ModelPartUtil.getChildren(root);
        if (children == null) return null;
        List<ModelPart> populated = children.values().stream().filter(ModelPartUtil::hasCubes).toList();
        if (populated.size() != 1) return null;
        ModelPart child = populated.getFirst();
        // Do not silently discard an intermediate transform when selecting reference geometry.
        if (child.x != 0 || child.y != 0 || child.z != 0 || child.xRot != 0 || child.yRot != 0
                || child.zRot != 0 || child.xScale != 1 || child.yScale != 1 || child.zScale != 1) return null;
        return uniqueGeometryOwner(child);
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
        if (canonicalCuboid == null) return reject("MISSING_RETAINED_VANILLA_GEOMETRY", pathText(semanticHeadPath));
        for (int index = semanticHeadPath.size() - 1; index >= 1; index--) {
            ModelPart candidate = semanticHeadPath.get(index).part();
            if (index < semanticHeadPath.size() - 1
                    && ModelPartUtil.hasDirectCubes(candidate)) {
                break;
            }
            List<CubeMatch> matches = findMatchingCubes(candidate, canonicalCuboid);
            if (matches.size() > 1) return reject("MULTIPLE_MATCHING_GEOMETRY_REGIONS", pathText(semanticHeadPath));
            if (matches.size() == 1) {
                return Optional.of(new GeometrySelection(
                        candidate,
                        List.copyOf(semanticHeadPath.subList(0, index + 1)),
                        matches.getFirst()
                ));
            }
        }
        // Resource packs may intentionally reshape a head, so identical cube
        // dimensions are not an ownership requirement.  A uniquely traced
        // semantic head with direct cubes remains a safe, bounded owner.  Do
        // not search siblings or arbitrary descendants for a substitute.
        ModelPart semanticHead = semanticHeadPath.getLast().part();
        if (!hasCubeBearingAncestor(semanticHeadPath)
                && ModelPartUtil.hasDirectCubes(semanticHead)) {
            ModelPart.Cube headCuboid = ModelPartUtil.getBiggestCuboid(semanticHead);
            if (headCuboid != null) {
                IconDiagnostics.event("SEMANTIC_HEAD_GEOMETRY_FALLBACK", pathText(semanticHeadPath));
                return Optional.of(new GeometrySelection(
                        semanticHead,
                        List.copyOf(semanticHeadPath),
                        new CubeMatch(headCuboid, List.of(semanticHead))
                ));
            }
        }
        return reject("NO_MATCHING_GEOMETRY", pathText(semanticHeadPath));
    }

    private static boolean hasCubeBearingAncestor(List<PathNode> path) {
        for (int index = 1; index < path.size() - 1; index++) {
            if (ModelPartUtil.hasDirectCubes(path.get(index).part())) {
                return true;
            }
        }
        return false;
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
            // EMFModelPartVanilla has a public getRoot() which leads back to
            // its EMFModelPartRoot.  Its own class has no vanillaRoot field,
            // so use that published relationship instead of treating the
            // selected leaf as an independent vanilla tree.
            try {
                Object emfRoot = root.getClass().getMethod("getRoot").invoke(root);
                if (emfRoot == null) {
                    return Optional.empty();
                }
                Field field = emfRoot.getClass().getField(VANILLA_ROOT_FIELD);
                Object value = field.get(emfRoot);
                return value instanceof ModelPart modelPart
                        ? Optional.of(modelPart)
                        : Optional.empty();
            } catch (ReflectiveOperationException ignoredAgain) {
                return Optional.empty();
            }
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

    /** True only for the two public EMF root families handled by this bridge. */
    public static boolean isSupportedEmfRoot(ModelPart root) {
        if (root == null) {
            return false;
        }
        String name = root.getClass().getName();
        return EMF_ROOT_CLASS.equals(name)
                || name.equals("traben.entity_model_features.models.parts.EMFModelPartVanilla");
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

    private record NamedPart(String name, ModelPart part) {
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

    private record CubeAnchor(ModelPart.Cube cube, List<ModelPart> ownerPath) {
    }

    private record TargetedPlan(
            List<PathNode> path,
            ModelPart source,
            ModelPart detached,
            ModelPart tracedPart,
            CubeAnchor anchor,
            String anchorPathText
    ) {
        String pathText() { return EmfIconPartResolver.pathText(path); }
    }
}
