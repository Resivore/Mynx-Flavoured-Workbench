package dev.resivore.bgectm;

import dev.aero.cnmterraincompat.BgeLayerBlock;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Objects;
import java.util.Optional;

/**
 * Central Canary 3 geometry/contact policy for Continuity neighbor decisions.
 *
 * <p>Every supported carrier is one axis-aligned cuboid expressed in exact sixteenths.
 * Material profiles identify canonical ownership and supported geometry only. Continuity owns
 * every material/rule relationship; this class can only veto its positive result when the
 * same-facing surfaces do not share a world plane and genuine evaluated-boundary contact.
 * Unrelated full-block Continuity decisions bypass unchanged.</p>
 */
public final class SurfaceContactResolver {
    private static final int BLOCK_UNITS = 16;

    private SurfaceContactResolver() {}

    public static Decision inspect(BlockState sourceState, BlockPos sourcePos,
            BlockState otherState, BlockPos otherPos, Direction face) {
        return inspect(sourceState, sourcePos, otherState, otherPos, face, null);
    }

    /** Applies the same policy using the exact current Continuity source-quad bounds. */
    public static Decision inspectWithSourceSurface(BlockState sourceState, BlockPos sourcePos,
            BlockState otherState, BlockPos otherPos, Direction face, QuadSurface sourceQuad) {
        return inspect(sourceState, sourcePos, otherState, otherPos, face,
                Objects.requireNonNull(sourceQuad, "sourceQuad"));
    }

    private static Decision inspect(BlockState sourceState, BlockPos sourcePos,
            BlockState otherState, BlockPos otherPos, Direction face, QuadSurface sourceQuad) {
        Objects.requireNonNull(sourceState, "sourceState");
        Objects.requireNonNull(sourcePos, "sourcePos");
        Objects.requireNonNull(otherState, "otherState");
        Objects.requireNonNull(otherPos, "otherPos");
        Objects.requireNonNull(face, "face");

        Endpoint source = endpoint(sourceState);
        Endpoint other = endpoint(otherState);
        if (!source.participates() && !other.participates()) {
            return Decision.BYPASS_UNRELATED;
        }
        if (!source.supported() || !other.supported()) {
            if (source.profile() == null || other.profile() == null) {
                return Decision.MISSING_MATERIAL_PROFILE;
            }
            return Decision.UNSUPPORTED_GEOMETRY;
        }
        if (sourceQuad != null && sourceQuad.normal() != face) {
            return Decision.INVALID_QUAD_SURFACE;
        }
        SurfaceDescriptor sourceSurface = sourceQuad == null
                ? descriptor(source, sourceState, sourcePos, face)
                : descriptor(sourceQuad, sourcePos);
        SurfaceDescriptor otherSurface = descriptor(other, otherState, otherPos, face);
        if (sourceSurface.plane16() != otherSurface.plane16()) {
            return Decision.NON_COPLANAR;
        }
        return meetAlongEvaluatedBoundary(sourceSurface, otherSurface)
                ? Decision.CONNECT
                : Decision.NO_BOUNDARY_CONTACT;
    }

    /** Returns a supported surface descriptor for focused fixtures and diagnostics. */
    public static Optional<SurfaceDescriptor> describe(BlockState state, BlockPos pos, Direction face) {
        Endpoint endpoint = endpoint(state);
        return endpoint.supported()
                ? Optional.of(descriptor(endpoint, state, pos, face))
                : Optional.empty();
    }

    /** Exact descriptor comparison, including face normal, plane, and relevant boundary. */
    public static boolean meetAlongEvaluatedBoundary(
            SurfaceDescriptor source, SurfaceDescriptor other) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(other, "other");
        if (source.normal() != other.normal()
                || source.plane16() != other.plane16()
                || source.uAxis() != other.uAxis()
                || source.vAxis() != other.vAxis()) {
            return false;
        }

