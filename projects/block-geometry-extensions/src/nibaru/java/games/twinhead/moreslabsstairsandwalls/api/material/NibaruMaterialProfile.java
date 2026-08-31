package games.twinhead.moreslabsstairsandwalls.api.material;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record NibaruMaterialProfile(
        String profileVersion,
        ModBlocks family,
        Block canonicalParent,
        Identifier canonicalParentId,
        Optional<Block> nativeSlab,
        Optional<Block> nativeStair,
        Optional<Block> nativeWall,
        Optional<Block> effectiveSlabSource,
        Optional<Block> effectiveStairSource,
        Optional<Identifier> nativeSlabId,
        Optional<Identifier> nativeStairId,
        Optional<Identifier> nativeWallId,
        Set<TagKey<Block>> derivedBlockTags,
        Set<BehaviorCapability> capabilities,
        VisualProfile visualProfile,
        VisualSupport visualSupport,
        TintProfile tintProfile,
        RenderLayer renderLayer,
        OrientationPolicy orientationPolicy,
        SurfaceSamplingPolicy surfaceSamplingPolicy,
        DoubleFormPolicy doubleFormPolicy,
        TextureRoles textureRoles,
        Optional<InsetVisualContract> insetVisualContract,
        Optional<WeatheringCopper.WeatherState> oxidationStage,
        boolean waxed,
        List<MaterialTransition> transitions) {

    public enum VisualSupport { GENERIC_SUPPORTED, CLASSIFIED_NOT_ADAPTED }
    public enum RenderLayer { SOLID, CUTOUT, CUTOUT_MIPPED, TRANSLUCENT }
    public enum OrientationPolicy { UNIFORM, AXIS_ALIGNED, HORIZONTAL_FACING }
    public enum SurfaceSamplingPolicy {
        BLOCK_ABSOLUTE,
        NATIVE_STAIR_SURFACE_BAND,
        /** Dirt Path's visible surface is lowered to 15/16; partial sides sample from V=1. */
        PATH_LOWERED_SURFACE
    }
    public enum DoubleFormPolicy { COMPOSE_SEMANTIC_SURFACES, CUSTOM_REQUIRED }

    public record TextureRoles(String side, String top, String bottom, String overlay, String particle) {}

    /** Pure material-owned dimensions used to compose inset visuals onto provider-external geometry. */
    public record InsetVisualContract(
            int fullSpanInset,
            int halfSpanInset,
            ShellTexture shellTexture,
            boolean includeInnerLayerOnFullCube) {
        public enum ShellTexture { MATERIAL_FACES, BOTTOM }

        public InsetVisualContract {
            if (fullSpanInset < 0 || halfSpanInset < 0)
                throw new IllegalArgumentException("Inset dimensions must be non-negative");
        }

        public int insetForSpan(int span) {
            return span >= 16 ? fullSpanInset : halfSpanInset;
        }
    }

    public NibaruMaterialProfile {
        nativeSlab = nativeSlab == null ? Optional.empty() : nativeSlab;
        nativeStair = nativeStair == null ? Optional.empty() : nativeStair;
        nativeWall = nativeWall == null ? Optional.empty() : nativeWall;
        effectiveSlabSource = effectiveSlabSource == null ? Optional.empty() : effectiveSlabSource;
        effectiveStairSource = effectiveStairSource == null ? Optional.empty() : effectiveStairSource;
        nativeSlabId = nativeSlabId == null ? Optional.empty() : nativeSlabId;
        nativeStairId = nativeStairId == null ? Optional.empty() : nativeStairId;
        nativeWallId = nativeWallId == null ? Optional.empty() : nativeWallId;
        insetVisualContract = insetVisualContract == null ? Optional.empty() : insetVisualContract;
        oxidationStage = oxidationStage == null ? Optional.empty() : oxidationStage;
        derivedBlockTags = Set.copyOf(derivedBlockTags);
        capabilities = Set.copyOf(capabilities);
        transitions = List.copyOf(transitions);
    }

    public Optional<MaterialTransition> transition(MaterialTransition.Type type) {
        return transitions.stream().filter(transition -> transition.type() == type).findFirst();
    }

    public Set<ExternalSemanticRequirement> externalSemanticRequirements() {
        java.util.EnumSet<ExternalSemanticRequirement> result =
                java.util.EnumSet.noneOf(ExternalSemanticRequirement.class);
        if (!derivedBlockTags.isEmpty()) result.add(ExternalSemanticRequirement.MATERIAL_BLOCK_TAGS);
        if (derivedBlockTags.contains(net.minecraft.tags.BlockTags.SOUL_SPEED_BLOCKS))
            result.add(ExternalSemanticRequirement.MOVEMENT_MATERIAL_POSITION);
        if (transition(MaterialTransition.Type.PODZOL_GROWTH).isPresent())
            result.add(ExternalSemanticRequirement.PODZOL_GROWTH_TRANSITION);
        if (family == ModBlocks.DRIPSTONE_BLOCK)
            result.add(ExternalSemanticRequirement.POINTED_DRIPSTONE_GROWTH);
        return Set.copyOf(result);
    }

    public boolean hasNativeGeometry(ModBlocks.BlockType type) {
        return switch (type) {
            case SLAB -> nativeSlab.isPresent();
            case STAIRS -> nativeStair.isPresent();
            case WALL -> nativeWall.isPresent();
        };
    }

    public BlockBehaviour.Properties transferableSettings(ModBlocks.BlockType sourceGeometry) {
        return family.getSettings(sourceGeometry);
    }

    public DerivedGeometrySupport supportFor(DerivedGeometrySupport.Geometry geometry,
            Set<BehaviorCapability> adaptedCapabilities) {
        return supportFor(geometry, adaptedCapabilities, Set.of(visualProfile));
    }

    public DerivedGeometrySupport supportFor(DerivedGeometrySupport.Geometry geometry,
            Set<BehaviorCapability> adaptedCapabilities, Set<VisualProfile> adaptedVisualProfiles) {
        boolean sourcePresent = switch (geometry) {
            case VERTICAL_SLAB -> effectiveSlabSource.isPresent();
            case STEP -> effectiveStairSource.isPresent();
            case LAYER -> true;
        };
        if (!sourcePresent) {
            return new DerivedGeometrySupport(DerivedGeometrySupport.Status.UNSUPPORTED_SOURCE, Set.of(), false);
        }
        Set<BehaviorCapability> missing = new java.util.LinkedHashSet<>(capabilities);
        missing.removeAll(adaptedCapabilities);
        if (!missing.isEmpty()) {
            return new DerivedGeometrySupport(adaptedVisualProfiles.contains(visualProfile)
                    ? DerivedGeometrySupport.Status.UNSUPPORTED_BEHAVIOR
                    : DerivedGeometrySupport.Status.UNSUPPORTED_BOTH, missing, false);
        }
        if (!adaptedVisualProfiles.contains(visualProfile))
            return new DerivedGeometrySupport(DerivedGeometrySupport.Status.UNSUPPORTED_VISUAL, Set.of(), false);
        return new DerivedGeometrySupport(capabilities.isEmpty()
                ? DerivedGeometrySupport.Status.SUPPORTED_GENERIC
                : DerivedGeometrySupport.Status.SUPPORTED_WITH_CAPABILITIES, Set.of(), true);
    }
}
