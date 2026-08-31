package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonObject;
import dev.aero.cnmterraincompat.AxisModelContract.AxisUvPolicy;
import dev.aero.cnmterraincompat.GlazedPatternState;
import dev.aero.cnmterraincompat.MaterialAxisState;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dev.aero.cnmterraincompat.client.CuboidListModelProjection.Bounds;
import static dev.aero.cnmterraincompat.client.CuboidListModelProjection.Cuboid;

/** Pure blockstate/model projection for BGE Corner and Quarter Column. */
public final class CornerColumnModelProjection {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final Half[] HALVES = {Half.BOTTOM, Half.TOP};
    private static final Direction.Axis[] AXES = {
            Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z
    };

    private CornerColumnModelProjection() {}

    /** Resolves the canonical parent's authored axis policy from client resources when needed. */
    public static Projection projectCorner(ResourceManager manager,
            NibaruMaterialProfile profile, Identifier shape) {
        Objects.requireNonNull(manager, "manager");
        return projectCorner(profile, shape, resolvedAxisPolicy(manager, profile));
    }

    /** Source-compatible seam for material frames that do not expose an axis property. */
    public static Projection projectCorner(NibaruMaterialProfile profile, Identifier shape) {
        requireExplicitAxisPolicy(profile);
        return projectCorner(profile, shape, AxisUvPolicy.DIRECT_UV_LOCKED);
    }

    /** Pure projection seam used after the canonical axis policy has been resolved. */
    public static Projection projectCorner(NibaruMaterialProfile profile, Identifier shape,
            AxisUvPolicy axisPolicy) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(axisPolicy, "axisPolicy");
        boolean axis = MaterialAxisState.applies(profile);
        boolean glazed = profile.capabilities().contains(BehaviorCapability.GLAZED_ORIENTATION);
        rejectCombinedFrames(profile, axis, glazed);

        LinkedHashMap<String, JsonObject> models = new LinkedHashMap<>();
        JsonObject variants = new JsonObject();
        if (axis) {
            for (Direction facing : HORIZONTAL) for (Half half : HALVES) {
                for (Direction.Axis materialAxis : AXES) {
                    String modelId = cornerAxisModelId(shape, facing, half, materialAxis);
                    models.put(modelId, cornerAxisWorldModel(
                            profile, facing, half, materialAxis, axisPolicy));
                    AxisTransform transform = axisTransform(axisPolicy, materialAxis);
                    variants.add(cornerVariantKey(facing, half, materialAxis, null),
                            apply(modelId, transform.x(), transform.y(), false));
                }
            }
        } else if (glazed) {
            for (Direction physical : HORIZONTAL) for (Half half : HALVES) {
                for (Direction pattern : HORIZONTAL) {
                    Direction relative = GlazedPatternState.relativePhysical(physical, pattern);
                    String modelId = cornerGlazedModelId(shape, relative, half);
                    models.putIfAbsent(modelId, cornerWorldModel(profile, relative, half, null, true));
                    variants.add(cornerVariantKey(physical, half, null, pattern),
                            apply(modelId, GlazedPatternState.patternYaw(pattern), true));
                }
            }
        } else {
            for (Direction facing : HORIZONTAL) for (Half half : HALVES) {
                String modelId = cornerModelId(shape, facing, half);
                models.put(modelId, cornerWorldModel(profile, facing, half, null, false));
                variants.add(cornerVariantKey(facing, half, null, null), apply(modelId, 0, false));
            }
        }