        int normalDelta = coordinate(other.blockPos(), source.normal().getAxis())
                - coordinate(source.blockPos(), source.normal().getAxis());
        int uDelta = coordinate(other.blockPos(), source.uAxis())
                - coordinate(source.blockPos(), source.uAxis());
        int vDelta = coordinate(other.blockPos(), source.vAxis())
                - coordinate(source.blockPos(), source.vAxis());
        if (normalDelta != 0 || Math.abs(uDelta) > 1 || Math.abs(vDelta) > 1
                || uDelta == 0 && vDelta == 0) {
            return false;
        }
        return meets(source.uBounds(), other.uBounds(), uDelta)
                && meets(source.vBounds(), other.vBounds(), vDelta);
    }

    private static boolean meets(Interval source, Interval other, int delta) {
        return switch (Integer.signum(delta)) {
            case -1 -> source.min16() == other.max16();
            case 0 -> Math.min(source.max16(), other.max16())
                    > Math.max(source.min16(), other.min16());
            case 1 -> source.max16() == other.min16();
            default -> false;
        };
    }

    private static Endpoint endpoint(BlockState state) {
        Optional<NibaruProviderAdapter.RuntimeBinding> runtime =
                NibaruProviderAdapter.runtimeBinding(state.getBlock());
        if (runtime.isPresent()) {
            CanonicalAppearanceResolver.MaterialBinding binding =
                    CanonicalAppearanceResolver.materialBinding(state).orElseThrow();
            return switch (binding.carrier()) {
                case LAYER -> CanonicalAppearanceResolver.inspect(state).inherited()
                        ? new Endpoint(binding.profile(), Kind.LAYER)
                        : new Endpoint(binding.profile(), Kind.INELIGIBLE_PROFILE_GEOMETRY);
                case VERTICAL_SLAB -> CanonicalAppearanceResolver.inspect(state).inherited()
                        ? new Endpoint(binding.profile(), Kind.VERTICAL_SLAB)
                        : new Endpoint(binding.profile(), Kind.INELIGIBLE_PROFILE_GEOMETRY);
                case STEP, CORNER, QUARTER_COLUMN, UNKNOWN ->
                        new Endpoint(binding.profile(), Kind.BGE_UNSUPPORTED);
                case ORDINARY_SLAB -> throw new IllegalStateException(
                        "Runtime BGE binding cannot be an ordinary slab");
            };
        }

        Optional<NibaruMaterialProfile> profile = NibaruMaterialProfiles.fromBlock(state.getBlock());
        if (profile.isEmpty()) {
            return new Endpoint(null, Kind.UNMANAGED);
        }
        NibaruMaterialProfile material = profile.get();
        if (material.canonicalParent() == state.getBlock()) {
            return new Endpoint(material, Kind.FULL_BLOCK);
        }
        if (state.getBlock() instanceof SlabBlock
                && material.effectiveSlabSource().orElse(null) == state.getBlock()) {
            return CanonicalAppearanceResolver.inspect(state).inherited()
                    ? new Endpoint(material, Kind.ORDINARY_SLAB)
                    : new Endpoint(material, Kind.INELIGIBLE_PROFILE_GEOMETRY);
        }
        return new Endpoint(material, Kind.PROFILE_UNSUPPORTED);
    }

    private static SurfaceDescriptor descriptor(Endpoint endpoint, BlockState state,
            BlockPos pos, Direction face) {
        return descriptor(cuboid(endpoint, state), pos, face);
    }

    private static Cuboid cuboid(Endpoint endpoint, BlockState state) {
        return switch (endpoint.kind()) {
            case FULL_BLOCK -> Cuboid.FULL;
            case ORDINARY_SLAB -> slabCuboid(state);
            case LAYER -> layerCuboid(state);
            case VERTICAL_SLAB -> verticalSlabCuboid(state);
            default -> throw new IllegalStateException("Unsupported endpoint: " + endpoint.kind());
        };
    }

    private static Cuboid slabCuboid(BlockState state) {
        SlabType type = state.getValue(BlockStateProperties.SLAB_TYPE);
        return switch (type) {
            case BOTTOM -> new Cuboid(0, 0, 0, 16, 8, 16);
            case TOP -> new Cuboid(0, 8, 0, 16, 16, 16);
            case DOUBLE -> Cuboid.FULL;
        };
    }

    private static Cuboid layerCuboid(BlockState state) {
        int depth = state.getValue(BgeLayerBlock.LAYERS) * 4;
        return switch (state.getValue(BgeLayerBlock.FACING)) {
            case UP -> new Cuboid(0, 0, 0, 16, depth, 16);
            case DOWN -> new Cuboid(0, 16 - depth, 0, 16, 16, 16);
            case NORTH -> new Cuboid(0, 0, 16 - depth, 16, 16, 16);
            case SOUTH -> new Cuboid(0, 0, 0, 16, 16, depth);
            case EAST -> new Cuboid(0, 0, 0, depth, 16, 16);
            case WEST -> new Cuboid(16 - depth, 0, 0, 16, 16, 16);
        };
    }

    private static Cuboid verticalSlabCuboid(BlockState state) {
        if (state.getValue(VerticalSlabBlock.DOUBLE)) {
            return Cuboid.FULL;
        }
        return switch (state.getValue(VerticalSlabBlock.FACING)) {
            case NORTH -> new Cuboid(0, 0, 0, 16, 16, 8);
            case SOUTH -> new Cuboid(0, 0, 8, 16, 16, 16);
            case EAST -> new Cuboid(8, 0, 0, 16, 16, 16);
            case WEST -> new Cuboid(0, 0, 0, 8, 16, 16);
            default -> throw new IllegalStateException("Vertical Slab has non-horizontal facing");
        };
    }

    private static SurfaceDescriptor descriptor(Cuboid cuboid, BlockPos pos, Direction face) {
        Direction.Axis normalAxis = face.getAxis();
        Direction.Axis[] inPlane = inPlaneAxes(normalAxis);
        long normalOrigin = (long) coordinate(pos, normalAxis) * BLOCK_UNITS;
        long localPlane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? cuboid.max(normalAxis)
                : cuboid.min(normalAxis);
        return new SurfaceDescriptor(face, normalOrigin + localPlane,
                inPlane[0], worldInterval(cuboid, pos, inPlane[0]),
                inPlane[1], worldInterval(cuboid, pos, inPlane[1]), pos.immutable());
    }

    private static SurfaceDescriptor descriptor(QuadSurface surface, BlockPos pos) {
        long normalOrigin = (long) coordinate(pos, surface.normal().getAxis()) * BLOCK_UNITS;
        long uOrigin = (long) coordinate(pos, surface.uAxis()) * BLOCK_UNITS;
        long vOrigin = (long) coordinate(pos, surface.vAxis()) * BLOCK_UNITS;
        return new SurfaceDescriptor(surface.normal(), normalOrigin + surface.plane16(),
                surface.uAxis(), new Interval(uOrigin + surface.uMin16(), uOrigin + surface.uMax16()),
                surface.vAxis(), new Interval(vOrigin + surface.vMin16(), vOrigin + surface.vMax16()),
                pos.immutable());
    }

    private static Interval worldInterval(Cuboid cuboid, BlockPos pos, Direction.Axis axis) {
        long origin = (long) coordinate(pos, axis) * BLOCK_UNITS;
        return new Interval(origin + cuboid.min(axis), origin + cuboid.max(axis));
    }

    private static Direction.Axis[] inPlaneAxes(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private static int coordinate(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private enum Kind {
        FULL_BLOCK,
        ORDINARY_SLAB,
        LAYER,
        VERTICAL_SLAB,
        BGE_UNSUPPORTED,
        INELIGIBLE_PROFILE_GEOMETRY,
        PROFILE_UNSUPPORTED,
        UNMANAGED
    }

    private record Endpoint(NibaruMaterialProfile profile, Kind kind) {
        boolean participates() {
            return kind == Kind.ORDINARY_SLAB || kind == Kind.LAYER
                    || kind == Kind.VERTICAL_SLAB || kind == Kind.BGE_UNSUPPORTED;
        }

        boolean supported() {
            return kind == Kind.FULL_BLOCK || kind == Kind.ORDINARY_SLAB
                    || kind == Kind.LAYER || kind == Kind.VERTICAL_SLAB;
        }
    }

    private record Cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private static final Cuboid FULL = new Cuboid(0, 0, 0, 16, 16, 16);

        Cuboid {
            if (minX < 0 || minY < 0 || minZ < 0 || maxX > 16 || maxY > 16 || maxZ > 16
                    || minX >= maxX || minY >= maxY || minZ >= maxZ) {
                throw new IllegalArgumentException("Invalid cuboid bounds");
            }
        }

        int min(Direction.Axis axis) {
            return switch (axis) {
                case X -> minX;
                case Y -> minY;
                case Z -> minZ;
            };
        }

        int max(Direction.Axis axis) {
            return switch (axis) {
                case X -> maxX;
                case Y -> maxY;
                case Z -> maxZ;
            };
        }
    }

    public enum Decision {
        BYPASS_UNRELATED(true),
        CONNECT(true),
        MISSING_MATERIAL_PROFILE(false),
        UNSUPPORTED_GEOMETRY(false),
        INVALID_QUAD_SURFACE(false),
        NON_COPLANAR(false),
        NO_BOUNDARY_CONTACT(false);

        private final boolean allowsOriginal;

        Decision(boolean allowsOriginal) {
            this.allowsOriginal = allowsOriginal;
        }

        /** Whether a positive underlying Continuity result may remain positive. */
        public boolean allowsOriginal() {
            return allowsOriginal;
        }
    }

    public record Interval(long min16, long max16) {
        public Interval {
            if (min16 >= max16) throw new IllegalArgumentException("Empty surface interval");
        }
    }

    public record SurfaceDescriptor(Direction normal, long plane16,
            Direction.Axis uAxis, Interval uBounds,
            Direction.Axis vAxis, Interval vBounds, BlockPos blockPos) {
        public SurfaceDescriptor {
            Objects.requireNonNull(normal, "normal");
            Objects.requireNonNull(uAxis, "uAxis");
            Objects.requireNonNull(uBounds, "uBounds");
            Objects.requireNonNull(vAxis, "vAxis");
            Objects.requireNonNull(vBounds, "vBounds");
            blockPos = Objects.requireNonNull(blockPos, "blockPos").immutable();
            if (normal.getAxis() == uAxis || normal.getAxis() == vAxis || uAxis == vAxis) {
                throw new IllegalArgumentException("Surface axes are not orthogonal");
            }
        }
    }

    /** Exact block-local bounds captured from Continuity's current rendered quad. */
    public record QuadSurface(Direction normal, int plane16,
            Direction.Axis uAxis, int uMin16, int uMax16,
            Direction.Axis vAxis, int vMin16, int vMax16) {
        public QuadSurface {
            Objects.requireNonNull(normal, "normal");
            Objects.requireNonNull(uAxis, "uAxis");
            Objects.requireNonNull(vAxis, "vAxis");
            if (normal.getAxis() == uAxis || normal.getAxis() == vAxis || uAxis == vAxis
                    || plane16 < 0 || plane16 > BLOCK_UNITS
                    || uMin16 < 0 || uMax16 > BLOCK_UNITS || uMin16 >= uMax16
                    || vMin16 < 0 || vMax16 > BLOCK_UNITS || vMin16 >= vMax16) {
                throw new IllegalArgumentException("Invalid block-local quad surface");
            }
        }
    }
}
