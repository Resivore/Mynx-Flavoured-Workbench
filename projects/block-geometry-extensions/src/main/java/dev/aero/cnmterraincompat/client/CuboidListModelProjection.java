package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.aero.cnmterraincompat.AxisModelContract;
import dev.aero.cnmterraincompat.AxisModelContract.AxisUvPolicy;
import dev.aero.cnmterraincompat.MaterialAxisState;
import dev.aero.cnmterraincompat.ProviderVisualAdapter;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Projects an explicit list of cuboids through one typed Nibaru material frame.
 *
 * <p>Geometry coordinates and texture coordinates are intentionally independent. World models use
 * identical geometry/UV bounds; dedicated item models may center the geometry while continuing to
 * sample the canonical world-space texture frame.</p>
 */
public final class CuboidListModelProjection {
    private static final Direction[] FACES = {
            Direction.DOWN, Direction.UP, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private CuboidListModelProjection() {}

    /** Creates one placed-world model. Boundary faces may participate in normal block culling. */
    public static JsonObject worldModel(NibaruMaterialProfile profile, List<Cuboid> cuboids,
            Direction.Axis materialAxis, boolean glazed) {
        MaterialFrame materialFrame = materialAxis == null ? null : MaterialFrame.direct(materialAxis);
        return model(profile, visualCuboids(profile, cuboids), materialFrame, glazed, true, false);
    }

    /**
     * Creates one axis-aware placed-world model using the canonical parent's authored UV policy.
     * Rotated policies retain world geometry through inverse model-coordinate projection; direct
     * policies retain their authored axis-specific face frames without selector rotation.
     */
    public static JsonObject axisWorldModel(NibaruMaterialProfile profile, List<Cuboid> cuboids,
            Direction.Axis worldMaterialAxis, AxisUvPolicy policy) {
        Objects.requireNonNull(worldMaterialAxis, "worldMaterialAxis");
        Objects.requireNonNull(policy, "policy");
        List<Cuboid> visual = visualCuboids(profile, cuboids);
        boolean rotated = policy != AxisUvPolicy.DIRECT_UV_LOCKED
                && worldMaterialAxis != Direction.Axis.Y;
        if (rotated) {
            visual = visual.stream()
                    .map(cuboid -> inverseMaterialCuboid(cuboid, worldMaterialAxis))
                    .toList();
        }
        MaterialFrame materialFrame = rotated
                ? new MaterialFrame(Direction.Axis.Y, policy)
                : MaterialFrame.direct(worldMaterialAxis);
        return model(profile, visual, materialFrame, false, true, false);
    }

    /** Creates one dedicated item-only model with the shared BGE display transform. */
    public static JsonObject itemModel(NibaruMaterialProfile profile, Cuboid canonical,
            Direction.Axis materialAxis, boolean glazed) {
        Cuboid visual = visualCuboid(profile, canonical);
        Bounds centered = visual.geometry().centered();
        MaterialFrame materialFrame = materialAxis == null ? null : MaterialFrame.direct(materialAxis);
        return model(profile, List.of(new Cuboid(centered, visual.uv())), materialFrame,
                glazed, false, true);
    }

    /** Applies only profile-owned geometry offsets; it never rotates a material frame. */
    public static List<Cuboid> visualCuboids(NibaruMaterialProfile profile, List<Cuboid> cuboids) {
        Objects.requireNonNull(profile, "profile");
        return cuboids.stream().map(cuboid -> visualCuboid(profile, cuboid)).toList();
    }

    private static Cuboid visualCuboid(NibaruMaterialProfile profile, Cuboid cuboid) {
        Objects.requireNonNull(cuboid, "cuboid");
        if (profile.surfaceSamplingPolicy()
                != NibaruMaterialProfile.SurfaceSamplingPolicy.PATH_LOWERED_SURFACE) {
            return cuboid;
        }
        return new Cuboid(lowerWorldTop(cuboid.geometry()), lowerWorldTop(cuboid.uv()));
    }

    private static Bounds lowerWorldTop(Bounds bounds) {
        return bounds.y1() > 15
                ? new Bounds(bounds.x0(), bounds.y0(), bounds.z0(),
                        bounds.x1(), 15, bounds.z1())
                : bounds;
    }

    private static JsonObject model(NibaruMaterialProfile profile, List<Cuboid> cuboids,
            MaterialFrame materialFrame, boolean glazed, boolean cullBoundary, boolean itemDisplay) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(cuboids, "cuboids");
        if (cuboids.isEmpty()) throw new IllegalArgumentException("A projected model needs at least one cuboid");
        if (materialFrame != null && !MaterialAxisState.applies(profile)) {
            throw new IllegalArgumentException("Material axis supplied for non-axis profile "
                    + profile.canonicalParentId());
        }
        if (glazed && profile.orientationPolicy()
                != NibaruMaterialProfile.OrientationPolicy.HORIZONTAL_FACING) {
            throw new IllegalArgumentException("Glazed projection supplied for non-oriented profile "
                    + profile.canonicalParentId());
        }

        JsonObject result = baseModel(profile, materialFrame, glazed);
        for (Cuboid cuboid : cuboids) {
            if (profile.visualProfile() == VisualProfile.GLASS_EDGE) {
                addGlassCuboid(result, cuboid, cullBoundary);
            } else if (profile.visualProfile() == VisualProfile.ROOTS) {
                addRoots(result, cuboid, cullBoundary);
            } else if (profile.insetVisualContract().isPresent()) {
                addInsetMaterial(result, profile, cuboid, materialFrame, cullBoundary);
            } else {
                addBox(result, profile, cuboid, materialFrame, false, cullBoundary);
                if (!profile.textureRoles().overlay().isEmpty()) {
                    addOverlay(result, profile, cuboid, cullBoundary);
                }
            }
        }
        if (itemDisplay) result.add("display", itemDisplay());
        return (JsonObject) ProviderVisualAdapter.decorateModel(profile, result);
    }

