package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedMaterialTraits;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableSemantics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.WeatheringCopper;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathSemantics;
import games.twinhead.moreslabsstairsandwalls.block.strippable.StrippingSemantics;
import games.twinhead.moreslabsstairsandwalls.block.oxidizable.CopperSemantics;
import games.twinhead.moreslabsstairsandwalls.block.translucent.TranslucentSemantics;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShovelItem;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/** Composes Nibaru material profiles with companion-owned derived geometry. */
public final class NibaruProviderAdapter {
    public static final Set<BehaviorCapability> ADAPTED_CAPABILITIES = Collections.unmodifiableSet(
            EnumSet.of(BehaviorCapability.LEAF_LIFECYCLE, BehaviorCapability.SPREADABLE,
                    BehaviorCapability.FLATTENABLE_TO_PATH, BehaviorCapability.PATH_CONVERSION,
                    BehaviorCapability.STRIPPABLE, BehaviorCapability.OXIDIZABLE,
                    BehaviorCapability.WAXABLE, BehaviorCapability.SCRAPEABLE,
                    BehaviorCapability.CORAL_DEATH, BehaviorCapability.FALLING,
                    BehaviorCapability.CONCRETE_HARDENING, BehaviorCapability.REDSTONE_POWER,
                    BehaviorCapability.TRANSLUCENT_ADJACENCY, BehaviorCapability.ICE_MELTING,
                    BehaviorCapability.MAGMA_DAMAGE, BehaviorCapability.SOUL_SAND_INTERACTION,
                    BehaviorCapability.GLAZED_ORIENTATION, BehaviorCapability.HONEY_INTERACTION,
                    BehaviorCapability.SLIME_INTERACTION));
    public static final Set<VisualProfile> ADAPTED_VISUALS = Collections.unmodifiableSet(EnumSet.of(
            VisualProfile.UNIFORM, VisualProfile.TOP_SIDE_BOTTOM, VisualProfile.PILLAR,
            VisualProfile.GRASS_OVERLAY, VisualProfile.LEAVES_CUTOUT_TINTED, VisualProfile.CUTOUT_UNIFORM,
            VisualProfile.PATH, VisualProfile.TRANSLUCENT_UNIFORM, VisualProfile.ROOTS,
            VisualProfile.GLASS_EDGE, VisualProfile.GLAZED_ORIENTED, VisualProfile.HONEY_INSET,
            VisualProfile.SLIME_INSET, VisualProfile.HUGE_MUSHROOM));
    private static final Identifier SHAPE_MAP_SOURCE = Identifier.fromNamespaceAndPath(
            CnmTerrainCompat.MOD_ID, "provider_profiles");
    private static final Map<NibaruMaterialProfile, EnumMap<BgeGeometryRole, Block>> DERIVED =
            new IdentityHashMap<>();
    private static final Map<Block, RuntimeBinding> RUNTIME_BINDINGS = new IdentityHashMap<>();
    private static final Map<NibaruMaterialProfile, Set<Block>> TINT_TARGETS = new IdentityHashMap<>();
    private static BiConsumer<NibaruMaterialProfile, Block> tintRegistrar;

    private NibaruProviderAdapter() {}

    public static Optional<NibaruMaterialProfile> profile(Block source) {
        return NibaruMaterialProfiles.fromBlock(source);
    }

    public static boolean cullsTranslucent(Block source, BlockState sourceState, BlockState neighborState) {
        if (!TranslucentSemantics.sameMaterial(source, neighborState.getBlock())) return false;
        NibaruMaterialProfile profile = profile(source).orElseThrow();
        return neighborState.getBlock() == profile.canonicalParent()
                || (neighborState.getBlock() == source && sourceState.equals(neighborState));
    }

    public static boolean cullsBoundTranslucent(Block source, BlockState sourceState, BlockState neighborState) {
        RuntimeBinding binding = RUNTIME_BINDINGS.get(source);
        if (binding == null) return false;
        NibaruMaterialProfile neighbor = profile(neighborState.getBlock())
                .orElseGet(() -> Optional.ofNullable(RUNTIME_BINDINGS.get(neighborState.getBlock()))
                        .map(RuntimeBinding::profile).orElse(null));
        if (neighbor == null || neighbor.canonicalParent() != binding.profile().canonicalParent()) return false;
        return neighborState.getBlock() == binding.profile().canonicalParent()
                || (neighborState.getBlock() == source && sourceState.equals(neighborState));
    }

