package dev.resivore.naturalistxaeroicons;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;

class NaturalistCoverageTest {
    private static final List<String> EXPECTED = List.of(
            "rhino", "lion", "elephant", "mammoth", "zebra", "giraffe", "hippo", "vulture", "boar", "dragonfly",
            "anglerfish", "ray", "blobfish", "piranha", "alligator", "bass", "lizard", "lizard_tail", "tortoise", "duck",
            "starfish", "clam", "giant_isopod", "jellyfish", "whale", "mole", "rat", "black_bear", "tiger", "komodo_dragon",
            "ostrich", "desert_scorpion", "jungle_scorpion", "great_white_shark", "turkey", "capybara", "hedgehog");

    @Test void exactTargetRosterIsClosedAndComplete() {
        assertEquals(EXPECTED.stream().sorted().toList(), NaturalistModelContracts.targetIds());
        assertEquals(37, NaturalistModelContracts.targetIds().size());
        assertFalse(NaturalistModelContracts.isTargetId("duck_egg"));
        assertFalse(NaturalistModelContracts.isTargetId("dirt_trail"));
    }

    @Test void knownWorkingControlsAreNotClaimed() {
        for (String id : List.of("bear", "bird", "butterfly", "catfish", "caterpillar", "crab", "deer", "firefly", "snake", "snail")) {
            assertTrue(NaturalistModelContracts.isNativeControlId(id));
            assertFalse(NaturalistModelContracts.isTargetId(id));
        }
    }

