package dev.aero.cnmterraincompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pure ordinary-model composition for independent CNM geometry and canonical-parent material axes.
 *
 * <p>Rotated-column policies keep the model-coordinate material axis at Y and use an
 * inverse-transformed cuboid signature to retain CNM world-space geometry. Canonical direct-model
 * policies keep their authored axis-specific face frames. Applicability never comes from a visual
 * profile, orientation policy, or registry-name list.</p>
 */
public final class AxisModelContract {
    public static final String GENERATED_NAMESPACE = "clutternomore";

    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final Direction[] SIGNATURE_DIRECTIONS = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN
    };
    private static final Direction.Axis[] MATERIAL_AXES = {
            Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z
    };
    private static final SlabType[] STEP_TYPES = {
            SlabType.BOTTOM, SlabType.TOP, SlabType.DOUBLE
    };

    private AxisModelContract() {}

    /** Data-derived rendering behavior of the canonical parent's own axis variants. */
    public enum AxisUvPolicy {
        /** One ordinary column model is rotated for X/Z. */
        STANDARD_ROTATED,
        /** A cube_column_horizontal-style model is rotated for X/Z. */
        HORIZONTAL_ROTATED,
        /** Three authored UV-locked coordinate models are selected without blockstate rotation. */
        DIRECT_UV_LOCKED
    }

    /** The canonical parent's actual state definition is the only material-axis predicate. */
    public static boolean applies(NibaruMaterialProfile profile) {
        return MaterialAxisState.applies(Objects.requireNonNull(profile, "profile"));
    }

    public static Direction[] horizontalDirections() {
        return HORIZONTAL.clone();
    }

    /** Six reusable inverse half-space signatures used by Vertical models. */
    public static Direction[] signatureDirections() {
        return SIGNATURE_DIRECTIONS.clone();
    }

    public static Direction.Axis[] materialAxes() {
        return MATERIAL_AXES.clone();
    }

    /**
     * Semantic model texture bindings for a canonical vanilla-style axis material.
     *
     * <p>Canonical axis blocks have one axial end texture. Both model-coordinate ends therefore use
     * {@code top}; {@code bottom} is intentionally ignored even if an older provider profile
     * synthesized a nonexistent {@code *_bottom} path. Vanilla column particles come from the side.</p>
     */
    public static Map<String, String> semanticTextures(NibaruMaterialProfile profile) {
        requireAxisProfile(profile);
        NibaruMaterialProfile.TextureRoles roles = profile.textureRoles();
        String side = texture(roles.side());
        String end = texture(roles.top());
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("side", side);
        result.put("top", end);
        result.put("bottom", end);
        result.put("particle", side);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Classifies the canonical parent's own blockstate without material IDs or visual-profile hints.
     * Unknown axis layouts fail loudly so future vanilla/provider forms cannot silently get the
     * wrong material UV contract.
     */
    public static AxisUvPolicy uvPolicy(Identifier canonicalParent, JsonObject blockState) {
        Objects.requireNonNull(canonicalParent, "canonicalParent");
        Objects.requireNonNull(blockState, "blockState");
        AxisSelector x = axisSelector(canonicalParent, blockState, Direction.Axis.X);
        AxisSelector y = axisSelector(canonicalParent, blockState, Direction.Axis.Y);
        AxisSelector z = axisSelector(canonicalParent, blockState, Direction.Axis.Z);
        // A pillar axis describes an unoriented line: either quarter-turn for X, either
        // half-turn for Y, and either half-turn for Z is semantically valid. In particular,
        // BBB's real beam resources use x=90,y=180 for Z (rather than the synthetic fixture's
        // x=90,y=0). Do not demand one preferred representative of an otherwise valid axis
        // layout, but retain the fail-loud contract for any transform outside these families.
        boolean vanillaX = x.transforms().stream().allMatch(transform -> transform.x() == 90
                && (transform.y() == 90 || transform.y() == 270) && transform.z() == 0);
        boolean vanillaY = y.transforms().stream().allMatch(transform -> transform.x() == 0
                && (transform.y() == 0 || transform.y() == 180) && transform.z() == 0);
        boolean vanillaZ = z.transforms().stream().allMatch(transform -> transform.x() == 90
                && (transform.y() == 0 || transform.y() == 180) && transform.z() == 0);
        boolean direct = allIdentity(x) && allIdentity(y) && allIdentity(z);

        if (vanillaX && vanillaY && vanillaZ
                && x.models().equals(y.models()) && z.models().equals(y.models())) {
            return AxisUvPolicy.STANDARD_ROTATED;
        }
        if (vanillaX && vanillaY && vanillaZ
                && x.models().equals(z.models()) && !x.models().equals(y.models())) {
            return AxisUvPolicy.HORIZONTAL_ROTATED;
        }
        if (direct && !x.models().equals(y.models()) && !x.models().equals(z.models())
                && !y.models().equals(z.models())) {
            return AxisUvPolicy.DIRECT_UV_LOCKED;
        }
        throw new IllegalArgumentException("Unsupported canonical axis blockstate for "
                + canonicalParent + ": x=" + x + ", y=" + y + ", z=" + z);
    }

    /** Normalizes one nonempty object-or-array canonical axis selector for focused coverage tests. */
    public static AxisSelector axisSelector(Identifier canonicalParent, JsonObject blockState,
            Direction.Axis axis) {
        Objects.requireNonNull(canonicalParent, "canonicalParent");
        JsonObject variants = requiredObject(blockState, "variants", canonicalParent);
        String key = "axis=" + axisName(axis);
        if (!variants.has(key)) {
            throw new IllegalArgumentException("Missing selector '" + key
                    + "' in canonical axis blockstate for " + canonicalParent);
        }
        JsonElement encoded = variants.get(key);
        List<JsonObject> entries;
        if (encoded.isJsonObject()) {
            entries = List.of(encoded.getAsJsonObject());
        } else if (encoded.isJsonArray() && !encoded.getAsJsonArray().isEmpty()) {
            java.util.ArrayList<JsonObject> decoded = new java.util.ArrayList<>();
            for (JsonElement entry : encoded.getAsJsonArray()) {
                if (!entry.isJsonObject()) {
                    throw new IllegalArgumentException("Non-object entry in selector '" + key
                            + "' for " + canonicalParent + ": " + entry);
                }
                decoded.add(entry.getAsJsonObject());
            }
            entries = List.copyOf(decoded);
        } else {
            throw new IllegalArgumentException("Selector '" + key
                    + "' must be a nonempty object or array for " + canonicalParent);
        }
        LinkedHashSet<String> models = new LinkedHashSet<>();
        LinkedHashSet<VariantTransform> transforms = new LinkedHashSet<>();
        for (JsonObject entry : entries) {
            requireNoUvLock(entry, canonicalParent, axis);
            models.add(requiredString(entry, "model", canonicalParent));
            transforms.add(new VariantTransform(
                    rotation(entry, "x"), rotation(entry, "y"), rotation(entry, "z")));
        }
        return new AxisSelector(models, transforms);
    }

    private static boolean allIdentity(AxisSelector selector) {
        return selector.transforms().size() == 1
                && selector.transforms().contains(VariantTransform.IDENTITY);
    }

    /** Deterministic 24-entry facing x double x axis selector map; waterlogged is model-irrelevant. */
    public static Map<String, VariantSelection> verticalSelectors(
            Identifier shape, AxisUvPolicy policy) {
        Objects.requireNonNull(policy, "policy");
        LinkedHashMap<String, VariantSelection> result = new LinkedHashMap<>();
        for (Direction facing : HORIZONTAL) {
            for (boolean doubled : new boolean[]{false, true}) {
                for (Direction.Axis axis : MATERIAL_AXES) {
                    VariantSelection selection;
                    if (policy == AxisUvPolicy.DIRECT_UV_LOCKED || axis == Direction.Axis.Y) {
                        String model = doubled
                                ? verticalDirectFullModelId(shape, axis)
                                : verticalDirectHalfModelId(shape, axis, facing);
                        selection = new VariantSelection(model, 0, 0);
                    } else {
                        String model = doubled
                                ? verticalFullModelId(shape)
                                : verticalHalfModelId(shape, inverseMaterialRotation(facing, axis));
                        selection = new VariantSelection(model, 90,
                                axis == Direction.Axis.X ? 90 : 0);
                    }
                    result.put(verticalVariantKey(facing, doubled, axis), selection);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /** Deterministic 36-entry facing x type x axis selector map; waterlogged is model-irrelevant. */
    public static Map<String, VariantSelection> stepSelectors(
            Identifier shape, AxisUvPolicy policy) {
        Objects.requireNonNull(policy, "policy");
        LinkedHashMap<String, VariantSelection> result = new LinkedHashMap<>();
        for (Direction facing : HORIZONTAL) {
            for (SlabType type : STEP_TYPES) {
                for (Direction.Axis axis : MATERIAL_AXES) {
                    VariantSelection selection;
                    if (policy == AxisUvPolicy.DIRECT_UV_LOCKED || axis == Direction.Axis.Y) {
                        String model;
                        if (type == SlabType.DOUBLE) {
                            model = stepDirectDoubleModelId(shape, axis,
                                    new HalfSpacePair(Direction.UP, facing));
                        } else {
                            Direction vertical = type == SlabType.BOTTOM ? Direction.DOWN : Direction.UP;
                            model = stepDirectPairModelId(shape, axis,
                                    new HalfSpacePair(vertical, facing));
                        }
                        selection = new VariantSelection(model, 0, 0);
                    } else if (type == SlabType.DOUBLE) {
                        HalfSpacePair upper = inversePair(Direction.UP, facing, axis);
                        selection = new VariantSelection(stepDoubleModelId(shape, upper), 90,
                                axis == Direction.Axis.X ? 90 : 0);
                    } else {
                        Direction vertical = type == SlabType.BOTTOM ? Direction.DOWN : Direction.UP;
                        HalfSpacePair cuboid = inversePair(vertical, facing, axis);
                        selection = new VariantSelection(stepPairModelId(shape, cuboid), 90,
                                axis == Direction.Axis.X ? 90 : 0);
                    }
                    result.put(stepVariantKey(facing, type, axis), selection);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Reusable Vertical signatures. Rotated policies emit seven inverse X/Z signatures plus seven
     * direct-Y companions; the direct policy emits seven signatures for each material axis.
     */
    public static Map<String, JsonObject> verticalGeneratedModels(
            NibaruMaterialProfile profile, Identifier shape, AxisUvPolicy policy) {
        requireAxisProfile(profile);
        Objects.requireNonNull(policy, "policy");
        LinkedHashMap<String, JsonObject> result = new LinkedHashMap<>();
        if (policy == AxisUvPolicy.DIRECT_UV_LOCKED) {
            for (Direction.Axis axis : MATERIAL_AXES) {
                for (Direction direction : SIGNATURE_DIRECTIONS) {
                    result.put(verticalDirectHalfModelId(shape, axis, direction),
                            cuboidModel(profile, List.of(bounds(direction)), true,
                                    FacePolicy.direct(axis)));
                }
                result.put(verticalDirectFullModelId(shape, axis),
                        cuboidModel(profile, List.of(fullBounds()), false, FacePolicy.direct(axis)));
            }
            return Collections.unmodifiableMap(result);
        }
        FacePolicy faces = policy == AxisUvPolicy.HORIZONTAL_ROTATED
                ? FacePolicy.HORIZONTAL_COLUMN : FacePolicy.STANDARD_COLUMN;
        for (Direction direction : SIGNATURE_DIRECTIONS) {
            result.put(verticalHalfModelId(shape, direction),
                    cuboidModel(profile, List.of(bounds(direction)), true, faces));
        }
        result.put(verticalFullModelId(shape),
                cuboidModel(profile, List.of(fullBounds()), false, faces));
        for (Direction direction : SIGNATURE_DIRECTIONS) {
            result.put(verticalDirectHalfModelId(shape, Direction.Axis.Y, direction),
                    cuboidModel(profile, List.of(bounds(direction)), true,
                            FacePolicy.direct(Direction.Axis.Y)));
        }
        result.put(verticalDirectFullModelId(shape, Direction.Axis.Y),
                cuboidModel(profile, List.of(fullBounds()), false,
                        FacePolicy.direct(Direction.Axis.Y)));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Reusable Step signatures. Single models intersect perpendicular half-spaces; double models
     * pair one such cuboid with its opposite. Rotated policies emit inverse X/Z and direct-Y sets.
     */
    public static Map<String, JsonObject> stepGeneratedModels(
            NibaruMaterialProfile profile, Identifier shape, AxisUvPolicy policy) {
        requireAxisProfile(profile);
        Objects.requireNonNull(policy, "policy");
        LinkedHashMap<String, JsonObject> result = new LinkedHashMap<>();
        if (policy == AxisUvPolicy.DIRECT_UV_LOCKED) {
            for (Direction.Axis axis : MATERIAL_AXES) {
                addStepSignatures(result, profile, shape, FacePolicy.direct(axis), axis);
            }
            return Collections.unmodifiableMap(result);
        }
        FacePolicy faces = policy == AxisUvPolicy.HORIZONTAL_ROTATED
                ? FacePolicy.HORIZONTAL_COLUMN : FacePolicy.STANDARD_COLUMN;
        addStepSignatures(result, profile, shape, faces, null);
        addStepSignatures(result, profile, shape,
                FacePolicy.direct(Direction.Axis.Y), Direction.Axis.Y);
        return Collections.unmodifiableMap(result);
    }

    private static void addStepSignatures(Map<String, JsonObject> result,
            NibaruMaterialProfile profile, Identifier shape, FacePolicy faces,
            Direction.Axis directAxis) {
        for (int first = 0; first < SIGNATURE_DIRECTIONS.length; first++) {
            for (int second = first + 1; second < SIGNATURE_DIRECTIONS.length; second++) {
                Direction a = SIGNATURE_DIRECTIONS[first];
                Direction b = SIGNATURE_DIRECTIONS[second];
                if (a.getAxis() == b.getAxis()) continue;
                HalfSpacePair pair = new HalfSpacePair(a, b);
                String pairId = directAxis == null ? stepPairModelId(shape, pair)
                        : stepDirectPairModelId(shape, directAxis, pair);
                result.put(pairId, cuboidModel(profile, List.of(bounds(pair)), false, faces));
                HalfSpacePair diagonal = diagonalStart(pair);
                String doubleId = directAxis == null ? stepDoubleModelId(shape, diagonal)
                        : stepDirectDoubleModelId(shape, directAxis, diagonal);
                result.putIfAbsent(doubleId, cuboidModel(profile,
                        List.of(bounds(diagonal), bounds(diagonal.opposite())), false, faces));
            }
        }
    }

    public static JsonObject verticalBlockState(Identifier shape, AxisUvPolicy policy) {
        return blockState(verticalSelectors(shape, policy));
    }

    public static JsonObject stepBlockState(Identifier shape, AxisUvPolicy policy) {
        return blockState(stepSelectors(shape, policy));
    }

    public static String verticalVariantKey(
            Direction facing, boolean doubled, Direction.Axis axis) {
        requireHorizontal(facing);
        return VerticalSlabBlock.FACING.getName() + "=" + facing.getSerializedName() + ","
                + VerticalSlabBlock.DOUBLE.getName() + "=" + doubled + ","
                + BlockStateProperties.AXIS.getName() + "=" + axisName(axis);
    }

    public static String stepVariantKey(Direction facing, SlabType type, Direction.Axis axis) {
        requireHorizontal(facing);
        return StepBlock.FACING.getName() + "=" + facing.getSerializedName() + ","
                + StepBlock.SLAB_TYPE.getName() + "=" + type.getSerializedName() + ","
                + BlockStateProperties.AXIS.getName() + "=" + axisName(axis);
    }

    public static String verticalHalfModelId(Identifier shape, Direction halfSpace) {
        return cnmModelId(shape, "_axis_half_" + halfSpace.getSerializedName());
    }

    public static String verticalFullModelId(Identifier shape) {
        return cnmModelId(shape, "_axis_full");
    }

    public static String verticalDirectHalfModelId(
            Identifier shape, Direction.Axis axis, Direction halfSpace) {
        return cnmModelId(shape, "_axis_direct_" + axisName(axis)
                + "_half_" + halfSpace.getSerializedName());
    }

    public static String verticalDirectFullModelId(Identifier shape, Direction.Axis axis) {
        return cnmModelId(shape, "_axis_direct_" + axisName(axis) + "_full");
    }

    public static String stepPairModelId(Identifier shape, HalfSpacePair pair) {
        return cnmModelId(shape, "_axis_pair_" + pair.signature());
    }

    public static String stepDoubleModelId(Identifier shape, HalfSpacePair pair) {
        HalfSpacePair start = diagonalStart(pair);
        return cnmModelId(shape, "_axis_double_" + start.signature()
                + "_and_" + start.opposite().signature());
    }

    public static String stepDirectPairModelId(
            Identifier shape, Direction.Axis axis, HalfSpacePair pair) {
        return cnmModelId(shape, "_axis_direct_" + axisName(axis)
                + "_pair_" + pair.signature());
    }

    public static String stepDirectDoubleModelId(
            Identifier shape, Direction.Axis axis, HalfSpacePair pair) {
        HalfSpacePair start = diagonalStart(pair);
        return cnmModelId(shape, "_axis_direct_" + axisName(axis)
                + "_double_" + start.signature() + "_and_" + start.opposite().signature());
    }

    /** Converts a generated model identifier to the relative path accepted by CNM's writer. */
    public static String generatedModelPath(String modelId) {
        String prefix = GENERATED_NAMESPACE + ":block/";
        if (!modelId.startsWith(prefix)) {
            throw new IllegalArgumentException("Not a generated CNM block model: " + modelId);
        }
        return "models/block/" + modelId.substring(prefix.length()) + ".json";
    }

    /**
     * Inverse of the vanilla pillar blockstate rotation: X uses x=90,y=90; Z uses x=90.
     * The returned direction describes the model-coordinate half-space needed to retain the given
     * world-space CNM geometry direction after that rotation.
     */
    public static Direction inverseMaterialRotation(Direction worldDirection, Direction.Axis axis) {
        Objects.requireNonNull(worldDirection, "worldDirection");
        Objects.requireNonNull(axis, "axis");
        return materialRotation(axis).inverse().rotate(worldDirection);
    }

    /** Uses the same discrete transform composition as Minecraft's blockstate model baker. */
    public static OctahedralGroup materialRotation(Direction.Axis axis) {
        return switch (Objects.requireNonNull(axis, "axis")) {
            case X -> Quadrant.fromXYZAngles(Quadrant.R90, Quadrant.R90, Quadrant.R0);
            case Y -> OctahedralGroup.IDENTITY;
            case Z -> Quadrant.fromXYZAngles(Quadrant.R90, Quadrant.R0, Quadrant.R0);
        };
    }

    public static HalfSpacePair inversePair(
            Direction first, Direction second, Direction.Axis axis) {
        return new HalfSpacePair(
                inverseMaterialRotation(first, axis),
                inverseMaterialRotation(second, axis));
    }

    private static JsonObject blockState(Map<String, VariantSelection> selectors) {
        JsonObject variants = new JsonObject();
        selectors.forEach((key, selection) -> variants.add(key, selection.json()));
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        return result;
    }

    private static JsonObject cuboidModel(NibaruMaterialProfile profile,
            List<int[]> cuboids, boolean cullBoundary, FacePolicy facePolicy) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        semanticTextures(profile).forEach(textures::addProperty);
        model.add("textures", textures);
        JsonArray elements = new JsonArray();
        for (int[] cuboid : cuboids) elements.add(element(cuboid, cullBoundary, facePolicy));
        model.add("elements", elements);
        ProviderVisualAdapter.decorateModel(profile, model);
        return model;
    }

    private static JsonObject element(
            int[] bounds, boolean cullBoundary, FacePolicy facePolicy) {
        JsonObject element = new JsonObject();
        element.add("from", numbers(bounds[0], bounds[1], bounds[2]));
        element.add("to", numbers(bounds[3], bounds[4], bounds[5]));
        JsonObject faces = new JsonObject();
        for (Direction direction : SIGNATURE_DIRECTIONS) {
            JsonObject face = new JsonObject();
            face.addProperty("texture", facePolicy.texture(direction));
            int rotation = facePolicy.rotation(direction);
            if (rotation != 0) face.addProperty("rotation", rotation);
            if (cullBoundary && liesOnBoundary(bounds, direction)) {
                face.addProperty("cullface", direction.getSerializedName());
            }
            faces.add(direction.getSerializedName(), face);
        }
        element.add("faces", faces);
        return element;
    }

    private static JsonObject requiredObject(
            JsonObject owner, String member, Identifier canonicalParent) {
        if (!owner.has(member) || !owner.get(member).isJsonObject()) {
            throw new IllegalArgumentException("Missing object '" + member
                    + "' in canonical axis blockstate for " + canonicalParent);
        }
        return owner.getAsJsonObject(member);
    }

    private static String requiredString(
            JsonObject owner, String member, Identifier canonicalParent) {
        if (!owner.has(member) || !owner.get(member).isJsonPrimitive()
                || !owner.getAsJsonPrimitive(member).isString()) {
            throw new IllegalArgumentException("Missing string '" + member
                    + "' in canonical axis blockstate for " + canonicalParent);
        }
        return owner.get(member).getAsString();
    }

    private static int rotation(JsonObject variant, String member) {
        if (!variant.has(member)) return 0;
        if (!variant.get(member).isJsonPrimitive()
                || !variant.getAsJsonPrimitive(member).isNumber()) {
            throw new IllegalArgumentException("Non-numeric axis model rotation '" + member
                    + "': " + variant.get(member));
        }
        return variant.get(member).getAsInt();
    }

    private static void requireNoUvLock(
            JsonObject variant, Identifier canonicalParent, Direction.Axis axis) {
        if (variant.has("uvlock") && variant.get("uvlock").getAsBoolean()) {
            throw new IllegalArgumentException("Canonical axis blockstate uses blockstate uvlock for "
                    + canonicalParent + " axis=" + axisName(axis));
        }
    }

    private static boolean liesOnBoundary(int[] bounds, Direction direction) {
        return switch (direction) {
            case WEST -> bounds[0] == 0;
            case DOWN -> bounds[1] == 0;
            case NORTH -> bounds[2] == 0;
            case EAST -> bounds[3] == 16;
            case UP -> bounds[4] == 16;
            case SOUTH -> bounds[5] == 16;
        };
    }

    private static int[] bounds(Direction halfSpace) {
        int[] result = fullBounds();
        applyHalfSpace(result, halfSpace);
        return result;
    }

    private static int[] bounds(HalfSpacePair pair) {
        int[] result = fullBounds();
        applyHalfSpace(result, pair.first());
        applyHalfSpace(result, pair.second());
        return result;
    }

    private static int[] fullBounds() {
        return new int[]{0, 0, 0, 16, 16, 16};
    }

    private static void applyHalfSpace(int[] bounds, Direction direction) {
        switch (direction) {
            case WEST -> bounds[3] = 8;
            case EAST -> bounds[0] = 8;
            case DOWN -> bounds[4] = 8;
            case UP -> bounds[1] = 8;
            case NORTH -> bounds[5] = 8;
            case SOUTH -> bounds[2] = 8;
        }
    }

    private static HalfSpacePair diagonalStart(HalfSpacePair pair) {
        HalfSpacePair opposite = pair.opposite();
        return compare(pair, opposite) <= 0 ? pair : opposite;
    }

    private static int compare(HalfSpacePair first, HalfSpacePair second) {
        int primary = Integer.compare(directionIndex(first.first()), directionIndex(second.first()));
        return primary != 0 ? primary
                : Integer.compare(directionIndex(first.second()), directionIndex(second.second()));
    }

    private static int directionIndex(Direction direction) {
        for (int index = 0; index < SIGNATURE_DIRECTIONS.length; index++) {
            if (SIGNATURE_DIRECTIONS[index] == direction) return index;
        }
        throw new IllegalArgumentException("Unknown direction: " + direction);
    }

    private static String cnmModelId(Identifier shape, String suffix) {
        Objects.requireNonNull(shape, "shape");
        return GENERATED_NAMESPACE + ":block/" + shape.getPath() + suffix;
    }

    private static String axisName(Direction.Axis axis) {
        return switch (Objects.requireNonNull(axis, "axis")) {
            case X -> "x";
            case Y -> "y";
            case Z -> "z";
        };
    }

    private static String texture(String path) {
        return path.contains(":") ? path : "minecraft:block/" + path;
    }

    private static JsonArray numbers(Number... values) {
        JsonArray result = new JsonArray();
        for (Number value : values) result.add(value);
        return result;
    }

    private static void requireAxisProfile(NibaruMaterialProfile profile) {
        if (!applies(profile)) {
            throw new IllegalArgumentException(
                    "Canonical parent does not expose axis: " + profile.canonicalParentId());
        }
    }

    private static void requireHorizontal(Direction direction) {
        if (direction.getAxis().isVertical()) {
            throw new IllegalArgumentException("CNM geometry must face horizontally: " + direction);
        }
    }

    /** Face-role and authored face-rotation behavior in model coordinates. */
    private record FacePolicy(boolean horizontalColumn, Direction.Axis directAxis) {
        private static final FacePolicy STANDARD_COLUMN = new FacePolicy(false, null);
        private static final FacePolicy HORIZONTAL_COLUMN = new FacePolicy(true, null);

        private static FacePolicy direct(Direction.Axis axis) {
            return new FacePolicy(false, Objects.requireNonNull(axis, "axis"));
        }

        private String texture(Direction face) {
            if (directAxis == null) {
                return switch (face) {
                    case UP -> "#top";
                    case DOWN -> "#bottom";
                    default -> "#side";
                };
            }
            if (face.getAxis() != directAxis) return "#side";
            return switch (face) {
                case EAST, UP, SOUTH -> "#top";
                case WEST, DOWN, NORTH -> "#bottom";
            };
        }

        private int rotation(Direction face) {
            if (directAxis == null) return horizontalColumn && face == Direction.UP ? 180 : 0;
            return switch (directAxis) {
                case X -> face.getAxis() == Direction.Axis.X ? 0 : 90;
                case Y -> 0;
                case Z -> face.getAxis() == Direction.Axis.X ? 90 : 0;
            };
        }
    }

    /** Deterministic normalized view of a canonical object-or-array axis selector. */
    public record AxisSelector(Set<String> models, Set<VariantTransform> transforms) {
        public AxisSelector {
            models = Collections.unmodifiableSet(new LinkedHashSet<>(models));
            transforms = Collections.unmodifiableSet(new LinkedHashSet<>(transforms));
            if (models.isEmpty() || transforms.isEmpty()) {
                throw new IllegalArgumentException("Axis selector must have models and transforms");
            }
        }
    }

    /** Normalized blockstate rotation attached to one canonical model alternative. */
    public record VariantTransform(int x, int y, int z) {
        public static final VariantTransform IDENTITY = new VariantTransform(0, 0, 0);
    }

    /** Testable selector value used to build one ordinary blockstate variant. */
    public record VariantSelection(String model, int x, int y) {
        public VariantSelection {
            Objects.requireNonNull(model, "model");
        }

        public JsonObject json() {
            JsonObject result = new JsonObject();
            result.addProperty("model", model);
            if (x != 0) result.addProperty("x", x);
            if (y != 0) result.addProperty("y", y);
            return result;
        }
    }

    /** Canonically ordered intersection of two perpendicular model-coordinate half-spaces. */
    public record HalfSpacePair(Direction first, Direction second) {
        public HalfSpacePair {
            Objects.requireNonNull(first, "first");
            Objects.requireNonNull(second, "second");
            if (first.getAxis() == second.getAxis()) {
                throw new IllegalArgumentException("Half-spaces must be perpendicular: " + first + ", " + second);
            }
            if (directionIndex(first) > directionIndex(second)) {
                Direction swap = first;
                first = second;
                second = swap;
            }
        }

        public HalfSpacePair opposite() {
            return new HalfSpacePair(first.getOpposite(), second.getOpposite());
        }

        public String signature() {
            return first.getSerializedName() + "_" + second.getSerializedName();
        }
    }
}
