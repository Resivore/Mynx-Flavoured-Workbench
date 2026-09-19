package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Thread-confined snapshot of the exact quad currently being processed by Continuity. */
public final class ContinuityQuadContext {
    private static final int VERTEX_COUNT = 4;
    private static final float SIXTEENTH_EPSILON = 0.001F;
    private static final ThreadLocal<Capture> CURRENT = new ThreadLocal<>();

    private ContinuityQuadContext() {}

    public static Scope push(MutableQuadView quad, BlockState receiverState, BlockPos receiverPos) {
        Capture previous = CURRENT.get();
        Capture next;
        try {
            next = new Capture(snapshot(quad), receiverState, receiverPos.immutable());
        } catch (IllegalArgumentException exception) {
            next = new Capture(null, receiverState, receiverPos.immutable());
        }
        CURRENT.set(next);
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    @Nullable
    public static Capture current() {
        return CURRENT.get();
    }

    private static QuadSurface snapshot(MutableQuadView quad) {
        Direction face = quad.lightFace();
        if (face == null) throw new IllegalArgumentException("Quad has no light face");
        Direction.Axis normal = face.getAxis();
        Direction.Axis[] inPlane = inPlaneAxes(normal);

        float normalMin = Float.POSITIVE_INFINITY;
        float normalMax = Float.NEGATIVE_INFINITY;
        float uMin = Float.POSITIVE_INFINITY;
        float uMax = Float.NEGATIVE_INFINITY;
        float vMin = Float.POSITIVE_INFINITY;
        float vMax = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < VERTEX_COUNT; vertex++) {
            float normalCoordinate = coordinate(quad, vertex, normal);
            float uCoordinate = coordinate(quad, vertex, inPlane[0]);
            float vCoordinate = coordinate(quad, vertex, inPlane[1]);
            normalMin = Math.min(normalMin, normalCoordinate);
            normalMax = Math.max(normalMax, normalCoordinate);
            uMin = Math.min(uMin, uCoordinate);
            uMax = Math.max(uMax, uCoordinate);
            vMin = Math.min(vMin, vCoordinate);
            vMax = Math.max(vMax, vCoordinate);
        }
        if (normalMax - normalMin > SIXTEENTH_EPSILON) {
            throw new IllegalArgumentException("Quad is not planar on its light-face axis");
        }
        return new QuadSurface(face, sixteenths(normalMin),
                inPlane[0], sixteenths(uMin), sixteenths(uMax),
                inPlane[1], sixteenths(vMin), sixteenths(vMax));
    }

    private static int sixteenths(float coordinate) {
        float scaled = coordinate * 16.0F;
        int rounded = Math.round(scaled);
        if (Math.abs(scaled - rounded) > SIXTEENTH_EPSILON) {
            throw new IllegalArgumentException("Quad coordinate is not an exact sixteenth");
        }
        return rounded;
    }

    private static float coordinate(MutableQuadView quad, int vertex, Direction.Axis axis) {
        return switch (axis) {
            case X -> quad.x(vertex);
            case Y -> quad.y(vertex);
            case Z -> quad.z(vertex);
        };
    }

    private static Direction.Axis[] inPlaneAxes(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }

    public static final class Capture {
        @Nullable private final QuadSurface surface;
        @Nullable private final BlockState receiverState;
        @Nullable private final BlockPos receiverPos;
        private final Map<BlockPos, List<QuadSurface>> overlayProbes = new LinkedHashMap<>();
        private final List<OverlaySpriteContribution> overlaySprites = new ArrayList<>();
        private final List<PendingOverlay> pendingOverlays = new ArrayList<>();
        @Nullable private Direction[] overlayDirections;
        @Nullable private int[] pendingSpriteIndices;
        private int emittedOverlaySprites;

        public Capture(@Nullable QuadSurface surface, @Nullable BlockState receiverState,
                @Nullable BlockPos receiverPos) {
            this.surface = surface;
            this.receiverState = receiverState;
            this.receiverPos = receiverPos == null ? null : receiverPos.immutable();
        }

        public Capture(@Nullable QuadSurface surface) {
            this(surface, null, null);
        }

        @Nullable
        public QuadSurface surface() {
            return surface;
        }

        @Nullable
        public BlockState receiverState() {
            return receiverState;
        }

        @Nullable
        public BlockPos receiverPos() {
            return receiverPos;
        }

        public boolean valid() {
            return surface != null;
        }

        /** Starts the exact Standard Overlay collector assembly for this processor invocation. */
        public void beginOverlayAssembly(Direction[] directions) {
            Objects.requireNonNull(directions, "directions");
            if (directions.length != 4 || Arrays.stream(directions).anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Standard Overlay requires four directions");
            }
            overlayDirections = directions.clone();
            overlayProbes.clear();
            overlaySprites.clear();
            pendingSpriteIndices = null;
            emittedOverlaySprites = 0;
        }

        /** Retains one canonically positive probe under its exact inducing block position. */
        public void addOverlayProbe(BlockPos inducingPos, List<QuadSurface> footprints) {
            Objects.requireNonNull(inducingPos, "inducingPos");
            Objects.requireNonNull(footprints, "footprints");
            if (footprints.isEmpty()) return;
            List<QuadSurface> existing = overlayProbes.computeIfAbsent(
                    inducingPos.immutable(), ignored -> new ArrayList<>());
            for (QuadSurface footprint : footprints) {
                QuadSurface exact = Objects.requireNonNull(footprint, "footprint");
                if (!existing.contains(exact)) existing.add(exact);
            }
        }