    public static DerivedGeometrySupport support(Block source, DerivedGeometrySupport.Geometry target) {
        return profile(source).map(p -> p.supportFor(target, ADAPTED_CAPABILITIES, ADAPTED_VISUALS))
                .orElseGet(() -> new DerivedGeometrySupport(DerivedGeometrySupport.Status.UNSUPPORTED_SOURCE, Set.of()));
    }

    public static int admissionSize(BlockState state) {
        Optional<NibaruMaterialProfile> profile = profile(state.getBlock());
        if (profile.isEmpty()) return state.getProperties().size();
        // A canonical external source is itself profile-owned but is not a CNM geometry input.
        // Only actual SlabBlock/StairBlock carriers may enter the two CNM generation branches.
        if (!(state.getBlock() instanceof SlabBlock) && !(state.getBlock() instanceof StairBlock))
            return state.getProperties().size();
        DerivedGeometrySupport.Geometry target = state.getBlock() instanceof SlabBlock
                ? DerivedGeometrySupport.Geometry.VERTICAL_SLAB
                : DerivedGeometrySupport.Geometry.STEP;
        if (ExistingDerivedGeometryBindings.contains(profile.get().canonicalParentId(), target)) return -1;
        return !profile.get().supportFor(target, ADAPTED_CAPABILITIES, ADAPTED_VISUALS).supported() ? -1
                : target == DerivedGeometrySupport.Geometry.VERTICAL_SLAB ? 2 : 4;
    }

