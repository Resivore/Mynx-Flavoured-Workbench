package dev.resivore.xaeroemfcompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.ModelPartUtil;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipFile;

import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.cubePart;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.cubePaths;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.emptyPart;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.traced;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Synthetic, non-redistributable equivalents of the effective EMF pack trees. */
class TargetedCompositionTest {
    private static final Path RIBBIT_VILLAGERS =
            Path.of(Objects.requireNonNull(System.getProperty("ribbitVillagersPack")));

    @Test
    void exactRibbitModelsBindOrdinaryAndFiveProfessionNoseContracts() throws Exception {
        Map<String, String> expectedHats = Map.of(
                "villager2.jem", "ribbits_farmer_hat",
                "villager3.jem", "ribbits_chef_hat",
                "villager4.jem", "ribbits_sorcerer_hat",
                "villager5.jem", "ribbits_prospector_hat",
                "villager6.jem", "ribbits_gardener_hat");
        assertEquals(Set.of("nose2", "face", "frog_eyes"),
                directChildIds(ribbitNose("villager.jem")));
        for (Map.Entry<String, String> expected : expectedHats.entrySet()) {
            assertEquals(Set.of("nose2", "face", "frog_eyes", expected.getValue()),
                    directChildIds(ribbitNose(expected.getKey())), expected.getKey());
        }
    }

    @Test
    void exactFreshAnimationsModelsBindTheC11SemanticOwners() throws Exception {
        JsonObject foxHead = direct("fox", "body", "head2");
        assertTrue(RelocatedHeadFailureMechanismTest.directBoxCount(foxHead) > 0);

        JsonObject goatHead = direct("goat", "body", "head2");
        assertEquals(0, RelocatedHeadFailureMechanismTest.directBoxCount(goatHead));
        assertTrue(RelocatedHeadFailureMechanismTest.directBoxCount(
                RelocatedHeadFailureMechanismTest.findDirectSubmodel(goatHead, "snout")) > 0);

        JsonObject frogBody2 = direct("frog", "body", "body2");
        assertTrue(RelocatedHeadFailureMechanismTest.directBoxCount(frogBody2) > 0);
        assertTrue(RelocatedHeadFailureMechanismTest.directBoxCount(
                RelocatedHeadFailureMechanismTest.findDirectSubmodel(frogBody2, "head2")) > 0);

        JsonObject boggedHeadwear = RelocatedHeadFailureMechanismTest.topLevelPart(
                RelocatedHeadFailureMechanismTest.jem("bogged"), "headwear");
        assertTrue(RelocatedHeadFailureMechanismTest.directBoxCount(boggedHeadwear) > 0);
        for (String mushroom : List.of("mushroom1", "mushroom2", "mushroom3")) {
            assertTrue(recursiveBoxCount(RelocatedHeadFailureMechanismTest.findDirectSubmodel(
                    boggedHeadwear, mushroom)) > 0, mushroom);
        }

        JsonObject witchBody = RelocatedHeadFailureMechanismTest.topLevelPart(
                RelocatedHeadFailureMechanismTest.jem("witch"), "body");
        assertTrue(RelocatedHeadFailureMechanismTest.directBoxCount(witchBody) > 0);
        assertTrue(RelocatedHeadFailureMechanismTest.directBoxCount(
                RelocatedHeadFailureMechanismTest.findDirectSubmodel(witchBody, "head2")) > 0);
        RelocatedHeadFailureMechanismTest.findDirectSubmodel(witchBody, "hat");
    }

    @Test
    void foxUsesEffectiveBodyWrapperAndKeepsOnlyHead2WithParentTransforms() {
        ModelPart head2 = cubePart(Map.of(
                "EMF_snout", cubePart(Map.of("EMF_eyes", cubePart(Map.of()))),
                "EMF_left_ear", cubePart(Map.of())));
        ModelPart customBody = cubePart(Map.of(
                "EMF_head2", head2,
                "EMF_body_rotation", cubePart(Map.of()),
                "EMF_tail2", cubePart(Map.of())));
        customBody.yRot = 0.31F;
        ModelPart bodyWrapper = emptyPart(Map.of("EMF_body", customBody));
        bodyWrapper.xRot = -0.22F;
        ModelPart canonical = emptyPart(Map.of());
        ModelPart root = emptyPart(Map.of("head", canonical, "body", bodyWrapper,
                "right_hind_leg", cubePart(Map.of())));

        var result = resolve(root, canonical, head2,
                IconTargetPolicy.select("minecraft:fox", null));
        List<String> paths = cubePaths(result.renderAdapter());
        assertSame(head2, result.tracedHead());
        assertEquals("root/body/EMF_body/EMF_head2", result.geometryPath());
        assertTrue(paths.stream().anyMatch(path -> path.contains("EMF_snout")));
        assertFalse(paths.stream().anyMatch(path -> path.contains("body_rotation")
                || path.contains("tail2") || path.contains("leg")));

        ModelPart copiedWrapper = child(result.renderAdapter(), "body");
        ModelPart copiedBody = child(copiedWrapper, "EMF_body");
        assertEquals(bodyWrapper.xRot, copiedWrapper.xRot);
        assertEquals(customBody.yRot, copiedBody.yRot);
    }