        String itemModel = modelId(shape, "_item");
        Cuboid itemCuboid = Cuboid.world(cornerProfileBounds(
                profile, Direction.SOUTH, Half.BOTTOM));
        models.put(itemModel, CuboidListModelProjection.itemModel(profile, itemCuboid,
                axis ? Direction.Axis.Y : null, glazed));
        return projection(variants, models, itemModel);
    }

    /** Resolves the canonical parent's authored axis policy from client resources when needed. */
    public static Projection projectColumn(ResourceManager manager,
            NibaruMaterialProfile profile, Identifier shape) {
        Objects.requireNonNull(manager, "manager");
        return projectColumn(profile, shape, resolvedAxisPolicy(manager, profile));
    }

    /** Source-compatible seam for material frames that do not expose an axis property. */
    public static Projection projectColumn(NibaruMaterialProfile profile, Identifier shape) {
        requireExplicitAxisPolicy(profile);
        return projectColumn(profile, shape, AxisUvPolicy.DIRECT_UV_LOCKED);
    }

    /** Pure projection seam used after the canonical axis policy has been resolved. */
    public static Projection projectColumn(NibaruMaterialProfile profile, Identifier shape,
            AxisUvPolicy axisPolicy) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(axisPolicy, "axisPolicy");
        boolean axis = MaterialAxisState.applies(profile);
        boolean glazed = profile.capabilities().contains(BehaviorCapability.GLAZED_ORIENTATION);
        rejectCombinedFrames(profile, axis, glazed);

        LinkedHashMap<String, JsonObject> models = new LinkedHashMap<>();
        JsonObject variants = new JsonObject();
        if (axis) {
            for (ColumnOccupancy occupancy : ColumnOccupancy.values()) {
                for (Direction.Axis materialAxis : AXES) {
                    String modelId = columnAxisModelId(shape, occupancy, materialAxis);
                    models.put(modelId, columnAxisWorldModel(
                            profile, occupancy, materialAxis, axisPolicy));
                    AxisTransform transform = axisTransform(axisPolicy, materialAxis);
                    variants.add(columnVariantKey(occupancy, materialAxis, null),
                            apply(modelId, transform.x(), transform.y(), false));
                }
            }
        } else if (glazed) {
            for (ColumnOccupancy physical : ColumnOccupancy.values()) {
                for (Direction pattern : HORIZONTAL) {
                    ColumnOccupancy relative = physical.relativeTo(pattern);
                    String modelId = columnGlazedModelId(shape, relative);
                    models.putIfAbsent(modelId, columnWorldModel(profile, relative, null, true));
                    variants.add(columnVariantKey(physical, null, pattern),
                            apply(modelId, GlazedPatternState.patternYaw(pattern), true));
                }
            }
        } else {
            for (ColumnOccupancy occupancy : ColumnOccupancy.values()) {
                String modelId = columnModelId(shape, occupancy);
                models.put(modelId, columnWorldModel(profile, occupancy, null, false));
                variants.add(columnVariantKey(occupancy, null, null), apply(modelId, 0, false));
            }
        }

        String itemModel = modelId(shape, "_item");
        Cuboid itemCuboid = Cuboid.world(ColumnOccupancy.SE.bounds().getFirst());
        models.put(itemModel, CuboidListModelProjection.itemModel(profile, itemCuboid,
                axis ? Direction.Axis.Y : null, glazed));
        return projection(variants, models, itemModel);
    }

    private static AxisUvPolicy resolvedAxisPolicy(ResourceManager manager,
            NibaruMaterialProfile profile) {
        Objects.requireNonNull(profile, "profile");
        return MaterialAxisState.applies(profile)
                ? AxisGeneratedResources.policy(manager, profile.canonicalParentId())
                : AxisUvPolicy.DIRECT_UV_LOCKED;
    }

    private static void requireExplicitAxisPolicy(NibaruMaterialProfile profile) {
        Objects.requireNonNull(profile, "profile");
        if (MaterialAxisState.applies(profile)) {
            throw new IllegalArgumentException("Axis-aware Corner/Column projection requires the "
                    + "canonical AxisUvPolicy for " + profile.canonicalParentId());
        }
    }

    private static void rejectCombinedFrames(NibaruMaterialProfile profile, boolean axis, boolean glazed) {
        if (axis && glazed) {
            throw new IllegalArgumentException("Corner/Column projection does not support a material that is both "
                    + "axis-aligned and glazed-oriented: " + profile.canonicalParentId());
        }
    }

    private static Projection projection(JsonObject variants, LinkedHashMap<String, JsonObject> models,
            String itemModel) {
        JsonObject blockState = new JsonObject();
        blockState.add("variants", variants);
        return new Projection(blockState, models, itemModel);
    }

    /** Exact reference Quarter Piece bounds; this identity deliberately does not compose. */
    public static Bounds cornerBounds(Direction facing, Half half) {
        Objects.requireNonNull(facing, "facing");
        Objects.requireNonNull(half, "half");
        if (!facing.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Corner facing must be horizontal: " + facing);
        }
        double y0 = half == Half.BOTTOM ? 0 : 8;
        double y1 = y0 + 8;
        return switch (facing) {
            case NORTH -> new Bounds(0, y0, 0, 16, y1, 8);
            case SOUTH -> new Bounds(0, y0, 8, 16, y1, 16);
            case EAST -> new Bounds(8, y0, 0, 16, y1, 16);
            case WEST -> new Bounds(0, y0, 0, 8, y1, 16);
            default -> throw new IllegalArgumentException("Corner facing must be horizontal: " + facing);
        };
    }

    private static JsonObject cornerWorldModel(NibaruMaterialProfile profile, Direction facing,
            Half half, Direction.Axis materialAxis, boolean glazed) {
        return CuboidListModelProjection.worldModel(profile,
                List.of(Cuboid.world(cornerProfileBounds(profile, facing, half))), materialAxis, glazed);
    }

    private static JsonObject cornerAxisWorldModel(NibaruMaterialProfile profile, Direction facing,
            Half half, Direction.Axis materialAxis, AxisUvPolicy axisPolicy) {
        return CuboidListModelProjection.axisWorldModel(profile,
                List.of(Cuboid.world(cornerProfileBounds(profile, facing, half))),
                materialAxis, axisPolicy);
    }

    /** Path Corner follows the accepted Step frame: 0..7 bottom and 7..15 top. */
    private static Bounds cornerProfileBounds(NibaruMaterialProfile profile,
            Direction facing, Half half) {
        Bounds physical = cornerBounds(facing, half);
        if (profile.surfaceSamplingPolicy()
                != NibaruMaterialProfile.SurfaceSamplingPolicy.PATH_LOWERED_SURFACE) {
            return physical;
        }
        double y0 = half == Half.BOTTOM ? 0 : 7;
        double y1 = half == Half.BOTTOM ? 7 : 15;
        return new Bounds(physical.x0(), y0, physical.z0(), physical.x1(), y1, physical.z1());
    }

    private static JsonObject columnWorldModel(NibaruMaterialProfile profile, ColumnOccupancy occupancy,
            Direction.Axis materialAxis, boolean glazed) {
        return CuboidListModelProjection.worldModel(profile,
                occupancy.bounds().stream().map(Cuboid::world).toList(), materialAxis, glazed);
    }

    private static JsonObject columnAxisWorldModel(NibaruMaterialProfile profile,
            ColumnOccupancy occupancy, Direction.Axis materialAxis, AxisUvPolicy axisPolicy) {
        return CuboidListModelProjection.axisWorldModel(profile,
                occupancy.bounds().stream().map(Cuboid::world).toList(), materialAxis, axisPolicy);
    }

    private static String cornerVariantKey(Direction facing, Half half,
            Direction.Axis axis, Direction pattern) {
        StringBuilder result = new StringBuilder("facing=").append(facing.getSerializedName())
                .append(",half=").append(half.getSerializedName());
        if (axis != null) result.append(",axis=").append(axisName(axis));
        if (pattern != null) result.append(",pattern_facing=").append(pattern.getSerializedName());
        return result.toString();
    }

    private static String columnVariantKey(ColumnOccupancy occupancy,
            Direction.Axis axis, Direction pattern) {
        StringBuilder result = new StringBuilder("occupancy=").append(occupancy.serializedName());
        if (axis != null) result.append(",axis=").append(axisName(axis));
        if (pattern != null) result.append(",pattern_facing=").append(pattern.getSerializedName());
        return result.toString();
    }

    private static JsonObject apply(String model, int yRotation, boolean explicitUvLock) {
        return apply(model, 0, yRotation, explicitUvLock);
    }

    private static JsonObject apply(
            String model, int xRotation, int yRotation, boolean explicitUvLock) {
        JsonObject result = new JsonObject();
        result.addProperty("model", model);
        if (xRotation != 0) result.addProperty("x", xRotation);
        if (yRotation != 0) result.addProperty("y", yRotation);
        if (explicitUvLock) result.addProperty("uvlock", false);
        return result;
    }

    private static AxisTransform axisTransform(AxisUvPolicy policy, Direction.Axis axis) {
        if (policy == AxisUvPolicy.DIRECT_UV_LOCKED || axis == Direction.Axis.Y) {
            return AxisTransform.IDENTITY;
        }
        return new AxisTransform(90, axis == Direction.Axis.X ? 90 : 0);
    }

    private static String cornerModelId(Identifier shape, Direction facing, Half half) {
        return modelId(shape, "_" + facing.getSerializedName() + "_" + half.getSerializedName());
    }

    private static String cornerAxisModelId(Identifier shape, Direction facing, Half half,
            Direction.Axis axis) {
        return modelId(shape, "_" + facing.getSerializedName() + "_" + half.getSerializedName()
                + "_axis_" + axisName(axis));
    }

    private static String cornerGlazedModelId(Identifier shape, Direction relative, Half half) {
        return modelId(shape, "_relative_" + relative.getSerializedName() + "_"
                + half.getSerializedName() + "_glazed");
    }

    private static String columnModelId(Identifier shape, ColumnOccupancy occupancy) {
        return modelId(shape, "_" + occupancy.serializedName());
    }

    private static String columnAxisModelId(Identifier shape, ColumnOccupancy occupancy,
            Direction.Axis axis) {
        return modelId(shape, "_" + occupancy.serializedName() + "_axis_" + axisName(axis));
    }

    private static String columnGlazedModelId(Identifier shape, ColumnOccupancy occupancy) {
        return modelId(shape, "_relative_" + occupancy.serializedName() + "_glazed");
    }

    private static String modelId(Identifier shape, String suffix) {
        return shape.getNamespace() + ":block/" + shape.getPath() + suffix;
    }

    private static String axisName(Direction.Axis axis) {
        return switch (axis) {
            case X -> "x";
            case Y -> "y";
            case Z -> "z";
        };
    }

    public static int cornerSelectorCount(NibaruMaterialProfile profile) {
        boolean axis = MaterialAxisState.applies(profile);
        boolean glazed = profile.capabilities().contains(BehaviorCapability.GLAZED_ORIENTATION);
        rejectCombinedFrames(profile, axis, glazed);
        if (axis) return 24;
        if (glazed) return 32;
        return 8;
    }

    public static int columnSelectorCount(NibaruMaterialProfile profile) {
        boolean axis = MaterialAxisState.applies(profile);
        boolean glazed = profile.capabilities().contains(BehaviorCapability.GLAZED_ORIENTATION);
        rejectCombinedFrames(profile, axis, glazed);
        if (axis) return 18;
        if (glazed) return 24;
        return 6;
    }

    /** Client-side exact occupancy table; serialized names match BgeColumnBlock.Occupancy. */
    public enum ColumnOccupancy {
        NW("north_west", List.of(new Bounds(0, 0, 0, 8, 16, 8))),
        NE("north_east", List.of(new Bounds(8, 0, 0, 16, 16, 8))),
        SW("south_west", List.of(new Bounds(0, 0, 8, 8, 16, 16))),
        SE("south_east", List.of(new Bounds(8, 0, 8, 16, 16, 16))),
        NW_SE("north_west_south_east", List.of(
                new Bounds(0, 0, 0, 8, 16, 8),
                new Bounds(8, 0, 8, 16, 16, 16))),
        NE_SW("north_east_south_west", List.of(
                new Bounds(8, 0, 0, 16, 16, 8),
                new Bounds(0, 0, 8, 8, 16, 16)));

        private final String serializedName;
        private final List<Bounds> bounds;

        ColumnOccupancy(String serializedName, List<Bounds> bounds) {
            this.serializedName = serializedName;
            this.bounds = List.copyOf(bounds);
        }

        public String serializedName() {
            return serializedName;
        }

        public List<Bounds> bounds() {
            return bounds;
        }

        /** Expresses physical occupancy in the unrotated glazed-pattern model frame. */
        public ColumnOccupancy relativeTo(Direction pattern) {
            int turns = GlazedPatternState.patternYaw(pattern) / 90;
            ColumnOccupancy result = this;
            for (int i = 0; i < turns; i++) result = result.counterClockwise();
            return result;
        }

        private ColumnOccupancy counterClockwise() {
            return switch (this) {
                case NW -> SW;
                case SW -> SE;
                case SE -> NE;
                case NE -> NW;
                case NW_SE -> NE_SW;
                case NE_SW -> NW_SE;
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

    private record AxisTransform(int x, int y) {
        private static final AxisTransform IDENTITY = new AxisTransform(0, 0);
    }
}
