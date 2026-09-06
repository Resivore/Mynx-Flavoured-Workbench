package dev.resivore.xaeroemfcompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.animal.turtle.AdultTurtleModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.ModelPartUtil;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelocatedHeadFailureMechanismTest {
    static final Path FRESH_ANIMATIONS =
            Path.of(Objects.requireNonNull(System.getProperty("freshAnimationsPack")));
    static final Set<Direction> ALL_DIRECTIONS =
            Set.copyOf(Arrays.asList(Direction.values()));

    @Test
    void exactJemsExposeTheThreeDifferentCanary2FailureShapes() throws Exception {
        JsonObject sheep = jem("sheep");
        JsonObject sheepBody = topLevelPart(sheep, "body");
        assertFalse(topLevelPart(sheep, "head").has("boxes"));
        assertFalse(topLevelPart(sheep, "head").has("attach"));
        assertEquals(0, directBoxCount(sheepBody));
        assertEquals(1, directBoxCount(findDirectSubmodel(sheepBody, "rotation")));
        assertEquals(1, directBoxCount(findDirectSubmodel(sheepBody, "head2")));

        JsonObject horse = jem("horse");
        JsonObject horseBody = topLevelPart(horse, "body");
        JsonObject neck2 = findDirectSubmodel(horseBody, "neck2");
        assertFalse(topLevelPart(horse, "neck").has("boxes"));
        assertFalse(topLevelPart(horse, "neck").has("attach"));
        assertEquals(1, directBoxCount(horseBody));
        assertEquals(0, directBoxCount(neck2));
        assertEquals(1, directBoxCount(findDirectSubmodel(neck2, "neck3")));
        assertEquals(1, directBoxCount(findDirectSubmodel(neck2, "head2")));

        JsonObject turtle = jem("turtle");
        JsonObject bodyRotation = findDirectSubmodel(
                topLevelPart(turtle, "body"), "body_rotation");
        assertFalse(topLevelPart(turtle, "head").has("boxes"));
        assertEquals(2, directBoxCount(bodyRotation));
        assertEquals(1, directBoxCount(findDirectSubmodel(bodyRotation, "head2")));
    }

    @Test
    void sheepUsesOnlyTheExactHeadSubtreeInTheVanillaHeadFrame() throws Exception {
        ModelPart vanillaRoot = SheepModel.createBodyLayer().bakeRoot();
        ModelPart canonicalHead = transformedEmpty(vanillaRoot.getChild("head"), Map.of());
        ModelPart customBody = customPart(topLevelPart(jem("sheep"), "body"));
        ModelPart bodyWrapper = transformedEmpty(
                vanillaRoot.getChild("body"), Map.of("EMF_body", customBody));
        ModelPart root = emptyPart(Map.of(
                "head", canonicalHead,
                "body", bodyWrapper
        ));
        ModelPart head2 = child(customBody, "EMF_head2");
        ModelRenderTrace trace = traced(head2, 0xFFA1B2C3);

        EmfIconPartResolver.Resolution result = EmfIconPartResolver
                .resolveRelocatedHead(root, canonicalHead, vanillaRoot, trace, true)
                .orElseThrow();

        assertSame(head2, result.tracedHead());
        assertSame(head2, result.geometryRoot());
        assertEquals("root/head", result.canonicalPath());
        assertEquals("root/body/EMF_body/EMF_head2", result.tracedPath());
        assertEquals(result.tracedPath(), result.geometryPath());
        assertEquals(0xFFA1B2C3, result.color());
        assertEquals(6, cubeCount(result.renderAdapter()));
        assertEquals(1, identityCount(result.renderAdapter(), head2));
        assertFalse(cubePaths(result.renderAdapter()).stream()
                .anyMatch(path -> path.contains("rotation")));

        Bounds vanilla = centeredBounds(vanillaRoot.getChild("head"),
                vanillaRoot.getChild("head"));
        Bounds adapted = centeredBounds(result.renderAdapter(), result.centeringPart());
        assertBoundsClose(vanilla, adapted, 0.02F);

        Bounds canary2WholeBody = centeredBounds(bodyWrapper, bodyWrapper);
        float largestCanary2Ratio = Math.max(
                canary2WholeBody.spanX() / vanilla.spanX(),
                Math.max(
                        canary2WholeBody.spanY() / vanilla.spanY(),
                        canary2WholeBody.spanZ() / vanilla.spanZ()
                )
        );
        assertTrue(largestCanary2Ratio > 2.5F,
                () -> "C2=" + canary2WholeBody + " vanilla=" + vanilla);
        assertTrue(cubeCount(bodyWrapper) > cubeCount(result.renderAdapter()));
    }

    @Test
    void horseUsesXaerosHeadPartsAliasAndPreservesTheNeckGroup() throws Exception {
        ModelPart vanillaRoot = LayerDefinition.create(
                AbstractEquineModel.createBodyMesh(CubeDeformation.NONE), 64, 64).bakeRoot();
        ModelPart vanillaHeadParts = vanillaRoot.getChild("head_parts");
        ModelPart canonicalHeadParts = transformedEmpty(
                vanillaHeadParts,
                Map.of("head", emptyPart(Map.of()))
        );
        ModelPart customBody = customPart(topLevelPart(jem("horse"), "body"));
        ModelPart neck2 = child(customBody, "EMF_neck2");
        neck2.xRot = (float) Math.PI / 6.0F;
        neck2.setInitialPose(neck2.storePose());
        ModelPart head2 = child(neck2, "EMF_head2");
        ModelPart root = emptyPart(Map.of(
                "head_parts", canonicalHeadParts,
                "body", transformedEmpty(
                        vanillaRoot.getChild("body"), Map.of("EMF_body", customBody))
        ));

        assertFalse(ModelPartUtil.getChildren(root).containsKey("head"));
        assertFalse(ModelPartUtil.hasCubes(canonicalHeadParts));
        EmfIconPartResolver.Resolution result = EmfIconPartResolver
                .resolveRelocatedHead(
                        root,
                        canonicalHeadParts,
                        vanillaRoot,
                        traced(head2, 0xFF445566),
                        true
                ).orElseThrow();

        assertSame(head2, result.tracedHead());
        assertSame(neck2, result.geometryRoot());
        assertEquals("root/head_parts", result.canonicalPath());
        assertEquals("root/body/EMF_body/EMF_neck2/EMF_head2", result.tracedPath());
        assertEquals("root/body/EMF_body/EMF_neck2", result.geometryPath());
        assertEquals(11, cubeCount(result.renderAdapter()));
        assertTrue(cubePaths(result.renderAdapter()).stream()
                .anyMatch(path -> path.contains("EMF_neck3")));
        assertTrue(cubePaths(result.renderAdapter()).stream()
                .anyMatch(path -> path.contains("EMF_mane2")));
        assertFalse(cubePaths(result.renderAdapter()).stream()
                .anyMatch(path -> path.equals("/EMF_body")));

        Bounds vanilla = centeredBounds(vanillaHeadParts, vanillaHeadParts);
        Bounds adapted = centeredBounds(result.renderAdapter(), result.centeringPart());
        assertBoundsClose(vanilla, adapted, 0.20F);

        Bounds headOnly = centeredBounds(head2, result.centeringPart());
        assertTrue(headOnly.spanY() < adapted.spanY() * 0.5F,
                () -> "headOnly=" + headOnly + " adapted=" + adapted);
    }

    @Test
    void turtleCarriesTheCounterRotatingAncestorWithoutItsShellCubes() throws Exception {
        ModelPart vanillaRoot = AdultTurtleModel.createBodyLayer().bakeRoot();
        ModelPart canonicalHead = transformedEmpty(vanillaRoot.getChild("head"), Map.of());
        ModelPart customBody = customPart(topLevelPart(jem("turtle"), "body"));
        ModelPart bodyRotation = child(customBody, "EMF_body_rotation");
        ModelPart head2 = child(bodyRotation, "EMF_head2");
        ModelPart root = emptyPart(Map.of(
                "head", canonicalHead,
                "body", transformedEmpty(
                        vanillaRoot.getChild("body"), Map.of("EMF_body", customBody))
        ));
        List<PartState> sourceState = partStates(root);

        EmfIconPartResolver.Resolution result = EmfIconPartResolver
                .resolveRelocatedHead(
                        root,
                        canonicalHead,
                        vanillaRoot,
                        traced(head2, 0xFF778899),
                        true
                ).orElseThrow();

        assertSame(head2, result.geometryRoot());
        assertEquals(5, cubeCount(result.renderAdapter()));
        assertFalse(cubePaths(result.renderAdapter()).stream()
                .anyMatch(path -> path.endsWith("/EMF_body_rotation")));

        Bounds vanilla = centeredBounds(vanillaRoot.getChild("head"),
                vanillaRoot.getChild("head"));
        Bounds adapted = centeredBounds(result.renderAdapter(), result.centeringPart());
        Bounds canary2HeadOnly = centeredBounds(head2, result.centeringPart());
        assertBoundsClose(vanilla, adapted, 0.02F);
        assertNotEquals(adapted.spanY(), canary2HeadOnly.spanY(), 0.25F);
        assertNotEquals(adapted.spanZ(), canary2HeadOnly.spanZ(), 0.25F);
        assertEquals(sourceState, partStates(root),
                "icon adapter must not mutate the live EMF render tree");
    }

    @Test
    void exactFreshAnimationsCreeperControlKeepsCanary2Framing() throws Exception {
        ModelPart vanillaRoot = CreeperModel.createBodyLayer(
                CubeDeformation.NONE).bakeRoot();
        ModelPart canonicalHead = transformedEmpty(
                vanillaRoot.getChild("head"), Map.of());
        ModelPart customBody = customPart(topLevelPart(jem("creeper"), "body"));
        ModelPart head2 = child(customBody, "EMF_head2");
        ModelPart root = emptyPart(Map.of(
                "head", canonicalHead,
                "body", transformedEmpty(
                        vanillaRoot.getChild("body"), Map.of("EMF_body", customBody))
        ));

        EmfIconPartResolver.Resolution result = EmfIconPartResolver
                .resolveRelocatedHead(
                        root,
                        canonicalHead,
                        vanillaRoot,
                        traced(head2, 0xFFABCDEF),
                        true
                ).orElseThrow();

        assertSame(head2, result.geometryRoot());
        Bounds canary2 = centeredBounds(head2, head2);
        Bounds adapted = centeredBounds(result.renderAdapter(), result.centeringPart());
        Bounds vanilla = centeredBounds(
                vanillaRoot.getChild("head"), vanillaRoot.getChild("head"));
        assertBoundsClose(canary2, adapted, 0.02F);
        assertBoundsClose(vanilla, adapted, 0.02F);
    }

    @Test
    void fullLiveAncestorAffineTransformIsCarriedThenRebased() throws Exception {
        ModelPart vanillaRoot = SheepModel.createBodyLayer().bakeRoot();
        ModelPart canonicalHead = transformedEmpty(
                vanillaRoot.getChild("head"), Map.of());
        ModelPart customBody = customPart(topLevelPart(jem("sheep"), "body"));
        ModelPart head2 = child(customBody, "EMF_head2");
        ModelPart bodyWrapper = transformedEmpty(
                vanillaRoot.getChild("body"), Map.of("EMF_body", customBody));

        bodyWrapper.yRot = 0.23F;
        bodyWrapper.xScale = 1.15F;
        bodyWrapper.yScale = 0.90F;
        bodyWrapper.zScale = 1.05F;
        customBody.zRot = -0.17F;
        customBody.xScale = 0.85F;
        head2.xRot = 0.31F;
        head2.yRot = -0.19F;
        head2.xScale = 1.10F;
        head2.yScale = 1.10F;
        head2.zScale = 1.10F;

        ModelPart root = emptyPart(Map.of(
                "head", canonicalHead,
                "body", bodyWrapper
        ));
        List<PartState> sourceState = partStates(root);
        EmfIconPartResolver.Resolution result = EmfIconPartResolver
                .resolveRelocatedHead(
                        root,
                        canonicalHead,
                        vanillaRoot,
                        traced(head2, 0xFF123456),
                        true
                ).orElseThrow();

        Bounds vanilla = centeredBounds(
                vanillaRoot.getChild("head"), vanillaRoot.getChild("head"));
        Bounds adapted = centeredBounds(result.renderAdapter(), result.centeringPart());
        assertBoundsClose(vanilla, adapted, 0.02F);
        assertEquals(sourceState, partStates(root));
    }

    @Test
    void rotationResetDisabledRecentersWithoutCancellingLiveAffineTransform() {
        ModelPart vanillaRoot = SheepModel.createBodyLayer().bakeRoot();
        ModelPart vanillaHead = vanillaRoot.getChild("head");
        ModelPart canonicalHead = transformedEmpty(vanillaHead, Map.of());
        ModelPart head2 = new ModelPart(
                List.copyOf(ModelPartUtil.getCubes(vanillaHead)), Map.of());
        ModelPart customBody = emptyPart(Map.of("EMF_head2", head2));
        ModelPart bodyWrapper = transformedEmpty(
                vanillaRoot.getChild("body"), Map.of("EMF_body", customBody));
        bodyWrapper.yRot = 0.31F;
        bodyWrapper.xScale = 1.25F;
        customBody.zRot = -0.22F;
        head2.xRot = 0.17F;
        ModelPart root = emptyPart(Map.of(
                "head", canonicalHead,
                "body", bodyWrapper
        ));

        EmfIconPartResolver.Resolution result = EmfIconPartResolver
                .resolveRelocatedHead(
                        root,
                        canonicalHead,
                        vanillaRoot,
                        traced(head2, 0xFF654321),
                        false
                ).orElseThrow();

        Bounds vanilla = centeredBounds(vanillaHead, vanillaHead);
        Bounds adapted = centeredBounds(result.renderAdapter(), result.centeringPart());
        assertCenterClose(vanilla, adapted, 0.02F);
        assertNotEquals(vanilla.spanX(), adapted.spanX(), 0.25F);
    }

    @Test
    void ambiguousMatchesBodyCubeBoundariesAndNonHeadAliasesFailClosed() {
        ModelPart vanillaRoot = SheepModel.createBodyLayer().bakeRoot();
        ModelPart vanillaHead = vanillaRoot.getChild("head");
        ModelPart canonicalHead = transformedEmpty(vanillaHead, Map.of());
        ModelPart.Cube canonicalCube = ModelPartUtil.getCubes(vanillaHead).getFirst();

        ModelPart ambiguousHead = new ModelPart(
                List.of(canonicalCube, canonicalCube), Map.of());
        ModelPart ambiguousRoot = emptyPart(Map.of(
                "head", canonicalHead,
                "body", emptyPart(Map.of("EMF_head2", ambiguousHead))
        ));
        assertTrue(EmfIconPartResolver.resolveRelocatedHead(
                ambiguousRoot,
                canonicalHead,
                vanillaRoot,
                traced(ambiguousHead, 0xFFFFFFFF),
                true
        ).isEmpty());

        ModelPart wrongHead = cubePart(Map.of());
        ModelPart cubeBearingBody = new ModelPart(
                List.of(canonicalCube), Map.of("EMF_head2", wrongHead));
        ModelPart boundedRoot = emptyPart(Map.of(
                "head", canonicalHead,
                "body", cubeBearingBody
        ));
        assertTrue(EmfIconPartResolver.resolveRelocatedHead(
                boundedRoot,
                canonicalHead,
                vanillaRoot,
                traced(wrongHead, 0xFFFFFFFF),
                true
        ).isEmpty());

        ModelPart vanillaTail = cubePart(Map.of());
        ModelPart emptyTail = transformedEmpty(vanillaTail, Map.of());
        ModelPart tailRoot = emptyPart(Map.of(
                "tail", emptyTail,
                "body", emptyPart(Map.of("EMF_head2", wrongHead))
        ));
        ModelPart retainedTailRoot = emptyPart(Map.of("tail", vanillaTail));
        assertTrue(EmfIconPartResolver.resolveRelocatedHead(
                tailRoot,
                emptyTail,
                retainedTailRoot,
                traced(wrongHead, 0xFFFFFFFF),
                true
        ).isEmpty());
    }

    @Test
    void cubeBearingCanonicalAndVanillaPathsRemainUntouched() throws Exception {
        ModelPart vanillaRoot = SheepModel.createBodyLayer().bakeRoot();
        ModelPart vanillaHead = vanillaRoot.getChild("head");
        ModelPart replacement = cubePart(Map.of());
        ModelPart root = emptyPart(Map.of(
                "head", vanillaHead,
                "body", emptyPart(Map.of("EMF_head2", replacement))
        ));
        ModelRenderTrace trace = traced(replacement, 0xFFFFFFFF);

        assertTrue(EmfIconPartResolver.resolveRelocatedHead(
                root, vanillaHead, vanillaRoot, trace, true).isEmpty());
        assertTrue(EmfIconPartResolver.resolve(
                vanillaRoot, vanillaHead, trace, true).isEmpty());
    }

    @Test
    void untracedOrDimensionMismatchedGeometryDoesNotCreateAnAdapter() throws Exception {
        ModelPart vanillaRoot = SheepModel.createBodyLayer().bakeRoot();
        ModelPart canonicalHead = transformedEmpty(vanillaRoot.getChild("head"), Map.of());
        ModelPart wrongHead = cubePart(Map.of());
        ModelPart root = emptyPart(Map.of(
                "head", canonicalHead,
                "body", emptyPart(Map.of("EMF_ear", wrongHead))
        ));

        assertTrue(EmfIconPartResolver.resolveRelocatedHead(
                root, canonicalHead, vanillaRoot, trace(), true).isEmpty());
        assertTrue(EmfIconPartResolver.resolveRelocatedHead(
                root, canonicalHead, vanillaRoot, traced(wrongHead, 0xFFFFFFFF), true)
                .isEmpty());
    }

    @Test
    void dimensionChangedButUniquelyTracedSemanticHeadUsesBoundedFallback() {
        ModelPart vanillaRoot = SheepModel.createBodyLayer().bakeRoot();
        ModelPart canonicalHead = transformedEmpty(vanillaRoot.getChild("head"), Map.of());
        ModelPart reshapedHead = cubePart(Map.of());
        ModelPart body = emptyPart(Map.of("EMF_head2", reshapedHead, "EMF_arm", cubePart(Map.of())));
        ModelPart root = emptyPart(Map.of("head", canonicalHead, "body", body));

        EmfIconPartResolver.Resolution result = EmfIconPartResolver.resolveRelocatedHead(
                root, canonicalHead, vanillaRoot, traced(reshapedHead, 0xFF102030), true).orElseThrow();

        assertSame(reshapedHead, result.geometryRoot());
        assertEquals(1, cubeCount(result.renderAdapter()));
        assertFalse(cubePaths(result.renderAdapter()).stream().anyMatch(path -> path.contains("EMF_arm")));
        assertEquals("RESOLVED", IconDiagnostics.lastReason());
    }

    @Test
    void canonicalEmfHeadIsDetachedWithOnlyFacialHeadwearAndNoBodySiblings() {
        ModelPart vanillaRoot = SheepModel.createBodyLayer().bakeRoot();
        ModelPart vanillaHead = vanillaRoot.getChild("head");
        ModelPart nose = cubePart(Map.of("ribbits_farmer_hat", cubePart(Map.of("band", cubePart(Map.of())))));
        ModelPart canonicalHead = new ModelPart(
                List.copyOf(ModelPartUtil.getCubes(vanillaHead)),
                Map.of("nose", nose, "body", cubePart(Map.of()), "left_arm", cubePart(Map.of())));
        canonicalHead.x = vanillaHead.x;
        canonicalHead.y = vanillaHead.y;
        canonicalHead.z = vanillaHead.z;
        canonicalHead.setInitialPose(canonicalHead.storePose());
        ModelPart root = emptyPart(Map.of("head", canonicalHead));

        EmfIconPartResolver.Resolution result = EmfIconPartResolver.resolveRelocatedHead(
                root, canonicalHead, vanillaRoot, traced(canonicalHead, 0xFFAABBCC), true).orElseThrow();

        assertSame(canonicalHead, result.tracedHead());
        assertSame(canonicalHead, result.geometryRoot());
        assertEquals("root/head", result.geometryPath());
        List<String> paths = cubePaths(result.renderAdapter());
        assertTrue(paths.stream().anyMatch(path -> path.contains("nose/ribbits_farmer_hat/band")));
        assertFalse(paths.stream().anyMatch(path -> path.contains("body")));
        assertFalse(paths.stream().anyMatch(path -> path.contains("left_arm")));
        assertEquals("RESOLVED_CANONICAL_GEOMETRY", IconDiagnostics.lastReason());
    }

    static ModelPart customPart(JsonObject data) {
        String invertAxis = data.has("invertAxis")
                ? data.get("invertAxis").getAsString().toLowerCase()
                : "";
        List<ModelPart.Cube> cubes = new ArrayList<>();
        if (data.has("boxes")) {
            for (var element : data.getAsJsonArray("boxes")) {
                JsonObject box = element.getAsJsonObject();
                float[] coordinates = floats(box.getAsJsonArray("coordinates"), 6);
                float x = invertedOrigin(coordinates[0], coordinates[3], invertAxis, 'x');
                float y = invertedOrigin(coordinates[1], coordinates[4], invertAxis, 'y');
                float z = invertedOrigin(coordinates[2], coordinates[5], invertAxis, 'z');
                float sizeAdd = box.has("sizeAdd") ? box.get("sizeAdd").getAsFloat() : 0.0F;
                cubes.add(new ModelPart.Cube(
                        0, 0,
                        x, y, z,
                        coordinates[3], coordinates[4], coordinates[5],
                        sizeAdd, sizeAdd, sizeAdd,
                        false, 128.0F, 128.0F, ALL_DIRECTIONS
                ));
            }
        }

        Map<String, ModelPart> children = new LinkedHashMap<>();
        if (data.has("submodels")) {
            for (var element : data.getAsJsonArray("submodels")) {
                JsonObject child = element.getAsJsonObject();
                children.put("EMF_" + child.get("id").getAsString(), customPart(child));
            }
        }

        ModelPart result = new ModelPart(cubes, children);
        float[] translate = data.has("translate")
                ? floats(data.getAsJsonArray("translate"), 3)
                : new float[3];
        float[] rotate = data.has("rotate")
                ? floats(data.getAsJsonArray("rotate"), 3)
                : new float[3];
        result.x = inverted(translate[0], invertAxis, 'x');
        result.y = inverted(translate[1], invertAxis, 'y');
        result.z = inverted(translate[2], invertAxis, 'z');
        result.xRot = radians(inverted(rotate[0], invertAxis, 'x'));
        result.yRot = radians(inverted(rotate[1], invertAxis, 'y'));
        result.zRot = radians(inverted(rotate[2], invertAxis, 'z'));
        result.setInitialPose(result.storePose());
        float scale = data.has("scale") ? data.get("scale").getAsFloat() : 1.0F;
        result.xScale = scale;
        result.yScale = scale;
        result.zScale = scale;
        return result;
    }

    static float invertedOrigin(
            float origin,
            float size,
            String invertAxis,
            char axis
    ) {
        return invertAxis.indexOf(axis) >= 0 ? -origin - size : origin;
    }

    static float inverted(float value, String invertAxis, char axis) {
        return invertAxis.indexOf(axis) >= 0 ? -value : value;
    }

    static float radians(float degrees) {
        return degrees * ((float) Math.PI / 180.0F);
    }

    static float[] floats(JsonArray values, int expected) {
        assertEquals(expected, values.size());
        float[] result = new float[expected];
        for (int index = 0; index < expected; index++) {
            result[index] = values.get(index).getAsFloat();
        }
        return result;
    }

    static Bounds centeredBounds(ModelPart part, ModelPart mainPart) {
        float centerX = mainPart.x;
        float centerY = mainPart.y;
        float centerZ = mainPart.z;
        ModelPart.Cube biggest = ModelPartUtil.getBiggestCuboid(mainPart);
        if (biggest != null) {
            centerY += (biggest.minY + biggest.maxY) / 2.0F;
            centerZ += (biggest.minZ + biggest.maxZ) / 2.0F;
        }

        float x = part.x;
        float y = part.y;
        float z = part.z;
        part.setPos(part.x - centerX, part.y - centerY, part.z - centerZ);
        Bounds bounds = new Bounds();
        try {
            PoseStack pose = new PoseStack();
            boolean adapted = EmfIconPartResolver.getAdapterExtentsForGui(
                    part,
                    pose,
                    point -> bounds.include(
                            point.x() * 16.0F,
                            point.y() * 16.0F,
                            point.z() * 16.0F
                    )
            );
            if (!adapted) {
                part.getExtentsForGui(pose, point -> bounds.include(
                        point.x() * 16.0F,
                        point.y() * 16.0F,
                        point.z() * 16.0F
                ));
            }
        } finally {
            part.setPos(x, y, z);
        }
        assertTrue(bounds.count > 0);
        return bounds;
    }

    static void assertSpanClose(Bounds expected, Bounds actual, float tolerance) {
        assertEquals(expected.spanX(), actual.spanX(), tolerance,
                () -> "X expected=" + expected + " actual=" + actual);
        assertEquals(expected.spanY(), actual.spanY(), tolerance,
                () -> "Y expected=" + expected + " actual=" + actual);
        assertEquals(expected.spanZ(), actual.spanZ(), tolerance,
                () -> "Z expected=" + expected + " actual=" + actual);
    }

    static void assertBoundsClose(
            Bounds expected,
            Bounds actual,
            float tolerance
    ) {
        assertSpanClose(expected, actual, tolerance);
        assertEquals(expected.minX, actual.minX, tolerance,
                () -> "minX expected=" + expected + " actual=" + actual);
        assertEquals(expected.minY, actual.minY, tolerance,
                () -> "minY expected=" + expected + " actual=" + actual);
        assertEquals(expected.minZ, actual.minZ, tolerance,
                () -> "minZ expected=" + expected + " actual=" + actual);
        assertEquals(expected.maxX, actual.maxX, tolerance,
                () -> "maxX expected=" + expected + " actual=" + actual);
        assertEquals(expected.maxY, actual.maxY, tolerance,
                () -> "maxY expected=" + expected + " actual=" + actual);
        assertEquals(expected.maxZ, actual.maxZ, tolerance,
                () -> "maxZ expected=" + expected + " actual=" + actual);
    }

    static void assertCenterClose(
            Bounds expected,
            Bounds actual,
            float tolerance
    ) {
        assertEquals(expected.centerX(), actual.centerX(), tolerance);
        assertEquals(expected.centerY(), actual.centerY(), tolerance);
        assertEquals(expected.centerZ(), actual.centerZ(), tolerance);
    }

    static int cubeCount(ModelPart part) {
        AtomicInteger count = new AtomicInteger();
        part.visit(new PoseStack(), (pose, path, index, cube) -> count.incrementAndGet());
        return count.get();
    }

    static List<String> cubePaths(ModelPart part) {
        List<String> paths = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> paths.add(path));
        return paths;
    }

    static int identityCount(ModelPart root, ModelPart target) {
        int result = root == target ? 1 : 0;
        for (ModelPart child : ModelPartUtil.getChildren(root).values()) {
            result += identityCount(child, target);
        }
        return result;
    }

    static List<PartState> partStates(ModelPart root) {
        List<PartState> result = new ArrayList<>();
        collectPartStates(root, "root", result);
        return List.copyOf(result);
    }

    static void collectPartStates(
            ModelPart part,
            String path,
            List<PartState> result
    ) {
        result.add(new PartState(
                path,
                part.x, part.y, part.z,
                part.xRot, part.yRot, part.zRot,
                part.xScale, part.yScale, part.zScale,
                part.visible, part.skipDraw,
                part.getInitialPose()
        ));
        ModelPartUtil.getChildren(part).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> collectPartStates(
                        entry.getValue(), path + "/" + entry.getKey(), result));
    }

    static ModelRenderTrace traced(ModelPart part, int color) {
        ModelRenderTrace trace = trace();
        trace.addVisibleModelPart(part, color);
        return trace;
    }

    static ModelRenderTrace trace() {
        return new ModelRenderTrace(
                null, Map.of(), null, false, false, false,
                null, null, null, null, 0xFFFFFFFF);
    }

    static ModelPart transformedEmpty(
            ModelPart source,
            Map<String, ModelPart> children
    ) {
        ModelPart result = emptyPart(children);
        result.x = source.x;
        result.y = source.y;
        result.z = source.z;
        result.xRot = source.xRot;
        result.yRot = source.yRot;
        result.zRot = source.zRot;
        result.xScale = source.xScale;
        result.yScale = source.yScale;
        result.zScale = source.zScale;
        result.setInitialPose(source.getInitialPose());
        return result;
    }

    static ModelPart emptyPart(Map<String, ModelPart> children) {
        return new ModelPart(List.of(), children);
    }

    static ModelPart cubePart(Map<String, ModelPart> children) {
        ModelPart.Cube cube = new ModelPart.Cube(
                0, 0,
                -1.0F, -1.0F, -1.0F,
                2.0F, 2.0F, 2.0F,
                0.0F, 0.0F, 0.0F,
                false, 16.0F, 16.0F, ALL_DIRECTIONS
        );
        return new ModelPart(List.of(cube), children);
    }

    static ModelPart child(ModelPart parent, String name) {
        return Objects.requireNonNull(ModelPartUtil.getChildren(parent).get(name), name);
    }

    static JsonObject jem(String entity) throws Exception {
        try (ZipFile pack = new ZipFile(FRESH_ANIMATIONS.toFile())) {
            String path = "assets/minecraft/optifine/cem/" + entity + ".jem";
            var entry = Objects.requireNonNull(pack.getEntry(path), path);
            try (var reader = new InputStreamReader(
                    pack.getInputStream(entry), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    static JsonObject topLevelPart(JsonObject jem, String part) {
        for (var element : jem.getAsJsonArray("models")) {
            JsonObject model = element.getAsJsonObject();
            if (model.has("part") && part.equals(model.get("part").getAsString())) {
                return model;
            }
        }
        throw new AssertionError("Missing top-level part " + part);
    }

    static JsonObject findDirectSubmodel(JsonObject parent, String id) {
        for (var element : parent.getAsJsonArray("submodels")) {
            JsonObject child = element.getAsJsonObject();
            if (id.equals(child.get("id").getAsString())) {
                return child;
            }
        }
        throw new AssertionError("Missing direct submodel " + id);
    }

    static int directBoxCount(JsonObject part) {
        return part.has("boxes") ? part.getAsJsonArray("boxes").size() : 0;
    }

    static final class Bounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;
        private int count;

        private void include(float x, float y, float z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
            count++;
        }

        float spanX() {
            return maxX - minX;
        }

        float spanY() {
            return maxY - minY;
        }

        float spanZ() {
            return maxZ - minZ;
        }

        private float centerX() {
            return (minX + maxX) / 2.0F;
        }

        private float centerY() {
            return (minY + maxY) / 2.0F;
        }

        private float centerZ() {
            return (minZ + maxZ) / 2.0F;
        }

        @Override
        public String toString() {
            return "Bounds{" + minX + ".." + maxX + ", "
                    + minY + ".." + maxY + ", "
                    + minZ + ".." + maxZ + '}';
        }
    }

    private record PartState(
            String path,
            float x,
            float y,
            float z,
            float xRot,
            float yRot,
            float zRot,
            float xScale,
            float yScale,
            float zScale,
            boolean visible,
            boolean skipDraw,
            net.minecraft.client.model.geom.PartPose initialPose
    ) {
    }
}