    private static JsonObject baseModel(NibaruMaterialProfile profile, MaterialFrame materialFrame,
            boolean glazed) {
        NibaruMaterialProfile.TextureRoles roles = profile.textureRoles();
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        if (glazed) {
            if (!roles.side().equals(roles.top()) || !roles.side().equals(roles.bottom())) {
                throw new IllegalArgumentException("Glazed cuboid projection requires one canonical texture: "
                        + profile.canonicalParentId());
            }
            textures.addProperty("side", texture(roles.side()));
            textures.addProperty("top", "#side");
            textures.addProperty("bottom", "#side");
            textures.addProperty("particle", "#side");
        } else if (materialFrame != null) {
            textures.addProperty("side", texture(roles.side()));
            textures.addProperty("top", texture(roles.top()));
            // Vanilla pillar axes are unsigned: both ends use the declared end texture.
            textures.addProperty("bottom", texture(roles.top()));
            textures.addProperty("particle", texture(roles.side()));
        } else {
            textures.addProperty("side", texture(roles.side()));
            textures.addProperty("top", texture(roles.top()));
            textures.addProperty("bottom", texture(roles.bottom()));
            textures.addProperty("particle", texture(roles.particle()));
        }
        if (!roles.overlay().isEmpty()) textures.addProperty("overlay", texture(roles.overlay()));
        model.add("textures", textures);
        model.add("elements", new JsonArray());
        return model;
    }

    private static void addInsetMaterial(JsonObject model, NibaruMaterialProfile profile,
            Cuboid outer, MaterialFrame materialFrame, boolean cullBoundary) {
        NibaruMaterialProfile.InsetVisualContract contract = profile.insetVisualContract().orElseThrow();
        boolean bottomShell = contract.shellTexture()
                == NibaruMaterialProfile.InsetVisualContract.ShellTexture.BOTTOM;
        addBox(model, profile, outer, materialFrame, bottomShell, cullBoundary);
        if (outer.geometry().isFullCube() && !contract.includeInnerLayerOnFullCube()) return;

        int xInset = contract.insetForSpan((int) Math.round(outer.geometry().xSpan()));
        int yInset = contract.insetForSpan((int) Math.round(outer.geometry().ySpan()));
        int zInset = contract.insetForSpan((int) Math.round(outer.geometry().zSpan()));
        Bounds geometry = outer.geometry().inset(xInset, yInset, zInset);
        Bounds uv = outer.uv().inset(xInset, yInset, zInset);
        if (geometry.hasVolume() && uv.hasVolume()) {
            addBox(model, profile, new Cuboid(geometry, uv), materialFrame, false, false);
        }
    }

