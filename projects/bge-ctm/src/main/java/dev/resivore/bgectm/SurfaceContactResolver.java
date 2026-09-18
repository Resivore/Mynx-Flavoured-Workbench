package dev.resivore.bgectm;

import dev.aero.cnmterraincompat.BgeMaterialBindings;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Bounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.Optional;

/** Geometry vetoes backed exclusively by C73 binding topology. */
public final class SurfaceContactResolver {
    private static final int BLOCK_UNITS = 16;
    private SurfaceContactResolver() {}

    /** Regular CTM: same face, exact world plane, and adjacent edge contact are required. */
    public static Decision inspect(BlockState source, BlockPos sourcePos, BlockState other,
            BlockPos otherPos, Direction face) {
        return inspectRegular(source, sourcePos, other, otherPos, face, null);
    }

    public static Decision inspectWithSourceSurface(BlockState source, BlockPos sourcePos,
            BlockState other, BlockPos otherPos, Direction face, QuadSurface quad) {
        return inspectRegular(source, sourcePos, other, otherPos, face,
                Objects.requireNonNull(quad, "quad"));
    }

    /**
     * Standard Overlay: a canonical positive may survive when the two real cuboids reach their
     * common neighbour boundary and overlap there. Unlike regular CTM, rendered top planes need
     * not be equal; full farmland (15/16) and grass (16/16) prove that requirement is invalid.
     */
    public static Decision inspectOverlay(BlockState receiver, BlockPos receiverPos,
            BlockState source, BlockPos sourcePos, Direction face, QuadSurface receiverQuad) {
        Endpoint receiverEndpoint = endpoint(receiver);
        Endpoint sourceEndpoint = endpoint(source);
        if (!receiverEndpoint.participates() && !sourceEndpoint.participates()) return Decision.BYPASS_UNRELATED;
        if (!receiverEndpoint.supported() || !sourceEndpoint.supported()) return Decision.UNSUPPORTED_GEOMETRY;
        if (receiverQuad != null && receiverQuad.normal() != face) return Decision.INVALID_QUAD_SURFACE;
        Cuboid receiverBody = receiverEndpoint.cuboid();
        Cuboid sourceBody = sourceEndpoint.cuboid();
        return reachCommonNeighbourBoundary(receiverBody, receiverPos, sourceBody, sourcePos)
                ? Decision.CONNECT : Decision.NO_BOUNDARY_CONTACT;
    }

    public static Decision inspectOverlay(BlockState receiver, BlockPos receiverPos,
            BlockState source, BlockPos sourcePos, Direction face) {
        return inspectOverlay(receiver, receiverPos, source, sourcePos, face, null);
    }

    public static boolean stateDerivedFallbackSafe(BlockState source, BlockState other) {
        return endpoint(source).fallbackSafe() && endpoint(other).fallbackSafe();
    }

    public static Optional<SurfaceDescriptor> describe(BlockState state, BlockPos pos, Direction face) {
        Endpoint endpoint = endpoint(state);
        return endpoint.supported() ? Optional.of(descriptor(endpoint.cuboid(), pos, face)) : Optional.empty();
    }

    public static Optional<QuadSurface> describeLocalForOverlay(BlockState state, Direction face) {
        Endpoint endpoint = endpoint(state);
        if (!endpoint.supported() || !endpoint.fallbackSafe()) return Optional.empty();
        Cuboid cuboid = endpoint.cuboid();
        Direction.Axis[] axes = inPlaneAxes(face.getAxis());
        int plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? cuboid.max(face.getAxis()) : cuboid.min(face.getAxis());
        return Optional.of(new QuadSurface(face, plane, axes[0], cuboid.min(axes[0]), cuboid.max(axes[0]),
                axes[1], cuboid.min(axes[1]), cuboid.max(axes[1])));
    }

