package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.aero.cnmterraincompat.GlazedPatternState;
import dev.aero.cnmterraincompat.MaterialAxisState;
import dev.aero.cnmterraincompat.ProviderVisualAdapter;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pure ordinary-JSON projection of one BGE Layer onto a typed material profile.
 *
 * <p>The seam projects ordinary typed cuboid bounds and the narrowly classified Glass-edge and
 * Roots topologies required by the current catalog. It does not extract or transform arbitrary
 * authored geometry; Corner is the phase that will exercise that larger UV problem.</p>
 */
public final class LayerModelProjection {
    private static final Direction[] FACINGS = {
            Direction.UP, Direction.DOWN, Direction.NORTH,
            Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private static final Direction.Axis[] AXES = {
            Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z
    };
    private static final Direction[] PATTERN_FACINGS = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private LayerModelProjection() {}

    /** Builds the complete model/blockstate projection for one registered Layer block. */
    public static Projection project(NibaruMaterialProfile profile, Identifier shape,
            boolean canonicalFullModelReusable) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(shape, "shape");
        boolean axis = MaterialAxisState.applies(profile);
        boolean glazed = profile.capabilities().contains(BehaviorCapability.GLAZED_ORIENTATION);
        if (axis && glazed) {
            throw new IllegalArgumentException("Layer projection does not support a material that is both axis-"
                    + "aligned and glazed-oriented: " + profile.canonicalParentId());
        }

        LinkedHashMap<String, JsonObject> models = new LinkedHashMap<>();
        JsonObject variants = new JsonObject();
        String itemModel;
        if (axis) {
            itemModel = axisModelId(shape, Direction.UP, 1, Direction.Axis.Y);
            for (Direction facing : FACINGS) for (int layers = 1; layers <= 4; layers++) {
                for (Direction.Axis materialAxis : AXES) {
                    String modelId = axisModelId(shape, facing, layers, materialAxis);
                    models.putIfAbsent(modelId, cuboidModel(profile, facing, layers, materialAxis, false));
                    variants.add(variantKey(facing, layers, materialAxis, null), apply(modelId, 0, false));
                }
            }
        } else if (glazed) {
            itemModel = glazedModelId(shape, Direction.UP, 1);
            for (Direction facing : FACINGS) for (int layers = 1; layers <= 4; layers++) {
                for (Direction pattern : PATTERN_FACINGS) {
                    Direction relative = relativeFacing(facing, pattern);
                    String modelId = glazedModelId(shape, relative, layers);
                    models.putIfAbsent(modelId, cuboidModel(profile, relative, layers, null, true));
                    variants.add(variantKey(facing, layers, null, pattern),
                            apply(modelId, GlazedPatternState.patternYaw(pattern), true));
                }
            }
        } else {
            itemModel = regularModelId(shape, Direction.UP, 1);
            String canonical = canonicalModelId(profile.canonicalParentId());
            for (Direction facing : FACINGS) for (int layers = 1; layers <= 4; layers++) {
                String modelId;
                if (layers == 4 && canonicalFullModelReusable) {
                    modelId = canonical;
                } else {
                    modelId = regularModelId(shape, facing, layers);
                    models.putIfAbsent(modelId, cuboidModel(profile, facing, layers, null, false));
                }
                variants.add(variantKey(facing, layers, null, null), apply(modelId, 0, false));
            }
        }

        JsonObject itemTarget = models.get(itemModel);
        if (itemTarget == null) {
            throw new IllegalStateException("Layer item model was not generated: " + itemModel);
        }
        itemTarget.add("display", itemDisplay());
        JsonObject blockState = new JsonObject();
        blockState.add("variants", variants);
        return new Projection(blockState, models, itemModel);
    }

    /** Exact BBB-compatible quarter-layer bounds for the stored exposed-face direction. */
    public static Bounds bounds(Direction facing, int layers) {
        Objects.requireNonNull(facing, "facing");
        if (layers < 1 || layers > 4) throw new IllegalArgumentException("Layer count must be 1..4");
        int depth = layers * 4;
        return switch (facing) {
            case UP -> new Bounds(0, 0, 0, 16, depth, 16);
            case DOWN -> new Bounds(0, 16 - depth, 0, 16, 16, 16);
            case NORTH -> new Bounds(0, 0, 16 - depth, 16, 16, 16);
            case SOUTH -> new Bounds(0, 0, 0, 16, 16, depth);
            case EAST -> new Bounds(0, 0, 0, depth, 16, 16);
            case WEST -> new Bounds(16 - depth, 0, 0, 16, 16, 16);
        };
    }

