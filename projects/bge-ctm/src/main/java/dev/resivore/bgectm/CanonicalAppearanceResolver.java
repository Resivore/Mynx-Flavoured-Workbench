package dev.resivore.bgectm;

import dev.aero.cnmterraincompat.BgeGeometryRole;
import dev.aero.cnmterraincompat.BgeLayerBlock;
import dev.aero.cnmterraincompat.GlazedPatternState;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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
 * Projects a deliberately small set of BGE states onto the canonical material state
 * already owned by BGE's typed runtime binding.
 *
 * <p>Canary 1 is a whole-state policy. The full appearance-call context is retained
 * in the API so a later evidence-backed contact policy can use it without replacing
 * the canonical material/state projector.</p>
 */
public final class CanonicalAppearanceResolver {
    private static final Set<VisualProfile> ELIGIBLE_VISUALS = Collections.unmodifiableSet(EnumSet.of(
            VisualProfile.UNIFORM,
            VisualProfile.TOP_SIDE_BOTTOM,
            VisualProfile.PILLAR,
            VisualProfile.LEAVES_CUTOUT_TINTED,
            VisualProfile.GLASS_EDGE,
            VisualProfile.TRANSLUCENT_UNIFORM,
            VisualProfile.GLAZED_ORIENTED));

    private CanonicalAppearanceResolver() {}

    /** Returns the exact policy decision without consulting registry identifiers. */
    public static Resolution inspect(BlockState derivedState) {
        Objects.requireNonNull(derivedState, "derivedState");
        Optional<NibaruProviderAdapter.RuntimeBinding> binding =
                NibaruProviderAdapter.runtimeBinding(derivedState.getBlock());
        if (binding.isEmpty()) {
            return new Resolution(Policy.NON_BGE, derivedState, Optional.empty());
        }

        Policy geometryPolicy = geometryPolicy(binding.get().role(), derivedState);
        if (!geometryPolicy.eligible()) {
            return new Resolution(geometryPolicy, derivedState, binding);
        }

        Optional<BlockState> canonical = projectCanonicalState(binding.get(), derivedState);
        if (canonical.isEmpty()) {
            return new Resolution(Policy.UNMAPPABLE_CANONICAL_STATE, derivedState, binding);
        }
        if (!ELIGIBLE_VISUALS.contains(binding.get().profile().visualProfile())) {
            return new Resolution(Policy.UNSUPPORTED_VISUAL_PROFILE, derivedState, binding);
        }
        return new Resolution(geometryPolicy, canonical.get(), binding);
    }

    /** Fabric appearance entry point used by the three targeted geometry bases. */
    public static BlockState resolve(BlockState derivedState, BlockAndLightGetter view, BlockPos pos,
            Direction side, @Nullable BlockState sourceState, @Nullable BlockPos sourcePos) {
        // Canary 1 intentionally makes no contact-specific inference. Keeping every
        // parameter here makes that boundary explicit and leaves a stable future seam.
        return inspect(derivedState).appearance();
    }

    public static Set<VisualProfile> eligibleVisuals() {
        return ELIGIBLE_VISUALS;
    }

    private static Policy geometryPolicy(BgeGeometryRole role, BlockState state) {
        return switch (role) {
            case LAYER -> state.hasProperty(BgeLayerBlock.LAYERS)
                    && state.getValue(BgeLayerBlock.LAYERS) == 4
                    ? Policy.ELIGIBLE_FULL_LAYER
                    : Policy.PARTIAL_LAYER;
            case VERTICAL_SLAB -> state.hasProperty(VerticalSlabBlock.DOUBLE)
                    && state.getValue(VerticalSlabBlock.DOUBLE)
                    ? Policy.ELIGIBLE_DOUBLE_VERTICAL_SLAB
                    : Policy.SINGLE_VERTICAL_SLAB;
            case STEP -> Policy.STEP_GEOMETRY;
            case CORNER -> Policy.CORNER_GEOMETRY;
            case QUARTER_COLUMN -> Policy.QUARTER_COLUMN_GEOMETRY;
            default -> Policy.UNKNOWN_GEOMETRY;
        };
    }

    private static Optional<BlockState> projectCanonicalState(
            NibaruProviderAdapter.RuntimeBinding binding, BlockState derivedState) {
        NibaruMaterialProfile profile = binding.profile();
        BlockState canonical = profile.canonicalParent().defaultBlockState();
        boolean leaf = profile.capabilities().contains(BehaviorCapability.LEAF_LIFECYCLE);
        boolean glazed = profile.capabilities().contains(BehaviorCapability.GLAZED_ORIENTATION);

        for (Property<?> property : canonical.getProperties()) {
            if (property == BlockStateProperties.AXIS) {
                if (!derivedState.hasProperty(BlockStateProperties.AXIS)) return Optional.empty();
                canonical = canonical.setValue(BlockStateProperties.AXIS,
                        derivedState.getValue(BlockStateProperties.AXIS));
            } else if (property == HorizontalDirectionalBlock.FACING) {
                if (!glazed || !derivedState.hasProperty(GlazedPatternState.PATTERN_FACING)) {
                    return Optional.empty();
                }
                canonical = canonical.setValue(HorizontalDirectionalBlock.FACING,
                        derivedState.getValue(GlazedPatternState.PATTERN_FACING));
            } else if (property == BlockStateProperties.DISTANCE) {
                if (!leaf || !derivedState.hasProperty(BlockStateProperties.DISTANCE)) {
                    return Optional.empty();
                }
                canonical = canonical.setValue(BlockStateProperties.DISTANCE,
                        derivedState.getValue(BlockStateProperties.DISTANCE));
            } else if (property == BlockStateProperties.PERSISTENT) {
                if (!leaf || !derivedState.hasProperty(BlockStateProperties.PERSISTENT)) {
                    return Optional.empty();
                }
                canonical = canonical.setValue(BlockStateProperties.PERSISTENT,
                        derivedState.getValue(BlockStateProperties.PERSISTENT));
            } else if (property == BlockStateProperties.WATERLOGGED && leaf) {
                // Waterlogging describes the derived geometry volume. It is deliberately
                // not copied into the canonical appearance; the canonical default stays dry.
            } else {
                // A canonical property without an explicit semantic projector is not guessed.
                return Optional.empty();
            }
        }
        return Optional.of(canonical);
    }

    /** Typed state/geometry result used by focused tests and future policy revisions. */
    public enum Policy {
        NON_BGE(false),
        ELIGIBLE_FULL_LAYER(true),
        ELIGIBLE_DOUBLE_VERTICAL_SLAB(true),
        PARTIAL_LAYER(false),
        SINGLE_VERTICAL_SLAB(false),
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
            Optional<NibaruProviderAdapter.RuntimeBinding> binding) {
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