    private static Decision inspectRegular(BlockState source, BlockPos sourcePos, BlockState other,
            BlockPos otherPos, Direction face, QuadSurface sourceQuad) {
        Endpoint sourceEndpoint = endpoint(source);
        Endpoint otherEndpoint = endpoint(other);
        if (!sourceEndpoint.participates() && !otherEndpoint.participates()) return Decision.BYPASS_UNRELATED;
        if (!sourceEndpoint.supported() || !otherEndpoint.supported()) return Decision.UNSUPPORTED_GEOMETRY;
        if (sourceQuad != null && sourceQuad.normal() != face) return Decision.INVALID_QUAD_SURFACE;
        SurfaceDescriptor sourceSurface = sourceQuad == null
                ? descriptor(sourceEndpoint.cuboid(), sourcePos, face) : descriptor(sourceQuad, sourcePos);
        SurfaceDescriptor otherSurface = descriptor(otherEndpoint.cuboid(), otherPos, face);
        if (sourceSurface.plane16() != otherSurface.plane16()) return Decision.NON_COPLANAR;
        return meetAlongEvaluatedBoundary(sourceSurface, otherSurface)
                ? Decision.CONNECT : Decision.NO_BOUNDARY_CONTACT;
    }

    /** Exact regular face-surface boundary test. */
    public static boolean meetAlongEvaluatedBoundary(SurfaceDescriptor source, SurfaceDescriptor other) {
        if (source.normal() != other.normal() || source.plane16() != other.plane16()
                || source.uAxis() != other.uAxis() || source.vAxis() != other.vAxis()) return false;
        int normal = coordinate(other.blockPos(), source.normal().getAxis()) - coordinate(source.blockPos(), source.normal().getAxis());
        int u = coordinate(other.blockPos(), source.uAxis()) - coordinate(source.blockPos(), source.uAxis());
        int v = coordinate(other.blockPos(), source.vAxis()) - coordinate(source.blockPos(), source.vAxis());
        if (normal != 0 || Math.abs(u) > 1 || Math.abs(v) > 1 || (u == 0 && v == 0)) return false;
        return meets(source.uBounds(), other.uBounds(), u) && meets(source.vBounds(), other.vBounds(), v);
    }

