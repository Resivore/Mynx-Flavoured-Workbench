package dev.aero.cnmterraincompat;

import dev.aero.cnmterraincompat.BgeMaterialBindings.Topology;
import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * BGE-owned description of the real axis-aligned rendered surfaces of a bound geometry state.
 *
 * <p>The contract is deliberately a collection of exposed planar patches rather than one bounding
 * box. Consumers do not need to know whether a state is a Slab, Step, Corner, or a future geometry:
 * a canonical binding and a supported surface model are sufficient. Native Stairs and Walls stay
 * explicitly unsupported until their contextual model topology can be represented without
 * guessing.</p>
 */
public final class BgeSurfaceGeometry {
    private static final int BLOCK_UNITS = 16;

    private BgeSurfaceGeometry() {}

    /** Exact planes are ordinary surfaces; terrain insets opt into one semantic 1/16 offset. */
    public enum PlaneRelation {
        EXACT,
        TERRAIN_HEIGHT_INSET
    }

    /** Supplies the surface model for one exact physical block state. */
    @FunctionalInterface
    public interface SurfaceProvider {
        SurfaceModel describe(BlockState state);
    }

    /**
     * One exposed, block-local surface region. {@code canonicalFace} identifies the corresponding
     * canonical material face whose Continuity semantics the rendered face presents.
     */
    public record SurfacePatch(Direction normal, int plane16, Direction.Axis uAxis,
            int uMin16, int uMax16, Direction.Axis vAxis, int vMin16, int vMax16,
            Direction canonicalFace, PlaneRelation planeRelation) {
        public SurfacePatch {
            Objects.requireNonNull(normal, "normal");
            Objects.requireNonNull(uAxis, "uAxis");
            Objects.requireNonNull(vAxis, "vAxis");
            Objects.requireNonNull(canonicalFace, "canonicalFace");
            Objects.requireNonNull(planeRelation, "planeRelation");
            if (normal.getAxis() == uAxis || normal.getAxis() == vAxis || uAxis == vAxis
                    || canonicalFace.getAxis() != normal.getAxis()
                    || plane16 < 0 || plane16 > BLOCK_UNITS
                    || uMin16 < 0 || uMax16 > BLOCK_UNITS || uMin16 >= uMax16
                    || vMin16 < 0 || vMax16 > BLOCK_UNITS || vMin16 >= vMax16) {
                throw new IllegalArgumentException("Invalid BGE surface patch");
            }
        }
    }

    /** Supported models own complete surface data; unsupported models carry a precise limitation. */
    public record SurfaceModel(List<SurfacePatch> patches, Optional<String> limitation) {
        public SurfaceModel {
            patches = List.copyOf(Objects.requireNonNull(patches, "patches"));
            limitation = Objects.requireNonNull(limitation, "limitation");
            if (patches.isEmpty() == limitation.isEmpty()) {
                throw new IllegalArgumentException(
                        "A surface model must contain patches or one limitation, but not both");
            }
        }

        public boolean supported() {
            return limitation.isEmpty();
        }

        public List<SurfacePatch> patches(Direction normal) {
            return patches.stream().filter(patch -> patch.normal() == normal).toList();
        }

        public static SurfaceModel unsupported(String limitation) {
            if (limitation == null || limitation.isBlank()) {
                throw new IllegalArgumentException("Unsupported surface model needs a limitation");
            }
            return new SurfaceModel(List.of(), Optional.of(limitation));
        }
    }

    /** Built-in provider used by current bindings; future bindings may supply another provider. */
    static SurfaceProvider provider(Topology topology, boolean terrainHeightInset) {
        Objects.requireNonNull(topology, "topology");
        return state -> describe(topology, state, terrainHeightInset);
    }

    private static SurfaceModel describe(Topology topology, BlockState state,
            boolean terrainHeightInset) {
        Objects.requireNonNull(state, "state");
        if (topology == Topology.STAIR) {
            return SurfaceModel.unsupported("Native Stair surface regions remain fail-closed: their "
                    + "vanilla inner/outer model topology is contextual and is not yet exported by BGE.");
        }
        if (topology == Topology.WALL) {
            return SurfaceModel.unsupported("Native Wall surface regions remain fail-closed: post and "
                    + "arm geometry is neighbor-contextual and is not yet exported by BGE.");
        }

        List<Cuboid> actual = cuboids(topology, state, terrainHeightInset);
        if (!terrainHeightInset) return model(actual, List.of());
        List<Cuboid> standard = cuboids(topology == Topology.FARMLAND_SLAB
                ? Topology.HORIZONTAL_SLAB : topology, state, false);
        return model(actual, exposed(standard));
    }