    @Test
    void goatUsesDrawableSnoutAnchorInsideItsEmptyHead2Container() {
        ModelPart snout = cubePart(Map.of("EMF_eyes", cubePart(Map.of()),
                "EMF_mouth", cubePart(Map.of())));
        ModelPart head2 = emptyPart(Map.of(
                "EMF_snout", snout,
                "EMF_goatee", cubePart(Map.of()),
                "EMF_left_horn", cubePart(Map.of())));
        ModelPart customBody = cubePart(Map.of(
                "EMF_head2", head2, "EMF_body_rotation", cubePart(Map.of())));
        ModelPart canonical = emptyPart(Map.of());
        ModelPart root = bodyRoot(canonical, customBody);

        var result = resolve(root, canonical, snout,
                IconTargetPolicy.select("minecraft:goat", null));
        List<String> paths = cubePaths(result.renderAdapter());
        assertSame(snout, result.tracedHead());
        assertEquals("root/body/EMF_body/EMF_head2/EMF_snout", result.tracedPath());
        assertTrue(paths.stream().anyMatch(path -> path.contains("EMF_snout")));
        assertTrue(paths.stream().anyMatch(path -> path.contains("EMF_left_horn")));
        assertFalse(paths.stream().anyMatch(path -> path.contains("body_rotation")));
    }

    @Test
    void frogUsesBody2FaceAndHeadButExcludesCroakTongueAndLimbs() {
        ModelPart head2 = cubePart(Map.of("EMF_eyes", cubePart(Map.of())));
        ModelPart body2 = cubePart(Map.of(
                "EMF_head2", head2,
                "EMF_croak", cubePart(Map.of()),
                "EMF_tongue2", cubePart(Map.of())));
        ModelPart customBody = cubePart(Map.of(
                "EMF_body2", body2, "EMF_left_arm", cubePart(Map.of())));
        ModelPart canonical = emptyPart(Map.of());

        var result = resolve(bodyRoot(canonical, customBody), canonical, head2,
                IconTargetPolicy.select("minecraft:frog", null));
        List<String> paths = cubePaths(result.renderAdapter());
        assertSame(head2, result.tracedHead());
        assertTrue(paths.stream().anyMatch(path -> path.endsWith("/EMF_body2")));
        assertTrue(paths.stream().anyMatch(path -> path.contains("EMF_head2")));
        assertFalse(paths.stream().anyMatch(path -> path.contains("croak")
                || path.contains("tongue") || path.contains("arm")));
    }

    @Test
    void boggedUsesHatMappedHeadwearWithOnlyItsMushroomDescendants() {
        ModelPart headwear = cubePart(Map.of(
                "EMF_mushroom1", cubePart(Map.of()),
                "EMF_mushroom2", cubePart(Map.of()),
                "EMF_mushroom3", cubePart(Map.of())));
        ModelPart canonical = emptyPart(Map.of());
        ModelPart root = emptyPart(Map.of(
                "head", canonical,
                "hat", emptyPart(Map.of("EMF_headwear", headwear)),
                "body", cubePart(Map.of()),
                "right_arm", cubePart(Map.of())));

        var result = resolve(root, canonical, headwear,
                IconTargetPolicy.select("minecraft:bogged", null));
        List<String> paths = cubePaths(result.renderAdapter());
        assertEquals("root/hat/EMF_headwear", result.geometryPath());
        assertTrue(paths.stream().anyMatch(path -> path.contains("EMF_mushroom3")));
        assertFalse(paths.stream().anyMatch(path -> path.contains("body") || path.contains("arm")));
    }