    private static boolean reachCommonNeighbourBoundary(Cuboid receiver, BlockPos receiverPos,
            Cuboid source, BlockPos sourcePos) {
        int dx = sourcePos.getX() - receiverPos.getX();
        int dy = sourcePos.getY() - receiverPos.getY();
        int dz = sourcePos.getZ() - receiverPos.getZ();
        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) return false;
        Direction.Axis boundaryAxis = dx != 0 ? Direction.Axis.X : dy != 0 ? Direction.Axis.Y : Direction.Axis.Z;
        int delta = dx != 0 ? dx : dy != 0 ? dy : dz;
        if (delta > 0 ? receiver.max(boundaryAxis) != 16 || source.min(boundaryAxis) != 0
                : receiver.min(boundaryAxis) != 0 || source.max(boundaryAxis) != 16) return false;
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis != boundaryAxis && !overlaps(receiver.min(axis), receiver.max(axis), source.min(axis), source.max(axis))) return false;
        }
        return true;
    }

    private static boolean overlaps(int a0, int a1, int b0, int b1) { return Math.min(a1, b1) > Math.max(a0, b0); }
    private static boolean meets(Interval a, Interval b, int delta) {
        return switch (Integer.signum(delta)) {
            case -1 -> a.min16() == b.max16();
            case 0 -> Math.min(a.max16(), b.max16()) > Math.max(a.min16(), b.min16());
            case 1 -> a.max16() == b.min16();
            default -> false;
        };
    }

    private static Endpoint endpoint(BlockState state) {
        Optional<Binding> binding = BgeMaterialBindings.fromBlock(state.getBlock());
        if (binding.isEmpty()) return Endpoint.UNMANAGED;
        Binding value = binding.get();
        if (value.topology() == BgeMaterialBindings.Topology.CANONICAL_ROOT) return Endpoint.FULL;
        if (!CanonicalAppearanceResolver.inspect(state).inherited()) return Endpoint.UNSUPPORTED;
        return value.topology().bounds(state).map(bounds -> new Endpoint(new Cuboid(bounds), true, true))
                .orElse(Endpoint.UNSUPPORTED);
    }

    private static SurfaceDescriptor descriptor(Cuboid cuboid, BlockPos pos, Direction face) {
        Direction.Axis normal = face.getAxis(); Direction.Axis[] planes = inPlaneAxes(normal);
        long plane = (long) coordinate(pos, normal) * BLOCK_UNITS
                + (face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? cuboid.max(normal) : cuboid.min(normal));
        return new SurfaceDescriptor(face, plane, planes[0], world(cuboid, pos, planes[0]),
                planes[1], world(cuboid, pos, planes[1]), pos.immutable());
    }
    private static SurfaceDescriptor descriptor(QuadSurface surface, BlockPos pos) {
        return new SurfaceDescriptor(surface.normal(), (long) coordinate(pos, surface.normal().getAxis()) * BLOCK_UNITS + surface.plane16(),
                surface.uAxis(), new Interval((long) coordinate(pos, surface.uAxis()) * BLOCK_UNITS + surface.uMin16(), (long) coordinate(pos, surface.uAxis()) * BLOCK_UNITS + surface.uMax16()),
                surface.vAxis(), new Interval((long) coordinate(pos, surface.vAxis()) * BLOCK_UNITS + surface.vMin16(), (long) coordinate(pos, surface.vAxis()) * BLOCK_UNITS + surface.vMax16()), pos.immutable());
    }
    private static Interval world(Cuboid cuboid, BlockPos pos, Direction.Axis axis) { long o = (long) coordinate(pos, axis) * BLOCK_UNITS; return new Interval(o + cuboid.min(axis), o + cuboid.max(axis)); }
    private static Direction.Axis[] inPlaneAxes(Direction.Axis normal) { return switch (normal) { case X -> new Direction.Axis[]{Direction.Axis.Y, Direction.Axis.Z}; case Y -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}; case Z -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y}; }; }
    private static int coordinate(BlockPos pos, Direction.Axis axis) { return switch (axis) { case X -> pos.getX(); case Y -> pos.getY(); case Z -> pos.getZ(); }; }

    private record Endpoint(Cuboid cuboid, boolean supported, boolean participates) {
        static final Endpoint FULL = new Endpoint(Cuboid.FULL, true, false);
        static final Endpoint UNMANAGED = new Endpoint(null, true, false);
        static final Endpoint UNSUPPORTED = new Endpoint(null, false, true);
        boolean fallbackSafe() { return !participates || supported; }
    }
    private record Cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        static final Cuboid FULL = new Cuboid(0, 0, 0, 16, 16, 16);
        Cuboid(Bounds b) { this(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()); }
        int min(Direction.Axis axis) { return switch (axis) { case X -> minX; case Y -> minY; case Z -> minZ; }; }
        int max(Direction.Axis axis) { return switch (axis) { case X -> maxX; case Y -> maxY; case Z -> maxZ; }; }
    }
    public enum Decision { BYPASS_UNRELATED(true), CONNECT(true), UNSUPPORTED_GEOMETRY(false), INVALID_QUAD_SURFACE(false), NON_COPLANAR(false), NO_BOUNDARY_CONTACT(false); private final boolean allow; Decision(boolean allow) { this.allow = allow; } public boolean allowsOriginal() { return allow; } }
    public record Interval(long min16, long max16) { public Interval { if (min16 >= max16) throw new IllegalArgumentException("Empty interval"); } }
    public record SurfaceDescriptor(Direction normal, long plane16, Direction.Axis uAxis, Interval uBounds, Direction.Axis vAxis, Interval vBounds, BlockPos blockPos) { public SurfaceDescriptor { blockPos = blockPos.immutable(); } }
    public record QuadSurface(Direction normal, int plane16, Direction.Axis uAxis, int uMin16, int uMax16, Direction.Axis vAxis, int vMin16, int vMax16) { public QuadSurface { if (normal == null || uAxis == null || vAxis == null || normal.getAxis() == uAxis || normal.getAxis() == vAxis || uAxis == vAxis || plane16 < 0 || plane16 > 16 || uMin16 < 0 || uMax16 > 16 || uMin16 >= uMax16 || vMin16 < 0 || vMax16 > 16 || vMin16 >= vMax16) throw new IllegalArgumentException("Invalid block-local quad surface"); } }
}
