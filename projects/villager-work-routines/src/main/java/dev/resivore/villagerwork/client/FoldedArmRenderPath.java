package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.resivore.villagerwork.mixin.client.ModelPartChildrenAccessor;
import dev.resivore.villagerwork.mixin.client.VillagerModelArmsAccessor;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import org.joml.Matrix4f;

/**
 * Replays the effective live {@link ModelPart} path used to draw the visible folded arms.
 *
 * <p>Minecraft and EMF render a part by applying its transform, drawing its direct cubes, and
 * then recursively rendering its children. The authored rod group is a child of the first part
 * on the folded-arm branch that owns visible cubes. Wrapper names are deliberately irrelevant:
 * this resolver follows the unique contributing branch and stops at that shallowest direct-cube
 * owner. Ambiguous or non-rendering trees fail closed instead of guessing an attachment.</p>
 */
final class FoldedArmRenderPath {
    static final String SELECTION_RULE =
            "unique visible child branch to shallowest non-skipDraw direct-cube owner";
    private static final int MAX_DEPTH = 16;
    private static final int MAX_PARTS = 128;

    private FoldedArmRenderPath() {
    }

    static Attachment apply(VillagerModel model, VillagerRenderState state, PoseStack poseStack) {
        try {
            ModelPart arms = ((VillagerModelArmsAccessor) model).villagerWork$getArms();
            PathSelection<ModelPart> selection = select(arms, MODEL_PART_VIEW);
            if (!selection.resolved()) return Attachment.failure(selection.failure());

            model.translateToArms(state, poseStack);
            Matrix4f afterTranslateToArms = new Matrix4f(poseStack.last().pose());

            // translateToArms already applies the root and outer arms part. Replay only the
            // selected descendants, exactly as ModelPart/EMF recursive rendering does.
            for (int index = 1; index < selection.steps().size(); index++) {
                selection.steps().get(index).node().translateAndRotate(poseStack);
            }
            Matrix4f afterEffectiveFoldedArms = new Matrix4f(poseStack.last().pose());
            List<PartStep> steps = selection.steps().stream()
                    .map(step -> new PartStep(step.name(), step.node()))
                    .toList();
            return new Attachment(true, path(steps), null, steps,
                    afterTranslateToArms, afterEffectiveFoldedArms);
        } catch (RuntimeException | LinkageError error) {
            return Attachment.failure(error.getClass().getName() + ": "
                    + String.valueOf(error.getMessage()));
        }
    }

    /** Package-visible generic seam for focused structural tests. */
    static <N> PathSelection<N> select(N root, NodeView<N> view) {
        if (root == null) return PathSelection.failure("arms part is absent");

        List<NamedNode<N>> steps = new ArrayList<>();
        steps.add(new NamedNode<>("arms", root));
        IdentityHashMap<N, Boolean> pathVisited = new IdentityHashMap<>();
        N current = root;

        for (int depth = 0; depth <= MAX_DEPTH; depth++) {
            if (pathVisited.put(current, Boolean.TRUE) != null) {
                return PathSelection.failure("cycle in folded-arm render path");
            }
            if (!view.visible(current)) {
                return PathSelection.failure("selected folded-arm part is not visible");
            }
            if (!view.skipDraw(current) && view.hasDirectGeometry(current)) {
                return PathSelection.success(steps);
            }
            if (depth == MAX_DEPTH) {
                return PathSelection.failure("folded-arm render path exceeds depth " + MAX_DEPTH);
            }

            List<NamedNode<N>> contributing = new ArrayList<>();
            int[] inspectedParts = {0};
            for (NamedNode<N> child : view.children(current)) {
                GeometryProbe probe = probeGeometry(child.node(), view,
                        new IdentityHashMap<>(), 0, inspectedParts);
                if (probe.failure() != null) {
                    return PathSelection.failure("invalid folded-arm subtree " + child.name()
                            + " beneath " + path(steps) + ": " + probe.failure());
                }
                if (probe.contributes()) contributing.add(child);
            }
            contributing.sort((left, right) -> left.name().compareTo(right.name()));
            if (contributing.size() != 1) {
                return PathSelection.failure("expected one contributing child beneath "
                        + path(steps) + " but found " + names(contributing));
            }
            NamedNode<N> next = contributing.getFirst();
            steps.add(next);
            current = next.node();
        }
        return PathSelection.failure("folded-arm render path was not resolved");
    }