    private static void addBox(JsonObject model, NibaruMaterialProfile profile, Cuboid cuboid,
            MaterialFrame materialFrame, boolean bottomOnly, boolean cullBoundary) {
        JsonObject element = element(cuboid.geometry());
        JsonObject faces = new JsonObject();
        for (Direction face : FACES) {
            JsonObject encoded = new JsonObject();
            encoded.addProperty("texture", bottomOnly ? "#bottom" : textureRole(face, materialFrame));
            encoded.add("uv", defaultUv(face, cuboid.uv()));
            int rotation = faceRotation(face, materialFrame);
            if (rotation != 0) encoded.addProperty("rotation", rotation);
            if (!bottomOnly && tintBaseFace(profile, face)) encoded.addProperty("tintindex", 0);
            if (cullBoundary && cuboid.geometry().onBoundary(face)) {
                encoded.addProperty("cullface", face.getSerializedName());
            }
            faces.add(face.getSerializedName(), encoded);
        }
        element.add("faces", faces);
        model.getAsJsonArray("elements").add(element);
    }

    /** Splits every cut edge into 1px cells so two-axis Corner/Column cuts retain glass borders. */
    private static void addGlassCuboid(JsonObject model, Cuboid cuboid, boolean cullBoundary) {
        List<Segment> xs = segments(cuboid.geometry().x0(), cuboid.geometry().x1(),
                Direction.WEST, Direction.EAST);
        List<Segment> ys = segments(cuboid.geometry().y0(), cuboid.geometry().y1(),
                Direction.DOWN, Direction.UP);
        List<Segment> zs = segments(cuboid.geometry().z0(), cuboid.geometry().z1(),
                Direction.NORTH, Direction.SOUTH);

        for (Segment x : xs) for (Segment y : ys) for (Segment z : zs) {
            Bounds geometry = new Bounds(x.min(), y.min(), z.min(), x.max(), y.max(), z.max());
            Bounds uv = mapBounds(geometry, cuboid.geometry(), cuboid.uv());
            EnumSet<Direction> rims = EnumSet.noneOf(Direction.class);
            if (x.rim() != null) rims.add(x.rim());
            if (y.rim() != null) rims.add(y.rim());
            if (z.rim() != null) rims.add(z.rim());

            JsonObject faces = new JsonObject();
            for (Direction face : FACES) {
                if (!geometry.touches(cuboid.geometry(), face)) continue;
                JsonObject encoded = new JsonObject();
                encoded.addProperty("texture", "#side");
                encoded.add("uv", glassUv(face, uv, rims));
                if (cullBoundary && cuboid.geometry().onBoundary(face)) {
                    encoded.addProperty("cullface", face.getSerializedName());
                }
                faces.add(face.getSerializedName(), encoded);
            }
            if (!faces.isEmpty()) {
                JsonObject element = element(geometry);
                element.add("faces", faces);
                model.getAsJsonArray("elements").add(element);
            }
        }
    }

    private static List<Segment> segments(double min, double max,
            Direction negative, Direction positive) {
        boolean cutMin = min > 0;
        boolean cutMax = max < 16;
        ArrayList<Segment> result = new ArrayList<>(3);
        double bodyMin = min;
        double bodyMax = max;
        if (cutMin) {
            double edge = Math.min(min + 1, max);
            result.add(new Segment(min, edge, negative));
            bodyMin = edge;
        }
        if (cutMax) bodyMax = Math.max(bodyMin, max - 1);
        if (bodyMax > bodyMin) result.add(new Segment(bodyMin, bodyMax, null));
        if (cutMax && max > bodyMax) result.add(new Segment(bodyMax, max, positive));
        return List.copyOf(result);
    }

