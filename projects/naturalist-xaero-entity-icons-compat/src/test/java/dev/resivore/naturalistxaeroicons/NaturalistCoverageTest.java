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
            }
        }
    }

    @Test void c3GeometryAndPresentationContractsAreExplicit() {
        assertEquals(List.of("body", "neck", "snout"), NaturalistModelContracts.contractsForId("alligator").getFirst().path());
        assertEquals(List.of("body", "neck", "neck_r1"), NaturalistModelContracts.contractsForId("boar").getFirst().path());
        assertEquals(List.of("body", "neck"), NaturalistModelContracts.contractsForId("komodo_dragon").getFirst().path());
        assertEquals(List.of("body", "skullRot"), NaturalistModelContracts.contractsForId("great_white_shark").getFirst().path());
        assertEquals(1.5708F, NaturalistModelContracts.contractsForId("starfish").getFirst().presentation().xRotation());
        assertEquals(0, NaturalistModelContracts.contractsForId("desert_scorpion").getFirst().path().size());
        assertTrue(NaturalistModelContracts.contractsForId("clam").getFirst().presentation().scale() < 0.5F);
        assertTrue(NaturalistModelContracts.contractsForId("whale").getFirst().presentation().scale() < 0.5F);
    }

    @Test void brownBearIsOnlyANativePresentationOverride() {
        assertTrue(NaturalistModelContracts.isNativeControlId("bear"));
        assertTrue(NaturalistModelContracts.isNativePresentationOverrideId("bear"));
        assertFalse(NaturalistModelContracts.isTargetId("bear"));
        for (String id : List.of("bird", "butterfly", "catfish", "caterpillar", "crab", "deer", "firefly", "snake", "snail")) {
            assertFalse(NaturalistModelContracts.isNativePresentationOverrideId(id), id);
        }
    }

    @Test void retainedC2ControlsRemainContractStable() {
        for (String id : List.of("capybara", "black_bear", "turkey", "lizard_tail")) assertTrue(NaturalistModelContracts.isTargetId(id), id);
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("capybara").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("black_bear").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("turkey").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("lizard_tail").getFirst().presentation().scale());
    }
}
