package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RibbitHeadSelectorTest {
    @Test
    void selectsEveryDirectBodyCubeAndPreservesTheExactMainBodyPath() {
        Bone main = bone("main", "main-owned");
        Bone body = main.child("body", "face", "eyes", "mouth");

        var selected = RibbitHeadSelector.select(List.of(main)).orElseThrow();

        assertSame(main, selected.main());
        assertSame(body, selected.body());
        assertEquals(List.of("face", "eyes", "mouth"), selected.cubes());
        assertThrows(UnsupportedOperationException.class,
                () -> selected.cubes().add("late-mutation"));
    }

    @Test
    void directCubeRuleDoesNotPruneByNameCountLocationOrModelVariant() {
        Bone main = bone("main");
        List<String> allDirectCubes = List.of(
                "tiny", "huge", "merchant-clothing-shaped", "chef-extra",
                "farmer-extra", "guard-looking", "left", "right", "high", "low");
        main.child("body", allDirectCubes.toArray(String[]::new));

        assertEquals(allDirectCubes,
                RibbitHeadSelector.select(List.of(main)).orElseThrow().cubes());
    }

    @Test
    void excludesBodyChildrenDescendantsMainCubesAndSiblings() {
        Bone main = bone("main", "main-cube");
        Bone body = main.child("body", "direct-body-a", "direct-body-b");
        Bone arm = body.child("arm", "arm-cube");
        arm.child("held_item", "held-item-cube");
        main.child("umbrella", "umbrella-cube");
        main.child("shield", "shield-cube");

        assertEquals(List.of("direct-body-a", "direct-body-b"),
                RibbitHeadSelector.select(List.of(main)).orElseThrow().cubes());
    }

    @Test
    void requiresExactlyOneTopLevelMainWithExactCase() {
        assertTrue(RibbitHeadSelector.<String>select(List.of()).isEmpty());
        assertTrue(RibbitHeadSelector.select(List.of(bone("Main").childlessRoot())).isEmpty());

        Bone first = bone("main");
        first.child("body", "a");
        Bone second = bone("main");
        second.child("body", "b");
        assertTrue(RibbitHeadSelector.select(List.of(first, second)).isEmpty());

        Bone wrapper = bone("wrapper");
        Bone nestedMain = wrapper.child("main");
        nestedMain.child("body", "nested");
        assertTrue(RibbitHeadSelector.select(List.of(wrapper)).isEmpty());
    }

    @Test
    void requiresExactlyOneDirectBodyChildWithExactCaseAndParentIdentity() {
        Bone noBody = bone("main");
        noBody.child("Body", "wrong-case");
        assertTrue(RibbitHeadSelector.select(List.of(noBody)).isEmpty());

        Bone duplicate = bone("main");
        duplicate.child("body", "one");
        duplicate.child("body", "two");
        assertTrue(RibbitHeadSelector.select(List.of(duplicate)).isEmpty());

        Bone main = bone("main");
        Bone wrapper = main.child("wrapper");
        wrapper.child("body", "too-deep");
        assertTrue(RibbitHeadSelector.select(List.of(main)).isEmpty());

        Bone mismatchedParent = bone("body", "orphan");
        main.children.add(mismatchedParent);
        assertTrue(RibbitHeadSelector.select(List.of(main)).isEmpty());
    }

    @Test
    void emptyOrStructurallyInvalidViewsFailClosed() {
        assertTrue(RibbitHeadSelector.<String>select(null).isEmpty());
        assertTrue(RibbitHeadSelector.<String>select(java.util.Collections.singletonList(null)).isEmpty());

        Bone empty = bone("main");
        empty.child("body");
        assertTrue(RibbitHeadSelector.select(List.of(empty)).isEmpty());

        Bone nullChildren = bone("main");
        nullChildren.children = null;
        assertTrue(RibbitHeadSelector.select(List.of(nullChildren)).isEmpty());

        Bone nullCubesMain = bone("main");
        Bone nullCubesBody = nullCubesMain.child("body", "temporary");
        nullCubesBody.directCubes = null;
        assertTrue(RibbitHeadSelector.select(List.of(nullCubesMain)).isEmpty());
    }

    private static Bone bone(String name, String... cubes) {
        return new Bone(name, cubes);
    }

    private static final class Bone implements RibbitHeadSelector.BoneView<String> {
        private final String name;
        private Bone parent;
        private List<Bone> children = new ArrayList<>();
        private List<String> directCubes;

        private Bone(String name, String... directCubes) {
            this.name = name;
            this.directCubes = new ArrayList<>(List.of(directCubes));
        }

        private Bone child(String name, String... cubes) {
            Bone result = new Bone(name, cubes);
            result.parent = this;
            children.add(result);
            return result;
        }

        private Bone childlessRoot() {
            child("body", "cube");
            return this;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Bone parent() {
            return parent;
        }

        @Override
        public List<Bone> children() {
            return children;
        }

        @Override
        public List<String> directCubes() {
            return directCubes;
        }
    }
}
