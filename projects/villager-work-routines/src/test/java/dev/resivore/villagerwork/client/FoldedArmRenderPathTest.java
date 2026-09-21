package dev.resivore.villagerwork.client;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoldedArmRenderPathTest {
    private static final FoldedArmRenderPath.NodeView<Node> VIEW = new FoldedArmRenderPath.NodeView<>() {
        @Override public boolean visible(Node node) { return node.visible; }
        @Override public boolean skipDraw(Node node) { return node.skipDraw; }
        @Override public boolean hasDirectGeometry(Node node) { return node.directGeometry; }
        @Override public List<FoldedArmRenderPath.NamedNode<Node>> children(Node node) {
            return List.copyOf(node.children);
        }
    };

    @Test
    void arbitraryWrapperNamesResolveToShallowestDirectGeometryOwner() {
        Node flipped = node(true, false, true);
        Node folded = node(true, false, true,
                child("generated_deeper_branch", flipped));
        Node wrapper = node(true, false, false,
                child("anything_runtime_generated", folded));
        Node arms = node(true, false, false,
                child("unrelated_wrapper_spelling", wrapper));

        FoldedArmRenderPath.PathSelection<Node> selection = FoldedArmRenderPath.select(arms, VIEW);

        assertTrue(selection.resolved());
        assertEquals(List.of("arms", "unrelated_wrapper_spelling",
                        "anything_runtime_generated"),
                selection.steps().stream().map(FoldedArmRenderPath.NamedNode::name).toList());
        assertFalse(selection.steps().stream()
                .anyMatch(step -> step.name().equals("generated_deeper_branch")),
                "the authored group is a sibling of the deeper flipped-arm branch");
    }

    @Test
    void skipDrawSuppressesOnlyCurrentCubesAndContinuesToRenderableChild() {
        Node visibleChild = node(true, false, true);
        Node skippedWrapper = node(true, true, true, child("live_child", visibleChild));
        Node arms = node(true, false, false, child("wrapper", skippedWrapper));

        FoldedArmRenderPath.PathSelection<Node> selection = FoldedArmRenderPath.select(arms, VIEW);

        assertTrue(selection.resolved());
        assertEquals("live_child", selection.steps().getLast().name());
    }

    @Test
    void ambiguityMissingGeometryAndInvisibleBranchesFailClosed() {
        Node ambiguous = node(true, false, false,
                child("left", node(true, false, true)),
                child("right", node(true, false, true)));
        Node missing = node(true, false, false);
        Node hidden = node(true, false, false,
                child("hidden", node(false, false, true)));

        assertFalse(FoldedArmRenderPath.select(ambiguous, VIEW).resolved());
        assertFalse(FoldedArmRenderPath.select(missing, VIEW).resolved());
        assertFalse(FoldedArmRenderPath.select(hidden, VIEW).resolved());
    }

    @Test
    void cyclesNeverBecomeAnAttachmentGuess() {
        Node arms = node(true, false, false);
        Node wrapper = node(true, false, false);
        arms.children.add(child("wrapper", wrapper));
        wrapper.children.add(child("back_to_arms", arms));

        assertFalse(FoldedArmRenderPath.select(arms, VIEW).resolved());
    }

    @Test
    void malformedSiblingCannotBeIgnoredInFavorOfAConvenientBranch() {
        Node malformed = node(true, false, false);
        malformed.children.add(child("self", malformed));
        Node arms = node(true, false, false,
                child("valid", node(true, false, true)),
                child("malformed", malformed));

        FoldedArmRenderPath.PathSelection<Node> selection =
                FoldedArmRenderPath.select(arms, VIEW);

        assertFalse(selection.resolved());
        assertTrue(selection.failure().contains("cycle detected"));
    }

    @Test
    void overDeepSiblingCannotBeIgnoredInFavorOfAConvenientBranch() {
        Node tooDeep = node(true, false, false);
        Node cursor = tooDeep;
        for (int index = 0; index < 18; index++) {
            Node next = node(true, false, false);
            cursor.children.add(child("wrapper_" + index, next));
            cursor = next;
        }
        cursor.directGeometry = true;
        Node arms = node(true, false, false,
                child("valid", node(true, false, true)),
                child("too_deep", tooDeep));

        FoldedArmRenderPath.PathSelection<Node> selection =
                FoldedArmRenderPath.select(arms, VIEW);

        assertFalse(selection.resolved());
        assertTrue(selection.failure().contains("depth exceeds"));
    }

    private static Node node(boolean visible, boolean skipDraw, boolean directGeometry,
                             FoldedArmRenderPath.NamedNode<Node>... children) {
        Node node = new Node(visible, skipDraw, directGeometry);
        node.children.addAll(List.of(children));
        return node;
    }

    private static FoldedArmRenderPath.NamedNode<Node> child(String name, Node node) {
        return new FoldedArmRenderPath.NamedNode<>(name, node);
    }

    private static final class Node {
        private final boolean visible;
        private final boolean skipDraw;
        private boolean directGeometry;
        private final List<FoldedArmRenderPath.NamedNode<Node>> children = new ArrayList<>();

        private Node(boolean visible, boolean skipDraw, boolean directGeometry) {
            this.visible = visible;
            this.skipDraw = skipDraw;
            this.directGeometry = directGeometry;
        }
    }
}