    private static Bounds mapBounds(Bounds cell, Bounds geometry, Bounds uv) {
        return new Bounds(
                map(cell.x0(), geometry.x0(), geometry.x1(), uv.x0(), uv.x1()),
                map(cell.y0(), geometry.y0(), geometry.y1(), uv.y0(), uv.y1()),
                map(cell.z0(), geometry.z0(), geometry.z1(), uv.z0(), uv.z1()),
                map(cell.x1(), geometry.x0(), geometry.x1(), uv.x0(), uv.x1()),
                map(cell.y1(), geometry.y0(), geometry.y1(), uv.y0(), uv.y1()),
                map(cell.z1(), geometry.z0(), geometry.z1(), uv.z0(), uv.z1()));
    }

    private static double map(double value, double from0, double from1, double to0, double to1) {
        return to0 + (value - from0) * (to1 - to0) / (from1 - from0);
    }

    private static JsonArray glassUv(Direction face, Bounds bounds, EnumSet<Direction> rims) {
        JsonArray uv = defaultUv(face, bounds);
        FaceFrame frame = faceFrame(face);
        for (Direction rim : rims) {
            if (rim.getAxis() == face.getAxis()) continue;
            boolean positive = rim.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            if (frame.uAxis() == rim.getAxis()) {
                boolean high = positive == frame.uPositive();
                uv.set(0, new JsonPrimitive(high ? 15 : 0));
                uv.set(2, new JsonPrimitive(high ? 16 : 1));
            } else if (frame.vAxis() == rim.getAxis()) {
                boolean high = positive == frame.vPositive();
                uv.set(1, new JsonPrimitive(high ? 15 : 0));
                uv.set(3, new JsonPrimitive(high ? 16 : 1));
            }
        }
        return uv;
    }

    /** Eight-element roots topology per cuboid: two crossed planes and six boundary shells. */
    private static void addRoots(JsonObject model, Cuboid cuboid, boolean cullBoundary) {
        Bounds geometry = cuboid.geometry();
        double zMid = (geometry.z0() + geometry.z1()) / 2.0;
        double xMid = (geometry.x0() + geometry.x1()) / 2.0;
        addRootPlane(model, new Bounds(geometry.x0(), geometry.y0(), zMid,
                        geometry.x1(), geometry.y1(), zMid),
                Direction.NORTH, Direction.SOUTH, cuboid.uv(), "#side");
        addRootPlane(model, new Bounds(xMid, geometry.y0(), geometry.z0(),
                        xMid, geometry.y1(), geometry.z1()),
                Direction.EAST, Direction.WEST, cuboid.uv(), "#side");
        for (Direction face : FACES) addRootShell(model, cuboid, face, cullBoundary);
    }

    private static void addRootPlane(JsonObject model, Bounds geometry, Direction first,
            Direction second, Bounds uv, String texture) {
        JsonObject element = element(geometry);
        JsonObject faces = new JsonObject();
        for (Direction face : new Direction[]{first, second}) {
            JsonObject encoded = new JsonObject();
            encoded.addProperty("texture", texture);
            encoded.add("uv", defaultUv(face, uv));
            faces.add(face.getSerializedName(), encoded);
        }
        element.add("faces", faces);
        model.getAsJsonArray("elements").add(element);
    }