    private static SurfaceModel model(List<Cuboid> cuboids, List<SurfacePatch> standardPatches) {
        List<SurfacePatch> patches = exposed(cuboids);
        if (!standardPatches.isEmpty()) {
            patches = patches.stream().map(patch -> terrainClassified(patch, standardPatches)).toList();
        }
        if (patches.isEmpty()) {
            return SurfaceModel.unsupported("The BGE surface provider produced no exposed regions.");
        }
        return new SurfaceModel(patches, Optional.empty());
    }

    private static SurfacePatch terrainClassified(SurfacePatch patch,
            List<SurfacePatch> standardPatches) {
        if (patch.normal() != Direction.UP) return patch;
        boolean intentionalInset = standardPatches.stream().anyMatch(standard ->
                standard.normal() == patch.normal()
                        && standard.plane16() == patch.plane16() + 1
                        && standard.uAxis() == patch.uAxis()
                        && standard.uMin16() == patch.uMin16()
                        && standard.uMax16() == patch.uMax16()
                        && standard.vAxis() == patch.vAxis()
                        && standard.vMin16() == patch.vMin16()
                        && standard.vMax16() == patch.vMax16());
        return intentionalInset
                ? new SurfacePatch(patch.normal(), patch.plane16(), patch.uAxis(),
                        patch.uMin16(), patch.uMax16(), patch.vAxis(), patch.vMin16(),
                        patch.vMax16(), patch.canonicalFace(), PlaneRelation.TERRAIN_HEIGHT_INSET)
                : patch;
    }

    private static List<Cuboid> cuboids(Topology topology, BlockState state,
            boolean terrainHeightInset) {
        return switch (topology) {
            case CANONICAL_ROOT -> List.of(terrainHeightInset
                    ? new Cuboid(0, 0, 0, 16, 15, 16) : Cuboid.FULL);
            case HORIZONTAL_SLAB -> List.of(horizontal(
                    state.getValue(SlabBlock.TYPE), terrainHeightInset));
            case FARMLAND_SLAB -> List.of(horizontal(
                    state.getValue(FarmlandSlabBlock.TYPE), true));
            case VERTICAL_SLAB -> List.of(vertical(state, terrainHeightInset));
            case STEP -> step(state, terrainHeightInset);
            case LAYER -> List.of(layer(state, terrainHeightInset));
            case CORNER -> corner(state, terrainHeightInset);
            case QUARTER_COLUMN -> column(state, terrainHeightInset);
            case STAIR, WALL -> throw new IllegalStateException("Unsupported topology was not gated");
        };
    }

    private static Cuboid horizontal(SlabType type, boolean terrain) {
        if (!terrain) {
            return switch (type) {
                case BOTTOM -> new Cuboid(0, 0, 0, 16, 8, 16);
                case TOP -> new Cuboid(0, 8, 0, 16, 16, 16);
                case DOUBLE -> Cuboid.FULL;
            };
        }
        return switch (type) {
            case BOTTOM -> new Cuboid(0, 0, 0, 16, 7, 16);
            case TOP -> new Cuboid(0, 7, 0, 16, 15, 16);
            case DOUBLE -> new Cuboid(0, 0, 0, 16, 15, 16);
        };
    }

    private static Cuboid vertical(BlockState state, boolean terrain) {
        int maxY = terrain ? 15 : 16;
        if (state.getValue(VerticalSlabBlock.DOUBLE)) {
            return new Cuboid(0, 0, 0, 16, maxY, 16);
        }
        return switch (state.getValue(VerticalSlabBlock.FACING)) {
            case NORTH -> new Cuboid(0, 0, 0, 16, maxY, 8);
            case EAST -> new Cuboid(8, 0, 0, 16, maxY, 16);
            case SOUTH -> new Cuboid(0, 0, 8, 16, maxY, 16);
            case WEST -> new Cuboid(0, 0, 0, 8, maxY, 16);
            default -> throw new IllegalArgumentException("Vertical Slab facing must be horizontal");
        };
    }

