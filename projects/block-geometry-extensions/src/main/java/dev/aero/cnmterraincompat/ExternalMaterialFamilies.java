package dev.aero.cnmterraincompat;

import dev.aero.cnmterraincompat.ExternalMaterialCatalog.Spec;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registry authority and exact source-to-nine-role inventory for optional provider families. */
public final class ExternalMaterialFamilies {
    private static final Map<Identifier, Pending> PENDING = new LinkedHashMap<>();
    private static final Map<Identifier, Binding> BY_SOURCE = new LinkedHashMap<>();
    private static final Map<Block, StandardFuelTrait> STANDARD_FUEL = new IdentityHashMap<>();

    private ExternalMaterialFamilies() {}

    public static synchronized void register(Spec spec) {
        Binding existing = BY_SOURCE.get(spec.id());
        if (existing != null) return;
        if (PENDING.containsKey(spec.id())) throw new IllegalStateException(
                "External family registered twice before CNM completion: " + spec.id());
        Block source = BuiltInRegistries.BLOCK.getValue(spec.id());
        if (source == null || !spec.id().equals(BuiltInRegistries.BLOCK.getKey(source))) {
            throw new IllegalStateException("Provider completed without required source block " + spec.id());
        }
        Block reference = BuiltInRegistries.BLOCK.getValue(spec.providerReference());
        if (reference == null || !spec.providerReference().equals(BuiltInRegistries.BLOCK.getKey(reference))) {
            throw new IllegalStateException("Provider completed without required Path reference "
                    + spec.providerReference());
        }

        // Five Macaw soil-path entries correctly root at existing native full-block profiles.
        // They retain their optional-provider membership, but cannot own duplicate BGE geometry.
        Optional<NibaruMaterialProfile> nativeProfile = NibaruMaterialProfiles.fromBlock(source);
        if (nativeProfile.isPresent()) {
            PENDING.put(spec.id(), Pending.nativeBinding(spec, nativeProfile.orElseThrow(), source));
            return;
        }

        Identifier slabId = id(spec, "slab");
        Identifier stairsId = id(spec, "stairs");
        Identifier wallId = id(spec, "wall");
        ExternalMaterialBlocks.StandardSet standard = ExternalMaterialBlocks.create(source,
                properties(slabId, source), properties(stairsId, source), wallProperties(wallId, source),
                spec.capabilities().contains(games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability.LEAF_LIFECYCLE));
        CnmTerrainCompat.register(slabId, standard.slab());
        CnmTerrainCompat.register(stairsId, standard.stairs());
        CnmTerrainCompat.register(wallId, standard.wall());

        NibaruMaterialProfile profile = new NibaruMaterialProfile(
                ExternalMaterialCatalog.PROFILE_VERSION, null, source, spec.id(),
                Optional.of(standard.slab()), Optional.of(standard.stairs()), Optional.of(standard.wall()),
                Optional.of(standard.slab()), Optional.of(standard.stairs()),
                Optional.of(slabId), Optional.of(stairsId), Optional.of(wallId),
                spec.blockTags(), spec.capabilities(), spec.visual(),
                NibaruMaterialProfile.VisualSupport.GENERIC_SUPPORTED, spec.tint(), spec.renderLayer(),
                spec.orientation(), NibaruMaterialProfile.SurfaceSamplingPolicy.BLOCK_ABSOLUTE,
                NibaruMaterialProfile.DoubleFormPolicy.COMPOSE_SEMANTIC_SURFACES,
                new NibaruMaterialProfile.TextureRoles(spec.side(), spec.top(), spec.bottom(), "", spec.side()),
                Optional.empty(), Optional.empty(), false, spec.transitions());
        NibaruMaterialProfiles.registerExternal(profile);

        Pending pending = new Pending(spec, profile, source, standard.slab(), standard.stairs(), standard.wall());
        PENDING.put(spec.id(), pending);
        registerStandardSemantics(pending);
    }

    /** Resolves CNM-created Vertical/Step and BGE-tail forms after the deferred registry scan. */
    public static synchronized void finalizeGeneratedBindings() {
        for (Pending pending : PENDING.values()) {
            if (BY_SOURCE.containsKey(pending.spec().id())) continue;
            NibaruMaterialProfile profile = pending.profile();
            Binding binding = new Binding(pending.spec(), profile, pending.source(),
                    pending.slab(), pending.stairs(), pending.wall(),
                    NibaruProviderAdapter.derived(profile, BgeGeometryRole.VERTICAL_SLAB).orElseThrow(),
                    NibaruProviderAdapter.derived(profile, BgeGeometryRole.STEP).orElseThrow(),
                    NibaruProviderAdapter.derived(profile, BgeGeometryRole.CORNER).orElseThrow(),
                    NibaruProviderAdapter.derived(profile, BgeGeometryRole.QUARTER_COLUMN).orElseThrow(),
                    NibaruProviderAdapter.derived(profile, BgeGeometryRole.LAYER).orElseThrow());
            BY_SOURCE.put(pending.spec().id(), binding);
            copyFireToAll(binding);
        }
    }

