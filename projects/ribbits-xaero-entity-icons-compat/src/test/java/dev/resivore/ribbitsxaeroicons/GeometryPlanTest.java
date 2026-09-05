package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.joml.Matrix4f;
import org.joml.Vector4f;

class GeometryPlanTest {
    private static final long GENERATION = 17L;

    @Test
    void universalSelectorAndFramingUseEveryDirectBodyCubeAcrossVisualStates() {
        List<VisualCase> cases = visualCases();

        for (int index = 0; index < cases.size(); index++) {
            VisualCase visual = cases.get(index);
            Fixture fixture = fixture(visual.directCubeCount(), index);
            var selection = RibbitHeadSelector.select(List.of(new BoneView(fixture.main())))
                    .orElseThrow(() -> new AssertionError(visual.name()));

            assertSame(fixture.main(), ((BoneView) selection.main()).bone(), visual.name());
            assertSame(fixture.body(), ((BoneView) selection.body()).bone(), visual.name());
            assertEquals(Arrays.asList(fixture.directCubes()), selection.cubes(), visual.name());
            assertTrue(selection.cubes().stream().noneMatch(
                    cube -> cube == fixture.descendantCube() || cube == fixture.siblingCube()),
                    visual.name());

            GeometryPlan plan = GeometryPlan.create(
                    fixture.main(), fixture.body(), fixture.directCubes());
            assertEquals(Arrays.asList(fixture.directCubes()), plan.cubes(), visual.name());
            for (int cube = 0; cube < fixture.directCubes().length; cube++) {
                assertSame(fixture.directCubes()[cube], plan.cubes().get(cube),
                        visual.name() + " cube " + cube);
            }
            assertThrows(UnsupportedOperationException.class,
                    () -> plan.cubes().add(fixture.siblingCube()), visual.name());

            float xaeroScale = visual.name().equals("baby") ? 0.5F : 1.0F;
            PoseStack first = new PoseStack();
            PoseStack second = new PoseStack();
            plan.applyFraming(first, xaeroScale);
            plan.applyFraming(second, xaeroScale);
            assertTrue(TransformSafety.isFiniteAndInvertibleMatrix(first.last().pose()), visual.name());
            assertArrayEquals(matrix(first), matrix(second), 0.0F, visual.name());

            ScreenBounds screen = screenBounds(first, plan.cubes());
            assertEquals(GeometryPlan.FRAME_CENTER, screen.centerX(), 0.001, visual.name());
            assertEquals(GeometryPlan.FRAME_CENTER, screen.centerY(), 0.001, visual.name());
            assertEquals(GeometryPlan.FRAME_SPAN * xaeroScale,
                    Math.max(screen.width(), screen.height()), 0.001, visual.name());
            assertTrue(Double.isFinite(screen.minZ) && Double.isFinite(screen.maxZ),
                    visual.name());
        }
    }

    @Test
    void realisticVisualIdentitiesStayBoundedAndNormalizeTheBabyCase() {
        List<VisualCase> cases = visualCases();
        CacheIdentity ordinary = cases.getFirst().identity();
        CacheIdentity baby = cases.getLast().identity();

        assertEquals(ordinary, baby,
                "normalized baby framing intentionally reuses the ordinary stable identity");
        assertEquals(5L, cases.stream().map(VisualCase::identity).distinct().count(),
                "profession, instrument, umbrella, and Pride inputs remain distinct");
        assertTrue(cases.stream().allMatch(visual -> visual.identity().babyPolicy()
                .equals(RibbitGeoIconProvider.BABY_POLICY)));
    }

    @Test
    void animationSnapshotsAreRejectedAtPlanCreationAndIfTheyAppearBeforeFraming() {
        Fixture fixture = fixture(3, 0);
        fixture.main().frameSnapshot = BoneSnapshot.create(fixture.main());
        assertThrows(IllegalArgumentException.class, () -> GeometryPlan.create(
                fixture.main(), fixture.body(), fixture.directCubes()));

        fixture.main().frameSnapshot = null;
        fixture.body().frameSnapshot = BoneSnapshot.create(fixture.body());
        assertThrows(IllegalArgumentException.class, () -> GeometryPlan.create(
                fixture.main(), fixture.body(), fixture.directCubes()));

        fixture.body().frameSnapshot = null;
        GeometryPlan plan = GeometryPlan.create(
                fixture.main(), fixture.body(), fixture.directCubes());
        fixture.body().frameSnapshot = BoneSnapshot.create(fixture.body());
        assertThrows(IllegalArgumentException.class,
                () -> plan.applyFraming(new PoseStack(), 1.0F));
    }