    private static List<Cuboid> step(BlockState state, boolean terrain) {
        Direction facing = state.getValue(StepBlock.FACING);
        SlabType type = state.getValue(StepBlock.SLAB_TYPE);
        int split = terrain ? 7 : 8;
        int top = terrain ? 15 : 16;
        if (type == SlabType.DOUBLE) {
            return List.of(stepMember(facing, split, top),
                    stepMember(facing.getOpposite(), 0, split));
        }
        return List.of(type == SlabType.TOP
                ? stepMember(facing, split, top) : stepMember(facing, 0, split));
    }

    private static Cuboid stepMember(Direction facing, int minY, int maxY) {
        return switch (facing) {
            case NORTH -> new Cuboid(0, minY, 0, 16, maxY, 8);
            case EAST -> new Cuboid(8, minY, 0, 16, maxY, 16);
            case SOUTH -> new Cuboid(0, minY, 8, 16, maxY, 16);
            case WEST -> new Cuboid(0, minY, 0, 8, maxY, 16);
            default -> throw new IllegalArgumentException("Step facing must be horizontal");
        };
    }

    private static Cuboid layer(BlockState state, boolean terrain) {
        int depth = state.getValue(BgeLayerBlock.LAYERS) * 4;
        Direction facing = state.getValue(BgeLayerBlock.FACING);
        if (!terrain) {
            return switch (facing) {
                case UP -> new Cuboid(0, 0, 0, 16, depth, 16);
                case DOWN -> new Cuboid(0, 16 - depth, 0, 16, 16, 16);
                case NORTH -> new Cuboid(0, 0, 16 - depth, 16, 16, 16);
                case EAST -> new Cuboid(16 - depth, 0, 0, 16, 16, 16);
                case SOUTH -> new Cuboid(0, 0, 0, 16, 16, depth);
                case WEST -> new Cuboid(0, 0, 0, depth, 16, 16);
            };
        }
        return switch (facing) {
            case UP -> new Cuboid(0, 0, 0, 16, Math.min(depth, 15), 16);
            case DOWN -> new Cuboid(0, 16 - depth, 0, 16, 15, 16);
            case NORTH -> new Cuboid(0, 0, 16 - depth, 16, 15, 16);
            case EAST -> new Cuboid(0, 0, 0, depth, 15, 16);
            case SOUTH -> new Cuboid(0, 0, 0, 16, 15, depth);
            case WEST -> new Cuboid(16 - depth, 0, 0, 16, 15, 16);
        };
    }

    private static List<Cuboid> corner(BlockState state, boolean terrain) {
        int maxY = terrain ? 15 : 16;
        return switch (BgeCornerBlock.orientation(state)) {
            case SOUTH_WEST -> List.of(
                    new Cuboid(0, 0, 0, 8, maxY, 8),
                    new Cuboid(0, 0, 8, 16, maxY, 16));
            case NORTH_WEST -> List.of(
                    new Cuboid(0, 0, 0, 16, maxY, 8),
                    new Cuboid(0, 0, 8, 8, maxY, 16));
            case NORTH_EAST -> List.of(
                    new Cuboid(0, 0, 0, 16, maxY, 8),
                    new Cuboid(8, 0, 8, 16, maxY, 16));
            case SOUTH_EAST -> List.of(
                    new Cuboid(8, 0, 0, 16, maxY, 8),
                    new Cuboid(0, 0, 8, 16, maxY, 16));
        };
    }

    private static List<Cuboid> column(BlockState state, boolean terrain) {
        int maxY = terrain ? 15 : 16;
        Cuboid nw = new Cuboid(0, 0, 0, 8, maxY, 8);
        Cuboid ne = new Cuboid(8, 0, 0, 16, maxY, 8);
        Cuboid sw = new Cuboid(0, 0, 8, 8, maxY, 16);
        Cuboid se = new Cuboid(8, 0, 8, 16, maxY, 16);
        return switch (state.getValue(BgeColumnBlock.OCCUPANCY)) {
            case NW -> List.of(nw);
            case NE -> List.of(ne);
            case SW -> List.of(sw);
            case SE -> List.of(se);
            case NW_SE -> List.of(nw, se);
            case NE_SW -> List.of(ne, sw);
        };
    }

