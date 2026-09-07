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

    public static ModelPart build(ModelPart modelRoot, ModelPart selected) {
        return build(modelRoot, selected, new NaturalistModelContracts.Presentation(1.0F, 0.0F, 0.0F, 0.0F));
    }

    public static ModelPart build(
            ModelPart modelRoot,
            ModelPart selected,
            NaturalistModelContracts.Presentation presentation
    ) {
        List<Node> path = new ArrayList<>();
        if (!find(modelRoot, "root", selected, path) || path.size() > 16) return null;
        ModelPart branch = selected;
        for (int i = path.size() - 2; i >= 0; i--) {
            Node parent = path.get(i);
            String childName = path.get(i + 1).name();
            ModelPart copy = new ModelPart(List.of(), Map.of(childName, branch));
            copyTransform(parent.part(), copy);
            copy.visible = parent.part().visible;
            copy.skipDraw = parent.part().skipDraw;
            copy.setInitialPose(copy.storePose());
            branch = copy;
        }
        ModelPart adapter = new ModelPart(List.of(), Map.of("naturalist_contract", branch));
        adapter.xScale = presentation.scale();
        adapter.yScale = presentation.scale();
        adapter.zScale = presentation.scale();
        adapter.xRot = presentation.xRotation();
        adapter.yRot = presentation.yRotation();
        adapter.zRot = presentation.zRotation();
        adapter.setInitialPose(adapter.storePose());
        List<ModelPart> traceSources = new ArrayList<>();
        for (int i = path.size() - 1; i >= 0; i--) traceSources.add(path.get(i).part());
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
    private static void copyTransform(ModelPart from, ModelPart to) {
        to.x = from.x; to.y = from.y; to.z = from.z;
        to.xRot = from.xRot; to.yRot = from.yRot; to.zRot = from.zRot;
        to.xScale = from.xScale; to.yScale = from.yScale; to.zScale = from.zScale;
    }
    private static boolean find(ModelPart current, String name, ModelPart target, List<Node> out) {
        out.add(new Node(name, current));
        if (current == target) return true;
        Map<String, ModelPart> children = ModelPartUtil.getChildren(current);
        if (children != null) for (var entry : new LinkedHashMap<>(children).entrySet()) if (find(entry.getValue(), entry.getKey(), target, out)) return true;
        out.removeLast();
        return false;
    }
    private record Node(String name, ModelPart part) {}
}
