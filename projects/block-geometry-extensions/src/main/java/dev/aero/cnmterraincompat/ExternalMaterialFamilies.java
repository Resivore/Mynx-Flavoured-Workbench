package dev.aero.cnmterraincompat;

import dev.aero.cnmterraincompat.ExternalMaterialCatalog.Spec;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

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
        spec.materialStateBridge().validateSource(source);
        Block reference = BuiltInRegistries.BLOCK.getValue(spec.providerReference());
        if (reference == null || !spec.providerReference().equals(BuiltInRegistries.BLOCK.getKey(reference))) {
            throw new IllegalStateException("Provider completed without required Path reference "
                    + spec.providerReference());
        }

        Identifier slabId = id(spec, "slab");
        Identifier stairsId = id(spec, "stairs");
        Identifier wallId = id(spec, "wall");
        boolean leaves = spec.capabilities().contains(
                games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability.LEAF_LIFECYCLE);
        boolean hugeMushroom = spec.visual()
                == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.HUGE_MUSHROOM;
        RoleSelection slab = selectStandardRole(spec, "slab", slabId, SlabBlock.class,
                () -> ExternalMaterialBlocks.createSlab(source, properties(slabId, source), leaves, hugeMushroom,
                        spec.materialStateBridge()));
        RoleSelection stairs = selectStandardRole(spec, "stairs", stairsId, StairBlock.class,
                () -> ExternalMaterialBlocks.createStairs(source, properties(stairsId, source), leaves, hugeMushroom,
                        spec.materialStateBridge()));
        RoleSelection wall = selectStandardRole(spec, "wall", wallId, WallBlock.class,
                () -> ExternalMaterialBlocks.createWall(source,
                        wallProperties(wallId, source, spec.materialStateBridge()), leaves, hugeMushroom,
                        spec.materialStateBridge(), ExternalMaterialCatalog.usesWoodenWall(spec)));
        Set<String> generatedStandardRoles = new LinkedHashSet<>();
        if (slab.generated()) generatedStandardRoles.add("slab");
        if (stairs.generated()) generatedStandardRoles.add("stairs");
        if (wall.generated()) generatedStandardRoles.add("wall");

        NibaruMaterialProfile profile = new NibaruMaterialProfile(
                ExternalMaterialCatalog.PROFILE_VERSION, null, source, spec.id(),
                Optional.of(slab.block()), Optional.of(stairs.block()), Optional.of(wall.block()),
                Optional.of(slab.block()), Optional.of(stairs.block()),
                Optional.of(registeredId(slab.block())), Optional.of(registeredId(stairs.block())),
                Optional.of(registeredId(wall.block())),
                spec.blockTags(), spec.capabilities(), spec.visual(),
                NibaruMaterialProfile.VisualSupport.GENERIC_SUPPORTED, spec.tint(), spec.renderLayer(),
                spec.orientation(), surfaceSampling(spec), doubleFormPolicy(spec),
                new NibaruMaterialProfile.TextureRoles(spec.side(), spec.top(), spec.bottom(), spec.overlay(), spec.side(),
                        spec.interior()),
                insetVisualContract(spec), Optional.empty(), false, spec.transitions());
        NibaruMaterialProfiles.registerExternal(profile);

        Pending pending = new Pending(spec, profile, source, slab.block(), stairs.block(), wall.block(),
                Set.copyOf(generatedStandardRoles));
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
                    NibaruProviderAdapter.derived(profile, BgeGeometryRole.LAYER).orElseThrow(),
                    generatedRoles(pending.generatedStandardRoles()));
            pending.spec().materialStateBridge().validateDerived(pending.spec().id(), binding.roles());
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
        for (Block block : binding.generatedStandard()) {
            NibaruProviderAdapter.registerTintTarget(binding.profile(), block);
        }
        if (binding.generatedStandardRoles().contains("slab"))
            STANDARD_FUEL.put(binding.slab(), new StandardFuelTrait(binding.slab(), binding.source(), 2));
        if (binding.generatedStandardRoles().contains("stairs"))
            STANDARD_FUEL.put(binding.stairs(), new StandardFuelTrait(binding.stairs(), binding.source(), 1));
        if (binding.generatedStandardRoles().contains("wall"))
            STANDARD_FUEL.put(binding.wall(), new StandardFuelTrait(binding.wall(), binding.source(), 1));

        FlammableBlockRegistry.Entry fire = FlammableBlockRegistry.getDefaultInstance().get(binding.source());
        if (fire != null) for (Block block : binding.generatedStandard())
            FlammableBlockRegistry.getDefaultInstance().add(block, fire.getIgniteOdds(), fire.getBurnOdds());

        binding.profile().transition(MaterialTransition.Type.STRIPPED)
                .flatMap(transition -> NibaruMaterialProfiles.fromFamily(transition.target()))
                .ifPresent(target -> {
                    if (binding.generatedStandardRoles().contains("slab"))
                        StrippableBlockRegistry.register(binding.slab(), target.nativeSlab().orElseThrow());
                    if (binding.generatedStandardRoles().contains("stairs"))
                        StrippableBlockRegistry.register(binding.stairs(), target.nativeStair().orElseThrow());
                    if (binding.generatedStandardRoles().contains("wall"))
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

    private static RoleSelection selectStandardRole(Spec spec, String role, Identifier generatedId,
            Class<? extends Block> expectedType, Supplier<Block> generated) {
        Identifier providerId = spec.providerRoles().get(role);
        if (providerId != null) {
            Block candidate = BuiltInRegistries.BLOCK.getValue(providerId);
            if (providerId.equals(BuiltInRegistries.BLOCK.getKey(candidate))) {
                if (!expectedType.isInstance(candidate)) {
                    throw new IllegalStateException("Provider role has incompatible block type: "
                            + providerId + " expected " + expectedType.getSimpleName());
                }
                return new RoleSelection(candidate, false);
            }
        }
        Block block = generated.get();
        CnmTerrainCompat.register(generatedId, block);
        return new RoleSelection(block, true);
    }

    private static Identifier registeredId(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || id.equals(BuiltInRegistries.BLOCK.getDefaultKey())) {
            throw new IllegalStateException("Unregistered external geometry " + block);
        }
        return id;
    }

    private static Set<String> generatedRoles(Set<String> standard) {
        LinkedHashSet<String> result = new LinkedHashSet<>(standard);
        result.addAll(List.of("vertical_slab", "step", "corner", "quarter_column", "layer"));
        return Set.copyOf(result);
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
    private static BlockBehaviour.Properties wallProperties(Identifier id, Block source,
            ExternalMaterialStateBridge materialStateBridge) {
        // This material state belongs to the parent rather than to wall geometry. Retain the
        // provider callbacks; the bridge installs their required property on the wall state.
        if (materialStateBridge.requiresBridge()) return properties(id, source);
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

    private static NibaruMaterialProfile.SurfaceSamplingPolicy surfaceSampling(Spec spec) {
        return spec.visual() == VisualProfile.PATH
                ? NibaruMaterialProfile.SurfaceSamplingPolicy.PATH_LOWERED_SURFACE
                : NibaruMaterialProfile.SurfaceSamplingPolicy.BLOCK_ABSOLUTE;
    }

    private static NibaruMaterialProfile.DoubleFormPolicy doubleFormPolicy(Spec spec) {
        return switch (spec.visual()) {
            case HONEY_INSET, SLIME_INSET -> NibaruMaterialProfile.DoubleFormPolicy.CUSTOM_REQUIRED;
            default -> NibaruMaterialProfile.DoubleFormPolicy.COMPOSE_SEMANTIC_SURFACES;
        };
    }

    private static Optional<NibaruMaterialProfile.InsetVisualContract> insetVisualContract(Spec spec) {
        return switch (spec.visual()) {
            case HONEY_INSET -> Optional.of(new NibaruMaterialProfile.InsetVisualContract(1, 1,
                    NibaruMaterialProfile.InsetVisualContract.ShellTexture.BOTTOM, true));
            case SLIME_INSET -> Optional.of(new NibaruMaterialProfile.InsetVisualContract(3, 2,
                    NibaruMaterialProfile.InsetVisualContract.ShellTexture.MATERIAL_FACES, false));
            default -> Optional.empty();
        };
    }

    public record Binding(Spec spec, NibaruMaterialProfile profile, Block source,
            Block slab, Block stairs, Block wall, Block verticalSlab, Block step,
            Block corner, Block quarterColumn, Block layer, Set<String> generatedRoles) {
        public Binding { generatedRoles = Set.copyOf(generatedRoles); }
        public List<Block> generated() {
            List<Block> result = new ArrayList<>();
            roles().forEach((role, block) -> {
                if (generatedRoles.contains(role)) result.add(block);
            });
            return List.copyOf(result);
        }
        public List<Block> canonicalDerived() {
            return List.of(slab, stairs, wall, verticalSlab, step, corner, quarterColumn, layer);
        }
        public boolean isGeneratedRole(String role) {
            return generatedRoles.contains(role);
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
            Block slab, Block stairs, Block wall, Set<String> generatedStandardRoles) {
        private List<Block> generatedStandard() {
            List<Block> result = new ArrayList<>();
            if (generatedStandardRoles.contains("slab")) result.add(slab);
            if (generatedStandardRoles.contains("stairs")) result.add(stairs);
            if (generatedStandardRoles.contains("wall")) result.add(wall);
            return List.copyOf(result);
        }
    }

    private record RoleSelection(Block block, boolean generated) {}

    public record StandardFuelTrait(Block derived, Block source, int divisor) {}
}
