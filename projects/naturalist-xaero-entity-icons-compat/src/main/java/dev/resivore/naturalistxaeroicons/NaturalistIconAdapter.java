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
    private static final Map<ModelPart, ModelPart> TRACE_PARTS =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private NaturalistIconAdapter() {}

    public static ModelPart build(ModelPart modelRoot, ModelPart selected) {
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
        adapter.setInitialPose(adapter.storePose());
        TRACE_PARTS.put(adapter, selected);
        return adapter;
    }

    public static ModelPart tracePart(ModelPart adapter) { return TRACE_PARTS.get(adapter); }

    /**
     * Resolves only a bridge created by this class to its original traced part.
     * A missing, cyclic, or untraced mapping deliberately has no substitute.
     */
    public static ModelPartRenderTrace resolveTrace(ModelRenderTrace trace, ModelPart adapter) {
        ModelPart original = tracePart(adapter);
        if (original == null || original == adapter || tracePart(original) != null) return null;
        return trace.getModelPartRenderInfo(original);
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