    private static void addRootShell(JsonObject model, Cuboid cuboid, Direction surface,
            boolean cullBoundary) {
        final double epsilon = 0.002;
        Bounds outer = cuboid.geometry();
        double x0 = outer.x0(), y0 = outer.y0(), z0 = outer.z0();
        double x1 = outer.x1(), y1 = outer.y1(), z1 = outer.z1();
        switch (surface) {
            case DOWN -> y1 = y0 + epsilon;
            case UP -> y0 = y1 - epsilon;
            case NORTH -> z1 = z0 + epsilon;
            case SOUTH -> z0 = z1 - epsilon;
            case WEST -> x1 = x0 + epsilon;
            case EAST -> x0 = x1 - epsilon;
        }
        JsonObject element = element(new Bounds(x0, y0, z0, x1, y1, z1));
        JsonObject faces = new JsonObject();
        String texture = surface.getAxis() == Direction.Axis.Y ? "#top" : "#side";
        for (Direction face : new Direction[]{surface, surface.getOpposite()}) {
            JsonObject encoded = new JsonObject();
            encoded.addProperty("texture", texture);
            encoded.add("uv", defaultUv(face, cuboid.uv()));
            if (cullBoundary && cuboid.geometry().onBoundary(surface)) {
                encoded.addProperty("cullface", surface.getSerializedName());
            }
            faces.add(face.getSerializedName(), encoded);
        }
        element.add("faces", faces);
        model.getAsJsonArray("elements").add(element);
    }

    private static void addOverlay(JsonObject model, NibaruMaterialProfile profile,
            Cuboid cuboid, boolean cullBoundary) {
        JsonObject element = element(cuboid.geometry());
        JsonObject faces = new JsonObject();
        for (Direction face : new Direction[]{Direction.NORTH, Direction.EAST,
                Direction.SOUTH, Direction.WEST}) {
            JsonObject encoded = new JsonObject();
            encoded.addProperty("texture", "#overlay");
            encoded.addProperty("tintindex", 0);
            encoded.add("uv", overlayUv(face, cuboid.uv()));
            if (cullBoundary && cuboid.geometry().onBoundary(face)) {
                encoded.addProperty("cullface", face.getSerializedName());
            }
            faces.add(face.getSerializedName(), encoded);
        }
        element.add("faces", faces);
        model.getAsJsonArray("elements").add(element);
    }

    private static JsonArray overlayUv(Direction face, Bounds bounds) {
        double height = bounds.ySpan();
        return switch (face) {
            case NORTH -> numbers(16 - bounds.x1(), 0, 16 - bounds.x0(), height);
            case SOUTH -> numbers(bounds.x0(), 0, bounds.x1(), height);
            case EAST -> numbers(16 - bounds.z1(), 0, 16 - bounds.z0(), height);
            case WEST -> numbers(bounds.z0(), 0, bounds.z1(), height);
            default -> throw new IllegalArgumentException("Overlay is horizontal-side only: " + face);
        };
    }

    private static JsonArray defaultUv(Direction face, Bounds bounds) {
        return switch (face) {
            case DOWN -> numbers(bounds.x0(), 16 - bounds.z1(), bounds.x1(), 16 - bounds.z0());
            case UP -> numbers(bounds.x0(), bounds.z0(), bounds.x1(), bounds.z1());
            case NORTH -> numbers(16 - bounds.x1(), 16 - bounds.y1(),
                    16 - bounds.x0(), 16 - bounds.y0());
            case SOUTH -> numbers(bounds.x0(), 16 - bounds.y1(), bounds.x1(), 16 - bounds.y0());
            case WEST -> numbers(bounds.z0(), 16 - bounds.y1(), bounds.z1(), 16 - bounds.y0());
            case EAST -> numbers(16 - bounds.z1(), 16 - bounds.y1(),
                    16 - bounds.z0(), 16 - bounds.y0());
        };
    }

    private static boolean tintBaseFace(NibaruMaterialProfile profile, Direction face) {
        if (profile.tintProfile() == TintProfile.NONE) return false;
        return profile.visualProfile() != VisualProfile.GRASS_OVERLAY || face == Direction.UP;
    }

    /** World-space TOP/SIDE/BOTTOM roles stay fixed even when geometry occupancy changes. */
    private static String textureRole(Direction face, MaterialFrame materialFrame) {
        if (materialFrame == null) {
            return switch (face) {
                case UP -> "#top";
                case DOWN -> "#bottom";
                default -> "#side";
            };
        }
        if (face.getAxis() != materialFrame.axis()) return "#side";
        return switch (face) {
            case EAST, UP, SOUTH -> "#top";
            case WEST, DOWN, NORTH -> "#bottom";
        };
    }