    @Test
    void witchCopiesHeadAndHatButNeverTheDirectTorsoCubes() {
        ModelPart face = cubePart(Map.of("EMF_nose2", cubePart(Map.of())));
        ModelPart hat = emptyPart(Map.of("EMF_hat_tip", cubePart(Map.of())));
        ModelPart customBody = cubePart(Map.of(
                "EMF_head2", face,
                "EMF_hat", hat,
                "EMF_crossed_arms", cubePart(Map.of())));
        ModelPart canonical = emptyPart(Map.of());

        var result = resolve(bodyRoot(canonical, customBody), canonical, face,
                IconTargetPolicy.select("minecraft:witch", null));
        List<String> paths = cubePaths(result.renderAdapter());
        assertTrue(paths.stream().anyMatch(path -> path.contains("EMF_head2")));
        assertTrue(paths.stream().anyMatch(path -> path.contains("EMF_hat_tip")));
        assertFalse(paths.stream().anyMatch(path -> path.endsWith("/EMF_body")
                || path.contains("crossed_arms")));
    }

    @Test
    void eachProfessionUsesNoseFaceEyesAndOnlyItsExactHat() {
        for (String profession : IconTargetPolicy.targetedVillagerProfessions()) {
            var selection = IconTargetPolicy.select("minecraft:villager", profession);
            ModelPart face = emptyPart(Map.of("EMF_mouth", cubePart(Map.of()),
                    "EMF_brows", cubePart(Map.of())));
            ModelPart nose = cubePart(Map.of(
                    "EMF_face", face,
                    "EMF_frog_eyes", cubePart(Map.of()),
                    "EMF_" + selection.expectedHat(),
                    emptyPart(Map.of("hat_cube", cubePart(Map.of()))),
                    "EMF_wrong_hat", cubePart(Map.of()),
                    "EMF_nose2", cubePart(Map.of())));
            ModelPart canonical = emptyPart(Map.of(
                    "nose", emptyPart(Map.of("EMF_nose", nose))));
            ModelPart root = emptyPart(Map.of(
                    "head", canonical, "body", cubePart(Map.of()), "arms", cubePart(Map.of())));

            var result = resolve(root, canonical, nose, selection);
            List<String> paths = cubePaths(result.renderAdapter());
            assertEquals("root/head/nose/EMF_nose", result.geometryPath(), profession);
            assertTrue(paths.stream().anyMatch(path -> path.contains("EMF_face")), profession);
            assertTrue(paths.stream().anyMatch(path -> path.contains("EMF_frog_eyes")), profession);
            assertTrue(paths.stream().anyMatch(path -> path.contains(selection.expectedHat())), profession);
            assertFalse(paths.stream().anyMatch(path -> path.contains("wrong_hat")
                    || path.contains("nose2") || path.contains("body") || path.contains("arms")), profession);
        }
    }

    private static ModelPart bodyRoot(ModelPart canonical, ModelPart customBody) {
        return emptyPart(Map.of(
                "head", canonical,
                "body", emptyPart(Map.of("EMF_body", customBody)),
                "left_leg", cubePart(Map.of())));
    }

    private static ModelPart child(ModelPart parent, String name) {
        return ModelPartUtil.getChildren(parent).get(name);
    }

    private static JsonObject ribbitNose(String file) throws Exception {
        try (ZipFile pack = new ZipFile(RIBBIT_VILLAGERS.toFile())) {
            String path = "assets/minecraft/optifine/cem/" + file;
            var entry = Objects.requireNonNull(pack.getEntry(path), path);
            try (var reader = new InputStreamReader(
                    pack.getInputStream(entry), StandardCharsets.UTF_8)) {
                return RelocatedHeadFailureMechanismTest.topLevelPart(
                        JsonParser.parseReader(reader).getAsJsonObject(), "nose");
            }
        }
    }

    private static JsonObject direct(String entity, String topLevel, String child) throws Exception {
        return RelocatedHeadFailureMechanismTest.findDirectSubmodel(
                RelocatedHeadFailureMechanismTest.topLevelPart(
                        RelocatedHeadFailureMechanismTest.jem(entity), topLevel), child);
    }

    private static Set<String> directChildIds(JsonObject part) {
        return part.getAsJsonArray("submodels").asList().stream()
                .map(element -> element.getAsJsonObject().get("id").getAsString())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static int recursiveBoxCount(JsonObject part) {
        int result = RelocatedHeadFailureMechanismTest.directBoxCount(part);
        if (part.has("submodels")) {
            for (var child : part.getAsJsonArray("submodels")) {
                result += recursiveBoxCount(child.getAsJsonObject());
            }
        }
        return result;
    }

    private static EmfIconPartResolver.Resolution resolve(
            ModelPart root, ModelPart canonical, ModelPart tracedPart,
            IconTargetPolicy.Selection selection) {
        return EmfIconPartResolver.resolveTargetedForFixture(root, canonical,
                emptyPart(Map.of("head", cubePart(Map.of()))),
                traced(tracedPart, 0xFF204060), true, selection).orElseThrow();
    }
}