    /** One directly testable projected model, including typed textures, tint and render layer. */
    public static JsonObject cuboidModel(NibaruMaterialProfile profile, Direction facing, int layers,
            Direction.Axis materialAxis, boolean glazed) {
        if (materialAxis != null && !MaterialAxisState.applies(profile)) {
            throw new IllegalArgumentException("Material axis supplied for non-axis profile "
                    + profile.canonicalParentId());
        }
        if (glazed && profile.orientationPolicy() != NibaruMaterialProfile.OrientationPolicy.HORIZONTAL_FACING) {
            throw new IllegalArgumentException("Glazed projection supplied for non-oriented profile "
                    + profile.canonicalParentId());
        }
        JsonObject model = baseModel(profile, materialAxis, glazed);
        Bounds cuboid = visualBounds(profile, facing, layers);
        if (profile.visualProfile() == VisualProfile.GLASS_EDGE) {
            addGlassEdge(model, cuboid, facing);
        } else if (profile.visualProfile() == VisualProfile.ROOTS) {
            addRoots(model, cuboid);
        } else if (profile.insetVisualContract().isPresent()) {
            addInsetMaterial(model, profile, cuboid, materialAxis);
        } else {
            addBox(model, profile, cuboid, materialAxis, false, true);
            if (!profile.textureRoles().overlay().isEmpty()) addOverlay(model, profile, cuboid);
        }
        return (JsonObject) ProviderVisualAdapter.decorateModel(profile, model);
    }

    public static int selectorCount(NibaruMaterialProfile profile) {
        boolean axis = MaterialAxisState.applies(profile);
        boolean glazed = profile.capabilities().contains(BehaviorCapability.GLAZED_ORIENTATION);
        if (axis && glazed) return 288;
        if (axis) return 72;
        if (glazed) return 96;
        return 24;
    }

