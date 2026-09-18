package dev.resivore.bgectm;

import dev.aero.cnmterraincompat.BgeGeometryRole;
import dev.aero.cnmterraincompat.BgeLayerBlock;
import dev.aero.cnmterraincompat.GlazedPatternState;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Projects supported simple geometry onto the canonical material state already owned by
 * BGE's typed material/profile authority.
 *
 * <p>Appearance answers rule selection only. Canary 4 deliberately exposes partial Layers,
 * single Vertical Slabs, and exact profile-owned ordinary slabs here, then applies the
 * independent per-face geometry decision in {@link SurfaceContactResolver}. Geometry state
 * is never copied into the canonical material state.</p>
 */
public final class CanonicalAppearanceResolver {
    private static final Set<VisualProfile> ELIGIBLE_VISUALS = Collections.unmodifiableSet(EnumSet.of(
            VisualProfile.UNIFORM,
            VisualProfile.TOP_SIDE_BOTTOM,
            VisualProfile.PILLAR,
            VisualProfile.GRASS_OVERLAY,
            VisualProfile.LEAVES_CUTOUT_TINTED,
            VisualProfile.GLASS_EDGE,
            VisualProfile.TRANSLUCENT_UNIFORM,
            VisualProfile.GLAZED_ORIENTED));

    private CanonicalAppearanceResolver() {}

    /** Returns the exact policy decision without consulting registry identifiers. */
    public static Resolution inspect(BlockState sourceState) {
        return inspect(sourceState, null, null);
    }

    private static Resolution inspect(BlockState sourceState,
            @Nullable BlockAndLightGetter view, @Nullable BlockPos pos) {
        Objects.requireNonNull(sourceState, "sourceState");
        Optional<MaterialBinding> binding = materialBinding(sourceState);
        if (binding.isEmpty()) {
            return new Resolution(Policy.NON_PROFILE_GEOMETRY, sourceState, Optional.empty());
        }

        Policy geometryPolicy = geometryPolicy(binding.get(), sourceState);
        if (!geometryPolicy.eligible()) {
            return new Resolution(geometryPolicy, sourceState, binding);
        }

        Optional<BlockState> canonical = projectCanonicalState(
                binding.get().profile(), sourceState, view, pos);
        if (canonical.isEmpty()) {
            return new Resolution(Policy.UNMAPPABLE_CANONICAL_STATE, sourceState, binding);
        }
        if (!ELIGIBLE_VISUALS.contains(binding.get().profile().visualProfile())) {
            return new Resolution(Policy.UNSUPPORTED_VISUAL_PROFILE, sourceState, binding);
        }
        return new Resolution(geometryPolicy, canonical.get(), binding);
    }

    /** Fabric appearance entry point shared by the supported carrier bases. */
    public static BlockState resolve(BlockState sourceState, BlockAndLightGetter view, BlockPos pos,
            Direction side, @Nullable BlockState querySourceState, @Nullable BlockPos sourcePos) {
        // Fabric's initial appearance query cannot identify every later neighbor direction.
        // Contact is therefore intentionally enforced at Continuity's connection predicate.
        Resolution resolution = inspect(sourceState, view, pos);
        BgeCtmDiagnostics.appearance(sourceState, resolution);
        return resolution.appearance();
    }

    /**
     * Returns BGE's typed relationship for a derived geometry or an exact effective slab source.
     * A broad SlabBlock mixin is therefore inert for every slab absent from this authority.
     */
    public static Optional<MaterialBinding> materialBinding(BlockState state) {
        Optional<NibaruProviderAdapter.RuntimeBinding> derived =
                NibaruProviderAdapter.runtimeBinding(state.getBlock());
        if (derived.isPresent()) {
            NibaruProviderAdapter.RuntimeBinding runtime = derived.get();
            return Optional.of(new MaterialBinding(runtime.profile(), carrier(runtime.role())));
        }
        if (!(state.getBlock() instanceof SlabBlock)) {
            return Optional.empty();
        }
        return NibaruMaterialProfiles.fromBlock(state.getBlock())
                .filter(profile -> profile.effectiveSlabSource().orElse(null) == state.getBlock())
                .map(profile -> new MaterialBinding(profile, GeometryCarrier.ORDINARY_SLAB));
    }

    public static Set<VisualProfile> eligibleVisuals() {
        return ELIGIBLE_VISUALS;
    }

    private static GeometryCarrier carrier(BgeGeometryRole role) {
        return switch (role) {
            case LAYER -> GeometryCarrier.LAYER;
            case VERTICAL_SLAB -> GeometryCarrier.VERTICAL_SLAB;
            case STEP -> GeometryCarrier.STEP;
            case CORNER -> GeometryCarrier.CORNER;
            case QUARTER_COLUMN -> GeometryCarrier.QUARTER_COLUMN;
            default -> GeometryCarrier.UNKNOWN;
        };
    }