    public static VerticalSlabBlock createVertical(BlockBehaviour.Properties properties, SlabBlock source) {
        NibaruMaterialProfile profile = profile(source).orElse(null);
        if (profile == null) return new VerticalSlabBlock(properties);
        VerticalSlabBlock result;
        ExternalMaterialStateBridge materialStateBridge = ExternalMaterialStateBridge.forProfile(profile);
        if (materialStateBridge.requiresBridge()) {
            result = EnderscapeMaterialGeometry.vertical(materialStateBridge, properties);
        } else if (HugeMushroomMaterial.isHugeMushroom(profile)) {
            result = new HugeMushroomVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.LEAF_LIFECYCLE)) {
            result = new NibaruLeavesVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.PATH_CONVERSION)) {
            result = new PathVerticalSlabBlock(properties, () -> transitionGeometry(profile,
                    MaterialTransition.Type.PATH_REVERSION, BgeGeometryRole.VERTICAL_SLAB));
        } else if (isSpreadableSurface(profile)) {
            result = new GrassVerticalSlab(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.SPREADABLE)) {
            result = new DirtVerticalSlab(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.FLATTENABLE_TO_PATH)) {
            result = new DirtVerticalSlab(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.OXIDIZABLE)) {
            result = new CopperVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.CORAL_DEATH)) {
            result = new CoralVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.CONCRETE_HARDENING)) {
            result = new ConcretePowderVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.REDSTONE_POWER)) {
            result = new RedstoneVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.ICE_MELTING)) {
            result = new IceVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.MAGMA_DAMAGE)) {
            result = new MagmaVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.SOUL_SAND_INTERACTION)) {
            result = new SoulSandVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.GLAZED_ORIENTATION)) {
            result = new GlazedVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.HONEY_INTERACTION)) {
            result = new HoneyVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.SLIME_INTERACTION)) {
            result = new SlimeVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.FALLING)) {
            result = new FallingVerticalSlabBlock(properties);
        } else if (MaterialAxisState.applies(profile)) {
            result = new AxisVerticalSlabBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.STRIPPABLE)
                || profile.capabilities().contains(BehaviorCapability.WAXABLE)) {
            result = new ProviderVerticalSlabBlock(properties);
        } else {
            result = new VerticalSlabBlock(properties);
        }
        registerTint(profile, result);
        capture(profile, BgeGeometryRole.VERTICAL_SLAB, result);
        return result;
    }

    public static StepBlock createStep(BlockBehaviour.Properties properties, StairBlock source) {
        NibaruMaterialProfile profile = profile(source).orElse(null);
        if (profile == null) return new StepBlock(properties);
        StepBlock result;
        ExternalMaterialStateBridge materialStateBridge = ExternalMaterialStateBridge.forProfile(profile);
        if (materialStateBridge.requiresBridge()) {
            result = EnderscapeMaterialGeometry.step(materialStateBridge, properties);
        } else if (HugeMushroomMaterial.isHugeMushroom(profile)) {
            result = new HugeMushroomStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.LEAF_LIFECYCLE)) {
            result = new NibaruLeavesStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.PATH_CONVERSION)) {
            result = new PathStepBlock(properties, () -> transitionGeometry(profile,
                    MaterialTransition.Type.PATH_REVERSION, BgeGeometryRole.STEP));
        } else if (isSpreadableSurface(profile)) {
            result = new GrassStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.FLATTENABLE_TO_PATH)) {
            result = new DirtStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.OXIDIZABLE)) {
            result = new CopperStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.CORAL_DEATH)) {
            result = new CoralStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.CONCRETE_HARDENING)) {
            result = new ConcretePowderStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.REDSTONE_POWER)) {
            result = new RedstoneStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.ICE_MELTING)) {
            result = new IceStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.MAGMA_DAMAGE)) {
            result = new MagmaStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.SOUL_SAND_INTERACTION)) {
            result = new SoulSandStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.GLAZED_ORIENTATION)) {
            result = new GlazedStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.HONEY_INTERACTION)) {
            result = new HoneyStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.SLIME_INTERACTION)) {
            result = new SlimeStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.FALLING)) {
            result = new FallingStepBlock(properties);
        } else if (MaterialAxisState.applies(profile)) {
            result = new AxisStepBlock(properties);
        } else if (profile.capabilities().contains(BehaviorCapability.STRIPPABLE)
                || profile.capabilities().contains(BehaviorCapability.WAXABLE)) {
            result = new ProviderStepBlock(properties);
        } else {
            result = new StepBlock(properties);
        }
        registerTint(profile, result);
        capture(profile, BgeGeometryRole.STEP, result);
        return result;
    }

    /** Creates and binds the BGE-owned Layer for one exact canonical material profile. */
    public static BgeLayerBlock createLayer(NibaruMaterialProfile profile,
            BlockBehaviour.Properties properties) {
        DerivedGeometrySupport support = profile.supportFor(DerivedGeometrySupport.Geometry.LAYER,
                ADAPTED_CAPABILITIES, ADAPTED_VISUALS);
        if (!support.supported()) {
            throw new IllegalStateException("Unsupported BGE Layer material " + profile.canonicalParentId()
                    + ": " + support.status() + " " + support.missingCapabilities());
        }
        ExternalMaterialStateBridge materialStateBridge = ExternalMaterialStateBridge.forProfile(profile);
        BgeLayerBlock result = materialStateBridge.requiresBridge()
                ? EnderscapeMaterialGeometry.layer(materialStateBridge, profile, properties)
                : HugeMushroomMaterial.isHugeMushroom(profile)
                        ? new HugeMushroomLayerBlock(profile, properties)
                        : BgeLayerSpecializedBlocks.create(profile, properties);
        bindExisting(profile, BgeGeometryRole.LAYER, result);
        return result;
    }

    /** Creates and binds the BGE-owned non-composing Corner. */
    public static BgeCornerBlock createCorner(NibaruMaterialProfile profile,
            BlockBehaviour.Properties properties) {
        requireLocalSupport(profile, BgeGeometryRole.CORNER);
        ExternalMaterialStateBridge materialStateBridge = ExternalMaterialStateBridge.forProfile(profile);
        BgeCornerBlock result = materialStateBridge.requiresBridge()
                ? EnderscapeMaterialGeometry.corner(materialStateBridge, profile, properties)
                : HugeMushroomMaterial.isHugeMushroom(profile)
                        ? new HugeMushroomCornerBlock(profile, properties)
                        : BgeCornerBlock.create(profile, properties);
        bindExisting(profile, BgeGeometryRole.CORNER, result);
        return result;
    }

    /** Creates and binds the BGE-owned compound Quarter Column. */
    public static BgeColumnBlock createQuarterColumn(NibaruMaterialProfile profile,
            BlockBehaviour.Properties properties) {
        requireLocalSupport(profile, BgeGeometryRole.QUARTER_COLUMN);
        ExternalMaterialStateBridge materialStateBridge = ExternalMaterialStateBridge.forProfile(profile);
        BgeColumnBlock result = materialStateBridge.requiresBridge()
                ? EnderscapeMaterialGeometry.column(materialStateBridge, profile, properties)
                : HugeMushroomMaterial.isHugeMushroom(profile)
                        ? new HugeMushroomColumnBlock(profile, properties)
                        : BgeColumnBlock.create(profile, properties);
        bindExisting(profile, BgeGeometryRole.QUARTER_COLUMN, result);
        return result;
    }

    private static void requireLocalSupport(NibaruMaterialProfile profile, BgeGeometryRole role) {
        DerivedGeometrySupport support = profile.supportFor(DerivedGeometrySupport.Geometry.LAYER,
                ADAPTED_CAPABILITIES, ADAPTED_VISUALS);
        if (!support.supported()) {
            throw new IllegalStateException("Unsupported BGE " + role + " material "
                    + profile.canonicalParentId() + ": " + support.status() + " "
                    + support.missingCapabilities());
        }
    }

    public static void bindExisting(NibaruMaterialProfile profile, DerivedGeometrySupport.Geometry geometry, Block block) {
        bindExisting(profile, BgeGeometryRole.fromLegacy(geometry), block);
    }

    public static void bindExisting(NibaruMaterialProfile profile, BgeGeometryRole geometry, Block block) {
        capture(profile, geometry, block);
        registerTint(profile, block);
    }

    /** Captures a CNM-owned specialized block whose exact source belongs to a provider profile. */
    public static Block bindGenerated(Block source, DerivedGeometrySupport.Geometry geometry, Block generated) {
        NibaruMaterialProfile profile = profile(source).orElse(null);
        if (profile != null && profile.supportFor(geometry, ADAPTED_CAPABILITIES, ADAPTED_VISUALS).supported()) {
            bindExisting(profile, geometry, generated);
        }
        return generated;
    }

    public static Optional<Block> derived(NibaruMaterialProfile profile, DerivedGeometrySupport.Geometry geometry) {
        return derived(profile, BgeGeometryRole.fromLegacy(geometry));
    }

    public static Optional<Block> derived(NibaruMaterialProfile profile, BgeGeometryRole geometry) {
        EnumMap<BgeGeometryRole, Block> geometries = DERIVED.get(profile);
        Block derived = geometries == null ? null : geometries.get(geometry);
        if (derived != null) return Optional.of(derived);
        Optional<DerivedGeometrySupport.Geometry> legacy = geometry.legacyGeometry();
        if (legacy.isEmpty()) return Optional.empty();
        Optional<Block> binding = ExistingDerivedGeometryBindings.resolve(
                profile.canonicalParentId(), legacy.get());
        if (binding.isEmpty()) return Optional.empty();
        Block existing = binding.get();
        bindExisting(profile, geometry, existing);
        return Optional.of(existing);
    }

    /** Runtime material semantics for both newly generated and save-compatible existing owners. */
    public static Optional<RuntimeBinding> runtimeBinding(Block block) {
        return Optional.ofNullable(RUNTIME_BINDINGS.get(block));
    }

    /** Local roles omitted from exact Nibaru C46's DerivedMaterialTraits vocabulary. */
    public static List<LocalMaterialTrait> localMaterialTraits() {
        List<LocalMaterialTrait> result = new ArrayList<>();
        RUNTIME_BINDINGS.forEach((block, binding) -> {
            if (binding.role().legacyGeometry().isEmpty()) {
                result.add(new LocalMaterialTrait(block, binding.profile().canonicalParent(),
                        binding.role(), binding.role().fuelDivisor()));
            }
        });
        return List.copyOf(result);
    }

    public static Optional<InteractionResult> useComposedCapabilities(Block block, ItemStack stack,
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        RuntimeBinding binding = RUNTIME_BINDINGS.get(block);
        if (binding == null) return Optional.empty();
        if (binding.profile().oxidationStage().isPresent()) {
            InteractionResult result = CopperSemantics.interact(state, binding.profile(), stack, level, pos,
                    player, hand, target -> derived(target, binding.role()));
            if (result != InteractionResult.TRY_WITH_EMPTY_HAND) return Optional.of(result);
        }
        if (stack.getItem() instanceof ShovelItem
                && binding.profile().capabilities().contains(BehaviorCapability.FLATTENABLE_TO_PATH)) {
            Block target = transitionGeometry(binding.profile(), MaterialTransition.Type.PATH_TARGET, binding.role());
            BlockState targetState = target.defaultBlockState();
            if (binding.role().isBgeOwned()) {
                targetState = PathSemantics.copySharedProperties(state, targetState);
            }
            return Optional.of(PathSemantics.flatten(stack, state, targetState, level, pos, player, hand));
        }
        if (stack.getItem() instanceof AxeItem
                && binding.profile().capabilities().contains(BehaviorCapability.STRIPPABLE)) {
            Block target = transitionGeometry(binding.profile(), MaterialTransition.Type.STRIPPED, binding.role());
            BlockState targetState = target.defaultBlockState();
            if (binding.role().isBgeOwned()) {
                targetState = PathSemantics.copySharedProperties(state, targetState);
            }
            return Optional.of(StrippingSemantics.strip(stack, state, targetState,
                    level, pos, player, hand));
        }
        return Optional.empty();
    }

    public static WeatheringCopper.WeatherState copperAge(Block block) {
        RuntimeBinding binding = RUNTIME_BINDINGS.get(block);
        if (binding == null) throw new IllegalStateException("Missing copper runtime binding");
        return binding.profile().oxidationStage().orElseThrow();
    }

    public static Optional<BlockState> nextOxidation(Block block, BlockState state) {
        RuntimeBinding binding = RUNTIME_BINDINGS.get(block);
        if (binding == null) return Optional.empty();
        return CopperSemantics.transition(state, binding.profile(), MaterialTransition.Type.NEXT_OXIDATION,
                target -> derived(target, binding.role()));
    }

    public static Block coralDeath(Block block) {
        RuntimeBinding binding = RUNTIME_BINDINGS.get(block);
        if (binding == null) throw new IllegalStateException("Missing coral runtime binding");
        return derived(NibaruMaterialProfiles.fromFamily(binding.profile().transition(MaterialTransition.Type.CORAL_DEATH)
                .orElseThrow().target()).orElseThrow(), binding.role()).orElseThrow();
    }

    public static Block concreteHardening(Block block) {
        RuntimeBinding binding = RUNTIME_BINDINGS.get(block);
        if (binding == null) throw new IllegalStateException("Missing concrete-powder runtime binding");
        NibaruMaterialProfile target = NibaruMaterialProfiles.fromFamily(binding.profile()
                .transition(MaterialTransition.Type.CONCRETE_HARDENING).orElseThrow().target()).orElseThrow();
        return derived(target, binding.role()).orElseThrow();
    }

    public static void addExactShapeMapEdges(List<ShapeMap.Mapping> mappings) {
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            Item parent = profile.canonicalParent().asItem();
            add(mappings, parent, profile.effectiveSlabSource().map(Block::asItem));
            add(mappings, parent, profile.effectiveStairSource().map(Block::asItem));
            add(mappings, parent, profile.nativeWall().map(Block::asItem));
            for (BgeGeometryCatalog.Descriptor geometry : BgeGeometryCatalog.ordered()) {
                add(mappings, parent, geometry.resolveItem(profile));
            }
        }
        ExplicitShapeMapFamilies.addDeclaredGroupEdges(mappings);
    }

    /** Replaces supported components with the exact declared nine-role selector sequences. */
    public static void applyProviderParentSegmentOrder() {
        ExplicitShapeMapFamilies.rebuildExactPresentation();
    }

    public static List<UnsupportedEntry> unsupportedMatrix() {
        List<UnsupportedEntry> result = new ArrayList<>();
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                DerivedGeometrySupport support = profile.supportFor(geometry, ADAPTED_CAPABILITIES, ADAPTED_VISUALS);
                if (!support.supported()) {
                    Optional<Block> source = switch (geometry) {
                        case VERTICAL_SLAB -> profile.effectiveSlabSource();
                        case STEP -> profile.effectiveStairSource();
                        case LAYER -> Optional.of(profile.canonicalParent());
                    };
                    source.map(BuiltInRegistries.BLOCK::getKey).ifPresent(id -> result.add(new UnsupportedEntry(profile.canonicalParentId(), id,
                            geometry, support.status(), support.missingCapabilities())));
                }
            }
        }
        return List.copyOf(result);
    }

    public static void configureTintRegistrar(BiConsumer<NibaruMaterialProfile, Block> registrar) {
        tintRegistrar = registrar;
        TINT_TARGETS.forEach((profile, blocks) -> blocks.forEach(block -> registrar.accept(profile, block)));
    }

    /** Adds a late-registered standard external form to the same client tint contract. */
    public static void registerTintTarget(NibaruMaterialProfile profile, Block block) {
        registerTint(profile, block);
    }

    private static void capture(NibaruMaterialProfile profile, BgeGeometryRole geometry, Block block) {
        EnumMap<BgeGeometryRole, Block> geometries = DERIVED.computeIfAbsent(profile,
                ignored -> new EnumMap<>(BgeGeometryRole.class));
        Block previous = geometries.putIfAbsent(geometry, block);
        if (previous != null && previous != block) throw new IllegalStateException(
                "Duplicate derived Nibaru geometry owner for " + profile.canonicalParentId() + " " + geometry);
        RuntimeBinding binding = new RuntimeBinding(profile, geometry);
        RuntimeBinding priorBinding = RUNTIME_BINDINGS.putIfAbsent(block, binding);
        if (priorBinding != null && !priorBinding.equals(binding)) throw new IllegalStateException(
                "Derived block is bound to multiple Nibaru profiles: " + BuiltInRegistries.BLOCK.getKey(block));
        CanonicalGeometryRegistry.register(block);
        geometry.legacyGeometry().ifPresent(legacy -> DerivedMaterialTraits.register(
                block, profile.canonicalParent(), legacy, geometry.fuelDivisor()));
        if (profile.family() != null && profile.canonicalParent().defaultBlockState().ignitedByLava()) {
            FlammableBlockRegistry.getDefaultInstance().add(block,
                    games.twinhead.moreslabsstairsandwalls.registry.ModRegistry.getBurnChance(profile.family()),
                    games.twinhead.moreslabsstairsandwalls.registry.ModRegistry.getSpreadChance(profile.family()));
        }
        registerSpreadablePairIfReady(profile, geometry, block);
    }

    private static void registerSpreadablePairIfReady(NibaruMaterialProfile profile,
            BgeGeometryRole geometry, Block block) {
        if (!profile.capabilities().contains(BehaviorCapability.SPREADABLE)) return;
        if (!isSpreadableSurface(profile)) {
            for (NibaruMaterialProfile candidate : NibaruMaterialProfiles.all()) {
                if (isSpreadableSurface(candidate)) derived(candidate, geometry)
                        .ifPresent(surface -> SpreadableSemantics.registerPair(block, surface, candidate.canonicalParent()));
            }
            return;
        }
        baseProfile(profile).flatMap(base -> derived(base, geometry))
                .ifPresent(base -> SpreadableSemantics.registerPair(base, block, profile.canonicalParent()));
    }

    private static boolean isSpreadableSurface(NibaruMaterialProfile profile) {
        return profile.transitions().stream().anyMatch(t -> t.type() == MaterialTransition.Type.SPREADABLE_BASE);
    }

    private static Optional<NibaruMaterialProfile> baseProfile(NibaruMaterialProfile profile) {
        return profile.transitions().stream()
                .filter(t -> t.type() == MaterialTransition.Type.SPREADABLE_BASE)
                .findFirst().flatMap(t -> NibaruMaterialProfiles.fromFamily(t.target()));
    }

    private static Block transitionGeometry(NibaruMaterialProfile profile, MaterialTransition.Type type,
            BgeGeometryRole geometry) {
        NibaruMaterialProfile target = profile.transitions().stream().filter(t -> t.type() == type).findFirst()
                .flatMap(t -> NibaruMaterialProfiles.fromFamily(t.target()))
                .orElseThrow(() -> new IllegalStateException("Missing " + type + " target for "
                        + profile.canonicalParentId()));
        return derived(target, geometry).orElseThrow(() -> new IllegalStateException(
                "Missing canonical transition geometry " + geometry + " for " + profile.canonicalParentId()));
    }

    private static void add(List<ShapeMap.Mapping> mappings, Item parent, Optional<Item> shape) {
        shape.filter(item -> item != parent).ifPresent(item -> mappings.add(
                new ShapeMap.Mapping(parent, item, 900, SHAPE_MAP_SOURCE)));
    }

    private static void registerTint(NibaruMaterialProfile profile, Block block) {
        if (profile.tintProfile() == TintProfile.NONE) return;
        TINT_TARGETS.computeIfAbsent(profile, ignored ->
                Collections.newSetFromMap(new IdentityHashMap<>())).add(block);
        if (tintRegistrar != null) tintRegistrar.accept(profile, block);
    }

    public record UnsupportedEntry(Identifier family, Identifier source,
            DerivedGeometrySupport.Geometry targetGeometry, DerivedGeometrySupport.Status status,
            Set<BehaviorCapability> missingCapabilities) {
        public UnsupportedEntry { missingCapabilities = Set.copyOf(missingCapabilities); }
    }

    public record LocalMaterialTrait(Block derived, Block canonicalParent,
            BgeGeometryRole role, int fuelDivisor) {}

    public record RuntimeBinding(NibaruMaterialProfile profile, BgeGeometryRole role) {
        /** Compatibility view for existing C46 consumers; local C55 roles have no legacy value. */
        public DerivedGeometrySupport.Geometry geometry() {
            return role.legacyGeometry().orElse(null);
        }
    }

}
