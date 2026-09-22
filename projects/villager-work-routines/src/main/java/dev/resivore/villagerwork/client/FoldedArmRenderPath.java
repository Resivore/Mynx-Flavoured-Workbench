package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.resivore.villagerwork.mixin.client.ModelPartChildrenAccessor;
import dev.resivore.villagerwork.mixin.client.VillagerModelArmsAccessor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import org.joml.Matrix4f;

/** Replays the live EMF path for the authored {@code arms_rotation} parent. */
final class FoldedArmRenderPath {
    static final String SELECTION_RULE =
            "EMF authored mapping: top-level partToBeAttached=arms, then direct authored id=arms_rotation";
    private static final String ARMS = "arms";
    private static final String ARMS_ROTATION = "arms_rotation";
    private static final String EMF_ID_PREFIX = "EMF_";

    private FoldedArmRenderPath() {
    }

    static Attachment apply(VillagerModel model, VillagerRenderState state, PoseStack poseStack) {
        try {
            ModelPart arms = ((VillagerModelArmsAccessor) model).villagerWork$getArms();
            PathSelection<ModelPart> selection = select(arms, MODEL_PART_VIEW);
            if (!selection.resolved()) return Attachment.failure(selection.failure());

            model.translateToArms(state, poseStack);
            Matrix4f afterTranslateToArms = new Matrix4f(poseStack.last().pose());

            // translateToArms already applies the vanilla root and arms wrapper. EMF attaches its
            // top-level custom part to that wrapper; the authored rod group is then a direct child
            // of the authored arms_rotation part. Replay exactly those two live transforms.
            for (int index = 1; index < selection.steps().size(); index++) {
                selection.steps().get(index).node().translateAndRotate(poseStack);
            }
            Matrix4f afterEffectiveFoldedArms = new Matrix4f(poseStack.last().pose());
            List<PartStep> steps = selection.steps().stream()
                    .map(step -> new PartStep(step.name(), step.node(),
                            MODEL_PART_VIEW.authoredId(step.node()),
                            MODEL_PART_VIEW.attachedPart(step.node())))
                    .toList();
            return new Attachment(true, path(steps), null, steps,
                    afterTranslateToArms, afterEffectiveFoldedArms);
        } catch (RuntimeException | LinkageError error) {
            return Attachment.failure(error.getClass().getName() + ": "
                    + String.valueOf(error.getMessage()));
        }
    }

    /** Package-visible generic seam for focused tests of the inspected EMF mapping contract. */
    static <N> PathSelection<N> select(N root, NodeView<N> view) {
        if (root == null) return PathSelection.failure("arms part is absent");
        if (!view.visible(root)) return PathSelection.failure("arms part is not visible");

        List<NamedNode<N>> attachedArms = view.children(root).stream()
                .filter(step -> view.visible(step.node()))
                .filter(step -> ARMS.equals(view.attachedPart(step.node())))
                .filter(step -> ARMS.equals(view.authoredId(step.node())))
                .toList();
        if (attachedArms.size() != 1) {
            return PathSelection.failure("expected one EMF top-level authored arms part attached to arms, found "
                    + descriptions(attachedArms, view));
        }

        NamedNode<N> authoredArms = attachedArms.getFirst();
        List<NamedNode<N>> authoredParents = view.children(authoredArms.node()).stream()
                .filter(step -> view.visible(step.node()))
                .filter(step -> view.attachedPart(step.node()) == null)
                .filter(step -> ARMS_ROTATION.equals(view.authoredId(step.node())))
                .toList();
        if (authoredParents.size() != 1) {
            return PathSelection.failure("expected one direct authored arms_rotation beneath "
                    + authoredArms.name() + ", found " + descriptions(authoredParents, view));
        }

        return PathSelection.success(List.of(
                new NamedNode<>(ARMS, root), authoredArms, authoredParents.getFirst()));
    }

    private static <N> String descriptions(List<NamedNode<N>> nodes, NodeView<N> view) {
        return nodes.stream().map(node -> node.name() + "{id=" + view.authoredId(node.node())
                + ",attached=" + view.attachedPart(node.node()) + "}").toList().toString();
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
        public String attachedPart(ModelPart part) {
            return EMF_FIELDS.get(part.getClass()).readAttachedPart(part);
        }

        @Override
        public String authoredId(ModelPart part) {
            String runtimeId = EMF_FIELDS.get(part.getClass()).readId(part);
            return runtimeId != null && runtimeId.startsWith(EMF_ID_PREFIX)
                    ? runtimeId.substring(EMF_ID_PREFIX.length()) : null;
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

    /** Public EMF 3.2.6 semantic fields, cached without a compile-time EMF dependency. */
    private static final ClassValue<EmfFields> EMF_FIELDS = new ClassValue<>() {
        @Override
        protected EmfFields computeValue(Class<?> type) {
            return new EmfFields(publicStringField(type, "id"),
                    publicStringField(type, "partToBeAttached"));
        }
    };

    private static Field publicStringField(Class<?> type, String name) {
        try {
            Field field = type.getField(name);
            return field.getType() == String.class ? field : null;
        } catch (NoSuchFieldException | SecurityException ignored) {
            return null;
        }
    }

    private record EmfFields(Field id, Field attachedPart) {
        private String readId(Object owner) {
            return read(id, owner);
        }

        private String readAttachedPart(Object owner) {
            return read(attachedPart, owner);
        }

        private static String read(Field field, Object owner) {
            if (field == null) return null;
            try {
                return (String) field.get(owner);
            } catch (IllegalAccessException | IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    interface NodeView<N> {
        boolean visible(N node);

        String attachedPart(N node);

        String authoredId(N node);

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

    record PartStep(String name, ModelPart part, String authoredId, String attachedPart)
            implements NamedPart {
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
