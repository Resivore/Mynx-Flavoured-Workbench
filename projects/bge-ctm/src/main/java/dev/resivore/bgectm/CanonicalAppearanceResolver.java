package dev.resivore.bgectm;

import dev.aero.cnmterraincompat.BgeMaterialBindings;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Topology;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** BGE C73 owns canonical material identity, state projection, and topology. */
public final class CanonicalAppearanceResolver {
    private static final Set<VisualProfile> ELIGIBLE_VISUALS = Set.copyOf(EnumSet.of(
            VisualProfile.UNIFORM, VisualProfile.TOP_SIDE_BOTTOM, VisualProfile.PILLAR,
            VisualProfile.GRASS_OVERLAY, VisualProfile.LEAVES_CUTOUT_TINTED,
            VisualProfile.GLASS_EDGE, VisualProfile.TRANSLUCENT_UNIFORM,
            VisualProfile.GLAZED_ORIENTED));

    private CanonicalAppearanceResolver() {}

    public static Resolution inspect(BlockState sourceState) { return inspect(sourceState, null, null); }

    private static Resolution inspect(BlockState sourceState, @Nullable BlockAndLightGetter view,
            @Nullable BlockPos pos) {
        Objects.requireNonNull(sourceState, "sourceState");
        Optional<Binding> binding = BgeMaterialBindings.fromBlock(sourceState.getBlock());
        if (binding.isEmpty()) return new Resolution(Policy.NON_BGE_GEOMETRY, sourceState, Optional.empty());
        Binding value = binding.get();
        Policy policy = policy(value);
        if (!policy.eligible()) return new Resolution(policy, sourceState, binding);
        Optional<BlockState> canonical = value.canonicalState(sourceState);
        if (canonical.isEmpty()) return new Resolution(Policy.UNMAPPABLE_CANONICAL_STATE, sourceState, binding);
        return new Resolution(policy, applyWorldSnow(canonical.get(), view, pos), binding);
    }

    public static BlockState resolve(BlockState sourceState, BlockAndLightGetter view, BlockPos pos,
            Direction side, @Nullable BlockState querySourceState, @Nullable BlockPos sourcePos) {
        Resolution resolution = inspect(sourceState, view, pos);
        if (BgeCtmDiagnostics.enabled()) BgeCtmDiagnostics.appearance(sourceState, resolution);
        return resolution.appearance();
    }

    public static Optional<Binding> materialBinding(BlockState state) {
        return BgeMaterialBindings.fromBlock(state.getBlock());
    }

    public static Set<VisualProfile> eligibleVisuals() { return ELIGIBLE_VISUALS; }

    private static Policy policy(Binding binding) {
        Topology topology = binding.topology();
        if (topology == Topology.CANONICAL_ROOT) return Policy.CANONICAL_ROOT;
        if (topology != Topology.HORIZONTAL_SLAB && topology != Topology.LAYER
                && topology != Topology.VERTICAL_SLAB && topology != Topology.FARMLAND_SLAB) {
            return Policy.UNSUPPORTED_TOPOLOGY;
        }
        // C73's special canonical-bound Farmland Slab deliberately has no Nibaru visual profile.
        if (binding.materialProfile().isPresent()
                && !ELIGIBLE_VISUALS.contains(binding.materialProfile().get().visualProfile())) {
            return Policy.UNSUPPORTED_VISUAL_PROFILE;
        }
        return switch (topology) {
            case HORIZONTAL_SLAB -> Policy.ELIGIBLE_HORIZONTAL_SLAB;
            case LAYER -> Policy.ELIGIBLE_LAYER;
            case VERTICAL_SLAB -> Policy.ELIGIBLE_VERTICAL_SLAB;
            case FARMLAND_SLAB -> Policy.ELIGIBLE_SPECIAL_HORIZONTAL;
            default -> throw new IllegalStateException("Unhandled topology " + topology);
        };
    }

    private static BlockState applyWorldSnow(BlockState state, @Nullable BlockAndLightGetter view,
            @Nullable BlockPos pos) {
        // C73 owns material projection; these two adjustments remain geometry/context policy.
        // A carrier's water volume is never a canonical material-waterlogging assertion.
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        if (!state.hasProperty(BlockStateProperties.SNOWY) || view == null || pos == null) return state;
        return state.setValue(BlockStateProperties.SNOWY, view.getBlockState(pos.above()).is(BlockTags.SNOW));
    }

    public enum Policy {
        NON_BGE_GEOMETRY(false), CANONICAL_ROOT(false),
        ELIGIBLE_HORIZONTAL_SLAB(true), ELIGIBLE_LAYER(true), ELIGIBLE_VERTICAL_SLAB(true),
        ELIGIBLE_SPECIAL_HORIZONTAL(true), UNSUPPORTED_TOPOLOGY(false),
        UNSUPPORTED_VISUAL_PROFILE(false), UNMAPPABLE_CANONICAL_STATE(false);
        private final boolean eligible;
        Policy(boolean eligible) { this.eligible = eligible; }
        public boolean eligible() { return eligible; }
    }

    public record Resolution(Policy policy, BlockState appearance, Optional<Binding> binding) {
        public Resolution {
            Objects.requireNonNull(policy, "policy");
            Objects.requireNonNull(appearance, "appearance");
            binding = Objects.requireNonNull(binding, "binding");
        }
        public boolean inherited() { return policy.eligible(); }
    }
}