    private static JsonObject baseModel(NibaruMaterialProfile profile, Direction.Axis materialAxis,
            boolean glazed) {
        NibaruMaterialProfile.TextureRoles roles = profile.textureRoles();
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        if (glazed) {
            if (!roles.side().equals(roles.top()) || !roles.side().equals(roles.bottom())) {
                throw new IllegalArgumentException("Glazed Layer requires one canonical texture: "
                        + profile.canonicalParentId());
            }
            textures.addProperty("side", texture(roles.side()));
            textures.addProperty("top", "#side");
            textures.addProperty("bottom", "#side");
            textures.addProperty("particle", "#side");
        } else if (materialAxis != null) {
            textures.addProperty("side", texture(roles.side()));
            textures.addProperty("top", texture(roles.top()));
            // Vanilla pillar axes are unsigned; both ends use the declared end role.
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
            Bounds outer, Direction.Axis materialAxis) {
        NibaruMaterialProfile.InsetVisualContract contract = profile.insetVisualContract().orElseThrow();
        boolean bottomShell = contract.shellTexture()
                == NibaruMaterialProfile.InsetVisualContract.ShellTexture.BOTTOM;
        addBox(model, profile, outer, materialAxis, bottomShell, true);
        if (outer.isFullCube() && !contract.includeInnerLayerOnFullCube()) return;

        int xInset = contract.insetForSpan(outer.x1() - outer.x0());
        int yInset = contract.insetForSpan(outer.y1() - outer.y0());
        int zInset = contract.insetForSpan(outer.z1() - outer.z0());
        Bounds inner = new Bounds(outer.x0() + xInset, outer.y0() + yInset, outer.z0() + zInset,
                outer.x1() - xInset, outer.y1() - yInset, outer.z1() - zInset);
        if (inner.hasVolume()) addBox(model, profile, inner, materialAxis, false, false);
    }

    private static void addBox(JsonObject model, NibaruMaterialProfile profile, Bounds bounds,
            Direction.Axis materialAxis, boolean bottomOnly, boolean cullBoundary) {
        JsonObject element = new JsonObject();
        element.add("from", numbers(bounds.x0(), bounds.y0(), bounds.z0()));
        element.add("to", numbers(bounds.x1(), bounds.y1(), bounds.z1()));
        JsonObject faces = new JsonObject();
        for (Direction face : FACINGS) {
            JsonObject encoded = new JsonObject();
            encoded.addProperty("texture", bottomOnly ? "#bottom" : textureRole(face, materialAxis));
            int rotation = faceRotation(face, materialAxis);
            if (rotation != 0) encoded.addProperty("rotation", rotation);
            if (!bottomOnly && tintBaseFace(profile, face)) encoded.addProperty("tintindex", 0);
            if (cullBoundary && bounds.onBoundary(face)) {
                encoded.addProperty("cullface", face.getSerializedName());
            }
            faces.add(face.getSerializedName(), encoded);
        }
        element.add("faces", faces);
        model.getAsJsonArray("elements").add(element);
    }

    /**
     * Preserves the accepted GLASS_EDGE cut-border contract for one cuboid without
     * importing the authored Step/Vertical templates. A partial Layer is split one
     * pixel inward from its exposed face; the thin rim samples the corresponding
     * texture border and the remainder keeps block-absolute UVs.
     */
    private static void addGlassEdge(JsonObject model, Bounds bounds, Direction exposed) {
        if (bounds.isFullCube()) {
            addGlassPart(model, bounds, exposed, null, false);
            return;
        }
        Bounds rim = exposedSlice(bounds, exposed, 1);
        Bounds body = withoutExposedSlice(bounds, exposed, 1);
        addGlassPart(model, body, exposed, exposed, false);
        addGlassPart(model, rim, exposed, exposed.getOpposite(), true);
    }

    private static void addGlassPart(JsonObject model, Bounds bounds, Direction exposed,
            Direction omittedFace, boolean rim) {
        JsonObject element = element(bounds);
        JsonObject faces = new JsonObject();
        for (Direction face : FACINGS) {
            if (face == omittedFace) continue;
            JsonObject encoded = new JsonObject();
            encoded.addProperty("texture", "#side");
            encoded.add("uv", rim && face.getAxis() != exposed.getAxis()
                    ? rimUv(face, exposed, bounds) : defaultUv(face, bounds));
            if (bounds.onBoundary(face)) encoded.addProperty("cullface", face.getSerializedName());
            faces.add(face.getSerializedName(), encoded);
        }
        element.add("faces", faces);
        model.getAsJsonArray("elements").add(element);
    }

    /** Eight-element ROOTS topology: two crossed planes plus six boundary shells. */
    private static void addRoots(JsonObject model, Bounds bounds) {
        double zMid = (bounds.z0() + bounds.z1()) / 2.0;
        double xMid = (bounds.x0() + bounds.x1()) / 2.0;
        addRootPlane(model, new double[]{bounds.x0(), bounds.y0(), zMid},
                new double[]{bounds.x1(), bounds.y1(), zMid}, Direction.NORTH, Direction.SOUTH,
                bounds, "#side");
        addRootPlane(model, new double[]{xMid, bounds.y0(), bounds.z0()},
                new double[]{xMid, bounds.y1(), bounds.z1()}, Direction.EAST, Direction.WEST,
                bounds, "#side");
        for (Direction face : FACINGS) addRootShell(model, bounds, face);
    }

    private static void addRootPlane(JsonObject model, double[] from, double[] to,
            Direction first, Direction second, Bounds uvBounds, String texture) {
        JsonObject element = element(from, to);
        JsonObject faces = new JsonObject();
        for (Direction face : new Direction[]{first, second}) {
            JsonObject encoded = new JsonObject();
            encoded.addProperty("texture", texture);
            encoded.add("uv", defaultUv(face, uvBounds));
            faces.add(face.getSerializedName(), encoded);
        }
        element.add("faces", faces);
        model.getAsJsonArray("elements").add(element);
    }

    private static void addRootShell(JsonObject model, Bounds bounds, Direction surface) {
        final double epsilon = 0.002;
        double[] from = {bounds.x0(), bounds.y0(), bounds.z0()};
        double[] to = {bounds.x1(), bounds.y1(), bounds.z1()};
        int axis = axisIndex(surface.getAxis());
        if (surface.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            from[axis] = to[axis] - epsilon;
        } else {
            to[axis] = from[axis] + epsilon;
        }
        JsonObject element = element(from, to);
        JsonObject faces = new JsonObject();
        String texture = surface.getAxis() == Direction.Axis.Y ? "#top" : "#side";
        for (Direction face : new Direction[]{surface, surface.getOpposite()}) {
            JsonObject encoded = new JsonObject();
            encoded.addProperty("texture", texture);
            encoded.add("uv", defaultUv(face, bounds));
            if (bounds.onBoundary(surface)) encoded.addProperty("cullface", surface.getSerializedName());
            faces.add(face.getSerializedName(), encoded);
        }
        element.add("faces", faces);
        model.getAsJsonArray("elements").add(element);
    }

    private static Bounds exposedSlice(Bounds bounds, Direction exposed, int width) {
        return switch (exposed) {
            case UP -> new Bounds(bounds.x0(), bounds.y1() - width, bounds.z0(),
                    bounds.x1(), bounds.y1(), bounds.z1());
            case DOWN -> new Bounds(bounds.x0(), bounds.y0(), bounds.z0(),
                    bounds.x1(), bounds.y0() + width, bounds.z1());
            case NORTH -> new Bounds(bounds.x0(), bounds.y0(), bounds.z0(),
                    bounds.x1(), bounds.y1(), bounds.z0() + width);
            case SOUTH -> new Bounds(bounds.x0(), bounds.y0(), bounds.z1() - width,
                    bounds.x1(), bounds.y1(), bounds.z1());
            case EAST -> new Bounds(bounds.x1() - width, bounds.y0(), bounds.z0(),
                    bounds.x1(), bounds.y1(), bounds.z1());
            case WEST -> new Bounds(bounds.x0(), bounds.y0(), bounds.z0(),
                    bounds.x0() + width, bounds.y1(), bounds.z1());
        };
    }

    private static Bounds withoutExposedSlice(Bounds bounds, Direction exposed, int width) {
        return switch (exposed) {
            case UP -> new Bounds(bounds.x0(), bounds.y0(), bounds.z0(),
                    bounds.x1(), bounds.y1() - width, bounds.z1());
            case DOWN -> new Bounds(bounds.x0(), bounds.y0() + width, bounds.z0(),
                    bounds.x1(), bounds.y1(), bounds.z1());
            case NORTH -> new Bounds(bounds.x0(), bounds.y0(), bounds.z0() + width,
                    bounds.x1(), bounds.y1(), bounds.z1());
            case SOUTH -> new Bounds(bounds.x0(), bounds.y0(), bounds.z0(),
                    bounds.x1(), bounds.y1(), bounds.z1() - width);
            case EAST -> new Bounds(bounds.x0(), bounds.y0(), bounds.z0(),
                    bounds.x1() - width, bounds.y1(), bounds.z1());
            case WEST -> new Bounds(bounds.x0() + width, bounds.y0(), bounds.z0(),
                    bounds.x1(), bounds.y1(), bounds.z1());
        };
    }

    private static JsonArray rimUv(Direction face, Direction exposed, Bounds bounds) {
        JsonArray uv = defaultUv(face, bounds);
        FaceFrame frame = faceFrame(face);
        boolean physicalPositive = exposed.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        if (frame.uAxis() == exposed.getAxis()) {
            boolean high = physicalPositive == frame.uPositive();
            uv.set(0, new JsonPrimitive(high ? 15 : 0));
            uv.set(2, new JsonPrimitive(high ? 16 : 1));
        } else if (frame.vAxis() == exposed.getAxis()) {
            boolean high = physicalPositive == frame.vPositive();
            uv.set(1, new JsonPrimitive(high ? 15 : 0));
            uv.set(3, new JsonPrimitive(high ? 16 : 1));
        } else {
            throw new IllegalArgumentException("Glass rim face is parallel to exposed axis: "
                    + face + " / " + exposed);
        }
        return uv;
    }

    /** Vanilla element-face UV frame, expressed in block-absolute model coordinates. */
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

    private static JsonObject element(Bounds bounds) {
        return element(new double[]{bounds.x0(), bounds.y0(), bounds.z0()},
                new double[]{bounds.x1(), bounds.y1(), bounds.z1()});
    }

    private static JsonObject element(double[] from, double[] to) {
        JsonObject element = new JsonObject();
        element.add("from", numbers(from[0], from[1], from[2]));
        element.add("to", numbers(to[0], to[1], to[2]));
        return element;
    }

    private static int axisIndex(Direction.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
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

    private record FaceFrame(Direction.Axis uAxis, boolean uPositive,
            Direction.Axis vAxis, boolean vPositive) {}

    private static void addOverlay(JsonObject model, NibaruMaterialProfile profile, Bounds bounds) {
        JsonObject element = new JsonObject();
        element.add("from", numbers(bounds.x0(), bounds.y0(), bounds.z0()));
        element.add("to", numbers(bounds.x1(), bounds.y1(), bounds.z1()));
        JsonObject faces = new JsonObject();
        for (Direction face : new Direction[]{Direction.NORTH, Direction.EAST,
                Direction.SOUTH, Direction.WEST}) {
            JsonObject encoded = new JsonObject();
            encoded.addProperty("texture", "#overlay");
            encoded.addProperty("tintindex", 0);
            encoded.add("uv", overlayUv(face, bounds));
            if (bounds.onBoundary(face)) encoded.addProperty("cullface", face.getSerializedName());
            faces.add(face.getSerializedName(), encoded);
        }
        element.add("faces", faces);
        model.getAsJsonArray("elements").add(element);
    }

    /**
     * Applies only the material-owned surface offset to the otherwise exact Layer cuboid.
     * Dirt Path keeps its canonical world-top surface at 15/16 for every orientation.
     */
    static Bounds visualBounds(NibaruMaterialProfile profile, Direction facing, int layers) {
        Bounds physical = bounds(facing, layers);
        if (profile.surfaceSamplingPolicy()
                != NibaruMaterialProfile.SurfaceSamplingPolicy.PATH_LOWERED_SURFACE) {
            return physical;
        }
        return new Bounds(physical.x0(), physical.y0(), physical.z0(),
                physical.x1(), Math.min(physical.y1(), 15), physical.z1());
    }

    /** Samples a grass-side overlay from its canonical top band regardless of cuboid height. */
    private static JsonArray overlayUv(Direction face, Bounds bounds) {
        int height = bounds.y1() - bounds.y0();
        return switch (face) {
            case NORTH -> numbers(16 - bounds.x1(), 0, 16 - bounds.x0(), height);
            case SOUTH -> numbers(bounds.x0(), 0, bounds.x1(), height);
            case EAST -> numbers(16 - bounds.z1(), 0, 16 - bounds.z0(), height);
            case WEST -> numbers(bounds.z0(), 0, bounds.z1(), height);
            default -> throw new IllegalArgumentException("Overlay is horizontal-side only: " + face);
        };
    }

    private static boolean tintBaseFace(NibaruMaterialProfile profile, Direction face) {
        if (profile.tintProfile() == TintProfile.NONE) return false;
        return profile.visualProfile() != VisualProfile.GRASS_OVERLAY || face == Direction.UP;
    }

    private static String textureRole(Direction face, Direction.Axis materialAxis) {
        if (materialAxis == null) {
            return switch (face) {
                case UP -> "#top";
                case DOWN -> "#bottom";
                default -> "#side";
            };
        }
        return face.getAxis() == materialAxis ? "#top" : "#side";
    }

    /** Matches the accepted direct-axis face frame used by Step/Vertical without changing them. */
    private static int faceRotation(Direction face, Direction.Axis materialAxis) {
        if (materialAxis == null) return 0;
        return switch (materialAxis) {
            case X -> face.getAxis() == Direction.Axis.X ? 0 : 90;
            case Y -> 0;
            case Z -> face.getAxis() == Direction.Axis.X ? 90 : 0;
        };
    }

    private static JsonObject apply(String model, int yRotation, boolean explicitUvLock) {
        JsonObject result = new JsonObject();
        result.addProperty("model", model);
        if (yRotation != 0) result.addProperty("y", yRotation);
        if (explicitUvLock) result.addProperty("uvlock", false);
        return result;
    }

    private static String variantKey(Direction facing, int layers, Direction.Axis axis, Direction pattern) {
        StringBuilder result = new StringBuilder("facing=").append(facing.getSerializedName())
                .append(",layers=").append(layers);
        if (axis != null) result.append(",axis=").append(axisName(axis));
        if (pattern != null) result.append(",pattern_facing=").append(pattern.getSerializedName());
        return result.toString();
    }

    private static Direction relativeFacing(Direction physical, Direction pattern) {
        return physical.getAxis().isVertical()
                ? physical : GlazedPatternState.relativePhysical(physical, pattern);
    }

    private static String regularModelId(Identifier shape, Direction facing, int layers) {
        if (layers == 4) return shape.getNamespace() + ":block/" + shape.getPath() + "_4_full";
        return shape.getNamespace() + ":block/" + shape.getPath() + "_" + layers
                + "_" + facing.getSerializedName();
    }

    private static String axisModelId(Identifier shape, Direction facing, int layers,
            Direction.Axis axis) {
        String form = layers == 4 ? "4_full" : layers + "_" + facing.getSerializedName();
        return shape.getNamespace() + ":block/" + shape.getPath() + "_" + form
                + "_axis_" + axisName(axis);
    }

    private static String glazedModelId(Identifier shape, Direction relativeFacing, int layers) {
        String form = layers == 4 ? "4_full" : layers + "_" + relativeFacing.getSerializedName();
        return shape.getNamespace() + ":block/" + shape.getPath() + "_" + form + "_glazed";
    }

    private static String canonicalModelId(Identifier parent) {
        return parent.getNamespace() + ":block/" + parent.getPath();
    }

    private static String axisName(Direction.Axis axis) {
        return switch (axis) {
            case X -> "x";
            case Y -> "y";
            case Z -> "z";
        };
    }

    private static String texture(String path) {
        return path.contains(":") ? path : "minecraft:block/" + path;
    }

    private static JsonObject itemDisplay() {
        JsonObject display = new JsonObject();
        JsonObject firstPerson = transform(new int[]{0, -45, 0}, null, new double[]{0.4, 0.4, 0.4});
        display.add("firstperson_righthand", firstPerson);
        display.add("firstperson_lefthand", firstPerson.deepCopy());
        display.add("gui", transform(new int[]{30, -135, 0}, new double[]{-1.75, 0, 0},
                new double[]{0.625, 0.625, 0.625}));
        display.add("fixed", transform(new int[]{0, 90, 0}, null, new double[]{0.5, 0.5, 0.5}));
        return display;
    }

    private static JsonObject transform(int[] rotation, double[] translation, double[] scale) {
        JsonObject result = new JsonObject();
        result.add("rotation", numbers(rotation[0], rotation[1], rotation[2]));
        if (translation != null) result.add("translation", numbers(
                translation[0], translation[1], translation[2]));
        result.add("scale", numbers(scale[0], scale[1], scale[2]));
        return result;
    }

    private static JsonArray numbers(Number... values) {
        JsonArray result = new JsonArray();
        for (Number value : values) result.add(value);
        return result;
    }

    /** Immutable exact model-unit bounds, exposed for focused geometry assertions. */
    public record Bounds(int x0, int y0, int z0, int x1, int y1, int z1) {
        public Bounds {
            if (x0 < 0 || y0 < 0 || z0 < 0 || x1 > 16 || y1 > 16 || z1 > 16
                    || x0 > x1 || y0 > y1 || z0 > z1) {
                throw new IllegalArgumentException("Invalid Layer model bounds");
            }
        }

        public boolean hasVolume() { return x0 < x1 && y0 < y1 && z0 < z1; }

        public boolean isFullCube() {
            return x0 == 0 && y0 == 0 && z0 == 0 && x1 == 16 && y1 == 16 && z1 == 16;
        }

        public boolean onBoundary(Direction direction) {
            return switch (direction) {
                case WEST -> x0 == 0;
                case EAST -> x1 == 16;
                case DOWN -> y0 == 0;
                case UP -> y1 == 16;
                case NORTH -> z0 == 0;
                case SOUTH -> z1 == 16;
            };
        }
    }

    public record Projection(JsonObject blockState, Map<String, JsonObject> models, String itemModel) {
        public Projection {
            Objects.requireNonNull(blockState, "blockState");
            models = Collections.unmodifiableMap(new LinkedHashMap<>(models));
            Objects.requireNonNull(itemModel, "itemModel");
        }
    }
}
