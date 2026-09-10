package dev.resivore.xaeroemfcompat;

import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.cubePart;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.cubePaths;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.emptyPart;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.traced;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Synthetic, non-redistributable equivalents of the supplied pack structures. */
class TargetedCompositionTest {
    @Test
    void foxAndGoatUseOnlyTheNamedBodyHead2Subtree() {
        for (String entity : List.of("minecraft:fox", "minecraft:goat")) {
            ModelPart head2 = cubePart(Map.of("snout", cubePart(Map.of("eye", cubePart(Map.of()))),
                    "left_ear", cubePart(Map.of()), "right_horn", cubePart(Map.of())));
            ModelPart root = emptyPart(Map.of("head", emptyPart(Map.of()),
                    "body", emptyPart(Map.of("head2", head2, "leg", cubePart(Map.of())))));
            var result = resolve(root, root.getChild("head"), head2,
                    IconTargetPolicy.select(entity, null));
            List<String> paths = cubePaths(result.renderAdapter());
            assertTrue(paths.stream().anyMatch(path -> path.contains("snout")), entity);
            assertFalse(paths.stream().anyMatch(path -> path.contains("leg")), entity);
        }
    }

    @Test
    void frogAndWitchComposeOnlyTheirDeclaredFacialBranches() {
        ModelPart frogHead = cubePart(Map.of("eyes", cubePart(Map.of())));
        ModelPart frogRoot = emptyPart(Map.of("head", emptyPart(Map.of()), "body", emptyPart(Map.of(
                "body2", cubePart(Map.of("head2", frogHead, "arm", cubePart(Map.of())))))));
        var frog = resolve(frogRoot, frogRoot.getChild("head"), frogHead,
                IconTargetPolicy.select("minecraft:frog", null));
        assertTrue(cubePaths(frog.renderAdapter()).stream().anyMatch(path -> path.contains("head2")));
        assertFalse(cubePaths(frog.renderAdapter()).stream().anyMatch(path -> path.contains("arm")));

        ModelPart face = cubePart(Map.of("nose2", cubePart(Map.of())));
        ModelPart witchRoot = emptyPart(Map.of("head", emptyPart(Map.of()), "body", emptyPart(Map.of(
                "head2", face, "hat", cubePart(Map.of("hat_tip", cubePart(Map.of()))),
                "arms", cubePart(Map.of()), "leg", cubePart(Map.of())))));
        var witch = resolve(witchRoot, witchRoot.getChild("head"), face,
                IconTargetPolicy.select("minecraft:witch", null));
        List<String> witchPaths = cubePaths(witch.renderAdapter());
        assertTrue(witchPaths.stream().anyMatch(path -> path.contains("head2")));
        assertTrue(witchPaths.stream().anyMatch(path -> path.contains("hat_tip")));
        assertFalse(witchPaths.stream().anyMatch(path -> path.contains("arms") || path.contains("leg")));
    }

    @Test
    void boggedAndProfessionVillagersUseOnlyBoundedDeclaredGeometry() {
        ModelPart headwear = cubePart(Map.of("mushroom", cubePart(Map.of())));
        ModelPart boggedRoot = emptyPart(Map.of("head", emptyPart(Map.of()), "headwear", headwear,
                "body", cubePart(Map.of())));
        var bogged = resolve(boggedRoot, boggedRoot.getChild("head"), headwear,
                IconTargetPolicy.select("minecraft:bogged", null));
        assertTrue(cubePaths(bogged.renderAdapter()).stream().anyMatch(path -> path.contains("mushroom")));
        assertFalse(cubePaths(bogged.renderAdapter()).stream().anyMatch(path -> path.contains("body")));

        for (String profession : IconTargetPolicy.targetedVillagerProfessions()) {
            var selection = IconTargetPolicy.select("minecraft:villager", profession);
            ModelPart nose = cubePart(Map.of("frog_eyes", cubePart(Map.of()),
                    selection.expectedHat(), cubePart(Map.of("nested_hat", cubePart(Map.of()))),
                    "wrong_hat", cubePart(Map.of()), "arms", cubePart(Map.of())));
            ModelPart root = emptyPart(Map.of("head", emptyPart(Map.of()), "nose", nose));
            var result = resolve(root, root.getChild("head"), nose, selection);
            List<String> paths = cubePaths(result.renderAdapter());
            assertTrue(paths.stream().anyMatch(path -> path.contains(selection.expectedHat())), profession);
            assertFalse(paths.stream().anyMatch(path -> path.contains("wrong_hat") || path.contains("arms")), profession);
        }
    }

    private static EmfIconPartResolver.Resolution resolve(
            ModelPart root, ModelPart canonical, ModelPart tracedPart, IconTargetPolicy.Selection selection) {
        return EmfIconPartResolver.resolveTargetedForFixture(root, canonical,
                emptyPart(Map.of("head", emptyPart(Map.of()))), traced(tracedPart, 0xFF204060), true, selection)
                .orElseThrow();
    }
}
