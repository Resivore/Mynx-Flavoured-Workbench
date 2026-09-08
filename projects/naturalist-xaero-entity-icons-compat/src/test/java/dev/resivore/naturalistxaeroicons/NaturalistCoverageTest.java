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

    @Test void c5GeometryAndPresentationContractsAreExplicit() {
        assertEquals(List.of("body", "neck"), NaturalistModelContracts.contractsForId("alligator").getFirst().path());
        assertEquals(List.of("body", "neck"), NaturalistModelContracts.contractsForId("boar").getFirst().path());
        assertEquals(List.of("body", "neck"), NaturalistModelContracts.contractsForId("boar").getFirst().tracePath());
        assertEquals(.72F, NaturalistModelContracts.contractsForId("boar").getFirst().presentation().scale());
        assertTrue(NaturalistModelContracts.contractsForId("boar").getFirst().preserveAncestorTransforms());
        var zebra = NaturalistModelContracts.contractsForId("zebra").getFirst();
        assertEquals(List.of("body", "neck", "neck_r1"), zebra.path());
        assertEquals(List.of(2, 3), zebra.cubeIndexes());
        assertEquals(List.of("body", "neck", "skull2"), NaturalistModelContracts.contractsForId("zebra").get(1).path());
        assertEquals(List.of("body", "neck"), NaturalistModelContracts.contractsForId("komodo_dragon").getFirst().path());
        var shark = NaturalistModelContracts.contractsForId("great_white_shark").getFirst();
        assertEquals(List.of("body"), shark.path());
        assertTrue(shark.neutralizeRootRotation());
        assertEquals(1.5708F, shark.presentation().yRotation());
        assertEquals(.21F, shark.presentation().scale());
        var starfish = NaturalistModelContracts.contractsForId("starfish").getFirst();
        assertEquals(List.of(), starfish.tracePath());
        assertEquals(1.5708F, starfish.presentation().xRotation());
        assertEquals(0, NaturalistModelContracts.contractsForId("desert_scorpion").getFirst().path().size());
        assertEquals(1.5708F, NaturalistModelContracts.contractsForId("desert_scorpion").getFirst().presentation().xRotation());
        assertEquals(1.5708F, NaturalistModelContracts.contractsForId("jungle_scorpion").getFirst().presentation().xRotation());
        assertTrue(NaturalistModelContracts.contractsForId("clam").getFirst().presentation().scale() < 0.5F);
        assertEquals(1.5708F, NaturalistModelContracts.contractsForId("clam").getFirst().presentation().xRotation());
        assertTrue(NaturalistModelContracts.contractsForId("whale").getFirst().presentation().scale() < 0.5F);
    }

    @Test void c5LabelFallbackTargetsHaveExplicitDrawableContractsAndDetachedCapture() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("ray", "RayModel:body:body"), Map.entry("bass", "BassModel:body:body"),
                Map.entry("giant_isopod", "GiantIsopodModel:rolled:rolled"), Map.entry("hedgehog", "HedgehogModel:rolled:rolled"),
                Map.entry("vulture", "VultureModel:neck:neck"), Map.entry("tortoise", "TortoiseModel:body/skullRot/neck:body/skullRot/neck"),
                Map.entry("zebra", "ZebraModel:body/neck/neck_r1:body/neck/neck_r1"), Map.entry("jellyfish", "JellyfishModel:body:body"),
                Map.entry("starfish", "StarfishModel::"), Map.entry("clam", "ClamModel::"),
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
    }

    @Test void brownBearIsOnlyANativePresentationOverride() {
        assertTrue(NaturalistModelContracts.isNativeControlId("bear"));
        assertTrue(NaturalistModelContracts.isNativePresentationOverrideId("bear"));
        assertFalse(NaturalistModelContracts.isTargetId("bear"));
        for (String id : List.of("bird", "butterfly", "catfish", "caterpillar", "crab", "deer", "firefly", "snake", "snail")) {
            assertFalse(NaturalistModelContracts.isNativePresentationOverrideId(id), id);
        }
        assertEquals(.35F, NaturalistModelContracts.nativePresentationForId("bear").scale());
    }

    @Test void retainedC2ControlsRemainContractStable() {
        for (String id : List.of("capybara", "black_bear", "turkey", "lizard_tail")) assertTrue(NaturalistModelContracts.isTargetId(id), id);
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("capybara").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("black_bear").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("turkey").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("lizard_tail").getFirst().presentation().scale());
        assertEquals(.76F, NaturalistModelContracts.contractsForId("tiger").getFirst().presentation().scale());
    }
}
