package dev.resivore.naturalistxaeroicons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.ModelPartUtil;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelPartRenderTrace;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

/** Creates a short-lived icon-only ancestry bridge; no live Naturalist part is reparented or changed. */
public final class NaturalistIconAdapter {
    private static final Map<ModelPart, List<ModelPart>> TRACE_PARTS =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private NaturalistIconAdapter() {}

    public static ModelPart build(
            ModelPart modelRoot,
            ModelPart source,
            ModelPart selected,
            ModelPart trace,
            NaturalistModelContracts.Presentation presentation,
            boolean neutralizeRootRotation,
            boolean normalizeSelectedRootTransform,
            List<String> sourcePath,
            boolean preserveAncestorTransforms
    ) {
        List<ModelPart> ancestors = ancestors(modelRoot, source, sourcePath);
        if (ancestors == null) return null;
        ModelPart branch = copySubtree(selected, 0);
        if (normalizeSelectedRootTransform) clearTransform(branch);
        if (preserveAncestorTransforms) for (int i = ancestors.size() - 2; i >= 0; i--) {
            ModelPart parent = ancestors.get(i);
            String childName = sourcePath.get(i);
            ModelPart copy = new ModelPart(List.of(), Map.of(childName, branch));
            copyTransform(parent, copy, neutralizeRootRotation && i == 0);
            copy.visible = parent.visible;
            copy.skipDraw = parent.skipDraw;
            copy.setInitialPose(copy.storePose());
            branch = copy;
        }
        ModelPart adapter = new ModelPart(List.of(), Map.of("naturalist_contract", branch));
        adapter.xScale = presentation.scale();
        adapter.yScale = presentation.scale();
        adapter.zScale = presentation.scale();
        // This is an icon-adapter-local model-space correction; it never changes the live
        // Naturalist renderer.  Great White uses it to expose its lower profile silhouette.
        adapter.y = presentation.frameYOffset();
        adapter.xRot = presentation.xRotation();
        adapter.yRot = presentation.yRotation();
        adapter.zRot = presentation.zRotation();
        adapter.setInitialPose(adapter.storePose());
        List<ModelPart> traceSources = new ArrayList<>();
        traceSources.add(trace);
        traceSources.add(source);
        for (int i = ancestors.size() - 1; i >= 0; i--) traceSources.add(ancestors.get(i));
        TRACE_PARTS.put(adapter, List.copyOf(traceSources));
        return adapter;
    }

    /**
     * Resolves only a bridge created by this class to one of its explicit contract-path traces.
     * A missing, cyclic, or untraced path deliberately has no substitute.
     */
    public static ModelPartRenderTrace resolveTrace(ModelRenderTrace trace, ModelPart adapter) {
        List<ModelPart> originals = TRACE_PARTS.get(adapter);
        if (originals == null) return null;
        for (ModelPart original : originals) {
            if (original == adapter || TRACE_PARTS.containsKey(original)) continue;
            ModelPartRenderTrace resolved = trace.getModelPartRenderInfo(original);
            if (resolved != null) return resolved;
        }
        return null;
    }

    public static boolean traceExists(ModelRenderTrace trace, ModelPart adapter) {
        return resolveTrace(trace, adapter) != null;
    }
    private static List<ModelPart> ancestors(ModelPart root, ModelPart source, List<String> sourcePath) {
        if (sourcePath.size() > 16) return null;
        List<ModelPart> result = new ArrayList<>();
        ModelPart current = root;
        result.add(current);
        for (String segment : sourcePath) {
            if (!current.hasChild(segment)) return null;
            current = current.getChild(segment);
            result.add(current);
        }
        return current == source ? result : null;
    }
    private static ModelPart copySubtree(ModelPart from, int depth) {
        if (depth > 16) throw new IllegalArgumentException("Naturalist contract subtree is too deep");
        Map<String, ModelPart> children = new LinkedHashMap<>();
        Map<String, ModelPart> originalChildren = ModelPartUtil.getChildren(from);
        if (originalChildren != null) originalChildren.forEach((name, child) -> children.put(name, copySubtree(child, depth + 1)));
        List<ModelPart.Cube> cubes = ModelPartUtil.getCubes(from);
        ModelPart copy = new ModelPart(cubes == null ? List.of() : List.copyOf(cubes), children);
        copyTransform(from, copy, false);
        copy.visible = from.visible;
        copy.skipDraw = from.skipDraw;
        copy.setInitialPose(copy.storePose());
        return copy;
    }
    private static void copyTransform(ModelPart from, ModelPart to, boolean neutralizeRotation) {
        to.x = from.x; to.y = from.y; to.z = from.z;
        to.xRot = neutralizeRotation ? 0.0F : from.xRot;
        to.yRot = neutralizeRotation ? 0.0F : from.yRot;
        to.zRot = neutralizeRotation ? 0.0F : from.zRot;
        to.xScale = from.xScale; to.yScale = from.yScale; to.zScale = from.zScale;
    }
    /** Clears only the copied root's gameplay placement; authored child geometry remains intact. */
    private static void clearTransform(ModelPart part) {
        part.x = 0.0F; part.y = 0.0F; part.z = 0.0F;
        part.xRot = 0.0F; part.yRot = 0.0F; part.zRot = 0.0F;
        part.xScale = 1.0F; part.yScale = 1.0F; part.zScale = 1.0F;
        part.setInitialPose(part.storePose());
    }
}