    public static synchronized List<Binding> all() { return List.copyOf(BY_SOURCE.values()); }
    public static synchronized Optional<Binding> fromSource(Identifier source) {
        return Optional.ofNullable(BY_SOURCE.get(source));
    }
    public static synchronized List<StandardFuelTrait> standardFuelTraits() {
        return List.copyOf(STANDARD_FUEL.values());
    }

    private static void registerStandardSemantics(Pending binding) {
        for (Block block : List.of(binding.slab(), binding.stairs(), binding.wall())) {
            NibaruProviderAdapter.registerTintTarget(binding.profile(), block);
        }
        STANDARD_FUEL.put(binding.slab(), new StandardFuelTrait(binding.slab(), binding.source(), 2));
        STANDARD_FUEL.put(binding.stairs(), new StandardFuelTrait(binding.stairs(), binding.source(), 1));
        STANDARD_FUEL.put(binding.wall(), new StandardFuelTrait(binding.wall(), binding.source(), 1));

        FlammableBlockRegistry.Entry fire = FlammableBlockRegistry.getDefaultInstance().get(binding.source());
        if (fire != null) for (Block block : List.of(binding.slab(), binding.stairs(), binding.wall()))
            FlammableBlockRegistry.getDefaultInstance().add(block, fire.getIgniteOdds(), fire.getBurnOdds());

        binding.profile().transition(MaterialTransition.Type.STRIPPED)
                .flatMap(transition -> NibaruMaterialProfiles.fromFamily(transition.target()))
                .ifPresent(target -> {
                    StrippableBlockRegistry.register(binding.slab(), target.nativeSlab().orElseThrow());
                    StrippableBlockRegistry.register(binding.stairs(), target.nativeStair().orElseThrow());
                    StrippableBlockRegistry.register(binding.wall(), target.nativeWall().orElseThrow());
                });
    }

    private static void copyFireToAll(Binding binding) {
        FlammableBlockRegistry.Entry fire = FlammableBlockRegistry.getDefaultInstance().get(binding.source());
        if (fire == null) return;
        for (Block block : binding.generated())
            FlammableBlockRegistry.getDefaultInstance().add(block, fire.getIgniteOdds(), fire.getBurnOdds());
    }

    public static Identifier id(Spec spec, String suffix) {
        return Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID,
                spec.generatedIdentity().getNamespace() + "/" + spec.generatedIdentity().getPath() + "_" + suffix);
    }

    private static BlockBehaviour.Properties properties(Identifier id, Block source) {
        return BlockBehaviour.Properties.ofFullCopy(source).setId(ResourceKey.create(Registries.BLOCK, id));
    }

    /**
     * A vanilla wall has no AXIS property.  In particular, copying a rotated-pillar
     * source would retain its log-state predicate and makes WallBlock construction
     * query a property it cannot have.  Copy the observable material semantics
     * without carrying source-only state predicates into the normal wall route.
     */
    private static BlockBehaviour.Properties wallProperties(Identifier id, Block source) {
        BlockState state = source.defaultBlockState();
        BlockBehaviour.Properties result = BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .mapColor(source.defaultMapColor())
                .strength(source.defaultDestroyTime(), source.getExplosionResistance())
                .friction(source.getFriction())
                .speedFactor(source.getSpeedFactor())
                .jumpFactor(source.getJumpFactor())
                .sound(state.getSoundType())
                .lightLevel(ignored -> state.getLightEmission());
        if (!state.canOcclude()) result.noOcclusion();
        if (state.requiresCorrectToolForDrops()) result.requiresCorrectToolForDrops();
        if (state.ignitedByLava()) result.ignitedByLava();
        return result;
    }

    public record Binding(Spec spec, NibaruMaterialProfile profile, Block source,
            Block slab, Block stairs, Block wall, Block verticalSlab, Block step,
            Block corner, Block quarterColumn, Block layer) {
        public List<Block> generated() {
            return List.of(slab, stairs, wall, verticalSlab, step, corner, quarterColumn, layer);
        }
        public Map<String, Block> roles() {
            LinkedHashMap<String, Block> result = new LinkedHashMap<>();
            result.put("block", source);
            result.put("slab", slab);
            result.put("stairs", stairs);
            result.put("wall", wall);
            result.put("vertical_slab", verticalSlab);
            result.put("step", step);
            result.put("corner", corner);
            result.put("quarter_column", quarterColumn);
            result.put("layer", layer);
            return Collections.unmodifiableMap(result);
        }
    }

    private record Pending(Spec spec, NibaruMaterialProfile profile, Block source,
            Block slab, Block stairs, Block wall) {
        private static Pending nativeBinding(Spec spec, NibaruMaterialProfile profile, Block source) {
            return new Pending(spec, profile, source, profile.nativeSlab().orElseThrow(),
                    profile.nativeStair().orElseThrow(), profile.nativeWall().orElseThrow());
        }
    }

    public record StandardFuelTrait(Block derived, Block source, int divisor) {}
}