    /** Deterministically tiles the exposed boundary of an arbitrary cuboid union. */
    private static List<SurfacePatch> exposed(List<Cuboid> cuboids) {
        if (cuboids.isEmpty()) return List.of();
        int[] xs = coordinates(cuboids, Direction.Axis.X);
        int[] ys = coordinates(cuboids, Direction.Axis.Y);
        int[] zs = coordinates(cuboids, Direction.Axis.Z);
        boolean[][][] occupied = new boolean[xs.length - 1][ys.length - 1][zs.length - 1];
        for (int x = 0; x < xs.length - 1; x++) {
            for (int y = 0; y < ys.length - 1; y++) {
                for (int z = 0; z < zs.length - 1; z++) {
                    int x0 = xs[x], x1 = xs[x + 1];
                    int y0 = ys[y], y1 = ys[y + 1];
                    int z0 = zs[z], z1 = zs[z + 1];
                    occupied[x][y][z] = cuboids.stream().anyMatch(cuboid ->
                            cuboid.contains(x0, y0, z0, x1, y1, z1));
                }
            }
        }

        List<SurfacePatch> result = new ArrayList<>();
        for (Direction normal : Direction.values()) {
            for (int x = 0; x < xs.length - 1; x++) {
                for (int y = 0; y < ys.length - 1; y++) {
                    for (int z = 0; z < zs.length - 1; z++) {
                        if (!occupied[x][y][z] || occupied(occupied,
                                x + normal.getStepX(), y + normal.getStepY(),
                                z + normal.getStepZ())) continue;
                        result.add(patch(normal, xs[x], xs[x + 1], ys[y], ys[y + 1],
                                zs[z], zs[z + 1]));
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static SurfacePatch patch(Direction normal, int x0, int x1, int y0, int y1,
            int z0, int z1) {
        int plane = switch (normal.getAxis()) {
            case X -> normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? x1 : x0;
            case Y -> normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? y1 : y0;
            case Z -> normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? z1 : z0;
        };
        Direction.Axis[] axes = inPlaneAxes(normal.getAxis());
        int[] u = interval(axes[0], x0, x1, y0, y1, z0, z1);
        int[] v = interval(axes[1], x0, x1, y0, y1, z0, z1);
        return new SurfacePatch(normal, plane, axes[0], u[0], u[1], axes[1], v[0], v[1],
                normal, PlaneRelation.EXACT);
    }

    private static boolean occupied(boolean[][][] occupied, int x, int y, int z) {
        return x >= 0 && x < occupied.length && y >= 0 && y < occupied[0].length
                && z >= 0 && z < occupied[0][0].length && occupied[x][y][z];
    }

    private static int[] coordinates(List<Cuboid> cuboids, Direction.Axis axis) {
        TreeSet<Integer> values = new TreeSet<>();
        values.add(0);
        values.add(BLOCK_UNITS);
        for (Cuboid cuboid : cuboids) {
            values.add(cuboid.min(axis));
            values.add(cuboid.max(axis));
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] interval(Direction.Axis axis, int x0, int x1, int y0, int y1,
            int z0, int z1) {
        return switch (axis) {
            case X -> new int[] {x0, x1};
            case Y -> new int[] {y0, y1};
            case Z -> new int[] {z0, z1};
        };
    }

    private static Direction.Axis[] inPlaneAxes(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private record Cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private static final Cuboid FULL = new Cuboid(0, 0, 0, 16, 16, 16);

        private Cuboid {
            if (minX < 0 || minY < 0 || minZ < 0 || maxX > BLOCK_UNITS
                    || maxY > BLOCK_UNITS || maxZ > BLOCK_UNITS
                    || minX >= maxX || minY >= maxY || minZ >= maxZ) {
                throw new IllegalArgumentException("Invalid BGE surface cuboid");
            }
        }

        private boolean contains(int x0, int y0, int z0, int x1, int y1, int z1) {
            return minX <= x0 && maxX >= x1 && minY <= y0 && maxY >= y1
                    && minZ <= z0 && maxZ >= z1;
        }

        private int min(Direction.Axis axis) {
            return switch (axis) {
                case X -> minX;
                case Y -> minY;
                case Z -> minZ;
            };
        }

        private int max(Direction.Axis axis) {
            return switch (axis) {
                case X -> maxX;
                case Y -> maxY;
                case Z -> maxZ;
            };
        }
    }
}