    private static int faceRotation(Direction face, MaterialFrame materialFrame) {
        if (materialFrame == null) return 0;
        if (materialFrame.policy() == AxisUvPolicy.HORIZONTAL_ROTATED) {
            return face == Direction.UP ? 180 : 0;
        }
        if (materialFrame.policy() == AxisUvPolicy.STANDARD_ROTATED) return 0;
        return switch (materialFrame.axis()) {
            case X -> face.getAxis() == Direction.Axis.X ? 0 : 90;
            case Y -> 0;
            case Z -> face.getAxis() == Direction.Axis.X ? 90 : 0;
        };
    }

    private static Cuboid inverseMaterialCuboid(Cuboid world, Direction.Axis materialAxis) {
        return new Cuboid(
                inverseMaterialBounds(world.geometry(), materialAxis),
                inverseMaterialBounds(world.uv(), materialAxis));
    }

    private static Bounds inverseMaterialBounds(Bounds world, Direction.Axis materialAxis) {
        var rotation = AxisModelContract.materialRotation(materialAxis);
        double[] worldMin = {world.x0(), world.y0(), world.z0()};
        double[] worldMax = {world.x1(), world.y1(), world.z1()};
        double[] modelMin = new double[3];
        double[] modelMax = new double[3];
        for (Direction.Axis modelAxis : Direction.Axis.values()) {
            Direction worldDirection = rotation.rotate(positiveDirection(modelAxis));
            int worldIndex = axisIndex(worldDirection.getAxis());
            int modelIndex = axisIndex(modelAxis);
            if (isPositive(worldDirection)) {
                modelMin[modelIndex] = worldMin[worldIndex];
                modelMax[modelIndex] = worldMax[worldIndex];
            } else {
                modelMin[modelIndex] = 16 - worldMax[worldIndex];
                modelMax[modelIndex] = 16 - worldMin[worldIndex];
            }
        }
        return new Bounds(modelMin[0], modelMin[1], modelMin[2],
                modelMax[0], modelMax[1], modelMax[2]);
    }