        /** Records the sprite indices passed to one of Continuity's collector helper methods. */
        public void beginPendingSprites(int... spriteIndices) {
            Objects.requireNonNull(spriteIndices, "spriteIndices");
            pendingSpriteIndices = spriteIndices.clone();
        }

        /** Attaches one non-null collector entry to the probes represented by its sprite index. */
        public void addPendingSprite(int argumentIndex, boolean present) {
            if (!present) return;
            if (pendingSpriteIndices == null || argumentIndex < 0
                    || argumentIndex >= pendingSpriteIndices.length) {
                throw new IllegalStateException("No Standard Overlay sprite argument at "
                        + argumentIndex);
            }
            addOverlaySprite(pendingSpriteIndices[argumentIndex]);
        }

        /** Clears helper-local sprite arguments after the corresponding helper returns. */
        public void endPendingSprites() {
            pendingSpriteIndices = null;
        }

        /** Attaches a constant-index collector entry used by Continuity's corner-only branch. */
        public void addOverlaySprite(int spriteIndex, boolean present) {
            if (present) addOverlaySprite(spriteIndex);
        }

        /** Returns the contribution aligned with the next sprite in Continuity's emission loop. */
        public Optional<OverlaySpriteContribution> nextOverlaySprite() {
            if (emittedOverlaySprites >= overlaySprites.size()) return Optional.empty();
            return Optional.of(overlaySprites.get(emittedOverlaySprites++));
        }

        /** Deterministic inspection seam for contribution-assembly tests. */
        public List<OverlaySpriteContribution> overlaySprites() {
            return List.copyOf(overlaySprites);
        }

        /** Delays managed overlay output until receiver and overlays share one tessellation. */
        public void addPendingOverlay(Direction face, TextureAtlasSprite sprite, int tint,
                ChunkSectionLayer layer, TriState ao, List<QuadSurface> surfaces, String reason) {
            pendingOverlays.add(new PendingOverlay(face, sprite, tint, layer, ao, surfaces, reason));
        }

        public List<PendingOverlay> pendingOverlays() {
            return List.copyOf(pendingOverlays);
        }

        private void addOverlaySprite(int spriteIndex) {
            overlaySprites.add(new OverlaySpriteContribution(spriteIndex,
                    footprintsForSprite(spriteIndex)));
        }

        private List<QuadSurface> footprintsForSprite(int spriteIndex) {
            if (receiverPos == null || overlayDirections == null) return List.of();
            List<BlockPos> inducingPositions = switch (spriteIndex) {
                // Corner sprites carry only the diagonal appliesOverlay probe that selected them.
                case 0 -> List.of(corner(1, 2));
                case 2 -> List.of(corner(0, 1));
                case 14 -> List.of(corner(2, 3));
                case 16 -> List.of(corner(0, 3));
                // Edge and combined sprites carry exactly their successful side probes.
                case 1 -> List.of(side(1));
                case 3 -> List.of(side(1), side(2));
                case 4 -> List.of(side(0), side(1));
                case 5 -> List.of(side(0), side(1), side(2));
                case 6 -> List.of(side(0), side(1), side(3));
                case 7 -> List.of(side(2));
                case 8 -> List.of(side(0), side(1), side(2), side(3));
                case 9 -> List.of(side(0));
                case 10 -> List.of(side(2), side(3));
                case 11 -> List.of(side(0), side(3));
                case 12 -> List.of(side(1), side(2), side(3));
                case 13 -> List.of(side(0), side(2), side(3));
                case 15 -> List.of(side(3));
                default -> throw new IllegalArgumentException(
                        "Unknown Standard Overlay sprite index: " + spriteIndex);
            };
            LinkedHashSet<QuadSurface> exact = new LinkedHashSet<>();
            for (BlockPos inducingPos : inducingPositions) {
                List<QuadSurface> probe = overlayProbes.get(inducingPos);
                if (probe != null) exact.addAll(probe);
            }
            return List.copyOf(exact);
        }

        private BlockPos side(int directionIndex) {
            return receiverPos.relative(overlayDirections[directionIndex]).immutable();
        }

        private BlockPos corner(int firstDirectionIndex, int secondDirectionIndex) {
            return receiverPos.relative(overlayDirections[firstDirectionIndex])
                    .relative(overlayDirections[secondDirectionIndex]).immutable();
        }
    }

    /** One logical Continuity sprite and only the BGE footprints of the probes that selected it. */
    public record OverlaySpriteContribution(int spriteIndex, List<QuadSurface> footprints) {
        public OverlaySpriteContribution {
            if (spriteIndex < 0 || spriteIndex > 16) {
                throw new IllegalArgumentException("Invalid Standard Overlay sprite index");
            }
            footprints = List.copyOf(Objects.requireNonNull(footprints, "footprints"));
        }
    }

    /** Native Continuity render attributes plus the authoritative regions for one logical sprite. */
    public record PendingOverlay(Direction face, TextureAtlasSprite sprite, int tint,
            ChunkSectionLayer layer, TriState ao, List<QuadSurface> surfaces, String reason) {
        public PendingOverlay {
            Objects.requireNonNull(face, "face");
            Objects.requireNonNull(sprite, "sprite");
            Objects.requireNonNull(layer, "layer");
            Objects.requireNonNull(ao, "ao");
            surfaces = List.copyOf(Objects.requireNonNull(surfaces, "surfaces"));
            if (surfaces.isEmpty() || surfaces.stream().anyMatch(surface -> surface.normal() != face)) {
                throw new IllegalArgumentException("Pending overlay must have matching surfaces");
            }
            Objects.requireNonNull(reason, "reason");
        }
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