    @Test
    void framingClampsOverscaleRejectsInvalidScaleAndKeepsTheFixedFrontOrientation() {
        Fixture fixture = flatFixture();
        GeometryPlan plan = GeometryPlan.create(
                fixture.main(), fixture.body(), fixture.directCubes());
        PoseStack normal = new PoseStack();
        PoseStack oversized = new PoseStack();
        plan.applyFraming(normal, 1.0F);
        plan.applyFraming(oversized, 4.0F);
        assertArrayEquals(matrix(normal), matrix(oversized), 0.0F);

        assertThrows(IllegalArgumentException.class,
                () -> plan.applyFraming(new PoseStack(), 0.0F));
        assertThrows(IllegalArgumentException.class,
                () -> plan.applyFraming(new PoseStack(), -1.0F));
        assertThrows(IllegalArgumentException.class,
                () -> plan.applyFraming(new PoseStack(), Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> plan.applyFraming(new PoseStack(), Float.POSITIVE_INFINITY));

        GeoCube cube = plan.cubes().getFirst();
        Vector4f left = transform(normal, cube, cube.quads()[0].vertices()[0]);
        Vector4f right = transform(normal, cube, cube.quads()[0].vertices()[1]);
        assertTrue(left.x < right.x, "baked X must retain its front-view direction");
    }

    @Test
    void planDefensivelyCopiesTheSelectedCubeArray() {
        Fixture fixture = fixture(3, 0);
        GeoCube first = fixture.directCubes()[0];
        GeometryPlan plan = GeometryPlan.create(
                fixture.main(), fixture.body(), fixture.directCubes());

        fixture.directCubes()[0] = fixture.siblingCube();

        assertSame(first, plan.cubes().getFirst());
        assertTrue(plan.cubes().stream().noneMatch(cube -> cube == fixture.siblingCube()));
    }

    @Test
    void invalidBoneCubeQuadAndVertexDataFailClosedBeforeFraming() {
        assertPlanRejected(new GeoCube[] {null});

        GeoCube valid = cube(0.0F, 0.0F);
        assertPlanRejected(new GeoCube[] {new GeoCube(
                new GeoQuad[] {valid.quads()[0], null},
                valid.pivot(), valid.rotation(), valid.size())});
        assertPlanRejected(new GeoCube[] {new GeoCube(
                valid.quads(), new Vec3(Double.NaN, 0.0, 0.0),
                valid.rotation(), valid.size())});

        GeoVertex[] invalidVertices = valid.quads()[0].vertices().clone();
        invalidVertices[0] = new GeoVertex(0.0F, 0.0F, 0.0F, Float.NaN, 0.0F);
        assertPlanRejected(new GeoCube[] {new GeoCube(
                new GeoQuad[] {new GeoQuad(
                        invalidVertices, 0.0F, 0.0F, 1.0F, Direction.NORTH)},
                valid.pivot(), valid.rotation(), valid.size())});

        GeoBone[] children = new GeoBone[1];
        CuboidGeoBone main = bone(
                null, "main", children, new GeoCube[0],
                Float.NaN, 0, 0, 0, 0, 0);
        CuboidGeoBone body = bone(
                main, "body", new GeoBone[0], new GeoCube[] {valid},
                0, 0, 0, 0, 0, 0);
        children[0] = body;
        assertThrows(IllegalArgumentException.class,
                () -> GeometryPlan.create(main, body, body.cubes));
    }

    private static List<VisualCase> visualCases() {
        CacheIdentity ordinary = identity(
                "ribbits:geckolib/models/ribbit.geo.json",
                "ribbits:textures/entity/ribbit.png", "nitwit", false);
        return List.of(
                new VisualCase("ordinary", 3, ordinary),
                new VisualCase("profession", 4, identity(
                        "ribbits:geckolib/models/chef_ribbit.geo.json",
                        "ribbits:textures/entity/chef_ribbit.png", "chef", false)),
                new VisualCase("instrument", 9, identity(
                        "ribbits:geckolib/models/instrument/banjo_ribbit.geo.json",
                        "ribbits:textures/entity/ribbit.png", "nitwit", false)),
                new VisualCase("umbrella", 10, identity(
                        "ribbits:geckolib/models/umbrella/nitwit_ribbit.geo.json",
                        "ribbits:textures/entity/ribbit.png", "nitwit", false)),
                new VisualCase("Pride", 3, identity(
                        "ribbits:geckolib/models/pride_ribbit.geo.json",
                        "ribbits:textures/entity/pride_ribbit.png", "nitwit", true)),
                new VisualCase("baby", 3, ordinary));
    }

    private static CacheIdentity identity(
            String modelId, String textureId, String professionId, boolean pride) {
        return new CacheIdentity(
                RibbitGeoIconProvider.ENTITY_TYPE,
                RibbitGeoIconProvider.PROVIDER_ID,
                RibbitGeoIconProvider.SELECTOR_VERSION,
                modelId,
                textureId,
                professionId,
                RibbitGeoIconProvider.BABY_POLICY,
                pride,
                GENERATION);
    }

    private static Fixture fixture(int directCubeCount, int variant) {
        GeoBone[] mainChildren = new GeoBone[2];
        CuboidGeoBone main = bone(
                null, "main", mainChildren, new GeoCube[] {cube(-2.0F, -2.0F)},
                1.0F + variant, 2.0F, 3.0F, 0.03F, -0.02F, 0.01F);

        GeoCube[] direct = new GeoCube[directCubeCount];
        for (int index = 0; index < direct.length; index++) {
            direct[index] = cube(index * 1.25F, (index % 3) * 0.75F);
        }
        GeoBone[] bodyChildren = new GeoBone[1];
        CuboidGeoBone body = bone(
                main, "body", bodyChildren, direct,
                -0.5F, 1.0F + variant * 0.1F, 0.25F, -0.01F, 0.04F, 0.02F);

        GeoCube descendantCube = cube(100.0F, 100.0F);
        bodyChildren[0] = bone(body, "held_item", new GeoBone[0],
                new GeoCube[] {descendantCube}, 0, 0, 0, 0, 0, 0);
        GeoCube siblingCube = cube(-100.0F, -100.0F);
        mainChildren[0] = body;
        mainChildren[1] = bone(main, "umbrella", new GeoBone[0],
                new GeoCube[] {siblingCube}, 0, 0, 0, 0, 0, 0);
        return new Fixture(main, body, direct, descendantCube, siblingCube);
    }

    private static Fixture flatFixture() {
        GeoBone[] mainChildren = new GeoBone[2];
        CuboidGeoBone main = bone(
                null, "main", mainChildren, new GeoCube[0],
                0, 0, 0, 0, 0, 0);
        GeoCube directCube = cube(0.0F, 0.0F);
        GeoBone[] bodyChildren = new GeoBone[1];
        CuboidGeoBone body = bone(
                main, "body", bodyChildren, new GeoCube[] {directCube},
                0, 0, 0, 0, 0, 0);
        GeoCube descendantCube = cube(100.0F, 100.0F);
        bodyChildren[0] = bone(body, "child", new GeoBone[0],
                new GeoCube[] {descendantCube}, 0, 0, 0, 0, 0, 0);
        GeoCube siblingCube = cube(-100.0F, -100.0F);
        mainChildren[0] = body;
        mainChildren[1] = bone(main, "sibling", new GeoBone[0],
                new GeoCube[] {siblingCube}, 0, 0, 0, 0, 0, 0);
        return new Fixture(
                main, body, new GeoCube[] {directCube}, descendantCube, siblingCube);
    }

    private static void assertPlanRejected(GeoCube[] directCubes) {
        GeoBone[] children = new GeoBone[1];
        CuboidGeoBone main = bone(
                null, "main", children, new GeoCube[0],
                0, 0, 0, 0, 0, 0);
        CuboidGeoBone body = bone(
                main, "body", new GeoBone[0], directCubes,
                0, 0, 0, 0, 0, 0);
        children[0] = body;
        assertThrows(IllegalArgumentException.class,
                () -> GeometryPlan.create(main, body, directCubes));
    }

    private static CuboidGeoBone bone(
            GeoBone parent,
            String name,
            GeoBone[] children,
            GeoCube[] cubes,
            float pivotX,
            float pivotY,
            float pivotZ,
            float rotX,
            float rotY,
            float rotZ) {
        return new CuboidGeoBone(
                parent,
                name,
                children,
                cubes,
                new GeoLocator[0],
                pivotX,
                pivotY,
                pivotZ,
                rotX,
                rotY,
                rotZ);
    }

    private static GeoCube cube(float x, float y) {
        GeoVertex[] vertices = {
                new GeoVertex(x, y, 0.0F, 0.0F, 0.0F),
                new GeoVertex(x + 1.0F, y, 0.0F, 1.0F, 0.0F),
                new GeoVertex(x + 1.0F, y + 1.0F, 0.0F, 1.0F, 1.0F),
                new GeoVertex(x, y + 1.0F, 0.0F, 0.0F, 1.0F)
        };
        GeoQuad quad = new GeoQuad(vertices, 0.0F, 0.0F, 1.0F, Direction.NORTH);
        return new GeoCube(
                new GeoQuad[] {quad},
                new Vec3(0.0, 0.0, 0.0),
                new Vec3(0.0, 0.0, 0.0),
                new Vec3(1.0, 1.0, 1.0));
    }

    private static float[] matrix(PoseStack pose) {
        return pose.last().pose().get(new float[16]);
    }

    private static ScreenBounds screenBounds(PoseStack pose, List<GeoCube> cubes) {
        ScreenBounds bounds = new ScreenBounds();
        for (GeoCube cube : cubes) {
            pose.pushPose();
            try {
                cube.translateToPivotPoint(pose);
                cube.rotate(pose);
                cube.translateAwayFromPivotPoint(pose);
                Matrix4f matrix = pose.last().pose();
                for (GeoQuad quad : cube.quads()) {
                    for (GeoVertex vertex : quad.vertices()) {
                        Vector4f transformed = matrix.transform(new Vector4f(
                                vertex.posX(), vertex.posY(), vertex.posZ(), 1.0F));
                        bounds.include(
                                transformed.x / transformed.w,
                                transformed.y / transformed.w,
                                transformed.z / transformed.w);
                    }
                }
            } finally {
                pose.popPose();
            }
        }
        return bounds;
    }

    private static Vector4f transform(PoseStack pose, GeoCube cube, GeoVertex vertex) {
        pose.pushPose();
        try {
            cube.translateToPivotPoint(pose);
            cube.rotate(pose);
            cube.translateAwayFromPivotPoint(pose);
            return pose.last().pose().transform(new Vector4f(
                    vertex.posX(), vertex.posY(), vertex.posZ(), 1.0F));
        } finally {
            pose.popPose();
        }
    }

    private record VisualCase(
            String name, int directCubeCount, CacheIdentity identity) {
    }

    private record Fixture(
            CuboidGeoBone main,
            CuboidGeoBone body,
            GeoCube[] directCubes,
            GeoCube descendantCube,
            GeoCube siblingCube) {
    }

    private static final class ScreenBounds {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private void include(double x, double y, double z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        private double width() {
            return maxX - minX;
        }

        private double height() {
            return maxY - minY;
        }

        private double centerX() {
            return (minX + maxX) * 0.5;
        }

        private double centerY() {
            return (minY + maxY) * 0.5;
        }
    }

    private record BoneView(GeoBone bone) implements RibbitHeadSelector.BoneView<GeoCube> {
        @Override
        public String name() {
            return bone.name();
        }

        @Override
        public BoneView parent() {
            return bone.parent() == null ? null : new BoneView(bone.parent());
        }

        @Override
        public List<BoneView> children() {
            return Arrays.stream(bone.children()).map(BoneView::new).toList();
        }

        @Override
        public List<GeoCube> directCubes() {
            return bone instanceof CuboidGeoBone cuboid
                    ? Arrays.asList(cuboid.cubes)
                    : List.of();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof BoneView view && bone == view.bone;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(bone);
        }
    }
}