    @Test void formerlyMissingModelsUseTheirAuditedC8Paths() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("anglerfish", "AnglerfishModel:root/body"), Map.entry("ray", "RayModel:body"),
                Map.entry("piranha", "PiranhaModel:body"), Map.entry("bass", "BassModel:body"),
                Map.entry("hedgehog", "HedgehogModel:unrolled/body"), Map.entry("lizard", "LizardModel:body/skullRot/neck"),
                Map.entry("giraffe", "GiraffeModel:hips/shoulders/body/neck/head"), Map.entry("jellyfish", "JellyfishModel:body"),
                Map.entry("giant_isopod", "GiantIsopodModel:body"), Map.entry("tortoise", "TortoiseModel:body/skullRot/neck"),
                Map.entry("vulture", "VultureModel:neck"), Map.entry("mole", "MoleModel:root/body/skull"));
        expected.forEach((id, contract) -> assertTrue(NaturalistModelContracts.contractsForId(id).stream()
                .anyMatch(value -> (value.modelClass().substring(value.modelClass().lastIndexOf('.') + 1)
                + ":" + String.join("/", value.path())).equals(contract)), id));
    }

    @Test void auditedC8SourceDefinesEveryFormerlyMissingContractPath() throws Exception {
        Path naturalistModels = Path.of(System.getProperty("projectRoot")).getParent()
                .resolve("naturalist/common/src/main/java/com/crispytwig/naturalist/client/model");
        for (String id : List.of("anglerfish", "ray", "piranha", "bass", "hedgehog", "lizard", "giraffe", "jellyfish",
                "giant_isopod", "tortoise", "vulture", "mole")) {
            for (NaturalistModelContracts.Contract contract : NaturalistModelContracts.contractsForId(id)) {
                String simpleName = contract.modelClass().substring(contract.modelClass().lastIndexOf('.') + 1);
                String source = Files.readString(naturalistModels.resolve(simpleName + ".java"));
                assertTrue(source.contains("class " + simpleName), simpleName);
                for (String segment : contract.path()) {
                    assertTrue(source.contains("\"" + segment + "\""), simpleName + " missing " + segment);
                }
                for (String segment : contract.tracePath()) {
                    assertTrue(source.contains("\"" + segment + "\""), simpleName + " missing trace " + segment);
                }
            }
        }
    }

    @Test void rendererSelectedC8VariantsAreExplicitlyCovered() throws Exception {
        Path renderers = Path.of(System.getProperty("projectRoot")).getParent()
                .resolve("naturalist/common/src/main/java/com/crispytwig/naturalist/client/renderer");
        Map<String, List<String>> expectedModels = Map.of(
                "AlligatorRenderer", List.of("AlligatorModel", "AlligatorBabyModel"),
                "BoarRenderer", List.of("BoarModel", "BoarBabyModel"),
                "ZebraRenderer", List.of("ZebraModel", "ZebraBabyModel"),
                "BassRenderer", List.of("BassModel", "MediumBassModel", "LargeBassModel"),
                "TortoiseRenderer", List.of("TortoiseModel", "TortoiseBabyModel"),
                "VultureRenderer", List.of("VultureModel", "VultureBabyModel"));
        for (var entry : expectedModels.entrySet()) {
            String source = Files.readString(renderers.resolve(entry.getKey() + ".java"));
            for (String model : entry.getValue()) assertTrue(source.contains(model), entry.getKey() + " " + model);
        }
        for (String id : List.of("alligator", "boar", "zebra", "bass", "tortoise", "vulture")) {
            assertTrue(NaturalistModelContracts.contractsForId(id).size() >= 2, id);
        }
    }

    @Test void c8GeometryAndPresentationContractsAreExplicit() {
        assertEquals(List.of("body", "neck"), NaturalistModelContracts.contractsForId("alligator").getFirst().path());
        assertEquals(List.of("body", "neck"), NaturalistModelContracts.contractsForId("boar").getFirst().path());
        assertEquals(List.of("body", "neck"), NaturalistModelContracts.contractsForId("boar").getFirst().tracePath());
        assertEquals(.72F, NaturalistModelContracts.contractsForId("boar").getFirst().presentation().scale());
        assertTrue(NaturalistModelContracts.contractsForId("boar").getFirst().preserveAncestorTransforms());
        var zebra = NaturalistModelContracts.contractsForId("zebra").getFirst();
        assertEquals(List.of("body", "neck"), zebra.path());
        assertEquals(List.of("body", "neck", "neck_r1"), zebra.tracePath());
        assertEquals(List.of("neck_r1", "leftEar", "rightEar"), zebra.drawableChildren());
        assertEquals(1.5708F, zebra.presentation().yRotation());
        assertEquals(List.of("body", "neck", "skull2"), NaturalistModelContracts.contractsForId("zebra").get(1).path());
        assertEquals(List.of("body", "neck"), NaturalistModelContracts.contractsForId("komodo_dragon").getFirst().path());
        var shark = NaturalistModelContracts.contractsForId("great_white_shark").getFirst();
        assertEquals(List.of("body"), shark.path());
        assertTrue(shark.neutralizeRootRotation());
        assertEquals(1.5708F, shark.presentation().yRotation());
        assertEquals(.21F, shark.presentation().scale());
        assertEquals(-4.0F, shark.presentation().frameYOffset());
        var starfish = NaturalistModelContracts.contractsForId("starfish").getFirst();
        assertEquals(List.of("root"), starfish.path());
        assertEquals(List.of("root", "body"), starfish.tracePath());
        assertEquals(1.5708F, starfish.presentation().xRotation());
        var desertScorpion = NaturalistModelContracts.contractsForId("desert_scorpion").getFirst();
        var jungleScorpion = NaturalistModelContracts.contractsForId("jungle_scorpion").getFirst();
        assertEquals(List.of("root"), desertScorpion.path());
        assertEquals(List.of("root", "body"), desertScorpion.tracePath());
        assertEquals(List.of("root"), jungleScorpion.path());
        assertEquals(List.of("root", "body"), jungleScorpion.tracePath());
        assertEquals(1.5708F, desertScorpion.presentation().xRotation());
        assertEquals(1.5708F, jungleScorpion.presentation().xRotation());
        assertEquals(List.of("body", "legs"), desertScorpion.drawableChildren());
        assertEquals(List.of("body", "legs"), jungleScorpion.drawableChildren());
        assertTrue(desertScorpion.normalizeSelectedRootTransform());
        assertTrue(jungleScorpion.normalizeSelectedRootTransform());
        var clam = NaturalistModelContracts.contractsForId("clam").getFirst();
        assertEquals(List.of("root"), clam.path());
        assertEquals(List.of("root", "bottom"), clam.tracePath());
        assertEquals(.20F, clam.presentation().scale());
        assertEquals(1.5708F, clam.presentation().xRotation());
        assertTrue(NaturalistModelContracts.contractsForId("whale").getFirst().presentation().scale() < 0.5F);
    }

    @Test void c8LabelFallbackTargetsHaveExplicitDrawableContractsAndDetachedCapture() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("ray", "RayModel:body:body"), Map.entry("bass", "BassModel:body:body"),
                Map.entry("giant_isopod", "GiantIsopodModel:rolled:rolled"), Map.entry("hedgehog", "HedgehogModel:rolled:rolled"),
                Map.entry("vulture", "VultureModel:neck:neck"), Map.entry("tortoise", "TortoiseModel:body/skullRot/neck:body/skullRot/neck"),
                Map.entry("zebra", "ZebraModel:body/neck:body/neck/neck_r1"), Map.entry("jellyfish", "JellyfishModel:body:body"),
                Map.entry("starfish", "StarfishModel:root:root/body"), Map.entry("clam", "ClamModel:root:root/bottom"),
                Map.entry("lizard", "LizardModel:body/skullRot/neck:body/skullRot/neck/neck_r1"),
                Map.entry("mole", "MoleModel:root/body/skull:root/body/skull"));
        expected.forEach((id, expectedContract) -> assertTrue(NaturalistModelContracts.contractsForId(id).stream().anyMatch(contract -> {
            String name = contract.modelClass().substring(contract.modelClass().lastIndexOf('.') + 1);
            return (name + ":" + String.join("/", contract.path()) + ":" + String.join("/", contract.tracePath()))
                    .equals(expectedContract);
        }), id));
        for (String id : expected.keySet()) assertFalse(NaturalistModelContracts.contractsForId(id).getFirst().preserveAncestorTransforms(), id);
        assertEquals(List.of("jaw", "dangly"), NaturalistModelContracts.contractsForId("anglerfish").getFirst().drawableChildren());
        assertFalse(NaturalistModelContracts.contractsForId("piranha").getFirst().preserveAncestorTransforms());
        assertEquals(List.of("body", "legs"), NaturalistModelContracts.contractsForId("starfish").getFirst().drawableChildren());
        assertEquals(List.of("top", "bottom", "hinge"), NaturalistModelContracts.contractsForId("clam").getFirst().drawableChildren());
        for (String id : List.of("starfish", "clam", "desert_scorpion", "jungle_scorpion")) {
            assertTrue(NaturalistModelContracts.contractsForId(id).getFirst().normalizeSelectedRootTransform(), id);
        }
    }

    @Test void brownBearIsOnlyANativeControl() {
        assertTrue(NaturalistModelContracts.isNativeControlId("bear"));
        assertFalse(NaturalistModelContracts.isTargetId("bear"));
    }

    @Test void brownBearC12PathAuditHasNoPresentationOverrideOrCacheMutation() throws Exception {
        Path module = Path.of(System.getProperty("projectRoot"));
        String manager = Files.readString(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/mixin/RadarIconManagerMixin.java"));
        String creator = Files.readString(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/mixin/RadarIconCreatorMixin.java"));
        String form = Files.readString(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/mixin/RadarIconModelFormPrerendererMixin.java"));
        assertTrue(manager.contains("BrownBearPathDiagnostic.cacheLookup(storage.containsKey(key))"));
        assertTrue(manager.contains("BrownBearPathDiagnostic.requestFinished"));
        assertTrue(creator.contains("parameters.form"));
        assertTrue(creator.contains("getTextureLocation"));
        assertTrue(form.contains("trace.textures"));
        assertFalse(creator.contains("pose.scale"));
        assertFalse(manager.contains("storage.remove(key)"));
        assertFalse(manager.contains("BrownBearIconCacheFreshness"));
        assertFalse(Files.exists(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/NaturalistIconPresentation.java")));
        assertFalse(Files.exists(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/BrownBearDiagnostic.java")));
        assertFalse(Files.exists(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/BrownBearIconCacheFreshness.java")));
    }

    @Test void retainedC2ControlsRemainContractStable() {
        for (String id : List.of("capybara", "black_bear", "turkey", "lizard_tail")) assertTrue(NaturalistModelContracts.isTargetId(id), id);
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("capybara").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("black_bear").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("turkey").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("lizard_tail").getFirst().presentation().scale());
        assertEquals(.76F, NaturalistModelContracts.contractsForId("tiger").getFirst().presentation().scale());
    }

    @Test void c8PreservesWorkingC7ContractsAndNarrowsOnlyTheClamFrame() {
        assertEquals(.38F, NaturalistModelContracts.contractsForId("ray").getFirst().presentation().scale());
        assertEquals(.68F, NaturalistModelContracts.contractsForId("hedgehog").getFirst().presentation().scale());
        assertEquals(.70F, NaturalistModelContracts.contractsForId("hedgehog").get(1).presentation().scale());
        assertEquals(.60F, NaturalistModelContracts.contractsForId("piranha").getFirst().presentation().scale());
        assertEquals(.45F, NaturalistModelContracts.contractsForId("jellyfish").getFirst().presentation().scale());
        assertEquals(.45F, NaturalistModelContracts.contractsForId("giant_isopod").getFirst().presentation().scale());
        assertEquals(.20F, NaturalistModelContracts.contractsForId("clam").getFirst().presentation().scale());
        for (var bass : NaturalistModelContracts.contractsForId("bass")) {
            assertEquals(1.5708F, bass.presentation().yRotation());
        }
        var largeBass = NaturalistModelContracts.contractsForId("bass").get(2);
        assertEquals(List.of(), largeBass.path());
        assertEquals(List.of("body"), largeBass.tracePath());
    }

    @Test void c8SourceAuditsUseAuthoredRootsForScorpionAndStarfishFallbacks() throws Exception {
        Path models = Path.of(System.getProperty("projectRoot")).getParent()
                .resolve("naturalist/common/src/main/java/com/crispytwig/naturalist/client/model");
        String desert = Files.readString(models.resolve("DesertScorpionModel.java"));
        String jungle = Files.readString(models.resolve("JungleScorpionModel.java"));
        String clam = Files.readString(models.resolve("ClamModel.java"));
        for (String name : List.of("root", "body", "leftArm", "rightArm", "legs")) assertTrue(desert.contains("\"" + name + "\""), name);
        assertTrue(desert.contains("\"tail_1\""));
        for (String name : List.of("root", "body", "arms", "tail", "legs", "leftClaw", "rightClaw")) assertTrue(jungle.contains("\"" + name + "\""), name);
        assertTrue(clam.contains("\"root\""));
        assertTrue(clam.contains("\"bottom\""));
        assertTrue(clam.contains("\"top\""));
        assertTrue(clam.contains("\"hinge\""));

        Path module = Path.of(System.getProperty("projectRoot"));
        String contracts = Files.readString(module.resolve("src/main/java/dev/resivore/naturalistxaeroicons/NaturalistModelContracts.java"));
        assertTrue(contracts.contains("naturalist:orange_starfish"));
        assertTrue(contracts.contains("naturalist:block/orange_starfish"));
        assertFalse(contracts.contains("new ItemStack"));
        assertFalse(Files.exists(module.resolve("src/main/resources/assets/naturalist/textures/block/orange_starfish.png")));
    }
}
