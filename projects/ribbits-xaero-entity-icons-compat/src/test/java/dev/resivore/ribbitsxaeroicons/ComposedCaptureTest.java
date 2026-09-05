package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.*;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.*;
import com.geckolib.loading.definition.geometry.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipFile;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ComposedCaptureTest {
    private static Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(GeometryBone.class, GeometryBone.gsonDeserializer())
                .registerTypeAdapter(GeometryCube.class, GeometryCube.gsonDeserializer())
                .registerTypeAdapter(GeometryDescription.class, GeometryDescription.gsonDeserializer())
                .registerTypeAdapter(GeometryUv.class, GeometryUv.gsonDeserializer())
                .registerTypeAdapter(GeometryUvPair.class, GeometryUvPair.gsonDeserializer())
                .registerTypeAdapter(GeometryUvMapping.class, GeometryUvMapping.gsonDeserializer())
                .registerTypeAdapter(GeometryUvMappingDetails.class, GeometryUvMappingDetails.gsonDeserializer())
                .registerTypeAdapter(GeometryLocator.class, GeometryLocator.gsonDeserializer())
                .create();
    }

    private static CuboidGeoBone bake(String name) throws Exception {
        try (ZipFile zip = new ZipFile(System.getProperty("ribbitsJar"))) {
            String path = "assets/ribbits/geckolib/models/" + name + ".geo.json";
            var root = JsonParser.parseString(new String(zip.getInputStream(zip.getEntry(path)).readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("minecraft:geometry")
                    .get(0).getAsJsonObject();
            Gson gson = gson();
            var description = gson.fromJson(root.get("description"), GeometryDescription.class);
            var bones = gson.fromJson(root.get("bones"), GeometryBone[].class);
            Map<String, List<GeometryBone>> children = new HashMap<>();
            for (var bone : bones) children.computeIfAbsent(bone.parent(), ignored -> new ArrayList<>()).add(bone);
            var main = Arrays.stream(bones).filter(b -> b.name().equals("main")).findFirst().orElseThrow();
            return (CuboidGeoBone) main.bake(null, description, children, (b, l) -> {});
        }
    }

    @Test
    void actualBakedFacesStayUprightCameraFacingAndVanillaSizedAtEveryScale() throws Exception {
        for (String model : List.of("nitwit_ribbit", "wandering_ribbit")) {
            var main = bake(model);
            var body = (CuboidGeoBone) Arrays.stream(main.children()).filter(b -> b.name().equals("body")).findFirst().orElseThrow();
            GeoCube[] selected = model.startsWith("wandering") ? WanderingRibbitHeadSelector.select(body.cubes) : body.cubes;
            assertEquals(3, selected.length);
            var plan = GeometryPlan.create(main, body, selected);
            for (float scale : new float[] {0.25F, 0.5F, 1F, 2F, 4F}) {
                // Xaero Creator.setupMatrices resets the incoming pose and model-view to identity.
                PoseStack pose = new PoseStack();
                pose.translate(123, 456, 789);
                pose.setIdentity();
                plan.applyFraming(pose, scale);
                Vector3f up = pose.last().pose().transformDirection(new Vector3f(0, 1, 0)).normalize();
                assertEquals(-1F, up.y, 1e-6F);
                assertEquals(0F, up.z, 1e-6F, "up must not point toward camera/top view");
                var north = Arrays.stream(selected[0].quads()).filter(q -> q != null && q.normalZ() < -0.9F).findFirst().orElseThrow();
                var normal = pose.last().normal().transform(north.normalVec()).normalize();
                assertEquals(1F, normal.z, 1e-6F, "actual baked face must face the camera");
                double[] bounds = {1e9, 1e9, -1e9, -1e9};
                for (var cube : selected) {
                    pose.pushPose();
                    cube.translateToPivotPoint(pose); cube.rotate(pose); cube.translateAwayFromPivotPoint(pose);
                    for (var quad : cube.quads()) if (quad != null) for (var v : quad.vertices()) {
                        var point = pose.last().pose().transformPosition(new Vector3f(v.posX(), v.posY(), v.posZ()));
                        bounds[0] = Math.min(bounds[0], point.x); bounds[1] = Math.min(bounds[1], point.y);
                        bounds[2] = Math.max(bounds[2], point.x); bounds[3] = Math.max(bounds[3], point.y);
                    }
                    pose.popPose();
                }
                double span = Math.max(bounds[2] - bounds[0], bounds[3] - bounds[1]);
                double vanillaCapture = 32 * (8.0 / 16) * Math.min(1, scale);
                assertEquals(vanillaCapture, span, 1e-5);
                assertEquals(32, (bounds[0] + bounds[2]) / 2, 1e-5);
                assertEquals(32, (bounds[1] + bounds[3]) / 2, 1e-5);
                // RadarRenderer displays the capture at max(1, scale); user size is not clamped overall.
                assertEquals(16 * scale, span * Math.max(1, scale), 1e-5);
            }
        }
    }

    @Test
    void wanderingSelectionExcludesClothesBackpackLimbsAndSurvivesOrderingOnly() throws Exception {
        var main = bake("wandering_ribbit");
        var body = (CuboidGeoBone) Arrays.stream(main.children()).filter(b -> b.name().equals("body")).findFirst().orElseThrow();
        var selected = WanderingRibbitHeadSelector.select(body.cubes);
        assertEquals(List.of(body.cubes[0], body.cubes[13], body.cubes[14]), Arrays.asList(selected));
        var shuffled = body.cubes.clone();
        Collections.reverse(Arrays.asList(shuffled));
        assertArrayEquals(selected, WanderingRibbitHeadSelector.select(shuffled));
        assertThrows(IllegalArgumentException.class, () -> WanderingRibbitHeadSelector.select(Arrays.copyOf(body.cubes, 14)));
        var ambiguous = body.cubes.clone(); ambiguous[1] = body.cubes[0];
        assertThrows(IllegalArgumentException.class, () -> WanderingRibbitHeadSelector.select(ambiguous));
        assertThrows(IllegalArgumentException.class, () -> WanderingRibbitHeadSelector.select(selected));
    }

    @Test
    void explicitWanderingProviderAndUnresolvedCacheAreSeparate() {
        var ordinary = new RibbitGeoIconProvider();
        var wandering = new RibbitGeoIconProvider(true);
        assertTrue(wandering.supports("ribbits:wandering_ribbit", true, false));
        assertFalse(wandering.supports("ribbits:ribbit", true, false));
        assertFalse(wandering.supports("other:gecko", true, false));
        assertFalse(wandering.supports("ribbits:wandering_ribbit", false, false));
        assertFalse(wandering.supports("ribbits:wandering_ribbit", true, true));
        var a = ordinary.cacheIdentity(null, null, null);
        var b = wandering.cacheIdentity(null, null, null);
        assertNotEquals(a, b);
        assertNotEquals(a.providerId(), b.providerId());
        assertNotEquals(a.selectorVersion(), b.selectorVersion());
        assertNotEquals(a.entityType(), b.entityType());
        ReloadGeneration.invalidate();
        assertNotEquals(b, wandering.cacheIdentity(null, null, null));
    }
}