    private static Policy geometryPolicy(MaterialBinding binding, BlockState state) {
        return switch (binding.carrier()) {
            case ORDINARY_SLAB -> state.hasProperty(BlockStateProperties.SLAB_TYPE)
                    ? Policy.ELIGIBLE_ORDINARY_SLAB
                    : Policy.UNKNOWN_GEOMETRY;
            case LAYER -> state.hasProperty(BgeLayerBlock.FACING)
                    && state.hasProperty(BgeLayerBlock.LAYERS)
                    ? Policy.ELIGIBLE_LAYER
                    : Policy.UNKNOWN_GEOMETRY;
            case VERTICAL_SLAB -> state.hasProperty(VerticalSlabBlock.FACING)
                    && state.hasProperty(VerticalSlabBlock.DOUBLE)
                    ? Policy.ELIGIBLE_VERTICAL_SLAB
                    : Policy.UNKNOWN_GEOMETRY;
            case STEP -> Policy.STEP_GEOMETRY;
            case CORNER -> Policy.CORNER_GEOMETRY;
            case QUARTER_COLUMN -> Policy.QUARTER_COLUMN_GEOMETRY;
            case UNKNOWN -> Policy.UNKNOWN_GEOMETRY;
        };
    }

    private static Optional<BlockState> projectCanonicalState(NibaruMaterialProfile profile,
            BlockState sourceState, @Nullable BlockAndLightGetter view, @Nullable BlockPos pos) {
        BlockState canonical = profile.canonicalParent().defaultBlockState();
        boolean leaf = profile.capabilities().contains(BehaviorCapability.LEAF_LIFECYCLE);
        boolean glazed = profile.capabilities().contains(BehaviorCapability.GLAZED_ORIENTATION);

        for (Property<?> property : canonical.getProperties()) {
            if (property == BlockStateProperties.AXIS) {
                if (!sourceState.hasProperty(BlockStateProperties.AXIS)) return Optional.empty();
                canonical = canonical.setValue(BlockStateProperties.AXIS,
                        sourceState.getValue(BlockStateProperties.AXIS));
            } else if (property == HorizontalDirectionalBlock.FACING) {
                if (!glazed || !sourceState.hasProperty(GlazedPatternState.PATTERN_FACING)) {
                    return Optional.empty();
                }
                canonical = canonical.setValue(HorizontalDirectionalBlock.FACING,
                        sourceState.getValue(GlazedPatternState.PATTERN_FACING));
            } else if (property == BlockStateProperties.DISTANCE) {
                if (!leaf || !sourceState.hasProperty(BlockStateProperties.DISTANCE)) {
                    return Optional.empty();
                }
                canonical = canonical.setValue(BlockStateProperties.DISTANCE,
                        sourceState.getValue(BlockStateProperties.DISTANCE));
            } else if (property == BlockStateProperties.PERSISTENT) {
                if (!leaf || !sourceState.hasProperty(BlockStateProperties.PERSISTENT)) {
                    return Optional.empty();
                }
                canonical = canonical.setValue(BlockStateProperties.PERSISTENT,
                        sourceState.getValue(BlockStateProperties.PERSISTENT));
            } else if (property == BlockStateProperties.SNOWY) {
                // Snowy dirt is a property of the canonical parent, not a visual-profile
                // classification. Podzol and mycelium use TOP_SIDE_BOTTOM today but have the
                // same positional snow-above semantics as grass. Context-free inspection only
                // carries an equivalent physical SNOWY value; otherwise preserve the canonical
                // default instead of rejecting a valid canonical material.
                boolean snowy = view != null && pos != null
                        ? view.getBlockState(pos.above()).is(BlockTags.SNOW)
                        : sourceState.hasProperty(BlockStateProperties.SNOWY)
                                && sourceState.getValue(BlockStateProperties.SNOWY);
                canonical = canonical.setValue(BlockStateProperties.SNOWY, snowy);
            } else if (property == BlockStateProperties.WATERLOGGED && leaf) {
                // Waterlogging belongs to the geometry volume; the canonical default stays dry.
            } else {
                // A canonical property without an explicit semantic projector is never guessed.
                return Optional.empty();
            }
        }
        return Optional.of(canonical);
    }

    public enum GeometryCarrier {
        ORDINARY_SLAB,
        LAYER,
        VERTICAL_SLAB,
        STEP,
        CORNER,
        QUARTER_COLUMN,
        UNKNOWN
    }

    /** Typed geometry/material relationship consumed by appearance and contact policy. */
    public record MaterialBinding(NibaruMaterialProfile profile, GeometryCarrier carrier) {
        public MaterialBinding {
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(carrier, "carrier");
        }
    }

    /** Typed state/geometry result used by focused tests and the contact layer. */
    public enum Policy {
        NON_PROFILE_GEOMETRY(false),
        ELIGIBLE_ORDINARY_SLAB(true),
        ELIGIBLE_LAYER(true),
        ELIGIBLE_VERTICAL_SLAB(true),
        STEP_GEOMETRY(false),
        CORNER_GEOMETRY(false),
        QUARTER_COLUMN_GEOMETRY(false),
        UNKNOWN_GEOMETRY(false),
        UNSUPPORTED_VISUAL_PROFILE(false),
        UNMAPPABLE_CANONICAL_STATE(false);

        private final boolean eligible;

        Policy(boolean eligible) {
            this.eligible = eligible;
        }

        public boolean eligible() {
            return eligible;
        }
    }

    public record Resolution(Policy policy, BlockState appearance,
            Optional<MaterialBinding> binding) {
        public Resolution {
            Objects.requireNonNull(policy, "policy");
            Objects.requireNonNull(appearance, "appearance");
            binding = Objects.requireNonNull(binding, "binding");
        }

        public boolean inherited() {
            return policy.eligible();
        }
    }
}
