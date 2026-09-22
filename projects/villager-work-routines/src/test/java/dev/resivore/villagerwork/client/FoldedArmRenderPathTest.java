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
        @Override public String attachedPart(Node node) { return node.attachedPart; }
        @Override public String authoredId(Node node) { return node.authoredId; }
        @Override public List<FoldedArmRenderPath.NamedNode<Node>> children(Node node) {
            return List.copyOf(node.children);
        }
    };

    @Test
    void resolvesAuthoredParentByEmfMetadataNotMapNamesOrCubeDepth() {
        Node flipped = node("arms_flipped", null, true);
        Node authoredParent = node("arms_rotation", null, true,
                child("generated_deeper_key", flipped));
        Node authoredArms = node("arms", "arms", true,
                child("unrelated_rotation_key", authoredParent));
        Node arms = node(null, null, true,
                child("unrelated_top_level_key", authoredArms));

        FoldedArmRenderPath.PathSelection<Node> selection = FoldedArmRenderPath.select(arms, VIEW);

        assertTrue(selection.resolved());
        assertEquals(List.of("arms", "unrelated_top_level_key", "unrelated_rotation_key"),
                selection.steps().stream().map(FoldedArmRenderPath.NamedNode::name).toList());
        assertFalse(selection.steps().stream()
                .anyMatch(step -> step.name().equals("generated_deeper_key")),
                "arms_flipped is the authored grip group's sibling, not its parent");
    }

    @Test
    void ignoresTemptingDeepMatchBecauseAuthoredParentMustBeDirect() {
        Node deepRotation = node("arms_rotation", null, true);
        Node flipped = node("arms_flipped", null, true, child("tempting", deepRotation));
        Node authoredArms = node("arms", "arms", true, child("flipped", flipped));
        Node arms = node(null, null, true, child("custom", authoredArms));

        FoldedArmRenderPath.PathSelection<Node> selection = FoldedArmRenderPath.select(arms, VIEW);

        assertFalse(selection.resolved());
        assertTrue(selection.failure().contains("direct authored arms_rotation"));
    }

    @Test
    void missingWrongInvisibleAndAmbiguousMetadataFailClosed() {
        Node validRotation = node("arms_rotation", null, true);
        Node missingTopMetadata = node(null, null, true, child("rotation", validRotation));
        Node wrongAttachment = node("arms", "head", true, child("rotation", validRotation));
        Node invisibleTop = node("arms", "arms", false, child("rotation", validRotation));
        Node first = node("arms", "arms", true, child("rotation", validRotation));
        Node second = node("arms", "arms", true, child("rotation", validRotation));

        assertFalse(FoldedArmRenderPath.select(
                node(null, null, true, child("missing", missingTopMetadata)), VIEW).resolved());
        assertFalse(FoldedArmRenderPath.select(
                node(null, null, true, child("wrong", wrongAttachment)), VIEW).resolved());
        assertFalse(FoldedArmRenderPath.select(
                node(null, null, true, child("hidden", invisibleTop)), VIEW).resolved());
        assertFalse(FoldedArmRenderPath.select(node(null, null, true,
                child("one", first), child("two", second)), VIEW).resolved());
    }

    @Test
    void duplicateOrAttachedNestedParentFailsClosed() {
        Node firstRotation = node("arms_rotation", null, true);
        Node secondRotation = node("arms_rotation", null, true);
        Node duplicate = node("arms", "arms", true,
                child("one", firstRotation), child("two", secondRotation));
        Node wronglyAttached = node("arms", "arms", true,
                child("rotation", node("arms_rotation", "arms", true)));

        assertFalse(FoldedArmRenderPath.select(
                node(null, null, true, child("custom", duplicate)), VIEW).resolved());
        assertFalse(FoldedArmRenderPath.select(
                node(null, null, true, child("custom", wronglyAttached)), VIEW).resolved());
    }

    private static Node node(String authoredId, String attachedPart, boolean visible,
                             FoldedArmRenderPath.NamedNode<Node>... children) {
        Node node = new Node(authoredId, attachedPart, visible);
        node.children.addAll(List.of(children));
        return node;
    }

    private static FoldedArmRenderPath.NamedNode<Node> child(String name, Node node) {
        return new FoldedArmRenderPath.NamedNode<>(name, node);
    }

    private static final class Node {
        private final String authoredId;
        private final String attachedPart;
        private final boolean visible;
        private final List<FoldedArmRenderPath.NamedNode<Node>> children = new ArrayList<>();

        private Node(String authoredId, String attachedPart, boolean visible) {
            this.authoredId = authoredId;
            this.attachedPart = attachedPart;
            this.visible = visible;
        }
    }
}
