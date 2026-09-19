package games.twinhead.moreslabsstairsandwalls.api.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pure ordinary-model contract for Nibaru's native horizontal slab/stair geometry with an
 * independent canonical material axis.
 *
 * <p>Applicability and UV policy are derived from the canonical parent's actual state and
 * blockstate JSON. Rotated-column policies inverse-transform the model cuboids before applying
 * the canonical X/Z transform, retaining the slab/stair's world geometry and vanilla texel
 * density. Authored axis-model policies instead use explicit axis face mappings.</p>
 */
public final class NativeAxisModelContract {
    public static final String PROVIDER_NAMESPACE = "more_slabs_stairs_and_walls";

    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final Direction[] FACE_ORDER = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN
    };
    private static final Direction.Axis[] AXES = {
            Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z
    };
    private static final SlabType[] SLAB_TYPES = {
            SlabType.BOTTOM, SlabType.TOP, SlabType.DOUBLE
    };
    private static final Half[] STAIR_HALVES = {Half.BOTTOM, Half.TOP};
    private static final StairsShape[] STAIR_SHAPES = {
            StairsShape.STRAIGHT, StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT,
            StairsShape.OUTER_LEFT, StairsShape.OUTER_RIGHT
    };

    private NativeAxisModelContract() {}

    /** Data-derived rendering behavior of the canonical parent's axis variants. */
    public enum AxisUvPolicy {
        /** One ordinary column model is rotated for X/Z. */
        STANDARD_ROTATED,
        /** A cube_column_horizontal-style model is rotated for X/Z. */
        HORIZONTAL_ROTATED,
        /** Three authored coordinate models are selected without blockstate rotation. */
        DIRECT_UV_LOCKED
    }

    /** The canonical parent's actual state definition is the only material-axis predicate. */
    public static boolean applies(ModBlocks family) {
        return MaterialAxisSemantics.applies(Objects.requireNonNull(family, "family"));
    }

    public static boolean applies(NibaruMaterialProfile profile) {
        return MaterialAxisSemantics.applies(Objects.requireNonNull(profile, "profile"));
    }

    public static Direction[] horizontalDirections() {
        return HORIZONTAL.clone();
    }

    public static Direction.Axis[] materialAxes() {
        return AXES.clone();
    }

    /** Semantic column texture bindings sourced directly from Nibaru's material catalog. */
    public static Map<String, String> semanticTextures(ModBlocks family) {
        requireAxisFamily(family);
        // Resource generation runs before live Minecraft registries can initialize the profile
        // inventory. Resolve this direct canonical model here instead: Purpur Pillar proves that
        // a registry path is not necessarily the side texture.
        var resolved = CanonicalPillarTextureResolver.resolve(family.parentBlock);
        if (resolved.isPresent()) return semanticTextures(resolved.orElseThrow());
        Identifier parentId = BuiltInRegistries.BLOCK.getKey(family.parentBlock);
        String base = parentId.getPath();
        String side = family.textureId == null || family.textureId.isEmpty()
                ? family.modelType == ModBlocks.ModelType.CUBE_BOTTOM_TOP ? base + "_side" : base
                : family.textureId;
        String end = family.topId == null || family.topId.isEmpty()
                ? switch (family.modelType) {
                    case LOG, CUBE_BOTTOM_TOP -> base + "_top";
                    default -> side;
                }
                : family.topId;
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("side", texture(side));
        result.put("top", texture(end));
        result.put("bottom", texture(end));
        result.put("particle", texture(side));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, String> semanticTextures(NibaruMaterialProfile profile) {
        requireAxisProfile(profile);
        return semanticTextures(profile.textureRoles());
    }

    private static Map<String, String> semanticTextures(NibaruMaterialProfile.TextureRoles roles) {
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
     * Classifies a canonical parent's own blockstate without material IDs or visual hints.
     * Unknown layouts fail loudly rather than silently choosing a wrong texture frame.
     */
    public static AxisUvPolicy uvPolicy(Identifier canonicalParent, JsonObject blockState) {
        Objects.requireNonNull(canonicalParent, "canonicalParent");
        Objects.requireNonNull(blockState, "blockState");
        AxisSelector x = axisSelector(canonicalParent, blockState, Direction.Axis.X);
        AxisSelector y = axisSelector(canonicalParent, blockState, Direction.Axis.Y);
        AxisSelector z = axisSelector(canonicalParent, blockState, Direction.Axis.Z);
        boolean vanillaX = x.transforms().stream().allMatch(transform -> transform.x() == 90
                && (transform.y() == 90 || transform.y() == 270) && transform.z() == 0)
                && x.transforms().contains(new VariantTransform(90, 90, 0));
        boolean vanillaY = y.transforms().stream().allMatch(transform -> transform.x() == 0
                && (transform.y() == 0 || transform.y() == 180) && transform.z() == 0)
                && y.transforms().contains(VariantTransform.IDENTITY);
        boolean vanillaZ = z.transforms().stream().allMatch(transform -> transform.x() == 90
                && (transform.y() == 0 || transform.y() == 180) && transform.z() == 0)
                && z.transforms().contains(new VariantTransform(90, 0, 0));
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

    /** Normalizes one nonempty object-or-array canonical axis selector. */
    public static AxisSelector axisSelector(Identifier canonicalParent, JsonObject blockState,
            Direction.Axis axis) {
        Objects.requireNonNull(canonicalParent, "canonicalParent");
        Objects.requireNonNull(axis, "axis");
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
            ArrayList<JsonObject> decoded = new ArrayList<>();
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

    public static GeneratedBlockResources slab(NibaruMaterialProfile profile, AxisUvPolicy policy) {
        requireAxisProfile(profile);
        Identifier blockId = profile.nativeSlabId().orElseThrow(() ->
                new IllegalArgumentException("Axis profile has no native slab: " + profile.canonicalParentId()));
        return slab(blockId, policy, semanticTextures(profile));
    }

    public static GeneratedBlockResources slab(ModBlocks family, AxisUvPolicy policy) {
        if (!family.hasBlock(ModBlocks.BlockType.SLAB)) {
            throw new IllegalArgumentException("Axis family has no native slab: " + family);
        }
        requireAxisFamily(family);
        return slab(family.getId(ModBlocks.BlockType.SLAB), policy, semanticTextures(family));
    }

    private static GeneratedBlockResources slab(Identifier blockId, AxisUvPolicy policy,
            Map<String, String> textures) {
        ResourceBuilder builder = new ResourceBuilder(textures, blockId, "minecraft:block/block");
        builder.reserveBase(spec(slabGeometry(SlabType.BOTTOM), Direction.Axis.Y, policy));
        for (SlabType type : SLAB_TYPES) {
            for (Direction.Axis axis : AXES) {
                builder.select(slabVariantKey(type, axis), spec(slabGeometry(type), axis, policy));
            }
        }
        return builder.build();
    }

    public static GeneratedBlockResources stairs(NibaruMaterialProfile profile, AxisUvPolicy policy) {
        requireAxisProfile(profile);
        Identifier blockId = profile.nativeStairId().orElseThrow(() ->
                new IllegalArgumentException("Axis profile has no native stair: " + profile.canonicalParentId()));
        return stairs(blockId, policy, semanticTextures(profile));
    }

    public static GeneratedBlockResources stairs(ModBlocks family, AxisUvPolicy policy) {
        if (!family.hasBlock(ModBlocks.BlockType.STAIRS)) {
            throw new IllegalArgumentException("Axis family has no native stair: " + family);
        }
        requireAxisFamily(family);
        return stairs(family.getId(ModBlocks.BlockType.STAIRS), policy, semanticTextures(family));
    }

    private static GeneratedBlockResources stairs(Identifier blockId, AxisUvPolicy policy,
            Map<String, String> textures) {
        ResourceBuilder builder = new ResourceBuilder(textures, blockId, "minecraft:block/stairs");
        builder.reserveBase(spec(stairGeometry(Direction.EAST, Half.BOTTOM, StairsShape.STRAIGHT),
                Direction.Axis.Y, policy));
        for (Direction facing : HORIZONTAL) {
            for (Half half : STAIR_HALVES) {
                for (StairsShape shape : STAIR_SHAPES) {
                    Geometry geometry = stairGeometry(facing, half, shape);
                    for (Direction.Axis axis : AXES) {
                        builder.select(stairVariantKey(facing, half, shape, axis),
                                spec(geometry, axis, policy));
                    }
                }
            }
        }
        return builder.build();
    }

    public static String slabVariantKey(SlabType type, Direction.Axis axis) {
        return BlockStateProperties.AXIS.getName() + "=" + axisName(axis) + ","
                + SlabBlock.TYPE.getName() + "=" + type.getSerializedName();
    }

    public static String stairVariantKey(
            Direction facing, Half half, StairsShape shape, Direction.Axis axis) {
        requireHorizontal(facing);
        return StairBlock.FACING.getName() + "=" + facing.getSerializedName() + ","
                + StairBlock.HALF.getName() + "=" + half.getSerializedName() + ","
                + StairBlock.SHAPE.getName() + "=" + shape.getSerializedName() + ","
                + BlockStateProperties.AXIS.getName() + "=" + axisName(axis);
    }

    public static Identifier blockStateResource(Identifier blockId) {
        return Identifier.fromNamespaceAndPath(blockId.getNamespace(),
                "blockstates/" + blockId.getPath() + ".json");
    }

    public static Identifier modelResource(String modelId) {
        Identifier model = Identifier.parse(modelId);
        return Identifier.fromNamespaceAndPath(model.getNamespace(),
                "models/" + model.getPath() + ".json");
    }

    /** Pure geometry seam used by focused exhaustive cuboid/selector tests. */
    public static Geometry slabGeometry(SlabType type) {
        return switch (Objects.requireNonNull(type, "type")) {
            case BOTTOM -> Geometry.of(Cuboid.of(Direction.DOWN));
            case TOP -> Geometry.of(Cuboid.of(Direction.UP));
            case DOUBLE -> Geometry.of(Cuboid.full());
        };
    }

    /** Pure world-space stair geometry retaining vanilla facing/half/shape semantics. */
    public static Geometry stairGeometry(Direction facing, Half half, StairsShape shape) {
        requireHorizontal(facing);
        Objects.requireNonNull(half, "half");
        Objects.requireNonNull(shape, "shape");
        Direction base = half == Half.BOTTOM ? Direction.DOWN : Direction.UP;
        Direction raised = base.getOpposite();
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        List<Cuboid> cuboids = new ArrayList<>();
        cuboids.add(Cuboid.of(base));
        switch (shape) {
            case STRAIGHT -> cuboids.add(Cuboid.of(raised, facing));
            case OUTER_LEFT -> cuboids.add(Cuboid.of(raised, facing, left));
            case OUTER_RIGHT -> cuboids.add(Cuboid.of(raised, facing, right));
            case INNER_LEFT -> {
                cuboids.add(Cuboid.of(raised, facing));
                cuboids.add(Cuboid.of(raised, facing.getOpposite(), left));
            }
            case INNER_RIGHT -> {
                cuboids.add(Cuboid.of(raised, facing));
                cuboids.add(Cuboid.of(raised, facing.getOpposite(), right));
            }
        }
        return new Geometry(cuboids);
    }

    /** Inverse of the canonical X/Z pillar transform. */
    public static Direction inverseMaterialRotation(Direction worldDirection, Direction.Axis axis) {
        Objects.requireNonNull(worldDirection, "worldDirection");
        Objects.requireNonNull(axis, "axis");
        return materialRotation(axis).inverse().rotate(worldDirection);
    }

    /** Uses Minecraft's discrete transform composition rather than hand-authored direction tables. */
    public static OctahedralGroup materialRotation(Direction.Axis axis) {
        return switch (Objects.requireNonNull(axis, "axis")) {
            case X -> Quadrant.fromXYZAngles(Quadrant.R90, Quadrant.R90, Quadrant.R0);
            case Y -> OctahedralGroup.IDENTITY;
            case Z -> Quadrant.fromXYZAngles(Quadrant.R90, Quadrant.R0, Quadrant.R0);
        };
    }

    private static ModelSpec spec(Geometry worldGeometry, Direction.Axis axis, AxisUvPolicy policy) {
        Objects.requireNonNull(axis, "axis");
        Objects.requireNonNull(policy, "policy");
        if (policy == AxisUvPolicy.DIRECT_UV_LOCKED || axis == Direction.Axis.Y) {
            return new ModelSpec(worldGeometry, FacePolicy.direct(axis), new VariantSelection("pending", 0, 0));
        }
        FacePolicy faces = policy == AxisUvPolicy.HORIZONTAL_ROTATED
                ? FacePolicy.HORIZONTAL_COLUMN : FacePolicy.STANDARD_COLUMN;
        return new ModelSpec(worldGeometry.inverse(axis), faces,
                new VariantSelection("pending", 90, axis == Direction.Axis.X ? 90 : 0));
    }

    private static JsonObject blockState(Map<String, VariantSelection> selectors) {
        JsonObject variants = new JsonObject();
        selectors.forEach((key, selection) -> variants.add(key, selection.json()));
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        return result;
    }

    private static JsonObject model(Map<String, String> semanticTextures, String parent, ModelSpec spec) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        JsonObject textures = new JsonObject();
        semanticTextures.forEach(textures::addProperty);
        model.add("textures", textures);
        JsonArray elements = new JsonArray();
        for (Cuboid cuboid : spec.geometry().cuboids()) {
            elements.add(element(cuboid.bounds(), spec.faces()));
        }
        model.add("elements", elements);
        return model;
    }

    private static JsonObject element(int[] bounds, FacePolicy policy) {
        JsonObject element = new JsonObject();
        element.add("from", numbers(bounds[0], bounds[1], bounds[2]));
        element.add("to", numbers(bounds[3], bounds[4], bounds[5]));
        JsonObject faces = new JsonObject();
        for (Direction direction : FACE_ORDER) {
            JsonObject face = new JsonObject();
            face.addProperty("texture", policy.texture(direction));
            int rotation = policy.rotation(direction);
            if (rotation != 0) face.addProperty("rotation", rotation);
            if (liesOnBoundary(bounds, direction)) {
                face.addProperty("cullface", direction.getSerializedName());
            }
            faces.add(direction.getSerializedName(), face);
        }
        element.add("faces", faces);
        return element;
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

    private static JsonArray numbers(Number... values) {
        JsonArray result = new JsonArray();
        for (Number value : values) result.add(value);
        return result;
    }

    private static String texture(String path) {
        return path.contains(":") ? path : "minecraft:block/" + path;
    }

    private static String axisName(Direction.Axis axis) {
        return switch (Objects.requireNonNull(axis, "axis")) {
            case X -> "x";
            case Y -> "y";
            case Z -> "z";
        };
    }

    private static void requireAxisFamily(ModBlocks family) {
        if (!applies(Objects.requireNonNull(family, "family"))) {
            throw new IllegalArgumentException("Canonical parent does not expose axis: " + family);
        }
    }

    private static void requireAxisProfile(NibaruMaterialProfile profile) {
        if (!applies(Objects.requireNonNull(profile, "profile"))) {
            throw new IllegalArgumentException(
                    "Canonical parent does not expose axis: " + profile.canonicalParentId());
        }
    }

    private static void requireHorizontal(Direction direction) {
        if (Objects.requireNonNull(direction, "direction").getAxis().isVertical()) {
            throw new IllegalArgumentException("Stair facing must be horizontal: " + direction);
        }
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

    private static boolean allIdentity(AxisSelector selector) {
        return selector.transforms().size() == 1
                && selector.transforms().contains(VariantTransform.IDENTITY);
    }

    public record GeneratedBlockResources(
            Map<String, VariantSelection> selectors, Map<String, JsonObject> models) {
        public GeneratedBlockResources {
            selectors = Collections.unmodifiableMap(new LinkedHashMap<>(selectors));
            models = Collections.unmodifiableMap(new LinkedHashMap<>(models));
        }

        public JsonObject blockState() {
            return NativeAxisModelContract.blockState(selectors);
        }
    }

    /** Canonical union of axis-aligned cuboids represented as half-space intersections. */
    public record Geometry(List<Cuboid> cuboids) {
        public Geometry {
            List<Cuboid> normalized = new ArrayList<>(cuboids);
            normalized.sort(Comparator.comparing(Cuboid::signature));
            cuboids = List.copyOf(normalized);
            if (cuboids.isEmpty()) throw new IllegalArgumentException("Geometry must contain a cuboid");
        }

        public static Geometry of(Cuboid... cuboids) {
            return new Geometry(List.of(cuboids));
        }

        public Geometry inverse(Direction.Axis axis) {
            ArrayList<Cuboid> result = new ArrayList<>();
            for (Cuboid cuboid : cuboids) result.add(cuboid.inverse(axis));
            return new Geometry(result);
        }

        public String signature() {
            return cuboids.stream().map(Cuboid::signature)
                    .collect(java.util.stream.Collectors.joining("_plus_"));
        }
    }

    /** One cuboid selected by zero to three perpendicular half-spaces. */
    public record Cuboid(List<Direction> halfSpaces) {
        public Cuboid {
            LinkedHashSet<Direction> normalized = new LinkedHashSet<>(halfSpaces);
            if (normalized.size() != halfSpaces.size()) {
                throw new IllegalArgumentException("Duplicate cuboid half-space: " + halfSpaces);
            }
            for (Direction first : normalized) {
                for (Direction second : normalized) {
                    if (first != second && first.getAxis() == second.getAxis()) {
                        throw new IllegalArgumentException("Contradictory cuboid half-spaces: " + halfSpaces);
                    }
                }
            }
            ArrayList<Direction> sorted = new ArrayList<>(normalized);
            sorted.sort(Comparator.comparingInt(NativeAxisModelContract::directionIndex));
            halfSpaces = List.copyOf(sorted);
        }

        public static Cuboid full() {
            return new Cuboid(List.of());
        }

        public static Cuboid of(Direction... halfSpaces) {
            return new Cuboid(List.of(halfSpaces));
        }

        public Cuboid inverse(Direction.Axis axis) {
            ArrayList<Direction> result = new ArrayList<>();
            for (Direction direction : halfSpaces) {
                result.add(inverseMaterialRotation(direction, axis));
            }
            return new Cuboid(result);
        }

        public int[] bounds() {
            int[] result = {0, 0, 0, 16, 16, 16};
            for (Direction direction : halfSpaces) {
                switch (direction) {
                    case WEST -> result[3] = 8;
                    case EAST -> result[0] = 8;
                    case DOWN -> result[4] = 8;
                    case UP -> result[1] = 8;
                    case NORTH -> result[5] = 8;
                    case SOUTH -> result[2] = 8;
                }
            }
            return result;
        }

        public String signature() {
            return halfSpaces.isEmpty() ? "full" : halfSpaces.stream()
                    .map(Direction::getSerializedName)
                    .collect(java.util.stream.Collectors.joining("_"));
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

    private static int directionIndex(Direction direction) {
        for (int index = 0; index < FACE_ORDER.length; index++) {
            if (FACE_ORDER[index] == direction) return index;
        }
        throw new IllegalArgumentException("Unknown direction: " + direction);
    }

    private record ModelSpec(Geometry geometry, FacePolicy faces, VariantSelection transform) {
        private ModelSpec {
            Objects.requireNonNull(geometry, "geometry");
            Objects.requireNonNull(faces, "faces");
            Objects.requireNonNull(transform, "transform");
        }

        private String suffix() {
            return "_material_" + faces.signature() + "_" + geometry.signature();
        }

        @Override
        public boolean equals(Object candidate) {
            return candidate instanceof ModelSpec other
                    && geometry.equals(other.geometry) && faces.equals(other.faces);
        }

        @Override
        public int hashCode() {
            return Objects.hash(geometry, faces);
        }
    }

    private record FacePolicy(Kind kind, Direction.Axis directAxis) {
        private static final FacePolicy STANDARD_COLUMN = new FacePolicy(Kind.STANDARD_COLUMN, null);
        private static final FacePolicy HORIZONTAL_COLUMN = new FacePolicy(Kind.HORIZONTAL_COLUMN, null);

        private static FacePolicy direct(Direction.Axis axis) {
            return new FacePolicy(Kind.DIRECT, Objects.requireNonNull(axis, "axis"));
        }

        private String texture(Direction face) {
            if (kind != Kind.DIRECT) {
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
            if (kind == Kind.STANDARD_COLUMN) return 0;
            if (kind == Kind.HORIZONTAL_COLUMN) return face == Direction.UP ? 180 : 0;
            return switch (directAxis) {
                case X -> face.getAxis() == Direction.Axis.X ? 0 : 90;
                case Y -> 0;
                case Z -> face.getAxis() == Direction.Axis.X ? 90 : 0;
            };
        }

        private String signature() {
            return kind == Kind.DIRECT ? "direct_" + axisName(directAxis)
                    : kind.name().toLowerCase(Locale.ROOT);
        }

        private enum Kind { STANDARD_COLUMN, HORIZONTAL_COLUMN, DIRECT }
    }

    private static final class ResourceBuilder {
        private final Map<String, String> textures;
        private final Identifier blockId;
        private final String parent;
        private final Map<ModelSpec, String> ids = new LinkedHashMap<>();
        private final Map<String, JsonObject> models = new LinkedHashMap<>();
        private final Map<String, VariantSelection> selectors = new LinkedHashMap<>();

        private ResourceBuilder(Map<String, String> textures, Identifier blockId, String parent) {
            this.textures = textures;
            this.blockId = blockId;
            this.parent = parent;
        }

        private void reserveBase(ModelSpec spec) {
            put(spec, baseModelId());
        }

        private void select(String key, ModelSpec spec) {
            String modelId = ids.get(spec);
            if (modelId == null) modelId = put(spec, baseModelId() + spec.suffix());
            VariantSelection transform = spec.transform();
            VariantSelection selection = new VariantSelection(modelId, transform.x(), transform.y());
            if (selectors.put(key, selection) != null) {
                throw new IllegalStateException("Duplicate native axis selector " + blockId + " " + key);
            }
        }

        private String put(ModelSpec spec, String modelId) {
            String previous = ids.putIfAbsent(spec, modelId);
            if (previous != null) return previous;
            JsonObject priorModel = models.putIfAbsent(modelId, model(textures, parent, spec));
            if (priorModel != null) throw new IllegalStateException("Duplicate native axis model " + modelId);
            return modelId;
        }

        private String baseModelId() {
            return blockId.getNamespace() + ":block/" + blockId.getPath();
        }

        private GeneratedBlockResources build() {
            return new GeneratedBlockResources(selectors, models);
        }
    }
}