    private static Direction positiveDirection(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.EAST;
            case Y -> Direction.UP;
            case Z -> Direction.SOUTH;
        };
    }

    private static int axisIndex(Direction.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }

    private static boolean isPositive(Direction direction) {
        return direction == Direction.EAST || direction == Direction.UP
                || direction == Direction.SOUTH;
    }

    private static FaceFrame faceFrame(Direction face) {
        return switch (face) {
            case DOWN -> new FaceFrame(Direction.Axis.X, true, Direction.Axis.Z, false);
            case UP -> new FaceFrame(Direction.Axis.X, true, Direction.Axis.Z, true);
            case NORTH -> new FaceFrame(Direction.Axis.X, false, Direction.Axis.Y, false);
            case SOUTH -> new FaceFrame(Direction.Axis.X, true, Direction.Axis.Y, false);
            case WEST -> new FaceFrame(Direction.Axis.Z, true, Direction.Axis.Y, false);
            case EAST -> new FaceFrame(Direction.Axis.Z, false, Direction.Axis.Y, false);
        };
    }

    private static JsonObject element(Bounds bounds) {
        JsonObject result = new JsonObject();
        result.add("from", numbers(bounds.x0(), bounds.y0(), bounds.z0()));
        result.add("to", numbers(bounds.x1(), bounds.y1(), bounds.z1()));
        return result;
    }

    private static JsonObject itemDisplay() {
        JsonObject display = new JsonObject();
        JsonObject firstPerson = transform(new int[]{0, -45, 0}, new double[]{0.4, 0.4, 0.4});
        display.add("firstperson_righthand", firstPerson);
        display.add("firstperson_lefthand", firstPerson.deepCopy());
        display.add("gui", transform(new int[]{30, -135, 0}, new double[]{0.625, 0.625, 0.625}));
        display.add("fixed", transform(new int[]{0, 90, 0}, new double[]{0.5, 0.5, 0.5}));
        return display;
    }

    private static JsonObject transform(int[] rotation, double[] scale) {
        JsonObject result = new JsonObject();
        result.add("rotation", numbers(rotation[0], rotation[1], rotation[2]));
        result.add("scale", numbers(scale[0], scale[1], scale[2]));
        return result;
    }

    private static String texture(String path) {
        return path.contains(":") ? path : "minecraft:block/" + path;
    }

    private static JsonArray numbers(double... values) {
        JsonArray result = new JsonArray();
        for (double value : values) {
            if (value == Math.rint(value) && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                result.add((int) value);
            } else {
                result.add(value);
            }
        }
        return result;
    }

    private record FaceFrame(Direction.Axis uAxis, boolean uPositive,
            Direction.Axis vAxis, boolean vPositive) {}

    /** Axis semantics in the coordinates authored by this model. */
    private record MaterialFrame(Direction.Axis axis, AxisUvPolicy policy) {
        private MaterialFrame {
            Objects.requireNonNull(axis, "axis");
            Objects.requireNonNull(policy, "policy");
        }

        private static MaterialFrame direct(Direction.Axis axis) {
            return new MaterialFrame(axis, AxisUvPolicy.DIRECT_UV_LOCKED);
        }
    }

    private record Segment(double min, double max, Direction rim) {}

    /** One render cuboid with separately addressable model and canonical UV bounds. */
    public record Cuboid(Bounds geometry, Bounds uv) {
        public Cuboid {
            Objects.requireNonNull(geometry, "geometry");
            Objects.requireNonNull(uv, "uv");
            if (!geometry.hasVolume() || !uv.hasVolume()) {
                throw new IllegalArgumentException("Cuboid bounds must have positive volume");
            }
        }

        public static Cuboid world(Bounds bounds) {
            return new Cuboid(bounds, bounds);
        }
    }

    /** Exact model-unit bounds. Zero-thickness bounds are allowed for ROOTS planes only. */
    public record Bounds(double x0, double y0, double z0,
            double x1, double y1, double z1) {
        public Bounds {
            if (x0 < 0 || y0 < 0 || z0 < 0 || x1 > 16 || y1 > 16 || z1 > 16
                    || x1 < x0 || y1 < y0 || z1 < z0) {
                throw new IllegalArgumentException("Invalid model bounds: "
                        + x0 + "," + y0 + "," + z0 + " -> " + x1 + "," + y1 + "," + z1);
            }
        }

        public double xSpan() { return x1 - x0; }
        public double ySpan() { return y1 - y0; }
        public double zSpan() { return z1 - z0; }
        public boolean hasVolume() { return xSpan() > 0 && ySpan() > 0 && zSpan() > 0; }
        public boolean isFullCube() {
            return x0 == 0 && y0 == 0 && z0 == 0 && x1 == 16 && y1 == 16 && z1 == 16;
        }

        public Bounds centered() {
            double nx0 = (16 - xSpan()) / 2.0;
            double ny0 = (16 - ySpan()) / 2.0;
            double nz0 = (16 - zSpan()) / 2.0;
            return new Bounds(nx0, ny0, nz0, nx0 + xSpan(), ny0 + ySpan(), nz0 + zSpan());
        }

        public Bounds inset(double x, double y, double z) {
            return new Bounds(x0 + x, y0 + y, z0 + z, x1 - x, y1 - y, z1 - z);
        }

        public boolean onBoundary(Direction face) {
            return switch (face) {
                case DOWN -> y0 == 0;
                case UP -> y1 == 16;
                case NORTH -> z0 == 0;
                case SOUTH -> z1 == 16;
                case WEST -> x0 == 0;
                case EAST -> x1 == 16;
            };
        }

        public boolean touches(Bounds outer, Direction face) {
            return switch (face) {
                case DOWN -> y0 == outer.y0;
                case UP -> y1 == outer.y1;
                case NORTH -> z0 == outer.z0;
                case SOUTH -> z1 == outer.z1;
                case WEST -> x0 == outer.x0;
                case EAST -> x1 == outer.x1;
            };
        }
    }
}