    private static <N> GeometryProbe probeGeometry(N node, NodeView<N> view,
                                                    IdentityHashMap<N, Boolean> visited,
                                                    int depth, int[] count) {
        if (node == null) return GeometryProbe.invalid("null model part");
        if (depth > MAX_DEPTH) return GeometryProbe.invalid("depth exceeds " + MAX_DEPTH);
        if (count[0] >= MAX_PARTS) return GeometryProbe.invalid("part count exceeds " + MAX_PARTS);
        if (visited.put(node, Boolean.TRUE) != null) return GeometryProbe.invalid("cycle detected");
        count[0]++;
        if (!view.visible(node)) return GeometryProbe.none();
        if (!view.skipDraw(node) && view.hasDirectGeometry(node)) return GeometryProbe.found();
        boolean contributes = false;
        for (NamedNode<N> child : view.children(node)) {
            GeometryProbe childProbe = probeGeometry(child.node(), view, visited, depth + 1, count);
            if (childProbe.failure() != null) return childProbe;
            contributes |= childProbe.contributes();
        }
        return contributes ? GeometryProbe.found() : GeometryProbe.none();
    }

    private static String names(List<? extends NamedPart> nodes) {
        return nodes.stream().map(NamedPart::name).toList().toString();
    }

    private static String path(List<? extends NamedPart> steps) {
        StringJoiner path = new StringJoiner("/");
        for (NamedPart step : steps) path.add(step.name());
        return path.toString();
    }

    private static final NodeView<ModelPart> MODEL_PART_VIEW = new NodeView<>() {
        @Override
        public boolean visible(ModelPart part) {
            return part.visible;
        }

        @Override
        public boolean skipDraw(ModelPart part) {
            return part.skipDraw;
        }

        @Override
        public boolean hasDirectGeometry(ModelPart part) {
            return !part.isEmpty();
        }

        @Override
        public List<NamedNode<ModelPart>> children(ModelPart part) {
            Map<String, ModelPart> children =
                    ((ModelPartChildrenAccessor) (Object) part).villagerWork$getChildren();
            return children.entrySet().stream()
                    .map(entry -> new NamedNode<>(entry.getKey(), entry.getValue()))
                    .toList();
        }
    };

    interface NodeView<N> {
        boolean visible(N node);

        boolean skipDraw(N node);

        boolean hasDirectGeometry(N node);

        List<NamedNode<N>> children(N node);
    }

    record NamedNode<N>(String name, N node) implements NamedPart {
    }

    record PathSelection<N>(boolean resolved, List<NamedNode<N>> steps, String failure) {
        private static <N> PathSelection<N> success(List<NamedNode<N>> steps) {
            return new PathSelection<>(true, List.copyOf(steps), null);
        }

        private static <N> PathSelection<N> failure(String failure) {
            return new PathSelection<>(false, List.of(), failure);
        }
    }

    private record GeometryProbe(boolean contributes, String failure) {
        private static GeometryProbe found() {
            return new GeometryProbe(true, null);
        }

        private static GeometryProbe none() {
            return new GeometryProbe(false, null);
        }

        private static GeometryProbe invalid(String failure) {
            return new GeometryProbe(false, failure);
        }
    }

    record PartStep(String name, ModelPart part) implements NamedPart {
    }

    private interface NamedPart {
        String name();
    }

    record Attachment(boolean applied, String path, String failure, List<PartStep> steps,
                      Matrix4f afterTranslateToArms, Matrix4f afterEffectiveFoldedArms) {
        private static Attachment failure(String failure) {
            return new Attachment(false, "<unresolved>", failure, List.of(), null, null);
        }
    }
}
