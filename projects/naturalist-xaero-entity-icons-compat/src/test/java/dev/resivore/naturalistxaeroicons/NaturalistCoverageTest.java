package dev.resivore.naturalistxaeroicons;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
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

    @Test void c30ChangesOnlyTheAdultWhaleToTheAuditedCompactBodyAndHeadAssembly() {
        var alligator = NaturalistModelContracts.contractsForId("alligator").getFirst();
        assertEquals("com.crispytwig.naturalist.client.model.AlligatorModel", alligator.modelClass());
        assertEquals(List.of("body", "neck"), alligator.path());
        assertEquals(List.of("body", "neck"), alligator.tracePath());
        assertEquals(.58F, alligator.presentation().scale());
        assertEquals(.7854F, alligator.presentation().yRotation());
        assertEquals(0.0F, alligator.presentation().frameYOffset());
        var alligatorBaby = NaturalistModelContracts.contractsForId("alligator").get(1);
        assertEquals("com.crispytwig.naturalist.client.model.AlligatorBabyModel", alligatorBaby.modelClass());
        assertEquals(List.of("body", "neck"), alligatorBaby.path());
        assertEquals(.76F, alligatorBaby.presentation().scale());
        assertEquals(0.0F, alligatorBaby.presentation().yRotation());

        var lizard = NaturalistModelContracts.contractsForId("lizard").getFirst();
        assertEquals("com.crispytwig.naturalist.client.model.LizardModel", lizard.modelClass());
        assertEquals(List.of("body", "skullRot", "neck"), lizard.path());
        assertEquals(List.of("body", "skullRot", "neck", "neck_r1"), lizard.tracePath());
        assertEquals(1.0F, lizard.presentation().scale());
        assertEquals(1.5708F, lizard.presentation().yRotation());

        var whaleAdult = NaturalistModelContracts.contractsForId("whale").getFirst();
        var whaleBaby = NaturalistModelContracts.contractsForId("whale").get(1);
        assertEquals("com.crispytwig.naturalist.client.model.WhaleModel", whaleAdult.modelClass());
        assertEquals(List.of("body"), whaleAdult.path());
        assertEquals(List.of("body"), whaleAdult.tracePath());
        assertEquals(.18F, whaleAdult.presentation().scale());
        assertEquals(0.0F, whaleAdult.presentation().xRotation());
        assertEquals(1.1781F, whaleAdult.presentation().yRotation());
        assertEquals(0.0F, whaleAdult.presentation().zRotation());
        assertEquals(-1.0F, whaleAdult.presentation().frameYOffset());
        assertEquals(List.of(), whaleAdult.cubeIndexes());
        assertEquals(List.of("skullRot"), whaleAdult.drawableChildren());
        assertTrue(whaleAdult.useTraceAsRenderCenter());
        assertFalse(whaleAdult.preserveAncestorTransforms());
        assertEquals("com.crispytwig.naturalist.client.model.WhaleBabyModel", whaleBaby.modelClass());
        assertEquals(List.of("body", "skull"), whaleBaby.path());
        assertEquals(List.of("body", "skull"), whaleBaby.tracePath());
        assertEquals(.60F, whaleBaby.presentation().scale());
        assertEquals(0.0F, whaleBaby.presentation().xRotation());
        assertEquals(.7854F, whaleBaby.presentation().yRotation());
        assertEquals(0.0F, whaleBaby.presentation().zRotation());
        assertEquals(0.0F, whaleBaby.presentation().frameYOffset());
        assertNotEquals(1.5708F, whaleBaby.presentation().yRotation());
        assertFalse(whaleBaby.useTraceAsRenderCenter());
        assertTrue(whaleBaby.preserveAncestorTransforms());

        for (var tortoise : NaturalistModelContracts.contractsForId("tortoise")) {
            assertTrue(tortoise.modelClass().endsWith("TortoiseModel") || tortoise.modelClass().endsWith("TortoiseBabyModel"));
            assertEquals(List.of("body", "skullRot", "neck"), tortoise.path());
            assertEquals(List.of("body", "skullRot", "neck"), tortoise.tracePath());
            assertEquals(1.0F, tortoise.presentation().scale());
            assertEquals(1.5708F, tortoise.presentation().yRotation());
        }

        var blackBear = NaturalistModelContracts.contractsForId("black_bear").getFirst();
        assertEquals("com.crispytwig.naturalist.client.model.BlackBearModel", blackBear.modelClass());
        assertEquals(List.of("body", "skullRot", "skull"), blackBear.path());
        assertEquals(List.of("body", "skullRot", "skull"), blackBear.tracePath());
        assertEquals(1.0F, blackBear.presentation().scale());
        assertEquals(-2.0F, blackBear.presentation().frameYOffset());
        assertEquals(0.0F, blackBear.presentation().xRotation());
        assertEquals(0.0F, blackBear.presentation().yRotation());
        assertEquals(0.0F, blackBear.presentation().zRotation());
        assertEquals(0.0F, NaturalistModelContracts.contractsForId("black_bear").get(1).presentation().frameYOffset());

        var hippo = NaturalistModelContracts.contractsForId("hippo").getFirst();
        assertEquals("com.crispytwig.naturalist.client.model.HippoModel", hippo.modelClass());
        assertEquals(List.of("body", "bone", "neck"), hippo.path());
        assertEquals(List.of("body", "bone", "neck"), hippo.tracePath());
        assertEquals(.75F, hippo.presentation().scale());
        assertNotEquals(.60F, hippo.presentation().scale());
        assertEquals(.70F, NaturalistModelContracts.contractsForId("hippo").get(1).presentation().scale());

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
        assertEquals("com.crispytwig.naturalist.client.model.StarfishModel", starfish.modelClass());
        assertEquals(List.of(), starfish.path());
        assertEquals(List.of("body"), starfish.tracePath());
        assertEquals(List.of("body", "legs"), starfish.drawableChildren());
        assertTrue(starfish.normalizeSelectedRootTransform());
        assertTrue(starfish.useTraceAsRenderCenter());
        assertEquals(.52F, starfish.presentation().scale());
        assertEquals(1.5708F, starfish.presentation().xRotation());
        var desertScorpion = NaturalistModelContracts.contractsForId("desert_scorpion").getFirst();
        var jungleScorpion = NaturalistModelContracts.contractsForId("jungle_scorpion").getFirst();
        assertEquals(List.of(), desertScorpion.path());
        assertEquals(List.of("body"), desertScorpion.tracePath());
        assertEquals(List.of(), jungleScorpion.path());
        assertEquals(List.of("body"), jungleScorpion.tracePath());
        assertEquals(1.5708F, desertScorpion.presentation().xRotation());
        assertEquals(1.5708F, jungleScorpion.presentation().xRotation());
        assertEquals(List.of("body", "legs"), desertScorpion.drawableChildren());
        assertEquals(List.of("body", "legs"), jungleScorpion.drawableChildren());
        assertTrue(desertScorpion.normalizeSelectedRootTransform());
        assertTrue(jungleScorpion.normalizeSelectedRootTransform());
        assertTrue(desertScorpion.useTraceAsRenderCenter());
        assertTrue(jungleScorpion.useTraceAsRenderCenter());
        assertEquals(.34F, desertScorpion.presentation().scale());
        assertEquals(.28F, jungleScorpion.presentation().scale());
        var clam = NaturalistModelContracts.contractsForId("clam").getFirst();
        assertEquals("com.crispytwig.naturalist.client.model.ClamModel", clam.modelClass());
        assertEquals(List.of("top"), clam.path());
        assertEquals(List.of("top"), clam.tracePath());
        assertEquals(List.of(), clam.drawableChildren());
        assertFalse(clam.useTraceAsRenderCenter());
        assertTrue(clam.normalizeSelectedRootTransform());
        assertEquals(.30F, clam.presentation().scale());
        assertEquals(1.5708F, clam.presentation().xRotation());
        assertEquals(0.0F, clam.presentation().yRotation());
        assertEquals(0.0F, clam.presentation().zRotation());
        assertEquals(0.0F, clam.presentation().frameYOffset());
        for (String id : NaturalistModelContracts.targetIds()) {
            if (!List.of("starfish", "desert_scorpion", "jungle_scorpion", "whale").contains(id)) {
                assertFalse(NaturalistModelContracts.contractsForId(id).getFirst().useTraceAsRenderCenter(), id);
            }
        }
        assertEquals(.18F, NaturalistModelContracts.contractsForId("whale").getFirst().presentation().scale());
    }

    @Test void c31SourceAuditBindsTheWhaleTorsoHeadAssemblyAndFrozenControls() throws Exception {
        Path models = Path.of(System.getProperty("projectRoot")).getParent()
                .resolve("naturalist/common/src/main/java/com/crispytwig/naturalist/client/model");
        String alligator = Files.readString(models.resolve("AlligatorModel.java"));
        assertTrue(alligator.contains("body.addOrReplaceChild(\"neck\""));
        assertTrue(alligator.contains("neck.addOrReplaceChild(\"snout\""));
        assertTrue(alligator.contains("addBox(0.5F, -5.0F, -6.0F"));
        String lizard = Files.readString(models.resolve("LizardModel.java"));
        assertTrue(lizard.contains("body.addOrReplaceChild(\"skullRot\""));
        assertTrue(lizard.contains("skullRot.addOrReplaceChild(\"neck\""));
        assertTrue(lizard.contains("neck.addOrReplaceChild(\"neck_r1\""));
        String adultWhale = Files.readString(models.resolve("WhaleModel.java"));
        assertTrue(adultWhale.contains("body.addOrReplaceChild(\"skullRot\""));
        assertTrue(adultWhale.contains("addBox(-17.0F, -18.5F, -25.0F, 34.0F, 36.0F, 58.0F"));
        assertTrue(adultWhale.contains("skullRot.addOrReplaceChild(\"topJaw\""));
        assertTrue(adultWhale.contains("skullRot.addOrReplaceChild(\"bottomJaw\""));
        assertTrue(adultWhale.contains("addBox(-14.0F, -10.0F, -37.0F, 28.0F, 11.0F, 42.0F"));
        assertTrue(adultWhale.contains("PartPose.offset(0.0F, -5.6667F, -25.0833F)"));
        assertTrue(adultWhale.contains("PartPose.offset(0.0F, -2.8333F, -4.9167F)"));
        String babyWhale = Files.readString(models.resolve("WhaleBabyModel.java"));
        assertTrue(babyWhale.contains("body.addOrReplaceChild(\"skull\""));
        assertTrue(babyWhale.contains("skull.addOrReplaceChild(\"jaw\""));
        assertTrue(babyWhale.contains("addBox(-6.0F, -4.0F, -19.0F, 12.0F, 6.0F, 18.0F"));
        for (String model : List.of("TortoiseModel", "TortoiseBabyModel")) {
            String source = Files.readString(models.resolve(model + ".java"));
            assertTrue(source.contains("body.addOrReplaceChild(\"skullRot\""), model);
            assertTrue(source.contains("skullRot.addOrReplaceChild(\"neck\""), model);
        }
        String blackBear = Files.readString(models.resolve("BlackBearModel.java"));
        assertTrue(blackBear.contains("body.addOrReplaceChild(\"skullRot\""));
        assertTrue(blackBear.contains("skullRot.addOrReplaceChild(\"skull\""));
        assertTrue(blackBear.contains("skull.addOrReplaceChild(\"snout\""));
        String hippo = Files.readString(models.resolve("HippoModel.java"));
        assertTrue(hippo.contains("body.addOrReplaceChild(\"bone\""));
        assertTrue(hippo.contains("bone.addOrReplaceChild(\"neck\""));
        assertTrue(hippo.contains("neck.addOrReplaceChild(\"topJaw\""));
    }

    @Test void c29RecordsTheExactC28TechnicalPassAndVisualFailure() throws Exception {
        Path root = Path.of(System.getProperty("projectRoot"));
        for (String record : List.of(Files.readString(root.resolve("WORKBENCH_STATUS.json")),
                Files.readString(root.resolve("TESTING.md")), Files.readString(root.resolve("CODEX_LOG.md")))) {
            assertTrue(record.contains("C28"));
            assertTrue(record.contains("technical/capture PASS"));
            assertTrue(record.contains("visual FAIL"));
            assertTrue(record.contains("3dec7da889191bc9ab6965373e2d70c30225e0dbe62f27f8a5de559279ff8b90"));
            assertTrue(record.contains("body/skullRot/topJaw"));
            assertTrue(record.contains("vertical sliver"));
        }
        assertTrue(NaturalistModelContracts.isNativeControlId("bear"));
        assertFalse(NaturalistModelContracts.isTargetId("bear"));
        assertEquals(.30F, NaturalistModelContracts.contractsForId("clam").getFirst().presentation().scale());
        assertEquals(.52F, NaturalistModelContracts.contractsForId("starfish").getFirst().presentation().scale());
        assertEquals(.34F, NaturalistModelContracts.contractsForId("desert_scorpion").getFirst().presentation().scale());
        assertEquals(.28F, NaturalistModelContracts.contractsForId("jungle_scorpion").getFirst().presentation().scale());
    }

    @Test void c33ReconcilesC32AndChangesOnlyTheAdultWhaleFrameOffset() throws Exception {
        var blackBear = NaturalistModelContracts.contractsForId("black_bear").getFirst();
        var hippo = NaturalistModelContracts.contractsForId("hippo").getFirst();
        var whale = NaturalistModelContracts.contractsForId("whale").getFirst();
        assertEquals(-2.0F, blackBear.presentation().frameYOffset(), "C25 Black Bear PASS is frozen");
        assertEquals(.75F, hippo.presentation().scale(), "C26 Hippo PASS is frozen");
        assertEquals(List.of("body", "bone", "neck"), hippo.path());
        assertEquals(List.of("body", "bone", "neck"), hippo.tracePath());
        assertEquals(.70F, NaturalistModelContracts.contractsForId("hippo").get(1).presentation().scale());
        assertEquals(1.1781F, whale.presentation().yRotation(), "C33 preserves C32's readable near-side profile");
        assertEquals(List.of("body"), whale.tracePath());
        assertTrue(whale.useTraceAsRenderCenter());
        assertEquals(.18F, whale.presentation().scale());
        assertEquals(-1.0F, whale.presentation().frameYOffset(), "C33 changes only the copied frame offset to reveal the lower body");
        assertEquals(List.of("skullRot"), whale.drawableChildren());
        Path root = Path.of(System.getProperty("projectRoot"));
        for (String record : List.of(Files.readString(root.resolve("WORKBENCH_STATUS.json")),
                Files.readString(root.resolve("TESTING.md")), Files.readString(root.resolve("CODEX_LOG.md")))) {
            assertTrue(record.contains("C32"));
            assertTrue(record.contains("nose"));
            assertTrue(record.contains("bottom"));
            assertTrue(record.contains("not label-only"));
        }
    }

    @Test void c31FreezesEveryNonWhaleEntityContract() throws Exception {
        StringBuilder snapshot = new StringBuilder();
        for (String id : NaturalistModelContracts.targetIds()) {
            if (id.equals("whale")) continue;
            snapshot.append(id).append('=');
            for (var contract : NaturalistModelContracts.contractsForId(id)) snapshot.append(contract).append(';');
            snapshot.append('\n');
        }
        String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(snapshot.toString().getBytes(StandardCharsets.UTF_8)));
        assertEquals("691fe6b709d633848f14d49230ba211cd3321c2ded5dad22b2a8ee54c549cad1", fingerprint,
                "Canary 29 must not modify any non-Whale entity contract");
    }

    @Test void c16ClamAndOtherLabelFallbackTargetsHaveExplicitDetachedCaptureContracts() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("ray", "RayModel:body:body"), Map.entry("bass", "BassModel:body:body"),
                Map.entry("giant_isopod", "GiantIsopodModel:rolled:rolled"), Map.entry("hedgehog", "HedgehogModel:rolled:rolled"),
                Map.entry("vulture", "VultureModel:neck:neck"), Map.entry("tortoise", "TortoiseModel:body/skullRot/neck:body/skullRot/neck"),
                Map.entry("zebra", "ZebraModel:body/neck:body/neck/neck_r1"), Map.entry("jellyfish", "JellyfishModel:body:body"),
                Map.entry("starfish", "StarfishModel::body"), Map.entry("clam", "ClamModel:top:top"),
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
        assertEquals(List.of(), NaturalistModelContracts.contractsForId("clam").getFirst().drawableChildren());
        for (String id : List.of("starfish", "clam", "desert_scorpion", "jungle_scorpion")) {
            assertTrue(NaturalistModelContracts.contractsForId(id).getFirst().normalizeSelectedRootTransform(), id);
        }
    }

    @Test void brownBearIsOnlyANativeControl() {
        assertTrue(NaturalistModelContracts.isNativeControlId("bear"));
        assertFalse(NaturalistModelContracts.isTargetId("bear"));
    }

    @Test void brownBearC14RetunesOnlyTheEvidencedSpritePathAndEvictsOnlyItsNativeCacheOnReload() throws Exception {
        Path module = Path.of(System.getProperty("projectRoot"));
        String manager = Files.readString(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/mixin/RadarIconManagerMixin.java"));
        String presentation = Files.readString(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/BrownBearSpritePresentation.java"));
        assertTrue(manager.contains("@ModifyVariable"));
        assertTrue(manager.contains("index = 19"));
        assertTrue(manager.contains("\"bear\".equals"));
        assertTrue(presentation.contains("RadarIconSpriteForm"));
        assertTrue(presentation.contains("\"naturalist:bear\""));
        assertTrue(presentation.contains("SCALE = 0.65F"));
        assertTrue(presentation.contains("new RadarIconCreator.Parameters"));
        assertTrue(presentation.contains("!(parameters.form instanceof RadarIconSpriteForm)"));
        assertFalse(manager.contains("@Redirect"));
        assertFalse(manager.contains("storage.remove(key)"));
        assertFalse(manager.contains("@Mixin(value = RadarIconSpriteForm.class"));
        assertFalse(manager.contains("PoseStack"));
        assertFalse(Files.exists(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/BrownBearPathDiagnostic.java")));
    }

    @Test void retainedC2ControlsRemainContractStable() {
        for (String id : List.of("capybara", "black_bear", "turkey", "lizard_tail")) assertTrue(NaturalistModelContracts.isTargetId(id), id);
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("capybara").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("black_bear").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("turkey").getFirst().presentation().scale());
        assertEquals(1.0F, NaturalistModelContracts.contractsForId("lizard_tail").getFirst().presentation().scale());
        assertEquals(.76F, NaturalistModelContracts.contractsForId("tiger").getFirst().presentation().scale());
    }

    @Test void c20PreservesWorkingControlsWhileChangingOnlyTheClamCaptureSurface() {
        assertEquals(.38F, NaturalistModelContracts.contractsForId("ray").getFirst().presentation().scale());
        assertEquals(.68F, NaturalistModelContracts.contractsForId("hedgehog").getFirst().presentation().scale());
        assertEquals(.70F, NaturalistModelContracts.contractsForId("hedgehog").get(1).presentation().scale());
        assertEquals(.60F, NaturalistModelContracts.contractsForId("piranha").getFirst().presentation().scale());
        assertEquals(.45F, NaturalistModelContracts.contractsForId("jellyfish").getFirst().presentation().scale());
        assertEquals(.45F, NaturalistModelContracts.contractsForId("giant_isopod").getFirst().presentation().scale());
        assertEquals(.30F, NaturalistModelContracts.contractsForId("clam").getFirst().presentation().scale());
        for (var bass : NaturalistModelContracts.contractsForId("bass")) {
            assertEquals(1.5708F, bass.presentation().yRotation());
        }
        var largeBass = NaturalistModelContracts.contractsForId("bass").get(2);
        assertEquals(List.of(), largeBass.path());
        assertEquals(List.of("body"), largeBass.tracePath());
    }

    @Test void c20ClamContractRemainsFrozenAfterItsRuntimePass() throws Exception {
        var clam = NaturalistModelContracts.contractsForId("clam").getFirst();
        assertEquals(List.of("top"), clam.path());
        assertEquals(List.of("top"), clam.tracePath());
        assertEquals(List.of(), clam.drawableChildren());
        assertEquals(.30F, clam.presentation().scale());
        assertEquals(1.5708F, clam.presentation().xRotation());

        Path module = Path.of(System.getProperty("projectRoot"));
        assertFalse(Files.exists(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/ClamCaptureDiagnostic.java")));
    }

    @Test void c23SourceAuditProvesModelRootsAndCompleteCompactAssemblies() throws Exception {
        Path models = Path.of(System.getProperty("projectRoot")).getParent()
                .resolve("naturalist/common/src/main/java/com/crispytwig/naturalist/client/model");
        String starfish = Files.readString(models.resolve("StarfishModel.java"));
        assertTrue(starfish.contains("super(root.getChild(\"root\"))"));
        assertTrue(starfish.contains("this.root = root.getChild(\"root\")"));
        assertTrue(starfish.contains("PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F"));
        assertTrue(starfish.contains("root.addOrReplaceChild(\"body\""));
        assertTrue(starfish.contains("body.addOrReplaceChild(\"skull\""));
        assertTrue(starfish.contains("skull.addOrReplaceChild(\"skull_r1\""));
        assertTrue(starfish.contains("root.addOrReplaceChild(\"legs\""));
        for (String arm : List.of("leftArm", "rightArm", "leftLeg", "rightLeg")) {
            assertTrue(starfish.contains("legs.addOrReplaceChild(\"" + arm + "\""), arm);
        }
        for (String model : List.of("DesertScorpionModel", "JungleScorpionModel")) {
            String scorpion = Files.readString(models.resolve(model + ".java"));
            assertTrue(scorpion.contains("super(root.getChild(\"root\"))"), model);
            assertTrue(scorpion.contains("this.root = root.getChild(\"root\")"), model);
            assertTrue(scorpion.contains("root.addOrReplaceChild(\"body\""), model);
            assertTrue(scorpion.contains("root.addOrReplaceChild(\"legs\""), model);
        }
        String desert = Files.readString(models.resolve("DesertScorpionModel.java"));
        assertTrue(desert.contains("body.addOrReplaceChild(\"leftArm\""));
        assertTrue(desert.contains("body.addOrReplaceChild(\"rightArm\""));
        assertTrue(desert.contains("body.addOrReplaceChild(\"tail_1\""));
        String jungle = Files.readString(models.resolve("JungleScorpionModel.java"));
        assertTrue(jungle.contains("body.addOrReplaceChild(\"tail\""));
        assertTrue(jungle.contains("body.addOrReplaceChild(\"arms\""));
    }

    @Test void c22StarfishDiagnosticReportsTheActualProductionContract() throws Exception {
        Path module = Path.of(System.getProperty("projectRoot"));
        String diagnostic = Files.readString(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/StarfishCaptureDiagnostic.java"));
        assertTrue(diagnostic.contains("naturalist:starfish"));
        assertTrue(diagnostic.contains("<model root>"));
        assertTrue(diagnostic.contains("selectedGeometry="));
        assertTrue(diagnostic.contains("rotations="));
        assertTrue(diagnostic.contains("fallback render destination before="));
        assertTrue(diagnostic.contains("adapterRecorded="));
        assertTrue(diagnostic.contains("selectedRecorded="));
        assertTrue(diagnostic.contains("live Starfish body trace"));
        assertTrue(diagnostic.contains("renderCenterIsTrace="));
        assertTrue(diagnostic.contains("renderCenterHasDirectMrt="));
        assertTrue(diagnostic.contains("renderCenterRecorded="));
        assertFalse(diagnostic.contains("naturalist:clam"));
    }

    @Test void c23ScorpionDiagnosticsAreNarrowAndReportOnlyActualXaeroSeams() throws Exception {
        Path module = Path.of(System.getProperty("projectRoot"));
        String diagnostic = Files.readString(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/ScorpionCaptureDiagnostic.java"));
        for (String expected : List.of("naturalist:desert_scorpion", "naturalist:jungle_scorpion",
                "canPrerender=", "RadarIconCreator#create form=", "rendererTexture=", "exactModelClass=",
                "selectedGeometry=", "renderCenterIsTrace=", "renderCenterHasDirectMrt=",
                "explicit trace resolution succeeds=", "adapterRecorded=", "selectedAssemblyRecorded=",
                "renderCenterRecorded=", "fallback threw", "cache write post-MISS", "manager result")) {
            assertTrue(diagnostic.contains(expected), expected);
        }
        assertFalse(diagnostic.contains("renderedDest.add("));
        assertFalse(diagnostic.contains("new XaeroIcon"));
        String contracts = Files.readString(module.resolve("src/main/java/dev/resivore/naturalistxaeroicons/NaturalistModelContracts.java"));
        assertFalse(contracts.contains("\"root\", \"root/body\""));
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
