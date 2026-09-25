package dev.aero.cnmterraincompat.gametest;

import java.lang.reflect.Method;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafSemantics;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableSemantics;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathSemantics;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.oxidizable.CopperSemantics;
import dev.aero.cnmterraincompat.AxisModelContract;
import dev.aero.cnmterraincompat.AxisModelContract.AxisUvPolicy;
import dev.aero.cnmterraincompat.AxisStepBlock;
import dev.aero.cnmterraincompat.AxisVerticalSlabBlock;
import dev.aero.cnmterraincompat.BgeGeometryRole;
import dev.aero.cnmterraincompat.BgeLayerBlock;
import dev.aero.cnmterraincompat.CnmGeneratedLanguage;
import dev.aero.cnmterraincompat.CnmTerrainCompat;
import dev.aero.cnmterraincompat.DirtVerticalSlab;
import dev.aero.cnmterraincompat.GrassFamilyBehavior;
import dev.aero.cnmterraincompat.GlazedModelContract;
import dev.aero.cnmterraincompat.GlazedPatternState;
import dev.aero.cnmterraincompat.GlazedStepBlock;
import dev.aero.cnmterraincompat.GlazedVerticalSlabBlock;
import dev.aero.cnmterraincompat.HoneyDerivedGeometry;
import dev.aero.cnmterraincompat.InsetModelContract;
import dev.aero.cnmterraincompat.MaterialAxisState;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import dev.aero.cnmterraincompat.ProviderStickyMaterialSemantics;
import dev.aero.cnmterraincompat.ProviderVisualAdapter;
import games.twinhead.moreslabsstairsandwalls.block.honey.HoneySemantics;
import dev.tazer.clutternomore.client.assets.AssetGenerator;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.WeatheringStepBlock;
import dev.tazer.clutternomore.common.blocks.WeatheringVerticalSlabBlock;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

public final class LeafSemanticsGameTests implements CustomTestMethodInvoker {
    private static final Identifier NIBARU_SLAB = id("more_slabs_stairs_and_walls:oak_leaves_slab");
    private static final Identifier NIBARU_STAIRS = id("more_slabs_stairs_and_walls:oak_leaves_stairs");
    private static final Identifier CNM_VERTICAL = id("clutternomore:more_slabs_stairs_and_walls/vertical_oak_leaves_slab");
    private static final Identifier CNM_STEP = id("clutternomore:more_slabs_stairs_and_walls/oak_leaves_step");
    private static final Identifier NIBARU_DIRT_SLAB = id("more_slabs_stairs_and_walls:dirt_slab");
    private static final Identifier CNM_DIRT_VERTICAL = id("clutternomore:more_slabs_stairs_and_walls/vertical_dirt_slab");
    private static final Identifier LEGACY_DIRT_SLAB = id("cnm_terrain_slabs_compat:dirt_slab");
    private static final Identifier LEGACY_DIRT_VERTICAL = id("cnm_terrain_slabs_compat:dirt_vertical_slab");
    private static final Identifier CNM_DIRT_STEP = id("clutternomore:more_slabs_stairs_and_walls/dirt_step");
    private static final Identifier CNM_GRASS_STEP = id("clutternomore:more_slabs_stairs_and_walls/grass_block_step");
    private static final Identifier NIBARU_GRASS_SLAB = id("more_slabs_stairs_and_walls:grass_block_slab");
    private static final Identifier NIBARU_DIRT_STAIRS = id("more_slabs_stairs_and_walls:dirt_stairs");
    private static final Identifier NIBARU_GRASS_STAIRS = id("more_slabs_stairs_and_walls:grass_block_stairs");
    private static final Identifier CNM_PATH_VERTICAL = id("clutternomore:more_slabs_stairs_and_walls/vertical_dirt_path_slab");
    private static final Identifier CNM_PATH_STEP = id("clutternomore:more_slabs_stairs_and_walls/dirt_path_step");

    @GameTest(maxTicks = 40)
    public void unifiedContainerOwnsBothLoaderIdentitiesAndVersionContracts(GameTestHelper helper) {
        var loader = FabricLoader.getInstance();
        var primary = loader.getModContainer("cnm_terrain_slabs_compat").orElseThrow();
        var legacy = loader.getModContainer("more_slabs_stairs_and_walls").orElseThrow();
        var version = primary.getMetadata().getVersion();

        helper.assertTrue(primary == legacy,
                "Fabric Loader did not resolve the legacy Nibaru alias to the unified BGE container");
        helper.assertTrue(primary.getMetadata().getId().equals("cnm_terrain_slabs_compat"),
                "Unified container primary identity changed");
        helper.assertTrue(version.getFriendlyString().equals("4.2.44-bge.canary100.bbb-beam-compat-visibility+26.2"),
                "Unified container version changed: " + version.getFriendlyString());
        try {
            helper.assertTrue(VersionPredicate.parse(">=4.2.0 <4.3.0-").test(version),
                    "Legacy Nibaru dependency range rejected the unified version");
            helper.assertTrue(VersionPredicate.parse(">=0.8.0-bge-canary56-vertical-stairs-catalog").test(version),
                    "Forward BGE dependency range rejected the unified version");
            helper.assertTrue(VersionPredicate.parse("=4.2.44-bge.canary100.bbb-beam-compat-visibility+26.2").test(version),
                    "Exact unified dependency rejected the unified version");
            helper.assertTrue(!VersionPredicate.parse("=4.2.0+26.2-port-canary46-bge-layer-contract").test(version),
                    "Exact predecessor Nibaru dependency falsely accepted the unified version");
            helper.assertTrue(!VersionPredicate.parse("=0.8.0-bge-canary56-vertical-stairs-catalog").test(version),
                    "Exact predecessor BGE dependency falsely accepted the unified version");
        } catch (net.fabricmc.loader.api.VersionParsingException e) {
            throw new IllegalStateException("Static unified version predicates did not parse", e);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void generatedProviderGeometriesHaveEffectiveEnglishNames(GameTestHelper helper) {
        helper.assertTrue(CnmGeneratedLanguage.isSelfGeneratedPack("clutternomore-runtime"),
                "CNM runtime generated language pack is not classified as self-generated");
        helper.assertTrue(CnmGeneratedLanguage.isSelfGeneratedPack("file/clutternomore"),
                "CNM persistent generated language pack is not classified as self-generated");
        helper.assertTrue(!CnmGeneratedLanguage.isSelfGeneratedPack("file/authored-overrides"),
                "An authored resource pack was classified as CNM-generated");
        helper.assertTrue(AssetGenerator.keys != null, "CNM generated language key inventory was not initialized");

        var names = new java.util.LinkedHashMap<String, String>();
        int providerPaths = 0;
        int ordinaryAliases = 0;
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.family() != null).toList()) {
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                Block block = NibaruProviderAdapter.derived(profile, geometry).orElseThrow();
                Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                if (!id.getNamespace().equals("clutternomore")) continue;

                helper.assertTrue(AssetGenerator.keys.contains(id.getPath()),
                        "CNM language inventory omitted generated Nibaru target " + id);
                String key = "block.clutternomore." + id.getPath().replace('/', '.');
                String name = AssetGenerator.langName(id.getPath());
                helper.assertTrue(!name.isBlank() && !name.equals(key),
                        "CNM generated an ineffective English name for " + id);
                helper.assertTrue(names.put(key, name) == null,
                        "Duplicate generated Nibaru translation key " + key);
                if (id.getPath().startsWith("more_slabs_stairs_and_walls/")) providerPaths++;
                else ordinaryAliases++;
            }
        }

        helper.assertTrue(names.size() == 627 && providerPaths == 558 && ordinaryAliases == 69,
                "Generated Nibaru English inventory changed: total=" + names.size()
                        + ", provider=" + providerPaths + ", ordinary=" + ordinaryAliases);
        helper.assertTrue("Oak Log Step".equals(names.get(
                        "block.clutternomore.more_slabs_stairs_and_walls.oak_log_step")),
                "Oak Log Step no longer follows CNM's normal generated naming");
        helper.assertTrue("Vertical Oak Log Slab".equals(names.get(
                        "block.clutternomore.more_slabs_stairs_and_walls.vertical_oak_log_slab")),
                "Vertical Oak Log Slab no longer follows CNM's normal generated naming");
        System.out.println("PROVIDER_ENGLISH_TRANSLATIONS|generated=627|providerPaths=558|ordinaryAliases=69");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void providerProfileInventoryAndLookup(GameTestHelper helper) {
        var profiles = NibaruMaterialProfiles.all();
        int expectedProfiles = 314 + (int) NibaruMaterialProfiles.eligibleVanillaFamilies().stream()
                .filter(family -> NibaruMaterialProfiles.fromBlock(family.parent()).orElseThrow().family() == null)
                .count() + (int) dev.aero.cnmterraincompat.ExternalMaterialFamilies.all().stream()
                .filter(binding -> binding.profile().family() == null
                        && !binding.profile().canonicalParentId().getNamespace().equals("minecraft")).count();
        helper.assertTrue(profiles.size() == expectedProfiles,
                "Expected " + expectedProfiles + " profiles, found " + profiles.size());
        var parents = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Block, Boolean>());
        for (NibaruMaterialProfile profile : profiles) {
            helper.assertTrue(parents.add(profile.canonicalParent()),
                    "Duplicate canonical profile parent: " + profile.canonicalParentId());
            helper.assertTrue(NibaruMaterialProfiles.fromBlock(profile.canonicalParent()).orElseThrow() == profile,
                    "Parent lookup did not return exact profile: " + profile.canonicalParentId());
            profile.nativeSlab().ifPresent(value -> helper.assertTrue(
                    NibaruMaterialProfiles.fromBlock(value).orElseThrow() == profile, "Slab lookup mismatch"));
            profile.nativeStair().ifPresent(value -> helper.assertTrue(
                    NibaruMaterialProfiles.fromBlock(value).orElseThrow() == profile, "Stair lookup mismatch"));
            profile.nativeWall().ifPresent(value -> helper.assertTrue(
                    NibaruMaterialProfiles.fromBlock(value).orElseThrow() == profile, "Wall lookup mismatch"));
            profile.effectiveSlabSource().ifPresent(value -> helper.assertTrue(
                    NibaruMaterialProfiles.fromBlock(value).orElseThrow() == profile, "Effective slab lookup mismatch"));
            profile.effectiveStairSource().ifPresent(value -> helper.assertTrue(
                    NibaruMaterialProfiles.fromBlock(value).orElseThrow() == profile, "Effective stair lookup mismatch"));
        }
        long genericSafe = profiles.stream().filter(profile -> profile.capabilities().isEmpty()).count();
        long behaviorSupported = targets(profiles).stream().filter(target -> target.profile.capabilities().stream()
                .allMatch(NibaruProviderAdapter.ADAPTED_CAPABILITIES::contains)).count();
        long visualSupported = targets(profiles).stream().filter(target ->
                NibaruProviderAdapter.ADAPTED_VISUALS.contains(target.profile.visualProfile())).count();
        long fullyReady = targets(profiles).stream().filter(target -> target.profile.supportFor(target.geometry,
                NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS).supported()).count();
        long unsupportedBehavior = targets(profiles).stream().filter(target -> target.profile.supportFor(target.geometry,
                NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS).status()
                == DerivedGeometrySupport.Status.UNSUPPORTED_BEHAVIOR).count();
        long unsupportedVisual = targets(profiles).stream().filter(target -> target.profile.supportFor(target.geometry,
                NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS).status()
                == DerivedGeometrySupport.Status.UNSUPPORTED_VISUAL).count();
        long unsupportedBoth = targets(profiles).stream().filter(target -> target.profile.supportFor(target.geometry,
                NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS).status()
                == DerivedGeometrySupport.Status.UNSUPPORTED_BOTH).count();
        long expectedTargets = expectedProfiles * 3L;
        helper.assertTrue(behaviorSupported == expectedTargets && visualSupported == expectedTargets
                        && fullyReady == expectedTargets
                        && unsupportedBehavior == 0 && unsupportedVisual == 0 && unsupportedBoth == 0,
                "Final-family support matrix changed: behavior=" + behaviorSupported + ", visual=" + visualSupported
                        + ", ready=" + fullyReady + ", unsupported=" + unsupportedBehavior + "/"
                        + unsupportedVisual + "/" + unsupportedBoth);
        System.out.println("PROFILE_ARCHITECTURE|profiles=" + profiles.size()
                + "|genericSafe=" + genericSafe
                + "|behaviorSupportedTargets=" + behaviorSupported + "|visualSupportedTargets=" + visualSupported
                + "|fullyReadyTargets=" + fullyReady + "|unsupportedBehavior=" + unsupportedBehavior
                + "|unsupportedVisual=" + unsupportedVisual + "|unsupportedBoth=" + unsupportedBoth);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void materialAxisProfileInventoryAndRuntimeBindings(GameTestHelper helper) {
        java.util.Set<Identifier> expectedParents = expectedMaterialAxisParentIds();
        helper.assertTrue(expectedParents.size() == 57,
                "Material-axis expectation must contain exactly 57 canonical parents: " + expectedParents);

        var axisProfiles = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.family() != null)
                .filter(profile -> profile.canonicalParent().defaultBlockState()
                        .hasProperty(BlockStateProperties.AXIS))
                .toList();
        var actualParents = new java.util.LinkedHashSet<Identifier>();
        axisProfiles.forEach(profile -> actualParents.add(profile.canonicalParentId()));
        helper.assertTrue(actualParents.equals(expectedParents),
                "Canonical material-axis inventory changed: expected=" + expectedParents + ", actual="
                        + actualParents);

        int axisTargets = 0;
        for (NibaruMaterialProfile profile : axisProfiles) {
            helper.assertTrue(MaterialAxisState.applies(profile),
                    "MaterialAxisState rejected canonical axis parent " + profile.canonicalParentId());
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                Block derived = NibaruProviderAdapter.derived(profile, geometry).orElseThrow();
                boolean expectedSubtype = switch (geometry) {
                    case VERTICAL_SLAB -> derived instanceof AxisVerticalSlabBlock;
                    case STEP -> derived instanceof AxisStepBlock;
                    case LAYER -> derived instanceof BgeLayerBlock.AxisLayerBlock;
                };
                helper.assertTrue(expectedSubtype,
                        "Axis parent used the wrong derived subclass for " + profile.canonicalParentId() + " "
                                + geometry + ": " + derived.getClass().getName());

                BlockState defaultState = derived.defaultBlockState();
                helper.assertTrue(defaultState.hasProperty(MaterialAxisState.AXIS),
                        "Axis-aware target omitted axis for " + profile.canonicalParentId() + " " + geometry);
                helper.assertTrue(defaultState.getValue(MaterialAxisState.AXIS) == Direction.Axis.Y,
                        "Axis-aware target did not default to Y for " + profile.canonicalParentId() + " "
                                + geometry);
                for (Direction.Axis axis : Direction.Axis.values()) {
                    helper.assertTrue(defaultState.setValue(MaterialAxisState.AXIS, axis)
                                    .getValue(MaterialAxisState.AXIS) == axis,
                            "Axis-aware target cannot expose " + axis + " for " + profile.canonicalParentId()
                                    + " " + geometry);
                }

                Identifier expectedId = expectedMaterialAxisDerivedId(profile, geometry);
                Identifier actualId = BuiltInRegistries.BLOCK.getKey(derived);
                helper.assertTrue(actualId.equals(expectedId),
                        "Wrong derived ID for " + profile.canonicalParentId() + " " + geometry + ": expected="
                                + expectedId + ", actual=" + actualId);
                helper.assertTrue(block(expectedId) == derived,
                        "Derived ID did not resolve back to the runtime block: " + expectedId);
                NibaruProviderAdapter.RuntimeBinding binding = NibaruProviderAdapter.runtimeBinding(derived)
                        .orElseThrow();
                helper.assertTrue(binding.profile() == profile && binding.geometry() == geometry,
                        "Wrong runtime binding for " + expectedId + ": " + binding);
                axisTargets++;
            }
        }
        helper.assertTrue(axisTargets == 171, "Expected 171 axis-aware derived targets, found " + axisTargets);

        int nonAxisTargets = 0;
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.family() != null).toList()) {
            if (profile.canonicalParent().defaultBlockState().hasProperty(BlockStateProperties.AXIS)) continue;
            helper.assertTrue(!MaterialAxisState.applies(profile),
                    "MaterialAxisState accepted non-axis parent " + profile.canonicalParentId());
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                Block derived = NibaruProviderAdapter.derived(profile, geometry).orElseThrow();
                helper.assertTrue(!derived.defaultBlockState().hasProperty(BlockStateProperties.AXIS),
                        "Non-axis profile gained axis state: " + profile.canonicalParentId() + " " + geometry);
                nonAxisTargets++;
            }
        }
        helper.assertTrue(nonAxisTargets == 771,
                "Expected 771 non-axis derived targets, found " + nonAxisTargets);
        System.out.println("MATERIAL_AXIS_INVENTORY|profiles=" + axisProfiles.size() + "|axisTargets="
                + axisTargets + "|nonAxisTargets=" + nonAxisTargets);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void materialAxisAndCnmGeometryTransformIndependently(GameTestHelper helper) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.all().stream()
                .filter(candidate -> candidate.canonicalParentId().equals(id("minecraft:oak_log")))
                .findFirst().orElseThrow();
        AxisVerticalSlabBlock vertical = (AxisVerticalSlabBlock) NibaruProviderAdapter.derived(profile,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        AxisStepBlock step = (AxisStepBlock) NibaruProviderAdapter.derived(profile,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow();

        helper.assertTrue(vertical.defaultBlockState().getValue(MaterialAxisState.AXIS) == Direction.Axis.Y
                        && step.defaultBlockState().getValue(MaterialAxisState.AXIS) == Direction.Axis.Y,
                "Axis-aware CNM geometries did not default to material axis Y");
        for (Direction.Axis axis : Direction.Axis.values()) {
            BlockState verticalState = vertical.defaultBlockState()
                    .setValue(MaterialAxisState.AXIS, axis)
                    .setValue(VerticalSlabBlock.FACING, Direction.WEST)
                    .setValue(VerticalSlabBlock.DOUBLE, true)
                    .setValue(BlockStateProperties.WATERLOGGED, true);
            helper.assertTrue(verticalState.getValue(MaterialAxisState.AXIS) == axis
                            && verticalState.getValue(VerticalSlabBlock.FACING) == Direction.WEST
                            && verticalState.getValue(VerticalSlabBlock.DOUBLE)
                            && verticalState.getValue(BlockStateProperties.WATERLOGGED),
                    "Vertical CNM geometry did not coexist with material axis " + axis);

            BlockState stepState = step.defaultBlockState()
                    .setValue(MaterialAxisState.AXIS, axis)
                    .setValue(StepBlock.FACING, Direction.EAST)
                    .setValue(StepBlock.SLAB_TYPE, SlabType.DOUBLE)
                    .setValue(BlockStateProperties.WATERLOGGED, true);
            helper.assertTrue(stepState.getValue(MaterialAxisState.AXIS) == axis
                            && stepState.getValue(StepBlock.FACING) == Direction.EAST
                            && stepState.getValue(StepBlock.SLAB_TYPE) == SlabType.DOUBLE
                            && stepState.getValue(BlockStateProperties.WATERLOGGED),
                    "Step CNM geometry did not coexist with material axis " + axis);
        }

        BlockState verticalState = vertical.defaultBlockState()
                .setValue(MaterialAxisState.AXIS, Direction.Axis.X)
                .setValue(VerticalSlabBlock.FACING, Direction.WEST)
                .setValue(VerticalSlabBlock.DOUBLE, true)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        BlockState verticalRotated = vertical.rotate(verticalState, Rotation.CLOCKWISE_90);
        helper.assertTrue(verticalRotated.getValue(MaterialAxisState.AXIS) == Direction.Axis.Z
                        && verticalRotated.getValue(VerticalSlabBlock.FACING)
                        == Rotation.CLOCKWISE_90.rotate(Direction.WEST),
                "Vertical quarter-turn did not rotate material X to Z and rotate CNM facing");
        helper.assertTrue(verticalRotated.getValue(VerticalSlabBlock.DOUBLE)
                        && verticalRotated.getValue(BlockStateProperties.WATERLOGGED),
                "Vertical quarter-turn changed double or waterlogged state");

        BlockState stepState = step.defaultBlockState()
                .setValue(MaterialAxisState.AXIS, Direction.Axis.Z)
                .setValue(StepBlock.FACING, Direction.SOUTH)
                .setValue(StepBlock.SLAB_TYPE, SlabType.DOUBLE)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        BlockState stepRotated = step.rotate(stepState, Rotation.COUNTERCLOCKWISE_90);
        helper.assertTrue(stepRotated.getValue(MaterialAxisState.AXIS) == Direction.Axis.X
                        && stepRotated.getValue(StepBlock.FACING)
                        == Rotation.COUNTERCLOCKWISE_90.rotate(Direction.SOUTH),
                "Step quarter-turn did not rotate material Z to X and rotate CNM facing");
        helper.assertTrue(stepRotated.getValue(StepBlock.SLAB_TYPE) == SlabType.DOUBLE
                        && stepRotated.getValue(BlockStateProperties.WATERLOGGED),
                "Step quarter-turn changed type or waterlogged state");

        BlockState verticalMirrored = vertical.mirror(verticalState, Mirror.FRONT_BACK);
        helper.assertTrue(verticalMirrored.getValue(VerticalSlabBlock.FACING)
                        == Mirror.FRONT_BACK.mirror(verticalState.getValue(VerticalSlabBlock.FACING))
                        && verticalMirrored.getValue(VerticalSlabBlock.FACING)
                        != verticalState.getValue(VerticalSlabBlock.FACING),
                "Vertical mirror did not mirror CNM facing");
        assertStateExceptFacing(helper, verticalState, verticalMirrored, VerticalSlabBlock.FACING,
                "Vertical mirror");

        BlockState stepMirrored = step.mirror(stepState, Mirror.LEFT_RIGHT);
        helper.assertTrue(stepMirrored.getValue(StepBlock.FACING)
                        == Mirror.LEFT_RIGHT.mirror(stepState.getValue(StepBlock.FACING))
                        && stepMirrored.getValue(StepBlock.FACING) != stepState.getValue(StepBlock.FACING),
                "Step mirror did not mirror CNM facing");
        assertStateExceptFacing(helper, stepState, stepMirrored, StepBlock.FACING, "Step mirror");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void materialAxisPlacementMatchesFaceAndRetainsMergeAxis(GameTestHelper helper) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.all().stream()
                .filter(candidate -> candidate.canonicalParentId().equals(id("minecraft:oak_log")))
                .findFirst().orElseThrow();
        Block vertical = NibaruProviderAdapter.derived(profile,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block step = NibaruProviderAdapter.derived(profile, DerivedGeometrySupport.Geometry.STEP).orElseThrow();

        for (Block block : java.util.List.of(vertical, step)) {
            for (Direction clickedFace : Direction.values()) {
                helper.assertTrue(MaterialAxisState.placementAxis(Blocks.AIR.defaultBlockState(), block,
                                clickedFace) == clickedFace.getAxis(),
                        "New " + BuiltInRegistries.BLOCK.getKey(block) + " placement did not follow clicked face "
                                + clickedFace);
            }
            for (Direction.Axis existingAxis : Direction.Axis.values()) {
                BlockState existing = block.defaultBlockState().setValue(MaterialAxisState.AXIS, existingAxis);
                for (Direction clickedFace : Direction.values()) {
                    helper.assertTrue(MaterialAxisState.placementAxis(existing, block, clickedFace) == existingAxis,
                            "Merge into " + BuiltInRegistries.BLOCK.getKey(block) + " replaced existing axis "
                                    + existingAxis + " from face " + clickedFace);
                }
            }
            BlockState differentBlock = Blocks.OAK_LOG.defaultBlockState()
                    .setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
            helper.assertTrue(MaterialAxisState.placementAxis(differentBlock, block, Direction.EAST)
                            == Direction.Axis.X,
                    "Placement retained axis from a different block instead of using the clicked face");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void materialAxisCanonicalModelPoliciesAreDataDerived(GameTestHelper helper) {
        Identifier parent = id("minecraft:synthetic_axis_parent");
        JsonObject standard = syntheticAxisBlockState(
                syntheticVariant("minecraft:block/column", 90, 90),
                syntheticVariant("minecraft:block/column", 0, 0),
                syntheticVariant("minecraft:block/column", 90, 0));
        helper.assertTrue(AxisModelContract.uvPolicy(parent, standard) == AxisUvPolicy.STANDARD_ROTATED,
                "Canonical same-model pillar layout was not classified STANDARD_ROTATED");

        JsonObject horizontal = syntheticAxisBlockState(
                syntheticVariant("minecraft:block/column_horizontal", 90, 90),
                syntheticVariant("minecraft:block/column", 0, 0),
                syntheticVariant("minecraft:block/column_horizontal", 90, 0));
        helper.assertTrue(AxisModelContract.uvPolicy(parent, horizontal) == AxisUvPolicy.HORIZONTAL_ROTATED,
                "Canonical horizontal-model pillar layout was not classified HORIZONTAL_ROTATED");

        JsonObject direct = syntheticAxisBlockState(
                syntheticVariant("minecraft:block/column_x", 0, 0),
                syntheticVariant("minecraft:block/column_y", 0, 0),
                syntheticVariant("minecraft:block/column_z", 0, 0));
        helper.assertTrue(AxisModelContract.uvPolicy(parent, direct) == AxisUvPolicy.DIRECT_UV_LOCKED,
                "Canonical coordinate-model pillar layout was not classified DIRECT_UV_LOCKED");

        JsonObject randomized = syntheticRandomizedStandardAxisBlockState();
        helper.assertTrue(AxisModelContract.uvPolicy(parent, randomized) == AxisUvPolicy.STANDARD_ROTATED,
                "Deepslate-style array selectors were not classified STANDARD_ROTATED");
        AxisModelContract.AxisSelector randomizedY = AxisModelContract.axisSelector(
                parent, randomized, Direction.Axis.Y);
        helper.assertTrue(randomizedY.models().equals(java.util.Set.of(
                                "minecraft:block/deepslate", "minecraft:block/deepslate_mirrored"))
                        && randomizedY.transforms().equals(java.util.Set.of(
                                AxisModelContract.VariantTransform.IDENTITY,
                                new AxisModelContract.VariantTransform(0, 180, 0))),
                "Deepslate-style array selector normalization changed: " + randomizedY);

        JsonObject unknown = syntheticAxisBlockState(
                syntheticVariant("minecraft:block/column", 0, 0),
                syntheticVariant("minecraft:block/column", 0, 0),
                syntheticVariant("minecraft:block/column", 0, 0));
        assertAxisPolicyRejected(helper, parent, unknown, "unknown same-model identity layout");

        JsonObject forbiddenUvLock = syntheticAxisBlockState(
                syntheticVariant("minecraft:block/column", 90, 90),
                syntheticVariant("minecraft:block/column", 0, 0),
                syntheticVariant("minecraft:block/column", 90, 0));
        forbiddenUvLock.getAsJsonObject("variants").getAsJsonObject("axis=x")
                .addProperty("uvlock", true);
        assertAxisPolicyRejected(helper, parent, forbiddenUvLock, "blockstate uvlock=true layout");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void materialAxisModelSelectorsResolveAndNormalizeTextures(GameTestHelper helper) {
        int malformedBottomProfiles = 0;
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            if (!AxisModelContract.applies(profile)) continue;
            var textures = AxisModelContract.semanticTextures(profile);
            String expectedSide = axisTexture(profile.textureRoles().side());
            String expectedEnd = axisTexture(profile.textureRoles().top());
            helper.assertTrue(textures.get("side").equals(expectedSide)
                            && textures.get("particle").equals(expectedSide)
                            && textures.get("top").equals(expectedEnd)
                            && textures.get("bottom").equals(expectedEnd),
                    "Axis semantic texture binding changed for " + profile.canonicalParentId() + ": " + textures);
            if (!profile.textureRoles().bottom().equals(profile.textureRoles().top())) {
                malformedBottomProfiles++;
                helper.assertTrue(!textures.containsValue(axisTexture(profile.textureRoles().bottom())),
                        "Axis model retained malformed bottom texture for " + profile.canonicalParentId()
                                + ": " + textures);
            }
        }
        helper.assertTrue(malformedBottomProfiles == 8,
                "Expected exactly eight accepted CUBE_BOTTOM_TOP malformed bottom roles, found "
                        + malformedBottomProfiles);

        for (AxisUvPolicy policy : AxisUvPolicy.values()) {
            NibaruMaterialProfile profile = representativeAxisProfile(policy);
            Identifier verticalShape = id("clutternomore:axis_contract_"
                    + policy.name().toLowerCase(java.util.Locale.ROOT) + "_vertical");
            Identifier stepShape = id("clutternomore:axis_contract_"
                    + policy.name().toLowerCase(java.util.Locale.ROOT) + "_step");
            var verticalSelectors = AxisModelContract.verticalSelectors(verticalShape, policy);
            var stepSelectors = AxisModelContract.stepSelectors(stepShape, policy);
            var verticalModels = AxisModelContract.verticalGeneratedModels(profile, verticalShape, policy);
            var stepModels = AxisModelContract.stepGeneratedModels(profile, stepShape, policy);

            helper.assertTrue(verticalSelectors.size() == 24
                            && AxisModelContract.verticalBlockState(verticalShape, policy)
                            .getAsJsonObject("variants").size() == 24,
                    policy + " Vertical selectors must cover exact facing x double x axis states");
            helper.assertTrue(stepSelectors.size() == 36
                            && AxisModelContract.stepBlockState(stepShape, policy)
                            .getAsJsonObject("variants").size() == 36,
                    policy + " Step selectors must cover exact facing x type x axis states");

            int expectedVerticalModels = policy == AxisUvPolicy.DIRECT_UV_LOCKED ? 21 : 14;
            int expectedStepModels = policy == AxisUvPolicy.DIRECT_UV_LOCKED ? 54 : 36;
            helper.assertTrue(verticalModels.size() == expectedVerticalModels,
                    policy + " Vertical generated-model count changed: " + verticalModels.size());
            helper.assertTrue(stepModels.size() == expectedStepModels,
                    policy + " Step generated-model count changed: " + stepModels.size());

            assertAxisSelectorModelsResolve(helper, verticalSelectors, verticalModels,
                    normalVerticalModelIds(verticalShape), policy, "Vertical");
            assertAxisSelectorModelsResolve(helper, stepSelectors, stepModels,
                    normalStepModelIds(stepShape), policy, "Step");
            assertGeneratedAxisTextures(helper, verticalModels, profile, policy + " Vertical");
            assertGeneratedAxisTextures(helper, stepModels, profile, policy + " Step");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void materialAxisModelFacesAndInverseGeometryRemainCanonical(GameTestHelper helper) {
        Identifier standardShape = id("clutternomore:axis_face_standard");
        NibaruMaterialProfile standardProfile = representativeAxisProfile(AxisUvPolicy.STANDARD_ROTATED);
        var standardSelectors = AxisModelContract.verticalSelectors(
                standardShape, AxisUvPolicy.STANDARD_ROTATED);
        var standardModels = AxisModelContract.verticalGeneratedModels(
                standardProfile, standardShape, AxisUvPolicy.STANDARD_ROTATED);
        JsonObject standardModel = selectedModel(standardSelectors, standardModels,
                AxisModelContract.verticalVariantKey(Direction.NORTH, true, Direction.Axis.X));
        assertColumnFacePolicy(helper, standardModel, false, "STANDARD_ROTATED");

        Identifier horizontalShape = id("clutternomore:axis_face_horizontal");
        NibaruMaterialProfile horizontalProfile = representativeAxisProfile(AxisUvPolicy.HORIZONTAL_ROTATED);
        var horizontalSelectors = AxisModelContract.verticalSelectors(
                horizontalShape, AxisUvPolicy.HORIZONTAL_ROTATED);
        var horizontalModels = AxisModelContract.verticalGeneratedModels(
                horizontalProfile, horizontalShape, AxisUvPolicy.HORIZONTAL_ROTATED);
        JsonObject horizontalModel = selectedModel(horizontalSelectors, horizontalModels,
                AxisModelContract.verticalVariantKey(Direction.NORTH, true, Direction.Axis.Z));
        assertColumnFacePolicy(helper, horizontalModel, true, "HORIZONTAL_ROTATED");

        Identifier directShape = id("clutternomore:axis_face_direct");
        NibaruMaterialProfile directProfile = representativeAxisProfile(AxisUvPolicy.DIRECT_UV_LOCKED);
        var directSelectors = AxisModelContract.verticalSelectors(directShape, AxisUvPolicy.DIRECT_UV_LOCKED);
        var directModels = AxisModelContract.verticalGeneratedModels(
                directProfile, directShape, AxisUvPolicy.DIRECT_UV_LOCKED);
        for (Direction.Axis axis : Direction.Axis.values()) {
            JsonObject model = selectedModel(directSelectors, directModels,
                    AxisModelContract.verticalVariantKey(Direction.NORTH, true, axis));
            assertDirectFacePolicy(helper, model, axis, "DIRECT_UV_LOCKED " + axis);
        }

        for (AxisUvPolicy policy : AxisUvPolicy.values()) {
            NibaruMaterialProfile profile = representativeAxisProfile(policy);
            Identifier verticalShape = id("clutternomore:axis_geometry_"
                    + policy.name().toLowerCase(java.util.Locale.ROOT) + "_vertical");
            Identifier stepShape = id("clutternomore:axis_geometry_"
                    + policy.name().toLowerCase(java.util.Locale.ROOT) + "_step");
            assertVerticalAxisGeometry(helper, policy, verticalShape,
                    AxisModelContract.verticalSelectors(verticalShape, policy),
                    AxisModelContract.verticalGeneratedModels(profile, verticalShape, policy));
            assertStepAxisGeometry(helper, policy, stepShape,
                    AxisModelContract.stepSelectors(stepShape, policy),
                    AxisModelContract.stepGeneratedModels(profile, stepShape, policy));
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void cutoutUniformInventoryAndRenderContract(GameTestHelper helper) {
        var cutout = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.visualProfile() == VisualProfile.CUTOUT_UNIFORM).toList();
        helper.assertTrue(cutout.size() == 8, "Expected exact eight-stage Copper Grate inventory: "
                + cutout.stream().map(NibaruMaterialProfile::canonicalParentId).toList());
        int targets = 0;
        for (NibaruMaterialProfile profile : cutout) {
            helper.assertTrue(profile.oxidationStage().isPresent(), "Cutout profile is not Copper Grate");
            helper.assertTrue(profile.renderLayer() == NibaruMaterialProfile.RenderLayer.CUTOUT,
                    "CUTOUT_UNIFORM profile lacks CUTOUT layer: " + profile.canonicalParentId());
            helper.assertTrue(profile.textureRoles().side().equals(profile.textureRoles().top())
                            && profile.textureRoles().top().equals(profile.textureRoles().bottom())
                            && profile.textureRoles().particle().equals(profile.textureRoles().bottom()),
                    "Copper Grate is not exact uniform/particle texture: " + profile.canonicalParentId());
            if (profile.waxed()) {
                NibaruMaterialProfile unwaxed = CopperSemantics.target(profile,
                        MaterialTransition.Type.UNWAXED).orElseThrow();
                helper.assertTrue(profile.textureRoles().equals(unwaxed.textureRoles()),
                        "Waxed Copper Grate did not reuse its exact unwaxed stage texture");
            }
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                helper.assertTrue(profile.supportFor(geometry, NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                        NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                        "Copper Grate remains withheld: " + profile.family() + " " + geometry);
                helper.assertTrue(NibaruProviderAdapter.derived(profile, geometry).isPresent(),
                        "Copper Grate derived target missing: " + profile.family() + " " + geometry);
                targets++;
            }
            JsonObject model = new JsonObject();
            ProviderVisualAdapter.decorateModel(profile, model);
            helper.assertTrue(model.get("render_type").getAsString().equals("cutout"),
                    "Provider CUTOUT layer did not decorate generated model");
        }
        JsonObject solid = new JsonObject();
        ProviderVisualAdapter.decorateModel(NibaruMaterialProfiles.fromFamily(ModBlocks.CALCITE).orElseThrow(), solid);
        helper.assertTrue(!solid.has("render_type"), "UNIFORM/SOLID model was moved to cutout");
        helper.assertTrue(targets == 24, "Unexpected behavior-supported CUTOUT_UNIFORM target count: " + targets);
        System.out.println("CUTOUT_UNIFORM|profiles=" + cutout.size() + "|fullyReadyTargets=" + targets);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void rootsVisualInventoryResourcesAndOrdering(GameTestHelper helper) {
        var rootsProfiles = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.visualProfile() == VisualProfile.ROOTS).toList();
        helper.assertTrue(rootsProfiles.size() == 1, "Unexpected ROOTS inventory: "
                + rootsProfiles.stream().map(NibaruMaterialProfile::canonicalParentId).toList());
        NibaruMaterialProfile roots = rootsProfiles.getFirst();
        NibaruMaterialProfile muddy = NibaruMaterialProfiles.fromFamily(ModBlocks.MUDDY_MANGROVE_ROOTS)
                .orElseThrow();
        helper.assertTrue(roots.family() == ModBlocks.MANGROVE_ROOTS
                        && roots.canonicalParent() == Blocks.MANGROVE_ROOTS,
                "ROOTS profile is not exact Mangrove Roots");
        helper.assertTrue(roots.nativeSlab().isPresent() && roots.nativeStair().isPresent()
                        && roots.nativeWall().isPresent(), "Mangrove Roots native family is incomplete");
        helper.assertTrue(roots.capabilities().isEmpty(), "Mangrove Roots gained fake behavior capabilities");
        helper.assertTrue(roots.renderLayer() == NibaruMaterialProfile.RenderLayer.CUTOUT
                        && roots.orientationPolicy() == NibaruMaterialProfile.OrientationPolicy.UNIFORM
                        && roots.doubleFormPolicy()
                        == NibaruMaterialProfile.DoubleFormPolicy.COMPOSE_SEMANTIC_SURFACES,
                "ROOTS provider visual metadata changed");
        helper.assertTrue(roots.textureRoles().side().equals("mangrove_roots_side")
                        && roots.textureRoles().top().equals("mangrove_roots_top")
                        && roots.textureRoles().bottom().equals("mangrove_roots_top")
                        && roots.textureRoles().particle().equals("mangrove_roots_side")
                        && roots.tintProfile() == TintProfile.NONE,
                "Mangrove Roots texture/tint contract changed: " + roots.textureRoles());

        var withoutRoots = java.util.EnumSet.copyOf(NibaruProviderAdapter.ADAPTED_VISUALS);
        withoutRoots.remove(VisualProfile.ROOTS);
        for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
            helper.assertTrue(roots.supportFor(geometry, NibaruProviderAdapter.ADAPTED_CAPABILITIES, withoutRoots)
                            .status() == DerivedGeometrySupport.Status.UNSUPPORTED_VISUAL,
                    "ROOTS was not the sole pre-adaptation blocker for " + geometry);
            helper.assertTrue(roots.supportFor(geometry, NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                    NibaruProviderAdapter.ADAPTED_VISUALS).supported(), "ROOTS remains withheld for " + geometry);
            Block derived = NibaruProviderAdapter.derived(roots, geometry).orElseThrow();
            helper.assertTrue(NibaruProviderAdapter.runtimeBinding(derived).orElseThrow().profile() == roots,
                    "ROOTS derived geometry lost exact profile binding");
            BlockState state = derived.defaultBlockState();
            helper.assertTrue(state.hasProperty(BlockStateProperties.WATERLOGGED),
                    "ROOTS derived geometry lost ordinary waterlogging state");
        }
        JsonObject decorated = new JsonObject();
        ProviderVisualAdapter.decorateModel(roots, decorated);
        helper.assertTrue(decorated.get("render_type").getAsString().equals("cutout"),
                "ROOTS model was not decorated CUTOUT");

        helper.assertTrue(muddy.canonicalParent() == Blocks.MUDDY_MANGROVE_ROOTS
                        && muddy != roots && muddy.visualProfile() == VisualProfile.TOP_SIDE_BOTTOM
                        && !muddy.textureRoles().equals(roots.textureRoles()),
                "Muddy Mangrove Roots collapsed into ROOTS semantics");
        var rootsComponent = ShapeMap.getShapes(Blocks.MANGROVE_ROOTS.asItem());
        var rootsSegment = rootsComponent.stream().filter(item -> providerProfile(item).orElse(null) == roots).toList();
        var expected = new java.util.ArrayList<Item>();
        appendExpectedSegment(expected, rootsComponent, roots);
        helper.assertTrue(rootsSegment.equals(expected) && rootsSegment.size() == 9
                        && rootsSegment.getFirst() == Blocks.MANGROVE_ROOTS.asItem()
                        && BuiltInRegistries.ITEM.getKey(rootsSegment.get(5)).getPath().endsWith("mangrove_roots_step")
                        && rootsSegment.get(6) == cornerItem(roots)
                        && rootsSegment.get(7) == columnItem(roots)
                        && rootsSegment.getLast() == layerItem(roots),
                "Mangrove Roots nine-role segment/order mismatch: " + ids(rootsComponent));
        helper.assertTrue(ShapeMap.getShapes(Blocks.MUDDY_MANGROVE_ROOTS.asItem()) != rootsComponent,
                "Muddy Mangrove Roots component merged into Mangrove Roots");
        System.out.println("ROOTS_VISUAL|profiles=1|newVisualTargets=3|newFullyReadyTargets=3|segment="
                + ids(rootsSegment));
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void magmaSemanticsTagsContactBubbleAndOrdering(GameTestHelper helper) throws ReflectiveOperationException {
        var magmaProfiles = NibaruMaterialProfiles.all().stream()
                .filter(p -> p.capabilities().contains(BehaviorCapability.MAGMA_DAMAGE)).toList();
        helper.assertTrue(magmaProfiles.size() == 1, "Unexpected MAGMA_DAMAGE inventory: "
                + magmaProfiles.stream().map(NibaruMaterialProfile::canonicalParentId).toList());
        NibaruMaterialProfile magma = magmaProfiles.getFirst();
        helper.assertTrue(magma.family() == ModBlocks.MAGMA_BLOCK && magma.canonicalParent() == Blocks.MAGMA_BLOCK
                        && magma.nativeSlab().isPresent() && magma.nativeStair().isPresent()
                        && magma.nativeWall().isPresent() && magma.visualProfile() == VisualProfile.UNIFORM
                        && magma.textureRoles().side().equals("magma"), "Magma profile contract changed");
        var before = java.util.EnumSet.copyOf(NibaruProviderAdapter.ADAPTED_CAPABILITIES);
        before.remove(BehaviorCapability.MAGMA_DAMAGE);
        for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
            helper.assertTrue(magma.supportFor(geometry, before, NibaruProviderAdapter.ADAPTED_VISUALS).status()
                            == DerivedGeometrySupport.Status.UNSUPPORTED_BEHAVIOR,
                    "Magma was not blocked solely by MAGMA_DAMAGE before adaptation");
        }
        NibaruMaterialProfile soul = NibaruMaterialProfiles.fromFamily(ModBlocks.SOUL_SAND).orElseThrow();
        helper.assertTrue(soul.supportFor(DerivedGeometrySupport.Geometry.STEP,
                NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                "Soul Sand integration did not remain admitted after the Magma regression");

        Block vertical = NibaruProviderAdapter.derived(magma,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block step = NibaruProviderAdapter.derived(magma, DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        Block layer = NibaruProviderAdapter.derived(magma, DerivedGeometrySupport.Geometry.LAYER).orElseThrow();
        helper.assertTrue(vertical.getClass().getSimpleName().equals("MagmaVerticalSlabBlock")
                        && step.getClass().getSimpleName().equals("MagmaStepBlock")
                        && layer.getClass().getSimpleName().equals("Magma"),
                "Magma derived factories did not install shared semantics");
        for (Block block : java.util.List.of(magma.nativeSlab().orElseThrow(), magma.nativeStair().orElseThrow(),
                magma.nativeWall().orElseThrow(), vertical, step, layer)) {
            helper.assertTrue(block.defaultBlockState().is(net.minecraft.tags.BlockTags.ENABLES_BUBBLE_COLUMN_DRAG_DOWN),
                    "Magma geometry lacks drag-down tag: " + BuiltInRegistries.BLOCK.getKey(block));
        }

        for (Block block : java.util.List.of(magma.nativeSlab().orElseThrow(), magma.nativeStair().orElseThrow(),
                vertical, step, layer)) {
            var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            float health = player.getHealth();
            block.stepOn(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)),
                    block.defaultBlockState(), player);
            helper.assertTrue(player.getHealth() == health - 1.0F, "Magma contact damage mismatch: " + block);
            player.setShiftKeyDown(true);
            health = player.getHealth();
            block.stepOn(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)),
                    block.defaultBlockState(), player);
            helper.assertTrue(player.getHealth() == health, "Careful stepping did not suppress Magma damage");
            player.setShiftKeyDown(false);
        }

        int x = 3;
        for (Block block : java.util.List.of(vertical, step, layer)) {
            BlockPos base = new BlockPos(x++, 1, 1);
            BlockState state = block.defaultBlockState();
            helper.setBlock(base, state);
            helper.setBlock(base.above(), Blocks.WATER);
            invokeTick(block, state, helper, base);
            helper.assertTrue(helper.getBlockState(base.above()).is(Blocks.BUBBLE_COLUMN),
                    "Derived Magma did not create downward bubble column");
            helper.assertTrue(helper.getBlockState(base).is(block), "Bubble update replaced Magma geometry");
        }
        var component = ShapeMap.getShapes(Blocks.MAGMA_BLOCK.asItem());
        var segment = component.stream().filter(item -> providerProfile(item).orElse(null) == magma).toList();
        var expected = new java.util.ArrayList<Item>();
        appendExpectedSegment(expected, component, magma);
        helper.assertTrue(segment.equals(expected) && segment.size() == 9
                        && BuiltInRegistries.ITEM.getKey(segment.get(5)).getPath().endsWith("magma_block_step")
                        && segment.get(6) == cornerItem(magma)
                        && segment.get(7) == columnItem(magma)
                        && segment.getLast() == layer.asItem(),
                "Magma nine-role order mismatch: " + ids(component));
        System.out.println("MAGMA_SEMANTICS|profiles=1|targets=3|tagged=6|segment=" + ids(segment));
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void soulSandCollisionTagsBubbleAndOrdering(GameTestHelper helper) throws ReflectiveOperationException {
        var soulProfiles = NibaruMaterialProfiles.all().stream()
                .filter(p -> p.capabilities().contains(BehaviorCapability.SOUL_SAND_INTERACTION)).toList();
        helper.assertTrue(soulProfiles.size() == 1, "Unexpected Soul Sand profile inventory: " + soulProfiles);
        NibaruMaterialProfile soul = soulProfiles.getFirst();
        helper.assertTrue(soul.family() == ModBlocks.SOUL_SAND && soul.visualProfile() == VisualProfile.UNIFORM,
                "Soul Sand profile identity changed");
        helper.assertTrue(soul.derivedBlockTags().containsAll(java.util.Set.of(
                net.minecraft.tags.BlockTags.SOUL_SPEED_BLOCKS,
                net.minecraft.tags.BlockTags.SOUL_FIRE_BASE_BLOCKS,
                net.minecraft.tags.BlockTags.ENABLES_BUBBLE_COLUMN_PUSH_UP)),
                "Soul Sand provider tag contract incomplete");
        var before = java.util.EnumSet.copyOf(NibaruProviderAdapter.ADAPTED_CAPABILITIES);
        before.remove(BehaviorCapability.SOUL_SAND_INTERACTION);
        for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values())
            helper.assertTrue(soul.supportFor(geometry, before, NibaruProviderAdapter.ADAPTED_VISUALS).status()
                            == DerivedGeometrySupport.Status.UNSUPPORTED_BEHAVIOR,
                    "Soul Sand was not blocked solely by SOUL_SAND_INTERACTION");

        Block vertical = NibaruProviderAdapter.derived(soul,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block step = NibaruProviderAdapter.derived(soul,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        Block layer = NibaruProviderAdapter.derived(soul,
                DerivedGeometrySupport.Geometry.LAYER).orElseThrow();
        helper.assertTrue(vertical.getClass().getSimpleName().equals("SoulSandVerticalSlabBlock")
                        && step.getClass().getSimpleName().equals("SoulSandStepBlock")
                        && layer.getClass().getSimpleName().equals("SoulSand"),
                "Soul Sand factories did not install specialized geometry");
        for (Block block : java.util.List.of(vertical, step, layer)) {
            BlockState state = block.defaultBlockState();
            helper.assertTrue(state.is(net.minecraft.tags.BlockTags.SOUL_SPEED_BLOCKS)
                            && state.is(net.minecraft.tags.BlockTags.SOUL_FIRE_BASE_BLOCKS)
                            && state.is(net.minecraft.tags.BlockTags.ENABLES_BUBBLE_COLUMN_PUSH_UP)
                            && !state.is(net.minecraft.tags.BlockTags.ENABLES_BUBBLE_COLUMN_DRAG_DOWN),
                    "Loaded Soul Sand tags incorrect: " + BuiltInRegistries.BLOCK.getKey(block));
        }

        Block nativeSlab = soul.nativeSlab().orElseThrow();
        for (SlabType type : SlabType.values()) {
            BlockState state = nativeSlab.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, type);
            var bounds = state.getCollisionShape(helper.getLevel(), helper.absolutePos(BlockPos.ZERO),
                    net.minecraft.world.phys.shapes.CollisionContext.empty()).bounds();
            helper.assertTrue(bounds.maxY == (type == SlabType.BOTTOM ? 0.375 : 0.875)
                            && bounds.minY == (type == SlabType.TOP ? 0.5 : 0.0),
                    "Native Soul Sand slab collision changed: " + type + " " + bounds);
        }
        helper.assertTrue(soul.nativeWall().orElseThrow().getClass().getSimpleName().equals("SoulSandWall"),
                "Native Soul Sand wall specialization changed");

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState state = vertical.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            var bounds = state.getCollisionShape(helper.getLevel(), helper.absolutePos(BlockPos.ZERO),
                    net.minecraft.world.phys.shapes.CollisionContext.empty()).bounds();
            helper.assertTrue(bounds.maxY == 0.875 && (bounds.maxX - bounds.minX == 0.5
                            || bounds.maxZ - bounds.minZ == 0.5),
                    "Soul Sand Vertical collision lost half footprint/inset: " + facing + " " + bounds);
        }
        for (SlabType type : SlabType.values()) {
            BlockState state = step.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, type)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
            var shape = state.getCollisionShape(helper.getLevel(), helper.absolutePos(BlockPos.ZERO),
                    net.minecraft.world.phys.shapes.CollisionContext.empty());
            double expectedMaxY = type == SlabType.BOTTOM ? 0.375 : 0.875;
            helper.assertTrue(shape.max(Direction.Axis.Y) == expectedMaxY,
                    "Soul Sand Step surface inset changed: " + type + " " + shape.bounds());
            if (type == SlabType.BOTTOM) helper.assertTrue(shape.bounds().maxY == 0.375,
                    "Lower Soul Sand Step is not six pixels high");
            if (type == SlabType.TOP) helper.assertTrue(shape.bounds().minY == 0.5,
                    "Top Soul Sand Step base changed");
        }

        int x = 3;
        for (Block block : java.util.List.of(vertical, step, layer)) {
            BlockPos base = new BlockPos(x++, 1, 1);
            BlockState state = block.defaultBlockState();
            helper.setBlock(base, state);
            helper.setBlock(base.above(), Blocks.WATER);
            invokeTick(block, state, helper, base);
            helper.assertTrue(helper.getBlockState(base.above()).is(Blocks.BUBBLE_COLUMN)
                            && helper.getBlockState(base.above()).getValue(
                            net.minecraft.world.level.block.BubbleColumnBlock.DRAG_DOWN) == false,
                    "Soul Sand companion did not create upward bubble column");
            helper.assertTrue(helper.getBlockState(base).equals(state), "Bubble update mutated Soul Sand state");
        }
        var segment = ShapeMap.getShapes(Blocks.SOUL_SAND.asItem()).stream()
                .filter(item -> providerProfile(item).orElse(null) == soul).toList();
        helper.assertTrue(segment.size() == 9 && segment.get(5).equals(step.asItem())
                        && segment.get(6).equals(cornerItem(soul))
                        && segment.get(7).equals(columnItem(soul))
                        && segment.getLast().equals(layer.asItem()),
                "Soul Sand nine-role ShapeMap order changed: " + ids(segment));
        System.out.println("SOUL_SAND_SEMANTICS|profiles=1|targets=3|verticalOrientations=4|stepForms=3|segment="
                + ids(segment));
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void derivedMaterialTagsAndPodzolTransitions(GameTestHelper helper) {
        var profiles = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.family() != null).toList();
        int authoredTagRelations = profiles.stream().mapToInt(p -> p.derivedBlockTags().size()).sum();
        helper.assertTrue(authoredTagRelations > 0, "Provider authored-tag inventory is empty");
        for (NibaruMaterialProfile profile : profiles) {
            helper.assertTrue(profile.derivedBlockTags().size() == new java.util.HashSet<>(profile.derivedBlockTags()).size(),
                    "Duplicate authored tag relation: " + profile.family());
        }

        NibaruMaterialProfile soulSoil = NibaruMaterialProfiles.fromFamily(ModBlocks.SOUL_SOIL).orElseThrow();
        helper.assertTrue(soulSoil.derivedBlockTags().containsAll(java.util.Set.of(
                net.minecraft.tags.BlockTags.SOUL_SPEED_BLOCKS,
                net.minecraft.tags.BlockTags.SOUL_FIRE_BASE_BLOCKS)), "Soul Soil material tags missing from provider profile");
        Block soulVertical = NibaruProviderAdapter.derived(soulSoil,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block soulStep = NibaruProviderAdapter.derived(soulSoil,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        Block soulLayer = NibaruProviderAdapter.derived(soulSoil,
                DerivedGeometrySupport.Geometry.LAYER).orElseThrow();
        for (Block block : java.util.List.of(soulVertical, soulStep, soulLayer)) {
            helper.assertTrue(block.defaultBlockState().is(net.minecraft.tags.BlockTags.SOUL_SPEED_BLOCKS),
                    "Loaded Soul Speed tag missing: " + BuiltInRegistries.BLOCK.getKey(block));
            helper.assertTrue(block.defaultBlockState().is(net.minecraft.tags.BlockTags.SOUL_FIRE_BASE_BLOCKS),
                    "Loaded Soul Fire tag missing: " + BuiltInRegistries.BLOCK.getKey(block));
        }

        for (ModBlocks family : java.util.List.of(ModBlocks.DIRT, ModBlocks.GRASS_BLOCK, ModBlocks.COARSE_DIRT,
                ModBlocks.MYCELIUM, ModBlocks.ROOTED_DIRT)) {
            NibaruMaterialProfile source = NibaruMaterialProfiles.fromFamily(family).orElseThrow();
            helper.assertTrue(source.transition(MaterialTransition.Type.PODZOL_GROWTH).orElseThrow().target()
                    == ModBlocks.PODZOL, "Missing typed Podzol transition: " + family);
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                Block from = NibaruProviderAdapter.derived(source, geometry).orElseThrow();
                Block to = NibaruProviderAdapter.derived(
                        NibaruMaterialProfiles.fromFamily(ModBlocks.PODZOL).orElseThrow(), geometry).orElseThrow();
                BlockState state = from.defaultBlockState();
                if (state.hasProperty(BlockStateProperties.WATERLOGGED))
                    state = state.setValue(BlockStateProperties.WATERLOGGED, true);
                BlockState converted = games.twinhead.moreslabsstairsandwalls.block.spreadable
                        .PodzolGeometryConversion.convert(state);
                helper.assertTrue(converted != null && converted.is(to),
                        "Derived Podzol conversion lost geometry: " + family + " " + geometry);
                if (state.hasProperty(BlockStateProperties.WATERLOGGED)) helper.assertTrue(
                        converted.getValue(BlockStateProperties.WATERLOGGED), "Podzol conversion lost waterlogging");
            }
        }
        helper.assertTrue(games.twinhead.moreslabsstairsandwalls.block.spreadable.PodzolGeometryConversion
                .convert(Blocks.STONE.defaultBlockState()) == null, "Unrelated material converted to Podzol");
        System.out.println("DERIVED_MATERIAL_PARITY|profiles=314|authoredTagRelations=" + authoredTagRelations
                + "|soulSoilTags=2|podzolFamilies=6|podzolTargets=15");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void pointedDripstoneCompanionExternalParity(GameTestHelper helper) throws ReflectiveOperationException {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromFamily(ModBlocks.DRIPSTONE_BLOCK).orElseThrow();
        helper.assertTrue(profile.canonicalParent() == Blocks.DRIPSTONE_BLOCK
                        && profile.nativeSlab().isPresent() && profile.nativeStair().isPresent()
                        && profile.nativeWall().isPresent()
                        && profile.externalSemanticRequirements().contains(
                        games.twinhead.moreslabsstairsandwalls.api.material.ExternalSemanticRequirement
                                .POINTED_DRIPSTONE_GROWTH),
                "Canonical Dripstone external requirement changed");
        Block vertical = NibaruProviderAdapter.derived(profile,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block step = NibaruProviderAdapter.derived(profile,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        helper.assertTrue(NibaruProviderAdapter.runtimeBinding(vertical).orElseThrow().profile() == profile
                        && NibaruProviderAdapter.runtimeBinding(step).orElseThrow().profile() == profile,
                "Dripstone companion binding is not exact");

        BlockPos drip = new BlockPos(2, 1, 2);
        for (Block nativeBlock : java.util.List.of(profile.nativeSlab().orElseThrow(),
                profile.nativeStair().orElseThrow(), profile.nativeWall().orElseThrow())) {
            BlockState wet = nativeBlock.defaultBlockState();
            if (wet.hasProperty(BlockStateProperties.WATERLOGGED))
                wet = wet.setValue(BlockStateProperties.WATERLOGGED, true);
            helper.setBlock(drip.above(), wet);
            helper.setBlock(drip.above(2), Blocks.AIR);
            helper.assertTrue(invokePointedDripstoneCanGrow(helper, drip),
                    "Native waterlogged Dripstone geometry stopped supporting growth: " + nativeBlock);
            helper.setBlock(drip.above(), wet.hasProperty(BlockStateProperties.WATERLOGGED)
                    ? wet.setValue(BlockStateProperties.WATERLOGGED, false) : wet);
            helper.assertTrue(!invokePointedDripstoneCanGrow(helper, drip),
                    "Native dry Dripstone geometry incorrectly supported growth: " + nativeBlock);
        }

        java.util.List<BlockState> companionStates = new java.util.ArrayList<>();
        BlockState verticalState = vertical.defaultBlockState();
        if (verticalState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            for (Direction direction : Direction.Plane.HORIZONTAL)
                companionStates.add(verticalState.setValue(BlockStateProperties.HORIZONTAL_FACING, direction));
        } else companionStates.add(verticalState);
        BlockState stepState = step.defaultBlockState();
        for (SlabType type : SlabType.values()) {
            BlockState state = stepState.setValue(BlockStateProperties.SLAB_TYPE, type);
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
            companionStates.add(state);
        }

        for (BlockState original : companionStates) {
            BlockState wet = original;
            if (wet.hasProperty(BlockStateProperties.WATERLOGGED)
                    && (!wet.hasProperty(BlockStateProperties.SLAB_TYPE)
                    || wet.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE))
                wet = wet.setValue(BlockStateProperties.WATERLOGGED, true);
            helper.setBlock(drip.above(), wet);
            helper.setBlock(drip.above(2), Blocks.AIR);
            if (wet.getFluidState().is(net.minecraft.tags.FluidTags.WATER))
                helper.assertTrue(invokePointedDripstoneCanGrow(helper, drip),
                        "Waterlogged companion Dripstone state rejected: " + wet);

            BlockState dry = wet.hasProperty(BlockStateProperties.WATERLOGGED)
                    ? wet.setValue(BlockStateProperties.WATERLOGGED, false) : wet;
            helper.setBlock(drip.above(), dry);
            helper.setBlock(drip.above(2), Blocks.WATER);
            helper.assertTrue(invokePointedDripstoneCanGrow(helper, drip),
                    "Source-water companion Dripstone state rejected: " + dry);
            helper.setBlock(drip.above(2), Blocks.AIR);
            helper.assertTrue(!invokePointedDripstoneCanGrow(helper, drip),
                    "Dry companion Dripstone state incorrectly accepted: " + dry);
            helper.assertTrue(helper.getBlockState(drip.above()).equals(dry),
                    "Growth query mutated companion Dripstone state");
        }

        NibaruMaterialProfile calcite = NibaruMaterialProfiles.fromFamily(ModBlocks.CALCITE).orElseThrow();
        for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
            Block unrelated = NibaruProviderAdapter.derived(calcite, geometry).orElseThrow();
            BlockState state = unrelated.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.WATERLOGGED))
                state = state.setValue(BlockStateProperties.WATERLOGGED, true);
            helper.setBlock(drip.above(), state);
            helper.setBlock(drip.above(2), Blocks.AIR);
            helper.assertTrue(!invokePointedDripstoneCanGrow(helper, drip),
                    "Unrelated companion geometry became Dripstone support");
        }
        System.out.println("POINTED_DRIPSTONE_PARITY|native=3|verticalOrientations="
                + (companionStates.size() - 3) + "|stepForms=3|waterPaths=2");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void providerParentsOwnContiguousShapeMapSegments(GameTestHelper helper) {
        var visited = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<java.util.List<Item>, Boolean>());
        int multiParentComponents = 0;
        for (NibaruMaterialProfile seed : NibaruMaterialProfiles.all()) {
            var component = ShapeMap.getShapes(seed.canonicalParent().asItem());
            if (!visited.add(component)) continue;
            var profiles = new java.util.ArrayList<NibaruMaterialProfile>();
            var seen = java.util.Collections.newSetFromMap(
                    new java.util.IdentityHashMap<NibaruMaterialProfile, Boolean>());
            for (Item item : component) providerProfile(item).filter(seen::add).ifPresent(profiles::add);
            if (profiles.size() > 1) multiParentComponents++;
            var expected = new java.util.ArrayList<Item>();
            for (NibaruMaterialProfile profile : profiles) appendExpectedSegment(expected, component, profile);
            var actual = component.stream().filter(item -> providerProfile(item).isPresent()).toList();
            helper.assertTrue(actual.equals(expected),
                    "Provider parents are not segmented in " + seed.canonicalParentId() + ": " + ids(component));
        }
        helper.assertTrue(multiParentComponents >= 20,
                "Expected the structural log/wood component matrix, found " + multiParentComponents);
        var oak = ShapeMap.getShapes(Items.OAK_LOG);
        NibaruMaterialProfile oakLog = NibaruMaterialProfiles.fromFamily(ModBlocks.OAK_LOG).orElseThrow();
        NibaruMaterialProfile oakWood = NibaruMaterialProfiles.fromFamily(ModBlocks.OAK_WOOD).orElseThrow();
        helper.assertTrue(oak.indexOf(Items.OAK_LOG) == 0 && oak.indexOf(Items.OAK_WOOD) == 9
                        && BuiltInRegistries.ITEM.getKey(oak.get(5)).getPath().endsWith("oak_log_step")
                        && oak.get(6) == cornerItem(oakLog)
                        && oak.get(7) == columnItem(oakLog)
                        && oak.get(8) == layerItem(oakLog)
                        && BuiltInRegistries.ITEM.getKey(oak.get(14)).getPath().endsWith("oak_wood_step")
                        && oak.get(15) == cornerItem(oakWood)
                        && oak.get(16) == columnItem(oakWood)
                        && oak.getLast() == layerItem(oakWood),
                "Oak log/wood parent segments are not canonical: " + ids(oak));
        System.out.println("PARENT_SEGMENT_ORDER|multiParentComponents=" + multiParentComponents
                + "|oak=" + ids(oak));
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void sparseSourcesBindAndOrderExactFamilies(GameTestHelper helper) {
        var profiles = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.family() != null).toList();
        long slabs = profiles.stream().filter(profile -> profile.nativeSlab().isPresent()).count();
        long stairs = profiles.stream().filter(profile -> profile.nativeStair().isPresent()).count();
        long walls = profiles.stream().filter(profile -> profile.nativeWall().isPresent()).count();
        var sparse = profiles.stream().filter(profile -> profile.nativeSlab().isEmpty()
                || profile.nativeStair().isEmpty() || profile.nativeWall().isEmpty()).toList();
        helper.assertTrue(slabs == 280 && stairs == 283 && walls == 314,
                "Native catalog changed: " + slabs + "/" + stairs + "/" + walls);
        helper.assertTrue(sparse.size() == 34, "Expected 34 sparse profiles, found " + sparse.size());

        NibaruMaterialProfile stone = NibaruMaterialProfiles.fromFamily(ModBlocks.STONE).orElseThrow();
        NibaruMaterialProfile oak = NibaruMaterialProfiles.fromFamily(ModBlocks.OAK_PLANKS).orElseThrow();
        helper.assertTrue(stone.effectiveSlabSource().orElseThrow() == Blocks.STONE_SLAB
                        && stone.effectiveStairSource().orElseThrow() == Blocks.STONE_STAIRS,
                "Stone did not use exact vanilla effective sources");
        helper.assertTrue(oak.effectiveSlabSource().orElseThrow() == Blocks.OAK_SLAB
                        && oak.effectiveStairSource().orElseThrow() == Blocks.OAK_STAIRS,
                "Oak Planks did not use exact vanilla effective sources");
        helper.assertTrue(NibaruMaterialProfiles.fromBlock(Blocks.STONE_SLAB).orElseThrow() == stone
                        && NibaruMaterialProfiles.fromBlock(Blocks.OAK_STAIRS).orElseThrow() == oak,
                "Exact effective-source reverse lookup failed");
        helper.assertTrue(NibaruMaterialProfiles.fromBlock(ModBlocks.CALCITE.getBlock(ModBlocks.BlockType.SLAB)).orElseThrow()
                        == NibaruMaterialProfiles.fromFamily(ModBlocks.CALCITE).orElseThrow(),
                "Complete native family lookup regressed");
        NibaruMaterialProfile paleOak = NibaruMaterialProfiles.fromFamily(ModBlocks.PALE_OAK_PLANKS).orElseThrow();
        helper.assertTrue(paleOak.effectiveSlabSource().orElseThrow() == Blocks.PALE_OAK_SLAB
                        && paleOak.effectiveStairSource().orElseThrow() == Blocks.PALE_OAK_STAIRS
                        && NibaruMaterialProfiles.fromBlock(Blocks.PALE_OAK_SLAB).orElseThrow() == paleOak,
                "Pale Oak Planks did not reconcile its vanilla slab/stair sources");

        for (NibaruMaterialProfile profile : sparse) {
            helper.assertTrue(profile.effectiveSlabSource().isPresent(),
                    "Sparse slab source absent: " + profile.canonicalParentId());
            helper.assertTrue(profile.effectiveStairSource().isPresent(),
                    "Sparse stair source absent: " + profile.canonicalParentId());
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                var support = profile.supportFor(geometry, NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                        NibaruProviderAdapter.ADAPTED_VISUALS);
                helper.assertTrue(support.supported(), "Sparse target remains blocked: "
                        + profile.canonicalParentId() + " " + geometry + " " + support.status());
                Block derived = NibaruProviderAdapter.derived(profile, geometry).orElseThrow();
                helper.assertTrue(NibaruProviderAdapter.runtimeBinding(derived).orElseThrow().profile() == profile,
                        "Sparse derived binding mismatch: " + profile.canonicalParentId() + " " + geometry);
            }
            var component = ShapeMap.getShapes(profile.canonicalParent().asItem());
            var expected = new java.util.ArrayList<Item>();
            appendExpectedSegment(expected, component, profile);
            var actual = component.stream().filter(item -> providerProfile(item).orElse(null) == profile).toList();
            helper.assertTrue(actual.equals(expected), "Sparse semantic order mismatch: "
                    + profile.canonicalParentId() + " " + ids(component));
        }
        helper.assertTrue(oak.nativeWall().isPresent(), "Oak Planks wall is not registered");
        helper.assertTrue(java.util.Collections.frequency(ShapeMap.getShapes(Items.OAK_PLANKS),
                oak.nativeWall().orElseThrow().asItem()) == 1, "Oak Planks wall is not present exactly once");
        System.out.println("SPARSE_RECONCILIATION|profiles=35|native=279/282/314|targets=105");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void paleCounterpartsHaveExactProfilesAdmissionAndOrder(GameTestHelper helper) {
        var families = java.util.List.of(ModBlocks.STRIPPED_PALE_OAK_LOG, ModBlocks.STRIPPED_PALE_OAK_WOOD,
                ModBlocks.PALE_OAK_LOG, ModBlocks.PALE_OAK_WOOD, ModBlocks.PALE_OAK_LEAVES,
                ModBlocks.PALE_OAK_PLANKS, ModBlocks.PALE_MOSS_BLOCK);
        var parents = java.util.List.of(Blocks.STRIPPED_PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_WOOD,
                Blocks.PALE_OAK_LOG, Blocks.PALE_OAK_WOOD, Blocks.PALE_OAK_LEAVES,
                Blocks.PALE_OAK_PLANKS, Blocks.PALE_MOSS_BLOCK);
        var seenProfiles = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<NibaruMaterialProfile, Boolean>());
        var derived = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Block, Boolean>());
        for (int index = 0; index < families.size(); index++) {
            NibaruMaterialProfile profile = NibaruMaterialProfiles.fromFamily(families.get(index)).orElseThrow();
            helper.assertTrue(profile.canonicalParent() == parents.get(index),
                    "Pale counterpart parent mismatch: " + profile.canonicalParentId());
            helper.assertTrue(seenProfiles.add(profile), "Duplicate Pale material profile: " + profile.family());
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                helper.assertTrue(profile.supportFor(geometry, NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                        NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                        "Pale target remains withheld: " + profile.family() + " " + geometry);
                helper.assertTrue(derived.add(NibaruProviderAdapter.derived(profile, geometry).orElseThrow()),
                        "Pale target recursively reused another geometry: " + profile.family() + " " + geometry);
            }
        }
        helper.assertTrue(derived.size() == 21, "Expected exact twenty-one Pale companion targets: " + derived.size());

        NibaruMaterialProfile planks = NibaruMaterialProfiles.fromFamily(ModBlocks.PALE_OAK_PLANKS).orElseThrow();
        NibaruMaterialProfile darkPlanks = NibaruMaterialProfiles.fromFamily(ModBlocks.DARK_OAK_PLANKS).orElseThrow();
        helper.assertTrue(planks.nativeSlab().isEmpty() && planks.nativeStair().isEmpty()
                        && planks.nativeWall().isPresent()
                        && planks.effectiveSlabSource().orElseThrow() == Blocks.PALE_OAK_SLAB
                        && planks.effectiveStairSource().orElseThrow() == Blocks.PALE_OAK_STAIRS,
                "Pale Oak Planks did not remain exact wall-only native coverage");
        helper.assertTrue(planks.visualProfile() == darkPlanks.visualProfile()
                        && planks.textureRoles().side().equals("pale_oak_planks"),
                "Pale Oak Planks differs from its Dark Oak counterpart classification");

        var plankShapes = ShapeMap.getShapes(Blocks.PALE_OAK_PLANKS.asItem());
        var paleBeam = dev.aero.cnmterraincompat.ExternalMaterialFamilies.fromSource(id("bbb:pale_oak_beam"))
                .orElseThrow();
        var expectedPlankShapes = new java.util.ArrayList<Item>(
                dev.aero.cnmterraincompat.ExplicitShapeMapFamilies.variationItems(planks));
        expectedPlankShapes.addAll(
                dev.aero.cnmterraincompat.ExplicitShapeMapFamilies.variationItems(paleBeam.profile()));
        helper.assertTrue(plankShapes.equals(expectedPlankShapes),
                "Pale Oak Planks/Beam selector is not the exact [Plank][Beam] sequence: "
                        + ids(plankShapes));
        Item bbbOriginalSlab = BuiltInRegistries.ITEM.getValue(id("bbb:pale_oak_beam_slab"));
        Item bbbOriginalStairs = BuiltInRegistries.ITEM.getValue(id("bbb:pale_oak_beam_stairs"));
        helper.assertTrue(bbbOriginalSlab != null && bbbOriginalStairs != null
                        && bbbOriginalSlab != paleBeam.slab().asItem()
                        && bbbOriginalStairs != paleBeam.stairs().asItem()
                        && !plankShapes.contains(bbbOriginalSlab)
                        && !plankShapes.contains(bbbOriginalStairs),
                "BBB provider originals must remain registered but outside the exact canonical selector: "
                        + ids(plankShapes));

        var paleLogShapes = ShapeMap.getShapes(Blocks.PALE_OAK_LOG.asItem());
        NibaruMaterialProfile paleLog = NibaruMaterialProfiles.fromFamily(ModBlocks.PALE_OAK_LOG).orElseThrow();
        NibaruMaterialProfile paleWood = NibaruMaterialProfiles.fromFamily(ModBlocks.PALE_OAK_WOOD).orElseThrow();
        helper.assertTrue(paleLogShapes.size() == 18 && paleLogShapes.getFirst() == Blocks.PALE_OAK_LOG.asItem()
                        && paleLogShapes.get(9) == Blocks.PALE_OAK_WOOD.asItem()
                        && BuiltInRegistries.ITEM.getKey(paleLogShapes.get(5)).getPath().endsWith("pale_oak_log_step")
                        && paleLogShapes.get(6) == cornerItem(paleLog)
                        && paleLogShapes.get(7) == columnItem(paleLog)
                        && paleLogShapes.get(8) == layerItem(paleLog)
                        && BuiltInRegistries.ITEM.getKey(paleLogShapes.get(14)).getPath().endsWith("pale_oak_wood_step")
                        && paleLogShapes.get(15) == cornerItem(paleWood)
                        && paleLogShapes.get(16) == columnItem(paleWood)
                        && paleLogShapes.getLast() == layerItem(paleWood),
                "Pale Oak log/wood parent segments changed: " + ids(paleLogShapes));
        System.out.println("PALE_COVERAGE|profiles=7|nativeBlocks=19|companionTargets=21|matrix=933");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void paleMaterialSemanticsPreserveStrippingLeavesAndMoss(GameTestHelper helper) {
        NibaruMaterialProfile log = NibaruMaterialProfiles.fromFamily(ModBlocks.PALE_OAK_LOG).orElseThrow();
        NibaruMaterialProfile stripped = NibaruMaterialProfiles.fromFamily(ModBlocks.STRIPPED_PALE_OAK_LOG)
                .orElseThrow();
        helper.assertTrue(log.visualProfile() == VisualProfile.PILLAR
                        && log.textureRoles().side().equals("pale_oak_log")
                        && log.textureRoles().top().equals("pale_oak_log_top")
                        && stripped.textureRoles().side().equals("stripped_pale_oak_log")
                        && stripped.textureRoles().top().equals("stripped_pale_oak_log_top")
                        && log.transition(MaterialTransition.Type.STRIPPED).orElseThrow().target()
                                == ModBlocks.STRIPPED_PALE_OAK_LOG,
                "Pale Oak log texture/stripping roles are not exact");

        Block vertical = NibaruProviderAdapter.derived(log,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        BlockState verticalState = vertical.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.WEST)
                .setValue(VerticalSlabBlock.DOUBLE, false)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        BlockPos stripPos = new BlockPos(1, 1, 1);
        helper.setBlock(stripPos, verticalState);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var axe = new net.minecraft.world.item.ItemStack(Items.IRON_AXE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, axe);
        NibaruProviderAdapter.useComposedCapabilities(vertical, axe, verticalState, helper.getLevel(),
                helper.absolutePos(stripPos), player, net.minecraft.world.InteractionHand.MAIN_HAND).orElseThrow();
        helper.assertTrue(helper.getBlockState(stripPos).is(NibaruProviderAdapter.derived(stripped,
                        DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow()),
                "Pale Oak Vertical did not strip to exact Pale geometry");
        helper.assertTrue(axe.getDamageValue() == 1, "Pale Oak stripping did not damage the axe exactly once");
        assertSharedState(helper, verticalState, helper.getBlockState(stripPos), "Pale Oak Vertical stripping");

        NibaruMaterialProfile leaves = NibaruMaterialProfiles.fromFamily(ModBlocks.PALE_OAK_LEAVES).orElseThrow();
        Block leafVertical = NibaruProviderAdapter.derived(leaves,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block leafStep = NibaruProviderAdapter.derived(leaves, DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        helper.assertTrue(leaves.capabilities().contains(BehaviorCapability.LEAF_LIFECYCLE)
                        && leaves.tintProfile() == TintProfile.NONE
                        && leaves.textureRoles().side().equals("pale_oak_leaves")
                        && leafVertical.getClass().getSimpleName().equals("NibaruLeavesVerticalSlabBlock")
                        && leafStep.getClass().getSimpleName().equals("NibaruLeavesStepBlock"),
                "Pale Oak Leaves did not reuse the shared untinted leaf lifecycle");
        helper.assertTrue(LeafSemantics.applyPlayerPlacementState(leafVertical.defaultBlockState())
                        .getValue(BlockStateProperties.PERSISTENT),
                "Pale Oak Leaves player-placement state is not persistent");
        BlockPos verticalDecay = new BlockPos(2, 1, 1);
        BlockPos stepDecay = new BlockPos(3, 1, 1);
        BlockState decayVertical = nonpersistent(leafVertical.defaultBlockState());
        BlockState decayStep = nonpersistent(leafStep.defaultBlockState());
        helper.setBlock(verticalDecay, decayVertical);
        helper.setBlock(stepDecay, decayStep);
        LeafSemantics.decayIfNeeded(decayVertical, helper.getLevel(), helper.absolutePos(verticalDecay));
        LeafSemantics.decayIfNeeded(decayStep, helper.getLevel(), helper.absolutePos(stepDecay));
        helper.assertBlockPresent(Blocks.AIR, verticalDecay);
        helper.assertBlockPresent(Blocks.AIR, stepDecay);

        NibaruMaterialProfile paleMoss = NibaruMaterialProfiles.fromFamily(ModBlocks.PALE_MOSS_BLOCK).orElseThrow();
        NibaruMaterialProfile moss = NibaruMaterialProfiles.fromFamily(ModBlocks.MOSS_BLOCK).orElseThrow();
        helper.assertTrue(paleMoss.visualProfile() == moss.visualProfile()
                        && paleMoss.capabilities().equals(moss.capabilities())
                        && paleMoss.textureRoles().side().equals("pale_moss_block"),
                "Pale Moss differs from ordinary Moss classification or texture identity");
        System.out.println("PALE_SEMANTICS|stripping=PASS|leafLifecycle=PASS|paleMoss=PASS");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void allLeafProfilesUseCapabilityAdapter(GameTestHelper helper) {
        var leaves = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.family() != null)
                .filter(profile -> profile.capabilities().contains(BehaviorCapability.LEAF_LIFECYCLE)).toList();
        helper.assertTrue(leaves.size() == 11, "Expected 11 true leaf-lifecycle profiles, found " + leaves.size());
        NibaruMaterialProfile oak = NibaruMaterialProfiles.fromFamily(ModBlocks.OAK_LEAVES).orElseThrow();
        NibaruMaterialProfile spruce = NibaruMaterialProfiles.fromFamily(ModBlocks.SPRUCE_LEAVES).orElseThrow();
        helper.assertTrue(oak.supportFor(DerivedGeometrySupport.Geometry.VERTICAL_SLAB,
                NibaruProviderAdapter.ADAPTED_CAPABILITIES).status()
                == DerivedGeometrySupport.Status.SUPPORTED_WITH_CAPABILITIES, "Oak leaf profile not supported");
        helper.assertTrue(spruce.supportFor(DerivedGeometrySupport.Geometry.VERTICAL_SLAB,
                NibaruProviderAdapter.ADAPTED_CAPABILITIES).status()
                == DerivedGeometrySupport.Status.SUPPORTED_WITH_CAPABILITIES, "Spruce leaf profile not on shared path");
        helper.assertTrue(NibaruProviderAdapter.admissionSize(spruce.nativeSlab().orElseThrow().defaultBlockState()) == 2,
                "Non-Oak leaf admission still depends on property count");
        helper.assertTrue(NibaruProviderAdapter.derived(spruce,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow().getClass()
                == NibaruProviderAdapter.derived(oak, DerivedGeometrySupport.Geometry.VERTICAL_SLAB)
                .orElseThrow().getClass(), "Oak and Spruce did not select the same leaf adapter class");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void pathProfileAndLoweredGeometryContract(GameTestHelper helper) {
        NibaruMaterialProfile path = NibaruMaterialProfiles.fromFamily(ModBlocks.DIRT_PATH).orElseThrow();
        helper.assertTrue(path.capabilities().equals(java.util.Set.of(BehaviorCapability.PATH_CONVERSION)),
                "Dirt Path must declare only PATH_CONVERSION");
        helper.assertTrue(path.transitions().stream().anyMatch(t -> t.type() == MaterialTransition.Type.PATH_REVERSION
                        && t.target() == ModBlocks.DIRT), "Dirt Path lacks typed Dirt reversion");
        for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
            helper.assertTrue(path.supportFor(geometry, NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                    NibaruProviderAdapter.ADAPTED_VISUALS).status()
                    == DerivedGeometrySupport.Status.SUPPORTED_WITH_CAPABILITIES,
                    "Dirt Path geometry remains withheld: " + geometry);
        }
        Block vertical = block(CNM_PATH_VERTICAL);
        Block step = block(CNM_PATH_STEP);
        helper.assertTrue(vertical.getClass().getSimpleName().equals("PathVerticalSlabBlock"),
                "Vertical did not select capability adapter");
        helper.assertTrue(step.getClass().getSimpleName().equals("PathStepBlock"),
                "Step did not select capability adapter");
        var collision = net.minecraft.world.phys.shapes.CollisionContext.empty();
        BlockPos absolute = helper.absolutePos(new BlockPos(1, 1, 1));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState state = vertical.defaultBlockState().setValue(VerticalSlabBlock.FACING, direction);
            var bounds = ((VerticalSlabBlock) vertical).getShape(state, helper.getLevel(), absolute, collision).bounds();
            helper.assertTrue(bounds.maxY == 15.0 / 16.0, "Path Vertical height changed");
        }
        for (SlabType type : SlabType.values()) {
            BlockState state = step.defaultBlockState().setValue(StepBlock.SLAB_TYPE, type)
                    .setValue(StepBlock.FACING, Direction.EAST);
            var bounds = ((StepBlock) step).getShape(state, helper.getLevel(), absolute, collision).bounds();
            helper.assertTrue(bounds.maxY == (type == SlabType.BOTTOM ? 7.0 : 15.0) / 16.0,
                    "Path Step height changed for " + type + ": " + bounds.maxY);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void flattenablePathLifecycleIsProviderDeclared(GameTestHelper helper) {
        var flattenable = NibaruMaterialProfiles.all().stream().filter(p ->
                p.capabilities().contains(BehaviorCapability.FLATTENABLE_TO_PATH)).toList();
        helper.assertTrue(flattenable.size() == 6, "Expected all six native shovel-flattenable profiles: "
                + flattenable.stream().map(NibaruMaterialProfile::canonicalParentId).toList());
        for (ModBlocks family : java.util.List.of(ModBlocks.DIRT, ModBlocks.GRASS_BLOCK,
                ModBlocks.MYCELIUM, ModBlocks.PODZOL, ModBlocks.COARSE_DIRT, ModBlocks.ROOTED_DIRT)) {
            NibaruMaterialProfile profile = NibaruMaterialProfiles.fromFamily(family).orElseThrow();
            helper.assertTrue(profile.transitions().stream().anyMatch(t ->
                    t.type() == MaterialTransition.Type.PATH_TARGET && t.target() == ModBlocks.DIRT_PATH),
                    family + " lacks typed Path target");
            String adapter = NibaruProviderAdapter.derived(profile,
                    DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow().getClass().getSimpleName();
            helper.assertTrue(adapter.equals("DirtVerticalSlab") || adapter.equals("GrassVerticalSlab"),
                    family + " lacks flattening adapter: " + adapter);
        }
        helper.assertTrue(!NibaruMaterialProfiles.fromFamily(ModBlocks.CALCITE).orElseThrow().capabilities()
                .contains(BehaviorCapability.FLATTENABLE_TO_PATH), "Calcite became flattenable");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void existingGrassVerticalConsumesProviderCapabilities(GameTestHelper helper) {
        Block existing = block(id("cnm_terrain_slabs_compat:grass_vertical_slab"));
        NibaruProviderAdapter.RuntimeBinding binding = NibaruProviderAdapter.runtimeBinding(existing).orElseThrow();
        helper.assertTrue(binding.profile() == NibaruMaterialProfiles.fromFamily(ModBlocks.GRASS_BLOCK).orElseThrow(),
                "Historical Grass Vertical did not resolve the canonical Grass profile");
        helper.assertTrue(binding.geometry() == DerivedGeometrySupport.Geometry.VERTICAL_SLAB,
                "Historical Grass Vertical did not retain its vertical-slab role");
        helper.assertTrue(binding.profile().capabilities().contains(BehaviorCapability.SPREADABLE),
                "Historical Grass Vertical did not compose SPREADABLE");
        helper.assertTrue(binding.profile().capabilities().contains(BehaviorCapability.FLATTENABLE_TO_PATH),
                "Historical Grass Vertical did not compose FLATTENABLE_TO_PATH");
        helper.assertTrue(NibaruProviderAdapter.derived(binding.profile(), binding.geometry()).orElseThrow() == existing,
                "Historical Grass Vertical registry owner was replaced or duplicated");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void pathConversionPreservesCnmGeometryAndWater(GameTestHelper helper) throws ReflectiveOperationException {
        Block pathVertical = block(CNM_PATH_VERTICAL);
        Block dirtVertical = block(CNM_DIRT_VERTICAL);
        Block pathStep = block(CNM_PATH_STEP);
        Block dirtStep = block(CNM_DIRT_STEP);
        BlockPos verticalPos = new BlockPos(1, 1, 1);
        BlockPos stepPos = new BlockPos(3, 1, 1);
        BlockPos lowStepPos = new BlockPos(5, 1, 1);
        BlockState vertical = pathVertical.defaultBlockState().setValue(VerticalSlabBlock.FACING, Direction.WEST)
                .setValue(VerticalSlabBlock.WATERLOGGED, true);
        BlockState step = pathStep.defaultBlockState().setValue(StepBlock.FACING, Direction.SOUTH)
                .setValue(StepBlock.SLAB_TYPE, SlabType.TOP).setValue(StepBlock.WATERLOGGED, true);
        helper.setBlock(verticalPos, vertical);
        helper.setBlock(stepPos, step);
        BlockState lowStep = pathStep.defaultBlockState().setValue(StepBlock.FACING, Direction.NORTH)
                .setValue(StepBlock.SLAB_TYPE, SlabType.BOTTOM);
        helper.setBlock(lowStepPos, lowStep);
        helper.setBlock(verticalPos.above(), Blocks.STONE);
        helper.setBlock(stepPos.above(), Blocks.STONE);
        helper.setBlock(lowStepPos.above(), Blocks.STONE);
        helper.assertTrue(!PathSemantics.canSurvive(vertical, helper.getLevel(), helper.absolutePos(verticalPos)),
                "Solid cover unexpectedly permits Dirt Path");
        invokeTick(pathVertical, vertical, helper, verticalPos);
        invokeTick(pathStep, step, helper, stepPos);
        invokeTick(pathStep, lowStep, helper, lowStepPos);
        BlockState convertedVertical = helper.getBlockState(verticalPos);
        BlockState convertedStep = helper.getBlockState(stepPos);
        helper.assertTrue(convertedVertical.is(dirtVertical), "Path Vertical did not convert to canonical Dirt Vertical");
        helper.assertTrue(convertedStep.is(dirtStep), "Path Step did not convert to canonical Dirt Step");
        helper.assertTrue(helper.getBlockState(lowStepPos).is(pathStep),
                "Exposed low Path Step reverted solely because pos.above was occupied");
        helper.assertTrue(convertedVertical.getValue(VerticalSlabBlock.FACING) == Direction.WEST
                && convertedVertical.getValue(VerticalSlabBlock.WATERLOGGED), "Vertical state was not preserved");
        assertStepGeometry(helper, step, convertedStep, "path reversion");
        helper.assertTrue(convertedVertical.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER)
                && convertedStep.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER),
                "Water fluid state was not coherent after reversion");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void tintAndWartSemanticsAreIndependent(GameTestHelper helper) {
        NibaruMaterialProfile grass = NibaruMaterialProfiles.fromFamily(ModBlocks.GRASS_BLOCK).orElseThrow();
        NibaruMaterialProfile mycelium = NibaruMaterialProfiles.fromFamily(ModBlocks.MYCELIUM).orElseThrow();
        NibaruMaterialProfile oak = NibaruMaterialProfiles.fromFamily(ModBlocks.OAK_LEAVES).orElseThrow();
        NibaruMaterialProfile spruce = NibaruMaterialProfiles.fromFamily(ModBlocks.SPRUCE_LEAVES).orElseThrow();
        NibaruMaterialProfile cherry = NibaruMaterialProfiles.fromFamily(ModBlocks.CHERRY_LEAVES).orElseThrow();
        helper.assertTrue(grass.capabilities().contains(BehaviorCapability.SPREADABLE)
                && grass.tintProfile() == TintProfile.GRASS_BIOME, "Grass behavior/tint contract mismatch");
        helper.assertTrue(mycelium.capabilities().contains(BehaviorCapability.SPREADABLE)
                && mycelium.tintProfile() == TintProfile.NONE, "Mycelium inherited grass tint from behavior");
        helper.assertTrue(oak.tintProfile() == TintProfile.FOLIAGE_BIOME, "Oak foliage tint mismatch");
        helper.assertTrue(spruce.tintProfile() == TintProfile.FOLIAGE_SPRUCE, "Spruce fixed tint mismatch");
        helper.assertTrue(cherry.tintProfile() == TintProfile.NONE, "Cherry leaves should use untinted texture color");
        for (ModBlocks nylium : new ModBlocks[]{ModBlocks.CRIMSON_NYLIUM, ModBlocks.WARPED_NYLIUM}) {
            NibaruMaterialProfile profile = NibaruMaterialProfiles.fromFamily(nylium).orElseThrow();
            helper.assertTrue(profile.visualProfile()
                            == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.TOP_SIDE_BOTTOM,
                    nylium + " inherited the Grass tint/texture classification");
            helper.assertTrue(profile.tintProfile() == TintProfile.NONE
                            && profile.textureRoles().bottom().equals("netherrack")
                            && profile.textureRoles().overlay().equals(profile.textureRoles().side())
                            && profile.surfaceSamplingPolicy()
                            == NibaruMaterialProfile.SurfaceSamplingPolicy.NATIVE_STAIR_SURFACE_BAND,
                    nylium + " semantic face contract does not match its native stair");
        }
        for (ModBlocks wart : new ModBlocks[]{ModBlocks.WARPED_WART, ModBlocks.CRIMSON_WART}) {
            NibaruMaterialProfile profile = NibaruMaterialProfiles.fromFamily(wart).orElseThrow();
            helper.assertTrue(!profile.capabilities().contains(BehaviorCapability.LEAF_LIFECYCLE),
                    wart + " inherited leaf decay from implementation reuse");
            helper.assertTrue(profile.tintProfile() == TintProfile.NONE, wart + " inherited foliage tint");
            helper.assertTrue(profile.visualProfile() == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.UNIFORM,
                    wart + " did not resolve its canonical opaque wart visual");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void spreadableProfilesAndMyceliumAdmission(GameTestHelper helper) {
        NibaruMaterialProfile grass = NibaruMaterialProfiles.fromFamily(ModBlocks.GRASS_BLOCK).orElseThrow();
        NibaruMaterialProfile mycelium = NibaruMaterialProfiles.fromFamily(ModBlocks.MYCELIUM).orElseThrow();
        NibaruMaterialProfile dirt = NibaruMaterialProfiles.fromFamily(ModBlocks.DIRT).orElseThrow();
        helper.assertTrue(grass.capabilities().contains(BehaviorCapability.SPREADABLE), "Grass lacks SPREADABLE");
        helper.assertTrue(mycelium.capabilities().contains(BehaviorCapability.SPREADABLE), "Mycelium lacks SPREADABLE");
        helper.assertTrue(dirt.capabilities().contains(BehaviorCapability.SPREADABLE), "Dirt base lacks SPREADABLE");
        helper.assertTrue(mycelium.transitions().stream().anyMatch(t ->
                t.type() == MaterialTransition.Type.SPREADABLE_BASE && t.target() == ModBlocks.DIRT),
                "Mycelium has no typed Dirt base transition");
        helper.assertTrue(NibaruProviderAdapter.admissionSize(mycelium.nativeSlab().orElseThrow().defaultBlockState()) == 2,
                "Mycelium slab admission still depends on property count");
        helper.assertTrue(NibaruProviderAdapter.derived(mycelium,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow().getClass()
                == NibaruProviderAdapter.derived(grass, DerivedGeometrySupport.Geometry.VERTICAL_SLAB)
                .orElseThrow().getClass(), "Grass and Mycelium did not select the same spreadable adapter class");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void exactProviderFamilyIdentity(GameTestHelper helper) {
        assertCanonical(helper, ModBlocks.DIRT_PATH, Blocks.DIRT_PATH);
        assertCanonical(helper, ModBlocks.WARPED_WART, Blocks.WARPED_WART_BLOCK);
        assertCanonical(helper, ModBlocks.CRIMSON_WART, Blocks.NETHER_WART_BLOCK);
        helper.assertTrue(ShapeMap.getParent(ModBlocks.DIRT_PATH.getBlock(ModBlocks.BlockType.WALL).asItem())
                == Blocks.DIRT_PATH.asItem(), "Dirt Path wall not joined by exact provider identity");
        helper.assertTrue(ShapeMap.getParent(ModBlocks.WARPED_WART.getBlock(ModBlocks.BlockType.SLAB).asItem())
                == Blocks.WARPED_WART_BLOCK.asItem(), "Warped Wart slab not joined to exact parent");
        helper.assertTrue(ShapeMap.getParent(ModBlocks.CRIMSON_WART.getBlock(ModBlocks.BlockType.STAIRS).asItem())
                == Blocks.NETHER_WART_BLOCK.asItem(), "Crimson Wart stairs not joined to exact parent");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void unsupportedCapabilitiesNeverGenericize(GameTestHelper helper) {
        for (ModBlocks family : java.util.List.of(ModBlocks.SAND, ModBlocks.RED_SAND, ModBlocks.GRAVEL)) {
            NibaruMaterialProfile falling = NibaruMaterialProfiles.fromFamily(family).orElseThrow();
            helper.assertTrue(falling.capabilities().equals(java.util.Set.of(BehaviorCapability.FALLING)),
                    family + " must be FALLING-only");
            helper.assertTrue(falling.supportFor(DerivedGeometrySupport.Geometry.VERTICAL_SLAB,
                    NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                    family + " Vertical must be admitted");
            helper.assertTrue(falling.supportFor(DerivedGeometrySupport.Geometry.STEP,
                    NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                    family + " Step must be admitted");
        }
        var powders = NibaruMaterialProfiles.all().stream().filter(p ->
                p.capabilities().contains(BehaviorCapability.CONCRETE_HARDENING)).toList();
        helper.assertTrue(powders.size() == 16, "Expected 16 Concrete Powder profiles, found " + powders.size());
        for (NibaruMaterialProfile powder : powders) {
            helper.assertTrue(powder.capabilities().containsAll(java.util.Set.of(
                    BehaviorCapability.FALLING, BehaviorCapability.CONCRETE_HARDENING)),
                    "Concrete Powder compound capabilities incomplete: " + powder.canonicalParentId());
            helper.assertTrue(powder.transition(MaterialTransition.Type.CONCRETE_HARDENING).stream().count() == 1,
                    "Concrete Powder must have one typed hardening edge: " + powder.canonicalParentId());
            helper.assertTrue(powder.supportFor(DerivedGeometrySupport.Geometry.VERTICAL_SLAB,
                    NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                    "Concrete Powder Vertical not admitted: " + powder.canonicalParentId());
            helper.assertTrue(powder.supportFor(DerivedGeometrySupport.Geometry.STEP,
                    NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                    "Concrete Powder Step not admitted: " + powder.canonicalParentId());
        }
        for (ModBlocks family : java.util.List.of(ModBlocks.HONEY_BLOCK, ModBlocks.SLIME_BLOCK)) {
            NibaruMaterialProfile profile = NibaruMaterialProfiles.fromFamily(family).orElseThrow();
            helper.assertTrue(profile.supportFor(DerivedGeometrySupport.Geometry.VERTICAL_SLAB,
                            NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                            NibaruProviderAdapter.ADAPTED_VISUALS).supported()
                            && profile.supportFor(DerivedGeometrySupport.Geometry.STEP,
                            NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                            NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                    family + " final specialized contract is not admitted");
        }
        NibaruMaterialProfile ice = NibaruMaterialProfiles.fromFamily(ModBlocks.ICE).orElseThrow();
        helper.assertTrue(ice.supportFor(DerivedGeometrySupport.Geometry.STEP,
                NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                "Ice complete behavior/visual contract is not admitted");
        var glassProfiles = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.visualProfile() == VisualProfile.GLASS_EDGE).toList();
        helper.assertTrue(glassProfiles.size() == 17,
                "Expected plain plus 16 stained Glass profiles, found " + glassProfiles.size());
        var expectedGlassIds = java.util.Set.of(
                id("minecraft:glass"), id("minecraft:white_stained_glass"),
                id("minecraft:orange_stained_glass"), id("minecraft:magenta_stained_glass"),
                id("minecraft:light_blue_stained_glass"), id("minecraft:yellow_stained_glass"),
                id("minecraft:lime_stained_glass"), id("minecraft:pink_stained_glass"),
                id("minecraft:gray_stained_glass"), id("minecraft:light_gray_stained_glass"),
                id("minecraft:cyan_stained_glass"), id("minecraft:purple_stained_glass"),
                id("minecraft:blue_stained_glass"), id("minecraft:brown_stained_glass"),
                id("minecraft:green_stained_glass"), id("minecraft:red_stained_glass"),
                id("minecraft:black_stained_glass"));
        var actualGlassIds = glassProfiles.stream().map(NibaruMaterialProfile::canonicalParentId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        helper.assertTrue(actualGlassIds.equals(expectedGlassIds),
                "GLASS_EDGE inventory differs: " + actualGlassIds);
        for (NibaruMaterialProfile glassProfile : glassProfiles) {
            String expectedTexture = glassProfile.canonicalParentId().getPath();
            var roles = glassProfile.textureRoles();
            helper.assertTrue(roles.side().equals(expectedTexture)
                            && roles.top().equals(expectedTexture)
                            && roles.bottom().equals(expectedTexture)
                            && roles.particle().equals(expectedTexture)
                            && roles.overlay().isEmpty(),
                    "Glass profile is not canonical-texture-only: " + glassProfile.canonicalParentId()
                            + " -> " + roles);
            for (DerivedGeometrySupport.Geometry geometry : java.util.List.of(
                    DerivedGeometrySupport.Geometry.VERTICAL_SLAB, DerivedGeometrySupport.Geometry.STEP)) {
                helper.assertTrue(glassProfile.supportFor(geometry, NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                                NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                        "Glass authored GLASS_EDGE contract is not admitted: "
                                + glassProfile.canonicalParentId() + " " + geometry);
            }
        }
        NibaruMaterialProfile path = NibaruMaterialProfiles.fromFamily(ModBlocks.DIRT_PATH).orElseThrow();
        DerivedGeometrySupport pathSupport = path.supportFor(DerivedGeometrySupport.Geometry.STEP,
                NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS);
        helper.assertTrue(pathSupport.status() == DerivedGeometrySupport.Status.SUPPORTED_WITH_CAPABILITIES,
                "Dirt Path PATH_CONVERSION adapter is not active: " + pathSupport);
        helper.assertTrue(NibaruProviderAdapter.ADAPTED_CAPABILITIES.equals(java.util.Set.of(
                BehaviorCapability.LEAF_LIFECYCLE, BehaviorCapability.SPREADABLE,
                BehaviorCapability.FLATTENABLE_TO_PATH, BehaviorCapability.PATH_CONVERSION,
                BehaviorCapability.STRIPPABLE, BehaviorCapability.OXIDIZABLE,
                BehaviorCapability.WAXABLE, BehaviorCapability.SCRAPEABLE,
                BehaviorCapability.CORAL_DEATH, BehaviorCapability.FALLING,
                BehaviorCapability.CONCRETE_HARDENING, BehaviorCapability.REDSTONE_POWER,
                BehaviorCapability.TRANSLUCENT_ADJACENCY, BehaviorCapability.ICE_MELTING,
                BehaviorCapability.MAGMA_DAMAGE, BehaviorCapability.SOUL_SAND_INTERACTION,
                BehaviorCapability.GLAZED_ORIENTATION, BehaviorCapability.HONEY_INTERACTION,
                BehaviorCapability.SLIME_INTERACTION)),
                "An unrelated behavior capability became adapted");
        helper.assertTrue(NibaruMaterialProfiles.fromFamily(ModBlocks.CALCITE).orElseThrow()
                .supportFor(DerivedGeometrySupport.Geometry.STEP, NibaruProviderAdapter.ADAPTED_CAPABILITIES)
                .status() == DerivedGeometrySupport.Status.SUPPORTED_GENERIC, "Calcite is not generic-safe");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void copperProviderGraphAndDerivedLifecycle(GameTestHelper helper) {
        var copper = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.oxidationStage().isPresent()).toList();
        helper.assertTrue(copper.size() == 32, "Expected 32 exact copper profiles, found " + copper.size());
        int derivedTargets = 0;
        for (NibaruMaterialProfile profile : copper) {
            var stage = profile.oxidationStage().orElseThrow();
            assertTransitionCount(helper, profile, MaterialTransition.Type.NEXT_OXIDATION,
                    !profile.waxed() && stage != net.minecraft.world.level.block.WeatheringCopper.WeatherState.OXIDIZED);
            assertTransitionCount(helper, profile, MaterialTransition.Type.PREVIOUS_OXIDATION,
                    !profile.waxed() && stage != net.minecraft.world.level.block.WeatheringCopper.WeatherState.UNAFFECTED);
            assertTransitionCount(helper, profile, MaterialTransition.Type.WAXED, !profile.waxed());
            assertTransitionCount(helper, profile, MaterialTransition.Type.UNWAXED, profile.waxed());
            for (MaterialTransition transition : profile.transitions()) {
                if (java.util.Set.of(MaterialTransition.Type.NEXT_OXIDATION,
                        MaterialTransition.Type.PREVIOUS_OXIDATION, MaterialTransition.Type.WAXED,
                        MaterialTransition.Type.UNWAXED).contains(transition.type())) {
                    NibaruMaterialProfile target = NibaruMaterialProfiles.fromFamily(transition.target()).orElseThrow();
                    helper.assertTrue(target.oxidationStage().isPresent(), "Copper edge escaped copper catalog");
                    helper.assertTrue(sameCopperLine(profile, target),
                            "Cross-family copper edge: " + profile.family() + " -> " + target.family());
                }
            }
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                if (profile.supportFor(geometry, NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                        NibaruProviderAdapter.ADAPTED_VISUALS).supported()) {
                    derivedTargets++;
                    helper.assertTrue(NibaruProviderAdapter.derived(profile, geometry).isPresent(),
                            "Missing admitted copper geometry: " + profile.family() + " " + geometry);
                }
            }
        }
        helper.assertTrue(derivedTargets == 96, "Expected 96 visually complete copper targets, found " + derivedTargets);

        NibaruMaterialProfile copperBlock = NibaruMaterialProfiles.fromFamily(ModBlocks.COPPER_BLOCK).orElseThrow();
        NibaruMaterialProfile oxidized = NibaruMaterialProfiles.fromFamily(ModBlocks.OXIDIZED_COPPER).orElseThrow();
        NibaruMaterialProfile waxed = CopperSemantics.target(copperBlock, MaterialTransition.Type.WAXED).orElseThrow();
        for (Block nativeBlock : java.util.List.of(copperBlock.nativeSlab().orElseThrow(),
                copperBlock.nativeStair().orElseThrow(), copperBlock.nativeWall().orElseThrow())) {
            BlockState nativeState = representativeCopperState(nativeBlock.defaultBlockState());
            assertSharedState(helper, nativeState, CopperSemantics.transition(nativeState, copperBlock,
                    MaterialTransition.Type.NEXT_OXIDATION,
                    target -> CopperSemantics.nativeGeometry(nativeBlock, target)).orElseThrow(),
                    "native oxidation");
            assertSharedState(helper, nativeState, CopperSemantics.transition(nativeState, copperBlock,
                    MaterialTransition.Type.WAXED,
                    target -> CopperSemantics.nativeGeometry(nativeBlock, target)).orElseThrow(),
                    "native waxing");
        }
        for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
            Block source = NibaruProviderAdapter.derived(copperBlock, geometry).orElseThrow();
            BlockState state = representativeCopperState(source.defaultBlockState());
            assertSharedState(helper, state, CopperSemantics.transition(state, copperBlock,
                    MaterialTransition.Type.NEXT_OXIDATION, target -> NibaruProviderAdapter.derived(target, geometry))
                    .orElseThrow(), "natural oxidation");
            assertSharedState(helper, state, CopperSemantics.transition(state, copperBlock,
                    MaterialTransition.Type.WAXED, target -> NibaruProviderAdapter.derived(target, geometry))
                    .orElseThrow(), "waxing");
            BlockState oxidizedState = representativeCopperState(
                    NibaruProviderAdapter.derived(oxidized, geometry).orElseThrow().defaultBlockState());
            assertSharedState(helper, oxidizedState, CopperSemantics.transition(oxidizedState, oxidized,
                    MaterialTransition.Type.PREVIOUS_OXIDATION,
                    target -> NibaruProviderAdapter.derived(target, geometry)).orElseThrow(), "scraping");
            BlockState waxedState = representativeCopperState(
                    NibaruProviderAdapter.derived(waxed, geometry).orElseThrow().defaultBlockState());
            assertSharedState(helper, waxedState, CopperSemantics.transition(waxedState, waxed,
                    MaterialTransition.Type.UNWAXED,
                    target -> NibaruProviderAdapter.derived(target, geometry)).orElseThrow(), "wax removal");
        }
        helper.assertTrue(!randomlyTicks(NibaruProviderAdapter.derived(oxidized,
                        DerivedGeometrySupport.Geometry.STEP).orElseThrow()),
                "Oxidized derived geometry still randomly ticks");
        helper.assertTrue(!randomlyTicks(NibaruProviderAdapter.derived(waxed,
                        DerivedGeometrySupport.Geometry.STEP).orElseThrow()),
                "Waxed derived geometry randomly oxidizes");
        System.out.println("COPPER_GRAPH|profiles=" + copper.size() + "|derivedTargets=" + derivedTargets);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void sparseCutCopperExecutesProviderNaturalOxidation(GameTestHelper helper)
            throws ReflectiveOperationException {
        var cutCopper = java.util.List.of(ModBlocks.CUT_COPPER, ModBlocks.EXPOSED_CUT_COPPER,
                ModBlocks.WEATHERED_CUT_COPPER, ModBlocks.OXIDIZED_CUT_COPPER,
                ModBlocks.WAXED_CUT_COPPER, ModBlocks.WAXED_EXPOSED_CUT_COPPER,
                ModBlocks.WAXED_WEATHERED_CUT_COPPER, ModBlocks.WAXED_OXIDIZED_CUT_COPPER);
        var waxedFamilies = java.util.Set.of(ModBlocks.WAXED_CUT_COPPER, ModBlocks.WAXED_EXPOSED_CUT_COPPER,
                ModBlocks.WAXED_WEATHERED_CUT_COPPER, ModBlocks.WAXED_OXIDIZED_CUT_COPPER);
        for (ModBlocks family : cutCopper) {
            NibaruMaterialProfile profile = NibaruMaterialProfiles.fromFamily(family).orElseThrow();
            helper.assertTrue(profile.oxidationStage().isPresent(), family + " lacks exact oxidation stage");
            helper.assertTrue(profile.waxed() == waxedFamilies.contains(family), family + " wax state mismatch");
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                Block block = NibaruProviderAdapter.derived(profile, geometry).orElseThrow();
                helper.assertTrue(NibaruProviderAdapter.runtimeBinding(block).orElseThrow().profile() == profile,
                        family + " " + geometry + " lost exact binding");
                boolean terminal = profile.waxed() || profile.oxidationStage().orElseThrow()
                        == WeatheringCopper.WeatherState.OXIDIZED;
                helper.assertTrue(randomlyTicks(block) == !terminal,
                        family + " " + geometry + " random tick contract mismatch");
            }
        }

        NibaruMaterialProfile unaffected = NibaruMaterialProfiles.fromFamily(ModBlocks.CUT_COPPER).orElseThrow();
        NibaruMaterialProfile exposed = NibaruMaterialProfiles.fromFamily(ModBlocks.EXPOSED_CUT_COPPER).orElseThrow();
        Block vertical = NibaruProviderAdapter.derived(unaffected,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block step = NibaruProviderAdapter.derived(unaffected, DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        helper.assertTrue(vertical.getClass() == WeatheringVerticalSlabBlock.class,
                "Unexpected sparse Cut Copper Vertical class: " + vertical.getClass());
        helper.assertTrue(step.getClass() == WeatheringStepBlock.class,
                "Unexpected sparse Cut Copper Step class: " + step.getClass());
        helper.assertTrue(vertical instanceof ChangeOverTimeBlock<?> && step instanceof ChangeOverTimeBlock<?>,
                "Sparse Cut Copper classes do not implement ChangeOverTimeBlock");

        for (var entry : java.util.List.of(
                new Target(unaffected, DerivedGeometrySupport.Geometry.VERTICAL_SLAB),
                new Target(unaffected, DerivedGeometrySupport.Geometry.STEP))) {
            Block source = NibaruProviderAdapter.derived(entry.profile(), entry.geometry()).orElseThrow();
            Block target = NibaruProviderAdapter.derived(exposed, entry.geometry()).orElseThrow();
            BlockState state = representativeCopperState(source.defaultBlockState());
            @SuppressWarnings("unchecked")
            var lifecycle = (ChangeOverTimeBlock<WeatheringCopper.WeatherState>) source;
            BlockState next = lifecycle.getNext(state).orElseThrow();
            helper.assertTrue(next.is(target), "Provider getNext did not preserve sparse geometry");
            assertSharedState(helper, state, next, "sparse provider getNext");
            BlockPos pos = entry.geometry() == DerivedGeometrySupport.Geometry.VERTICAL_SLAB
                    ? new BlockPos(4, 1, 1) : new BlockPos(5, 1, 1);
            helper.setBlock(pos, state);
            invokeRandomTickUntilChanged(source, state, helper, pos, target);
        }
        System.out.println("SPARSE_CUT_COPPER_LIFECYCLE|profiles=8|classes=WeatheringVerticalSlabBlock,WeatheringStepBlock");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void copperInteractionsConsumeExactlyOnce(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        NibaruMaterialProfile copper = NibaruMaterialProfiles.fromFamily(ModBlocks.COPPER_BLOCK).orElseThrow();
        NibaruMaterialProfile oxidized = NibaruMaterialProfiles.fromFamily(ModBlocks.OXIDIZED_COPPER).orElseThrow();
        NibaruMaterialProfile waxed = CopperSemantics.target(copper, MaterialTransition.Type.WAXED).orElseThrow();

        BlockPos verticalPos = new BlockPos(1, 1, 1);
        Block vertical = NibaruProviderAdapter.derived(copper,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        BlockState verticalState = representativeCopperState(vertical.defaultBlockState());
        helper.setBlock(verticalPos, verticalState);
        var honeycomb = new net.minecraft.world.item.ItemStack(Items.HONEYCOMB, 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, honeycomb);
        NibaruProviderAdapter.useComposedCapabilities(vertical, honeycomb, verticalState, helper.getLevel(),
                helper.absolutePos(verticalPos), player, net.minecraft.world.InteractionHand.MAIN_HAND).orElseThrow();
        helper.assertTrue(honeycomb.getCount() == 1, "Honeycomb was not consumed exactly once");
        helper.assertTrue(helper.getBlockState(verticalPos).is(NibaruProviderAdapter.derived(waxed,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow()), "Vertical did not wax exactly once");
        assertSharedState(helper, verticalState, helper.getBlockState(verticalPos), "interactive Vertical waxing");

        BlockPos stepPos = new BlockPos(2, 1, 1);
        Block waxedStep = NibaruProviderAdapter.derived(waxed, DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        BlockState waxedStepState = representativeCopperState(waxedStep.defaultBlockState());
        helper.setBlock(stepPos, waxedStepState);
        var axe = new net.minecraft.world.item.ItemStack(Items.IRON_AXE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, axe);
        NibaruProviderAdapter.useComposedCapabilities(waxedStep, axe, waxedStepState, helper.getLevel(),
                helper.absolutePos(stepPos), player, net.minecraft.world.InteractionHand.MAIN_HAND).orElseThrow();
        helper.assertTrue(axe.getDamageValue() == 1, "Wax removal did not damage the axe exactly once");
        helper.assertTrue(helper.getBlockState(stepPos).is(NibaruProviderAdapter.derived(copper,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow()), "Wax removal changed oxidation stage");
        assertSharedState(helper, waxedStepState, helper.getBlockState(stepPos), "interactive Step wax removal");

        BlockPos scrapePos = new BlockPos(3, 1, 1);
        Block oxidizedStep = NibaruProviderAdapter.derived(oxidized, DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        BlockState oxidizedState = representativeCopperState(oxidizedStep.defaultBlockState());
        helper.setBlock(scrapePos, oxidizedState);
        var scrapeAxe = new net.minecraft.world.item.ItemStack(Items.IRON_AXE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, scrapeAxe);
        NibaruProviderAdapter.useComposedCapabilities(oxidizedStep, scrapeAxe, oxidizedState, helper.getLevel(),
                helper.absolutePos(scrapePos), player, net.minecraft.world.InteractionHand.MAIN_HAND).orElseThrow();
        NibaruMaterialProfile weathered = CopperSemantics.target(oxidized,
                MaterialTransition.Type.PREVIOUS_OXIDATION).orElseThrow();
        helper.assertTrue(helper.getBlockState(scrapePos).is(NibaruProviderAdapter.derived(weathered,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow()), "Scrape did not move exactly one stage");
        helper.assertTrue(scrapeAxe.getDamageValue() == 1, "Scrape did not damage the axe exactly once");
        assertSharedState(helper, oxidizedState, helper.getBlockState(scrapePos), "interactive Step scrape");

        NibaruMaterialProfile cut = NibaruMaterialProfiles.fromFamily(ModBlocks.CUT_COPPER).orElseThrow();
        NibaruMaterialProfile oxidizedCut = NibaruMaterialProfiles.fromFamily(ModBlocks.OXIDIZED_CUT_COPPER)
                .orElseThrow();
        NibaruMaterialProfile waxedCut = CopperSemantics.target(cut, MaterialTransition.Type.WAXED).orElseThrow();

        BlockPos cutVerticalPos = new BlockPos(4, 1, 1);
        Block cutVertical = NibaruProviderAdapter.derived(cut,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        BlockState cutVerticalState = representativeCopperState(cutVertical.defaultBlockState());
        helper.setBlock(cutVerticalPos, cutVerticalState);
        var cutHoneycomb = new net.minecraft.world.item.ItemStack(Items.HONEYCOMB, 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, cutHoneycomb);
        NibaruProviderAdapter.useComposedCapabilities(cutVertical, cutHoneycomb, cutVerticalState, helper.getLevel(),
                helper.absolutePos(cutVerticalPos), player, net.minecraft.world.InteractionHand.MAIN_HAND).orElseThrow();
        helper.assertTrue(cutHoneycomb.getCount() == 1, "Cut Copper honeycomb was not consumed exactly once");
        helper.assertTrue(helper.getBlockState(cutVerticalPos).is(NibaruProviderAdapter.derived(waxedCut,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow()),
                "Cut Copper Vertical did not wax to the exact same-stage geometry");
        assertSharedState(helper, cutVerticalState, helper.getBlockState(cutVerticalPos),
                "interactive Cut Copper Vertical waxing");

        BlockPos cutStepPos = new BlockPos(5, 1, 1);
        Block waxedCutStep = NibaruProviderAdapter.derived(waxedCut,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        BlockState waxedCutStepState = representativeCopperState(waxedCutStep.defaultBlockState());
        helper.setBlock(cutStepPos, waxedCutStepState);
        var cutAxe = new net.minecraft.world.item.ItemStack(Items.IRON_AXE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, cutAxe);
        NibaruProviderAdapter.useComposedCapabilities(waxedCutStep, cutAxe, waxedCutStepState, helper.getLevel(),
                helper.absolutePos(cutStepPos), player, net.minecraft.world.InteractionHand.MAIN_HAND).orElseThrow();
        helper.assertTrue(cutAxe.getDamageValue() == 1, "Cut Copper wax removal did not damage the axe exactly once");
        helper.assertTrue(helper.getBlockState(cutStepPos).is(NibaruProviderAdapter.derived(cut,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow()),
                "Cut Copper Step wax removal changed oxidation stage or geometry");
        assertSharedState(helper, waxedCutStepState, helper.getBlockState(cutStepPos),
                "interactive Cut Copper Step wax removal");

        BlockPos cutScrapePos = new BlockPos(6, 1, 1);
        Block oxidizedCutVertical = NibaruProviderAdapter.derived(oxidizedCut,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        BlockState oxidizedCutState = representativeCopperState(oxidizedCutVertical.defaultBlockState());
        helper.setBlock(cutScrapePos, oxidizedCutState);
        var cutScrapeAxe = new net.minecraft.world.item.ItemStack(Items.IRON_AXE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, cutScrapeAxe);
        NibaruProviderAdapter.useComposedCapabilities(oxidizedCutVertical, cutScrapeAxe, oxidizedCutState,
                helper.getLevel(), helper.absolutePos(cutScrapePos), player,
                net.minecraft.world.InteractionHand.MAIN_HAND).orElseThrow();
        NibaruMaterialProfile weatheredCut = CopperSemantics.target(oxidizedCut,
                MaterialTransition.Type.PREVIOUS_OXIDATION).orElseThrow();
        helper.assertTrue(helper.getBlockState(cutScrapePos).is(NibaruProviderAdapter.derived(weatheredCut,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow()),
                "Cut Copper scrape did not move exactly one stage while retaining geometry");
        helper.assertTrue(cutScrapeAxe.getDamageValue() == 1,
                "Cut Copper scrape did not damage the axe exactly once");
        assertSharedState(helper, oxidizedCutState, helper.getBlockState(cutScrapePos),
                "interactive Cut Copper Vertical scrape");
        System.out.println("SPARSE_CUT_COPPER_INTERACTIONS|wax=vertical|unwax=step|scrape=vertical");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void strippableInventoryAndCanonicalDerivedTargets(GameTestHelper helper) {
        var strippable = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.family() != null)
                .filter(profile -> profile.capabilities().contains(BehaviorCapability.STRIPPABLE)).toList();
        java.util.Set<Identifier> expectedSources = expectedStrippableMaterialAxisParentIds();
        var actualSources = new java.util.LinkedHashSet<Identifier>();
        strippable.forEach(profile -> actualSources.add(profile.canonicalParentId()));
        helper.assertTrue(strippable.size() == 23 && actualSources.equals(expectedSources),
                "Expected exact 23-source stripping inventory: expected=" + expectedSources + ", actual="
                        + actualSources);
        for (NibaruMaterialProfile source : strippable) {
            var transitions = source.transitions().stream()
                    .filter(transition -> transition.type() == MaterialTransition.Type.STRIPPED).toList();
            helper.assertTrue(transitions.size() == 1,
                    source.canonicalParentId() + " must have exactly one STRIPPED transition");
            NibaruMaterialProfile target = NibaruMaterialProfiles.fromFamily(transitions.getFirst().target()).orElseThrow();
            helper.assertTrue(target != source, source.canonicalParentId() + " strips to itself");
            helper.assertTrue(!target.capabilities().contains(BehaviorCapability.STRIPPABLE),
                    target.canonicalParentId() + " incorrectly strips again");
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                Block sourceBlock = NibaruProviderAdapter.derived(source, geometry).orElseThrow();
                Block targetBlock = NibaruProviderAdapter.derived(target, geometry).orElseThrow();
                helper.assertTrue(sourceBlock != targetBlock, "Derived strip transition retained source identity");
                helper.assertTrue(NibaruProviderAdapter.runtimeBinding(sourceBlock).orElseThrow().profile() == source,
                        "Source derived geometry lost runtime binding");
                helper.assertTrue(NibaruProviderAdapter.runtimeBinding(targetBlock).orElseThrow().profile() == target,
                        "Target derived geometry lost runtime binding");

                BlockState sourceState = switch (geometry) {
                    case VERTICAL_SLAB -> sourceBlock.defaultBlockState()
                            .setValue(MaterialAxisState.AXIS, Direction.Axis.Z)
                            .setValue(VerticalSlabBlock.FACING, Direction.WEST)
                            .setValue(VerticalSlabBlock.DOUBLE, false)
                            .setValue(BlockStateProperties.WATERLOGGED, true);
                    case STEP -> sourceBlock.defaultBlockState()
                            .setValue(MaterialAxisState.AXIS, Direction.Axis.Z)
                            .setValue(StepBlock.FACING, Direction.EAST)
                            .setValue(StepBlock.SLAB_TYPE, SlabType.TOP)
                            .setValue(BlockStateProperties.WATERLOGGED, true);
                    case LAYER -> sourceBlock.defaultBlockState()
                            .setValue(MaterialAxisState.AXIS, Direction.Axis.Z)
                            .setValue(BgeLayerBlock.FACING, Direction.WEST)
                            .setValue(BgeLayerBlock.LAYERS, 3)
                            .setValue(BlockStateProperties.WATERLOGGED, true);
                };
                BlockState strippedState = PathSemantics.copySharedProperties(sourceState,
                        targetBlock.defaultBlockState());
                helper.assertTrue(strippedState.is(targetBlock),
                        "Shared-property copy did not bind the exact stripped target for "
                                + source.canonicalParentId() + " " + geometry);
                helper.assertTrue(strippedState.getValue(MaterialAxisState.AXIS) == Direction.Axis.Z,
                        "Shared-property copy lost material axis for " + source.canonicalParentId() + " "
                                + geometry);
                assertSharedState(helper, sourceState, strippedState,
                        source.canonicalParentId() + " stripped " + geometry);
            }
        }
        System.out.println("STRIPPABLE_INVENTORY|count=" + strippable.size() + "|matrix="
                + strippable.stream().map(source -> source.canonicalParentId() + "->"
                + source.transitions().stream().filter(t -> t.type() == MaterialTransition.Type.STRIPPED)
                .findFirst().orElseThrow().target()).toList());
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void explicitNonpersistentDecay(GameTestHelper helper) {
        BlockPos[] positions = {new BlockPos(1, 1, 1), new BlockPos(2, 1, 1), new BlockPos(3, 1, 1), new BlockPos(4, 1, 1)};
        Block[] blocks = {block(NIBARU_SLAB), block(NIBARU_STAIRS), block(CNM_VERTICAL), block(CNM_STEP)};
        for (int i = 0; i < blocks.length; i++) {
            BlockState state = nonpersistent(blocks[i].defaultBlockState());
            helper.setBlock(positions[i], state);
            LeafSemantics.decayIfNeeded(state, helper.getLevel(), helper.absolutePos(positions[i]));
            helper.assertBlockPresent(Blocks.AIR, positions[i]);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 160)
    public void nativeDistancePropagationAndStatePreservation(GameTestHelper helper) {
        BlockPos log = new BlockPos(1, 1, 1);
        BlockPos slab = new BlockPos(2, 1, 1);
        BlockPos stairs = new BlockPos(3, 1, 1);
        BlockState slabState = nonpersistent(block(NIBARU_SLAB).defaultBlockState())
                .setValue(BlockStateProperties.PERSISTENT, true)
                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        BlockState stairState = nonpersistent(block(NIBARU_STAIRS).defaultBlockState())
                .setValue(BlockStateProperties.PERSISTENT, true)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        helper.setBlock(log, Blocks.OAK_LOG);
        helper.setBlock(slab, slabState);
        helper.setBlock(stairs, stairState);
        updateDistance(helper, slab);
        updateDistance(helper, stairs);
        assertDistance(helper, slab, 1);
        assertDistance(helper, stairs, 2);
        helper.assertTrue(helper.getBlockState(slab).getValue(BlockStateProperties.SLAB_TYPE) == SlabType.TOP,
                "Nibaru slab type changed during distance propagation");
        helper.assertTrue(helper.getBlockState(slab).getValue(BlockStateProperties.WATERLOGGED),
                "Nibaru slab waterlogging changed during distance propagation");
        helper.assertTrue(helper.getBlockState(stairs).getValue(BlockStateProperties.WATERLOGGED),
                "Nibaru stairs waterlogging changed during distance propagation");
        helper.setBlock(log, Blocks.AIR);
        helper.runAfterDelay(20, () -> {
            assertDistance(helper, slab, 7);
            assertDistance(helper, stairs, 7);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 160)
    public void cnmDistancePropagationAndStatePreservation(GameTestHelper helper) {
        BlockPos log = new BlockPos(1, 1, 1);
        BlockPos vertical = new BlockPos(2, 1, 1);
        BlockPos step = new BlockPos(3, 1, 1);
        BlockState verticalState = nonpersistent(block(CNM_VERTICAL).defaultBlockState())
                .setValue(BlockStateProperties.PERSISTENT, true)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        BlockState stepState = nonpersistent(block(CNM_STEP).defaultBlockState())
                .setValue(BlockStateProperties.PERSISTENT, true)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        helper.setBlock(log, Blocks.OAK_LOG);
        helper.setBlock(vertical, verticalState);
        helper.setBlock(step, stepState);
        updateDistance(helper, vertical);
        updateDistance(helper, step);
        assertDistance(helper, vertical, 1);
        assertDistance(helper, step, 2);
        BlockState verticalExpected = helper.getBlockState(vertical);
        BlockState stepExpected = helper.getBlockState(step);
        helper.setBlock(log, Blocks.AIR);
        helper.runAfterDelay(20, () -> {
            assertDistance(helper, vertical, 7);
            assertDistance(helper, step, 7);
            assertNonSemanticProperties(helper, verticalExpected, helper.getBlockState(vertical), "CNM vertical slab");
            assertNonSemanticProperties(helper, stepExpected, helper.getBlockState(step), "CNM step");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void vanillaCarrierInteroperation(GameTestHelper helper) {
        BlockPos log = new BlockPos(1, 1, 1);
        BlockPos vanilla = new BlockPos(2, 1, 1);
        BlockPos nibaru = new BlockPos(3, 1, 1);
        helper.setBlock(log, Blocks.OAK_LOG);
        helper.setBlock(vanilla, nonpersistent(Blocks.OAK_LEAVES.defaultBlockState()));
        helper.setBlock(nibaru, nonpersistent(block(NIBARU_SLAB).defaultBlockState()));
        updateDistance(helper, vanilla);
        updateDistance(helper, nibaru);
        assertDistance(helper, vanilla, 1);
        assertDistance(helper, nibaru, 2);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void canonicalDirtOwnershipAndLegacyMigration(GameTestHelper helper) {
        Block canonicalDirt = block(CNM_DIRT_VERTICAL);
        helper.assertTrue(block(NIBARU_DIRT_SLAB) != block(LEGACY_DIRT_SLAB),
                "Legacy horizontal dirt ID must remain registered separately for save compatibility");
        helper.assertTrue(canonicalDirt != block(LEGACY_DIRT_VERTICAL),
                "Legacy vertical dirt ID must remain registered separately for save compatibility");
        helper.assertTrue(canonicalDirt instanceof DirtVerticalSlab,
                "Nibaru-derived canonical dirt vertical slab lacks specialized dirt behavior");

        BlockState dirt = canonicalDirt.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true);
        BlockState grass = GrassFamilyBehavior.convertDirtGeometryToGrass(dirt);
        helper.assertTrue(BuiltInRegistries.BLOCK.getKey(grass.getBlock())
                        .equals(id("cnm_terrain_slabs_compat:grass_vertical_slab")),
                "Canonical dirt vertical slab did not convert to the behavior-bearing grass vertical slab");
        BlockState convertedBack = GrassFamilyBehavior.convertGrassGeometryToDirt(grass);
        helper.assertTrue(convertedBack.is(canonicalDirt),
                "Grass vertical slab did not resolve back to the canonical Nibaru-derived dirt vertical slab");
        helper.assertTrue(convertedBack.getValue(BlockStateProperties.WATERLOGGED),
                "Waterlogging was lost during canonical dirt/grass conversion");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void dirtPresentationUsesOnlyCanonicalGeometry(GameTestHelper helper) {
        var dirtShapes = ShapeMap.getShapes(Items.DIRT);
        assertShapePresent(helper, dirtShapes, "minecraft:dirt");
        assertShapePresent(helper, dirtShapes, "more_slabs_stairs_and_walls:dirt_slab");
        assertShapePresent(helper, dirtShapes, "more_slabs_stairs_and_walls:dirt_stairs");
        assertShapePresent(helper, dirtShapes, "more_slabs_stairs_and_walls:dirt_wall");
        assertShapePresent(helper, dirtShapes, "clutternomore:more_slabs_stairs_and_walls/vertical_dirt_slab");
        assertShapePresent(helper, dirtShapes, "clutternomore:more_slabs_stairs_and_walls/dirt_step");
        assertShapePresent(helper, dirtShapes, CnmTerrainCompat.layerId(
                NibaruMaterialProfiles.fromFamily(ModBlocks.DIRT).orElseThrow()).toString());
        NibaruMaterialProfile dirt = NibaruMaterialProfiles.fromFamily(ModBlocks.DIRT).orElseThrow();
        assertShapePresent(helper, dirtShapes, CnmTerrainCompat.cornerId(dirt).toString());
        assertShapePresent(helper, dirtShapes, CnmTerrainCompat.quarterColumnId(dirt).toString());
        assertShapeAbsent(helper, dirtShapes, "cnm_terrain_slabs_compat:dirt_slab");
        assertShapeAbsent(helper, dirtShapes, "cnm_terrain_slabs_compat:dirt_vertical_slab");

        long horizontal = dirtShapes.stream().filter(item -> Block.byItem(item) instanceof SlabBlock).count();
        long vertical = dirtShapes.stream().filter(item -> Block.byItem(item) instanceof VerticalSlabBlock).count();
        helper.assertTrue(horizontal == 1, "Dirt ShapeMap exposes " + horizontal + " horizontal slabs: " + ids(dirtShapes));
        helper.assertTrue(vertical == 1, "Dirt ShapeMap exposes " + vertical + " vertical slabs: " + ids(dirtShapes));
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void grassPresentationHasNoHistoricalHorizontalDuplicate(GameTestHelper helper) {
        var grassShapes = ShapeMap.getShapes(Items.GRASS_BLOCK);
        assertShapePresent(helper, grassShapes, "minecraft:grass_block");
        assertShapePresent(helper, grassShapes, "more_slabs_stairs_and_walls:grass_block_slab");
        assertShapePresent(helper, grassShapes, "cnm_terrain_slabs_compat:grass_vertical_slab");
        assertShapePresent(helper, grassShapes, "clutternomore:more_slabs_stairs_and_walls/grass_block_step");
        NibaruMaterialProfile grass = NibaruMaterialProfiles.fromFamily(ModBlocks.GRASS_BLOCK).orElseThrow();
        assertShapePresent(helper, grassShapes, CnmTerrainCompat.layerId(grass).toString());
        assertShapePresent(helper, grassShapes, CnmTerrainCompat.cornerId(grass).toString());
        assertShapePresent(helper, grassShapes, CnmTerrainCompat.quarterColumnId(grass).toString());
        assertShapeAbsent(helper, grassShapes, "cnm_terrain_slabs_compat:grass_slab");
        long horizontal = grassShapes.stream().filter(item -> Block.byItem(item) instanceof SlabBlock).count();
        long vertical = grassShapes.stream().filter(item -> Block.byItem(item) instanceof VerticalSlabBlock).count();
        helper.assertTrue(horizontal == 1, "Grass ShapeMap exposes " + horizontal + " horizontal slabs: " + ids(grassShapes));
        helper.assertTrue(vertical == 1, "Grass ShapeMap exposes " + vertical + " vertical slabs: " + ids(grassShapes));
        helper.assertTrue(grassShapes.get(grassShapes.size() - 4)
                        .equals(BuiltInRegistries.ITEM.getValue(CNM_GRASS_STEP))
                        && grassShapes.get(grassShapes.size() - 3) == cornerItem(grass)
                        && grassShapes.get(grassShapes.size() - 2) == columnItem(grass)
                        && grassShapes.getLast() == layerItem(grass),
                "Grass Step/Corner/Column/Layer tail order changed: " + ids(grassShapes));
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void bottomGrassStepExposureAndWaterLifecycle(GameTestHelper helper) throws ReflectiveOperationException {
        Block grassStep = block(CNM_GRASS_STEP);
        Block dirtStep = block(CNM_DIRT_STEP);
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockState exposedBottom = grassStep.defaultBlockState()
                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM)
                .setValue(BlockStateProperties.WATERLOGGED, false);
        helper.setBlock(pos, exposedBottom);
        helper.setBlock(pos.above(), Blocks.STONE);
        invokeRandomTick(grassStep, exposedBottom, helper, pos);
        helper.assertTrue(helper.getBlockState(pos).is(grassStep),
                "Covered bottom grass step lost Nibaru partial-surface exposure semantics");

        BlockState waterlogged = exposedBottom.setValue(BlockStateProperties.WATERLOGGED, true);
        helper.setBlock(pos, waterlogged);
        invokeRandomTick(grassStep, waterlogged, helper, pos);
        helper.assertTrue(helper.getBlockState(pos).is(dirtStep),
                "Waterlogged bottom grass step did not revert through shared semantics");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void crossProviderSpreadUsesSharedPairs(GameTestHelper helper) {
        BlockPos cnmTarget = new BlockPos(1, 1, 1);
        BlockPos nibaruTarget = new BlockPos(2, 1, 1);
        helper.setBlock(cnmTarget, block(CNM_DIRT_STEP));
        helper.setBlock(nibaruTarget, block(NIBARU_DIRT_SLAB));
        helper.assertTrue(SpreadableSemantics.trySpread(helper.getLevel(), Blocks.GRASS_BLOCK,
                        helper.absolutePos(cnmTarget)),
                "Nibaru material semantics could not convert a CNM dirt target");
        helper.assertTrue(helper.getBlockState(cnmTarget).is(block(CNM_GRASS_STEP)),
                "CNM target resolved to the wrong shared grass pair");
        helper.assertTrue(SpreadableSemantics.trySpread(helper.getLevel(), Blocks.GRASS_BLOCK,
                        helper.absolutePos(nibaruTarget)),
                "CNM material semantics could not convert a Nibaru dirt target");
        helper.assertTrue(helper.getBlockState(nibaruTarget).is(block(NIBARU_GRASS_SLAB)),
                "Nibaru target resolved to the wrong shared grass pair");

        BlockPos vertical = new BlockPos(3, 1, 1);
        BlockPos stairs = new BlockPos(4, 1, 1);
        helper.setBlock(vertical, block(CNM_DIRT_VERTICAL));
        helper.setBlock(stairs, block(NIBARU_DIRT_STAIRS));
        helper.assertTrue(SpreadableSemantics.trySpread(helper.getLevel(), Blocks.GRASS_BLOCK,
                helper.absolutePos(vertical)), "Nibaru grass stairs could not resolve CNM dirt vertical");
        helper.assertTrue(SpreadableSemantics.trySpread(helper.getLevel(), Blocks.GRASS_BLOCK,
                helper.absolutePos(stairs)), "CNM grass vertical could not resolve Nibaru dirt stairs");
        helper.assertTrue(helper.getBlockState(stairs).is(block(NIBARU_GRASS_STAIRS)),
                "Nibaru stair target resolved to the wrong shared grass pair");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void canonicalGrassStepPairPreservesGeometry(GameTestHelper helper) {
        Block dirtStep = block(CNM_DIRT_STEP);
        Block grassStep = block(CNM_GRASS_STEP);
        helper.assertTrue(dirtStep instanceof StepBlock, "Canonical dirt step is not CNM StepBlock geometry");
        helper.assertTrue(grassStep instanceof StepBlock, "Canonical grass step is not CNM StepBlock geometry");
        BlockState dirt = dirtStep.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.WEST)
                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        helper.assertTrue(GrassFamilyBehavior.isDirtTarget(dirt), "Dirt step is not a registered grass-family target");
        BlockState grass = GrassFamilyBehavior.convertDirtGeometryToGrass(dirt);
        helper.assertTrue(grass.is(grassStep), "Dirt step did not convert to the exact canonical grass step");
        assertStepGeometry(helper, dirt, grass, "dirt to grass");
        helper.assertTrue(GrassFamilyBehavior.isGrassSource(grass), "Grass step is not a registered grass-family source");
        BlockState reverted = GrassFamilyBehavior.convertGrassGeometryToDirt(grass);
        helper.assertTrue(reverted.is(dirtStep), "Grass step did not revert to the exact canonical dirt step");
        assertStepGeometry(helper, grass, reverted, "grass to dirt");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void controlledGrassStepGrowthAndCoveredDeath(GameTestHelper helper) throws ReflectiveOperationException {
        Block dirtStep = block(CNM_DIRT_STEP);
        Block grassStep = block(CNM_GRASS_STEP);
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockState dirt = dirtStep.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.EAST)
                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP)
                .setValue(BlockStateProperties.WATERLOGGED, false);
        helper.setBlock(pos, dirt);
        helper.assertTrue(GrassFamilyBehavior.tryConvertDirtAt(helper.getLevel(), helper.absolutePos(pos)),
                "Eligible dirt step did not grow through the controlled grass-family conversion path");
        BlockState grass = helper.getBlockState(pos);
        helper.assertTrue(grass.is(grassStep), "Controlled conversion did not produce grass step");
        assertStepGeometry(helper, dirt, grass, "controlled growth");

        helper.setBlock(pos.above(), Blocks.STONE);
        invokeRandomTick(grassStep, grass, helper, pos);
        BlockState reverted = helper.getBlockState(pos);
        helper.assertTrue(reverted.is(dirtStep), "Covered grass step did not revert to dirt step");
        assertStepGeometry(helper, grass, reverted, "covered death");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void glazedProfilesUseIndependentSpecializedState(GameTestHelper helper) {
        var glazed = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.visualProfile() == VisualProfile.GLAZED_ORIENTED).toList();
        helper.assertTrue(glazed.size() == 16, "Expected 16 Glazed profiles, found " + glazed.size());
        for (NibaruMaterialProfile profile : glazed) {
            helper.assertTrue(profile.capabilities().equals(java.util.Set.of(BehaviorCapability.GLAZED_ORIENTATION)),
                    "Glazed capability contract changed: " + profile.canonicalParentId() + " "
                            + profile.capabilities());
            helper.assertTrue(profile.orientationPolicy()
                            == NibaruMaterialProfile.OrientationPolicy.HORIZONTAL_FACING,
                    "Glazed orientation policy changed: " + profile.canonicalParentId());
            helper.assertTrue(profile.doubleFormPolicy() == NibaruMaterialProfile.DoubleFormPolicy.CUSTOM_REQUIRED,
                    "Glazed double form stopped being explicit: " + profile.canonicalParentId());
            String texture = profile.canonicalParentId().getPath();
            helper.assertTrue(profile.textureRoles().side().equals(texture)
                            && profile.textureRoles().top().equals(texture)
                            && profile.textureRoles().bottom().equals(texture),
                    "Glazed texture identity is not canonical: " + profile.canonicalParentId());

            Block vertical = NibaruProviderAdapter.derived(profile,
                    DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
            Block step = NibaruProviderAdapter.derived(profile,
                    DerivedGeometrySupport.Geometry.STEP).orElseThrow();
            helper.assertTrue(vertical instanceof GlazedVerticalSlabBlock,
                    "Glazed Vertical did not use specialized state: " + profile.canonicalParentId());
            helper.assertTrue(step instanceof GlazedStepBlock,
                    "Glazed Step did not use specialized state: " + profile.canonicalParentId());
            helper.assertTrue(vertical.defaultBlockState().hasProperty(GlazedPatternState.PATTERN_FACING)
                            && step.defaultBlockState().hasProperty(GlazedPatternState.PATTERN_FACING),
                    "Glazed derived state omitted pattern_facing: " + profile.canonicalParentId());
            helper.assertTrue(profile.supportFor(DerivedGeometrySupport.Geometry.VERTICAL_SLAB,
                            NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                            NibaruProviderAdapter.ADAPTED_VISUALS).supported()
                            && profile.supportFor(DerivedGeometrySupport.Geometry.STEP,
                            NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                            NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                    "Glazed profile remains withheld: " + profile.canonicalParentId());
            helper.assertTrue(vertical.defaultBlockState().getPistonPushReaction()
                            == net.minecraft.world.level.material.PushReaction.PUSH_ONLY
                            && step.defaultBlockState().getPistonPushReaction()
                            == net.minecraft.world.level.material.PushReaction.PUSH_ONLY,
                    "Glazed PUSH_ONLY property did not transfer: " + profile.canonicalParentId());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void glazedPhysicalAndPatternTransformsRemainIndependent(GameTestHelper helper) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.all().stream()
                .filter(candidate -> candidate.visualProfile() == VisualProfile.GLAZED_ORIENTED).findFirst()
                .orElseThrow();
        GlazedVerticalSlabBlock vertical = (GlazedVerticalSlabBlock) NibaruProviderAdapter.derived(profile,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        BlockState verticalState = vertical.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.WEST)
                .setValue(GlazedPatternState.PATTERN_FACING, Direction.NORTH)
                .setValue(VerticalSlabBlock.DOUBLE, false)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        BlockState verticalRotated = vertical.rotate(verticalState, Rotation.CLOCKWISE_90);
        helper.assertTrue(verticalRotated.getValue(VerticalSlabBlock.FACING) == Direction.NORTH
                        && verticalRotated.getValue(GlazedPatternState.PATTERN_FACING) == Direction.EAST,
                "Vertical rotation did not transform physical and pattern directions independently");
        helper.assertTrue(verticalRotated.getValue(BlockStateProperties.WATERLOGGED),
                "Vertical rotation changed waterlogging");

        GlazedStepBlock step = (GlazedStepBlock) NibaruProviderAdapter.derived(profile,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        BlockState stepState = step.defaultBlockState()
                .setValue(StepBlock.FACING, Direction.EAST)
                .setValue(GlazedPatternState.PATTERN_FACING, Direction.SOUTH)
                .setValue(StepBlock.SLAB_TYPE, SlabType.DOUBLE)
                .setValue(BlockStateProperties.WATERLOGGED, false);
        BlockState mirrored = step.mirror(stepState, Mirror.FRONT_BACK);
        helper.assertTrue(mirrored.getValue(StepBlock.FACING) == Mirror.FRONT_BACK.mirror(Direction.EAST)
                        && mirrored.getValue(GlazedPatternState.PATTERN_FACING)
                        == Mirror.FRONT_BACK.mirror(Direction.SOUTH),
                "Step mirror did not transform physical and pattern directions independently");
        helper.assertTrue(mirrored.getValue(StepBlock.SLAB_TYPE) == SlabType.DOUBLE,
                "Step mirror changed the double form");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void glazedGeneratedJsonCoversEveryIndependentState(GameTestHelper helper) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.all().stream()
                .filter(candidate -> candidate.visualProfile() == VisualProfile.GLAZED_ORIENTED).findFirst()
                .orElseThrow();
        Identifier verticalId = id("clutternomore:more_slabs_stairs_and_walls/vertical_white_glazed_terracotta_slab");
        Identifier stepId = id("clutternomore:more_slabs_stairs_and_walls/white_glazed_terracotta_step");
        JsonObject vertical = GlazedModelContract.verticalBlockState(verticalId).getAsJsonObject("variants");
        JsonObject step = GlazedModelContract.stepBlockState(stepId).getAsJsonObject("variants");
        helper.assertTrue(vertical.size() == 32, "Glazed Vertical requires 32 model states, found " + vertical.size());
        helper.assertTrue(step.size() == 48, "Glazed Step requires 48 model states, found " + step.size());

        for (Direction physical : GlazedModelContract.horizontalDirections()) {
            for (Direction pattern : GlazedModelContract.horizontalDirections()) {
                Direction relative = GlazedPatternState.relativePhysical(physical, pattern);
                for (boolean doubled : new boolean[]{false, true}) {
                    String key = GlazedModelContract.verticalVariantKey(physical, pattern, doubled);
                    helper.assertTrue(vertical.has(key), "Missing Glazed Vertical state " + key);
                    String model = vertical.getAsJsonObject(key).get("model").getAsString();
                    helper.assertTrue(doubled ? model.endsWith("_glazed_double")
                                    : model.endsWith(GlazedModelContract.relativeSuffix(relative, SlabType.BOTTOM)),
                            "Wrong Glazed Vertical relative model for " + key + ": " + model);
                }
                for (SlabType type : SlabType.values()) {
                    String key = GlazedModelContract.stepVariantKey(physical, pattern, type);
                    helper.assertTrue(step.has(key), "Missing Glazed Step state " + key);
                    helper.assertTrue(step.getAsJsonObject(key).get("model").getAsString()
                                    .endsWith(GlazedModelContract.relativeSuffix(relative, type)),
                            "Wrong Glazed Step relative model for " + key);
                }
            }
        }

        JsonObject doubleStep = GlazedModelContract.stepModel(profile, Direction.WEST, SlabType.DOUBLE);
        helper.assertTrue(StepBlock.SLAB_TYPE.getName().equals("type")
                        && step.keySet().stream().allMatch(key -> key.contains(",type=")),
                "Glazed Step blockstate did not use CNM's serialized type property");
        helper.assertTrue(doubleStep.getAsJsonArray("elements").size() == 2,
                "Glazed double Step is not one deterministic two-element model");
        helper.assertTrue(GlazedModelContract.verticalDoubleModel(profile).getAsJsonArray("elements").size() == 1,
                "Glazed double Vertical is not one deterministic full-block model");
        helper.assertTrue(doubleStep.getAsJsonObject("textures").get("all").getAsString()
                        .equals("minecraft:block/" + profile.canonicalParentId().getPath()),
                "Glazed generated JSON did not bind the canonical parent texture");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void honeySlimeProfilesAdmissionAndSpecializedOwners(GameTestHelper helper) {
        NibaruMaterialProfile honey = NibaruMaterialProfiles.fromFamily(ModBlocks.HONEY_BLOCK).orElseThrow();
        NibaruMaterialProfile slime = NibaruMaterialProfiles.fromFamily(ModBlocks.SLIME_BLOCK).orElseThrow();
        helper.assertTrue(honey.capabilities().contains(BehaviorCapability.HONEY_INTERACTION)
                        && honey.visualProfile() == VisualProfile.HONEY_INSET,
                "Honey typed behavior/visual identity changed");
        helper.assertTrue(slime.capabilities().contains(BehaviorCapability.SLIME_INTERACTION)
                        && slime.visualProfile() == VisualProfile.SLIME_INSET,
                "Slime typed behavior/visual identity changed");
        helper.assertTrue(honey.textureRoles().side().equals("honey_block_side")
                        && honey.textureRoles().top().equals("honey_block_top")
                        && honey.textureRoles().bottom().equals("honey_block_bottom")
                        && honey.textureRoles().particle().equals("honey_block_bottom"),
                "Honey canonical texture roles changed: " + honey.textureRoles());
        helper.assertTrue(slime.textureRoles().side().equals("slime_block")
                        && slime.textureRoles().side().equals(slime.textureRoles().top())
                        && slime.textureRoles().top().equals(slime.textureRoles().bottom()),
                "Slime canonical texture identity changed: " + slime.textureRoles());

        var honeyInset = honey.insetVisualContract().orElseThrow();
        var slimeInset = slime.insetVisualContract().orElseThrow();
        helper.assertTrue(honeyInset.fullSpanInset() == 1 && honeyInset.halfSpanInset() == 1
                        && honeyInset.includeInnerLayerOnFullCube(),
                "Honey material inset contract changed: " + honeyInset);
        helper.assertTrue(slimeInset.fullSpanInset() == 3 && slimeInset.halfSpanInset() == 2
                        && !slimeInset.includeInnerLayerOnFullCube(),
                "Slime material inset contract changed: " + slimeInset);

        var beforeBehaviors = java.util.EnumSet.copyOf(NibaruProviderAdapter.ADAPTED_CAPABILITIES);
        beforeBehaviors.remove(BehaviorCapability.HONEY_INTERACTION);
        beforeBehaviors.remove(BehaviorCapability.SLIME_INTERACTION);
        var beforeVisuals = java.util.EnumSet.copyOf(NibaruProviderAdapter.ADAPTED_VISUALS);
        beforeVisuals.remove(VisualProfile.HONEY_INSET);
        beforeVisuals.remove(VisualProfile.SLIME_INSET);
        for (NibaruMaterialProfile profile : java.util.List.of(honey, slime)) {
            for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
                helper.assertTrue(profile.supportFor(geometry, beforeBehaviors, beforeVisuals).status()
                                == DerivedGeometrySupport.Status.UNSUPPORTED_BOTH,
                        profile.family() + " did not require its complete behavior plus visual contract");
                helper.assertTrue(profile.supportFor(geometry, NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                        NibaruProviderAdapter.ADAPTED_VISUALS).supported(),
                        profile.family() + " remains withheld for " + geometry);
            }
        }

        Block honeyVertical = NibaruProviderAdapter.derived(honey,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block honeyStep = NibaruProviderAdapter.derived(honey,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        Block slimeVertical = NibaruProviderAdapter.derived(slime,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block slimeStep = NibaruProviderAdapter.derived(slime,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        helper.assertTrue(honeyVertical.getClass().getSimpleName().equals("HoneyVerticalSlabBlock")
                        && honeyStep.getClass().getSimpleName().equals("HoneyStepBlock")
                        && slimeVertical.getClass().getSimpleName().equals("SlimeVerticalSlabBlock")
                        && slimeStep.getClass().getSimpleName().equals("SlimeStepBlock"),
                "Final families were admitted without their specialized runtime owners");
        for (Block block : java.util.List.of(honeyVertical, honeyStep, slimeVertical, slimeStep)) {
            helper.assertTrue(block.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED),
                    "Specialized final-family geometry lost CNM waterlogging: " + block);
            helper.assertTrue(NibaruProviderAdapter.runtimeBinding(block).isPresent(),
                    "Specialized final-family geometry lacks exact runtime binding: " + block);
        }
        System.out.println("FINAL_FAMILY_ADMISSION|profiles=2|targets=6|behavior=933|visual=933|ready=933");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void honeyCollisionAndGeometryAwareSlide(GameTestHelper helper) {
        NibaruMaterialProfile honey = NibaruMaterialProfiles.fromFamily(ModBlocks.HONEY_BLOCK).orElseThrow();
        Block vertical = NibaruProviderAdapter.derived(honey,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block step = NibaruProviderAdapter.derived(honey, DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        var context = net.minecraft.world.phys.shapes.CollisionContext.empty();
        BlockPos origin = new BlockPos(2, 2, 2);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState state = vertical.defaultBlockState().setValue(VerticalSlabBlock.FACING, facing)
                    .setValue(VerticalSlabBlock.DOUBLE, false);
            var shape = state.getCollisionShape(helper.getLevel(), helper.absolutePos(origin), context);
            var bounds = shape.bounds();
            helper.assertTrue(bounds.maxY == 0.9375 && bounds.minY == 0.0
                            && ((bounds.maxX - bounds.minX == 0.375 && bounds.maxZ - bounds.minZ == 0.875)
                            || (bounds.maxX - bounds.minX == 0.875 && bounds.maxZ - bounds.minZ == 0.375)),
                    "Honey Vertical collision did not preserve half occupancy plus native inset: " + facing
                            + " " + bounds);
        }
        BlockState north = vertical.defaultBlockState().setValue(VerticalSlabBlock.FACING, Direction.NORTH)
                .setValue(VerticalSlabBlock.DOUBLE, false);
        var northShape = HoneyDerivedGeometry.vertical(north);
        helper.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(northShape,
                        Block.box(0, 0, 8, 16, 16, 16),
                        net.minecraft.world.phys.shapes.BooleanOp.AND),
                "Honey Vertical collision leaked into its empty half");
        BlockState full = north.setValue(VerticalSlabBlock.DOUBLE, true);
        var fullBounds = HoneyDerivedGeometry.vertical(full).bounds();
        helper.assertTrue(fullBounds.minX == 0.0625 && fullBounds.maxX == 0.9375
                        && fullBounds.maxY == 0.9375 && fullBounds.minZ == 0.0625
                        && fullBounds.maxZ == 0.9375,
                "Honey Vertical double collision changed: " + fullBounds);

        for (SlabType type : SlabType.values()) {
            BlockState state = step.defaultBlockState().setValue(StepBlock.FACING, Direction.NORTH)
                    .setValue(StepBlock.SLAB_TYPE, type);
            var shape = state.getCollisionShape(helper.getLevel(), helper.absolutePos(origin), context);
            helper.assertTrue(shape.toAabbs().size() == (type == SlabType.DOUBLE ? 2 : 1),
                    "Honey Step form has duplicate/missing collision segments: " + type + " " + shape.toAabbs());
            helper.assertTrue(shape.max(Direction.Axis.Y) == (type == SlabType.BOTTOM ? 0.4375 : 0.9375),
                    "Honey Step top inset changed: " + type + " " + shape.bounds());
            if (type == SlabType.TOP) helper.assertTrue(shape.min(Direction.Axis.Y) == 0.5,
                    "Honey top Step base changed");
        }

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos absolute = helper.absolutePos(origin);
        player.setOnGround(false);
        player.setDeltaMovement(0.0, -0.2, 0.0);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 0.2,
                absolute.getZ() + 0.4375 + player.getBbWidth() / 2.0);
        helper.assertTrue(HoneySemantics.isSliding(absolute, player, northShape),
                "Honey side contact against occupied Vertical geometry was not eligible");
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 0.2, absolute.getZ() + 0.9);
        helper.assertTrue(!HoneySemantics.isSliding(absolute, player, northShape),
                "Honey slide triggered in the empty Vertical region without side contact");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void slimeContactMovementAndStaticBounceContract(GameTestHelper helper) {
        NibaruMaterialProfile slime = NibaruMaterialProfiles.fromFamily(ModBlocks.SLIME_BLOCK).orElseThrow();
        Block vertical = NibaruProviderAdapter.derived(slime,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block step = NibaruProviderAdapter.derived(slime, DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        helper.assertTrue(vertical.getJumpFactor() == slime.nativeSlab().orElseThrow().getJumpFactor()
                        && step.getJumpFactor() == slime.nativeStair().orElseThrow().getJumpFactor()
                        && vertical.getFriction() == slime.nativeSlab().orElseThrow().getFriction(),
                "Slime static bounce/friction properties did not transfer");

        BlockState north = vertical.defaultBlockState().setValue(VerticalSlabBlock.FACING, Direction.NORTH)
                .setValue(VerticalSlabBlock.DOUBLE, false);
        var shape = north.getCollisionShape(helper.getLevel(), helper.absolutePos(BlockPos.ZERO),
                net.minecraft.world.phys.shapes.CollisionContext.empty());
        helper.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,
                        Block.box(0, 0, 8, 16, 16, 16), net.minecraft.world.phys.shapes.BooleanOp.AND),
                "Slime Vertical collision leaked into its empty half");
        for (SlabType type : SlabType.values()) {
            BlockState state = step.defaultBlockState().setValue(StepBlock.FACING, Direction.WEST)
                    .setValue(StepBlock.SLAB_TYPE, type);
            var stepShape = state.getCollisionShape(helper.getLevel(), helper.absolutePos(BlockPos.ZERO),
                    net.minecraft.world.phys.shapes.CollisionContext.empty());
            helper.assertTrue(stepShape.toAabbs().size() == (type == SlabType.DOUBLE ? 2 : 1),
                    "Slime Step contact geometry changed: " + type + " " + stepShape.toAabbs());
        }

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setDeltaMovement(1.0, 0.05, 1.0);
        vertical.stepOn(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), north, player);
        double factor = 0.4 + 0.05 * 0.2;
        helper.assertTrue(Math.abs(player.getDeltaMovement().x - factor) < 1.0E-9
                        && Math.abs(player.getDeltaMovement().z - factor) < 1.0E-9,
                "Slime low-vertical-speed horizontal movement contract changed: " + player.getDeltaMovement());

        float health = player.getHealth();
        player.setShiftKeyDown(false);
        vertical.fallOn(helper.getLevel(), north, helper.absolutePos(BlockPos.ZERO), player, 10.0);
        helper.assertTrue(player.getHealth() == health, "Ordinary Slime landing caused fall damage");
        player.setShiftKeyDown(true);
        vertical.fallOn(helper.getLevel(), north, helper.absolutePos(BlockPos.ZERO), player, 10.0);
        helper.assertTrue(player.getHealth() < health, "Crouch/suppress-bounce path did not restore ordinary fall damage");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void honeySlimeInsetModelsAreDeterministic(GameTestHelper helper) {
        NibaruMaterialProfile honey = NibaruMaterialProfiles.fromFamily(ModBlocks.HONEY_BLOCK).orElseThrow();
        NibaruMaterialProfile slime = NibaruMaterialProfiles.fromFamily(ModBlocks.SLIME_BLOCK).orElseThrow();
        Identifier verticalId = id("clutternomore:more_slabs_stairs_and_walls/vertical_honey_block_slab");
        Identifier stepId = id("clutternomore:more_slabs_stairs_and_walls/honey_block_step");
        helper.assertTrue(InsetModelContract.verticalBlockState(verticalId).getAsJsonObject("variants").size() == 8,
                "Inset Vertical blockstate must cover four facings times single/double");
        helper.assertTrue(InsetModelContract.stepBlockState(stepId).getAsJsonObject("variants").size() == 12,
                "Inset Step blockstate must cover four facings times three forms");
        for (NibaruMaterialProfile profile : java.util.List.of(honey, slime)) {
            JsonObject single = InsetModelContract.verticalModel(profile, Direction.EAST, false);
            JsonObject doubled = InsetModelContract.verticalModel(profile, Direction.NORTH, true);
            JsonObject lower = InsetModelContract.stepModel(profile, Direction.SOUTH, SlabType.BOTTOM);
            JsonObject top = InsetModelContract.stepModel(profile, Direction.WEST, SlabType.TOP);
            JsonObject doubleStep = InsetModelContract.stepModel(profile, Direction.NORTH, SlabType.DOUBLE);
            helper.assertTrue(single.get("render_type").getAsString().equals("translucent")
                            && lower.get("render_type").getAsString().equals("translucent"),
                    profile.family() + " inset model lost translucent rendering");
            helper.assertTrue(single.getAsJsonArray("elements").size() == 2
                            && lower.getAsJsonArray("elements").size() == 2
                            && top.getAsJsonArray("elements").size() == 2
                            && doubleStep.getAsJsonArray("elements").size() == 4,
                    profile.family() + " inset single/double segment structure changed");
            int expectedFull = profile == honey ? 2 : 1;
            helper.assertTrue(doubled.getAsJsonArray("elements").size() == expectedFull,
                    profile.family() + " full double form duplicated/omitted inner surfaces");
            JsonObject textures = single.getAsJsonObject("textures");
            helper.assertTrue(textures.get("side").getAsString().endsWith(profile.textureRoles().side())
                            && textures.get("top").getAsString().endsWith(profile.textureRoles().top())
                            && textures.get("bottom").getAsString().endsWith(profile.textureRoles().bottom()),
                    profile.family() + " generated model ignored provider texture roles");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void actualPistonResolverRecognizesTypedDerivedFamilies(GameTestHelper helper)
            throws ReflectiveOperationException {
        NibaruMaterialProfile honey = NibaruMaterialProfiles.fromFamily(ModBlocks.HONEY_BLOCK).orElseThrow();
        NibaruMaterialProfile slime = NibaruMaterialProfiles.fromFamily(ModBlocks.SLIME_BLOCK).orElseThrow();
        BlockState honeyVertical = NibaruProviderAdapter.derived(honey,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow().defaultBlockState();
        BlockState honeyStep = NibaruProviderAdapter.derived(honey,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow().defaultBlockState();
        BlockState slimeVertical = NibaruProviderAdapter.derived(slime,
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow().defaultBlockState();
        BlockState slimeStep = NibaruProviderAdapter.derived(slime,
                DerivedGeometrySupport.Geometry.STEP).orElseThrow().defaultBlockState();
        Method isSticky = net.minecraft.world.level.block.piston.PistonStructureResolver.class
                .getDeclaredMethod("isSticky", BlockState.class);
        Method canStick = net.minecraft.world.level.block.piston.PistonStructureResolver.class
                .getDeclaredMethod("canStickToEachOther", BlockState.class, BlockState.class);
        isSticky.setAccessible(true);
        canStick.setAccessible(true);
        for (BlockState state : java.util.List.of(honeyVertical, honeyStep, slimeVertical, slimeStep,
                honey.nativeSlab().orElseThrow().defaultBlockState(),
                slime.nativeSlab().orElseThrow().defaultBlockState(),
                Blocks.HONEY_BLOCK.defaultBlockState(), Blocks.SLIME_BLOCK.defaultBlockState())) {
            helper.assertTrue((boolean) isSticky.invoke(null, state),
                    "Actual piston resolver did not classify sticky state " + state);
        }
        helper.assertTrue(!(boolean) canStick.invoke(null, honeyVertical, slimeStep)
                        && !(boolean) canStick.invoke(null, slimeVertical, honeyStep)
                        && !(boolean) canStick.invoke(null, Blocks.HONEY_BLOCK.defaultBlockState(),
                        Blocks.SLIME_BLOCK.defaultBlockState()),
                "Actual piston resolver allowed a Honey/Slime opposed pair");
        boolean vanillaHoneyStone = (boolean) canStick.invoke(null, Blocks.HONEY_BLOCK.defaultBlockState(),
                Blocks.STONE.defaultBlockState());
        boolean vanillaSlimeStone = (boolean) canStick.invoke(null, Blocks.SLIME_BLOCK.defaultBlockState(),
                Blocks.STONE.defaultBlockState());
        helper.assertTrue((boolean) canStick.invoke(null, honeyVertical, Blocks.STONE.defaultBlockState())
                        == vanillaHoneyStone
                        && (boolean) canStick.invoke(null, slimeStep, Blocks.STONE.defaultBlockState())
                        == vanillaSlimeStone,
                "Derived piston adhesion differs from its vanilla family");
        helper.assertTrue(ProviderStickyMaterialSemantics.opposed(honeyVertical, slimeStep)
                        && !ProviderStickyMaterialSemantics.opposed(honeyVertical, Blocks.STONE.defaultBlockState()),
                "Typed runtime binding does not match actual resolver classification");
        helper.succeed();
    }

    private static void invokeRandomTick(Block block, BlockState state, GameTestHelper helper, BlockPos pos)
            throws ReflectiveOperationException {
        Method randomTick = block.getClass().getDeclaredMethod("randomTick", BlockState.class,
                net.minecraft.server.level.ServerLevel.class, BlockPos.class, net.minecraft.util.RandomSource.class);
        randomTick.setAccessible(true);
        randomTick.invoke(block, state, helper.getLevel(), helper.absolutePos(pos),
                net.minecraft.util.RandomSource.create(1L));
    }

    private static void invokeRandomTickUntilChanged(Block block, BlockState state, GameTestHelper helper,
            BlockPos pos, Block expected) throws ReflectiveOperationException {
        Method randomTick = block.getClass().getDeclaredMethod("randomTick", BlockState.class,
                net.minecraft.server.level.ServerLevel.class, BlockPos.class, net.minecraft.util.RandomSource.class);
        randomTick.setAccessible(true);
        var random = net.minecraft.util.RandomSource.create(918273L);
        for (int attempt = 0; attempt < 1024 && helper.getBlockState(pos).is(block); attempt++) {
            randomTick.invoke(block, state, helper.getLevel(), helper.absolutePos(pos), random);
        }
        helper.assertTrue(helper.getBlockState(pos).is(expected),
                "Actual randomTick callback did not reach exact next-stage geometry");
        assertSharedState(helper, state, helper.getBlockState(pos), "actual sparse random oxidation");
    }

    private static void invokeTick(Block block, BlockState state, GameTestHelper helper, BlockPos pos)
            throws ReflectiveOperationException {
        Method tick = block.getClass().getDeclaredMethod("tick", BlockState.class,
                net.minecraft.server.level.ServerLevel.class, BlockPos.class, net.minecraft.util.RandomSource.class);
        tick.setAccessible(true);
        tick.invoke(block, state, helper.getLevel(), helper.absolutePos(pos),
                net.minecraft.util.RandomSource.create(2L));
    }

    private static void assertStepGeometry(GameTestHelper helper, BlockState expected, BlockState actual, String direction) {
        helper.assertTrue(expected.getValue(BlockStateProperties.HORIZONTAL_FACING)
                        == actual.getValue(BlockStateProperties.HORIZONTAL_FACING), direction + " changed facing");
        helper.assertTrue(expected.getValue(BlockStateProperties.SLAB_TYPE)
                        == actual.getValue(BlockStateProperties.SLAB_TYPE), direction + " changed type");
        helper.assertTrue(expected.getValue(BlockStateProperties.WATERLOGGED)
                        == actual.getValue(BlockStateProperties.WATERLOGGED), direction + " changed waterlogging");
    }

    private static void assertCanonical(GameTestHelper helper, ModBlocks family, Block expectedParent) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromFamily(family).orElseThrow();
        helper.assertTrue(profile.canonicalParent() == expectedParent,
                family + " canonical parent mismatch: " + profile.canonicalParentId());
        for (ModBlocks.BlockType type : ModBlocks.BlockType.values()) {
            if (family.hasBlock(type)) helper.assertTrue(
                    NibaruMaterialProfiles.fromBlock(family.getBlock(type)).orElseThrow() == profile,
                    family + " " + type + " lookup did not preserve exact family identity");
        }
    }

    private static void assertUnsupported(GameTestHelper helper, ModBlocks family,
            BehaviorCapability expectedMissing) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromFamily(family).orElseThrow();
        DerivedGeometrySupport support = profile.supportFor(DerivedGeometrySupport.Geometry.STEP,
                NibaruProviderAdapter.ADAPTED_CAPABILITIES, NibaruProviderAdapter.ADAPTED_VISUALS);
        helper.assertTrue(!support.supported(),
                family + " was silently classified as supported");
        helper.assertTrue(support.missingCapabilities().contains(expectedMissing),
                family + " unsupported result omitted " + expectedMissing + ": " + support.missingCapabilities());
        helper.assertTrue(NibaruProviderAdapter.admissionSize(profile.nativeStair().orElseThrow().defaultBlockState()) == -1,
                family + " unsupported stair was admitted by CNM");
    }

    private static java.util.List<Target> targets(java.util.List<NibaruMaterialProfile> profiles) {
        java.util.List<Target> result = new java.util.ArrayList<>();
        for (NibaruMaterialProfile profile : profiles) {
            if (profile.effectiveSlabSource().isPresent()) result.add(new Target(profile, DerivedGeometrySupport.Geometry.VERTICAL_SLAB));
            if (profile.effectiveStairSource().isPresent()) result.add(new Target(profile, DerivedGeometrySupport.Geometry.STEP));
            result.add(new Target(profile, DerivedGeometrySupport.Geometry.LAYER));
        }
        return result;
    }

    private record Target(NibaruMaterialProfile profile, DerivedGeometrySupport.Geometry geometry) {}

    private static void assertTransitionCount(GameTestHelper helper, NibaruMaterialProfile profile,
            MaterialTransition.Type type, boolean expected) {
        long count = profile.transitions().stream().filter(transition -> transition.type() == type).count();
        helper.assertTrue(count == (expected ? 1 : 0), profile.family() + " has " + count + " " + type + " edges");
    }

    private static boolean sameCopperLine(NibaruMaterialProfile first, NibaruMaterialProfile second) {
        return copperRoot(first) == copperRoot(second);
    }

    private static NibaruMaterialProfile copperRoot(NibaruMaterialProfile profile) {
        NibaruMaterialProfile current = profile.waxed()
                ? CopperSemantics.target(profile, MaterialTransition.Type.UNWAXED).orElseThrow() : profile;
        while (current.transition(MaterialTransition.Type.PREVIOUS_OXIDATION).isPresent()) {
            current = CopperSemantics.target(current, MaterialTransition.Type.PREVIOUS_OXIDATION).orElseThrow();
        }
        return current;
    }

    private static BlockState representativeCopperState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE))
            state = state.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                && (!state.hasProperty(BlockStateProperties.SLAB_TYPE)
                || state.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE))
            state = state.setValue(BlockStateProperties.WATERLOGGED, true);
        return state;
    }

    private static JsonObject syntheticAxisBlockState(JsonElement x, JsonElement y, JsonElement z) {
        JsonObject variants = new JsonObject();
        variants.add("axis=x", x);
        variants.add("axis=y", y);
        variants.add("axis=z", z);
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        return result;
    }

    private static JsonObject syntheticVariant(String model, int x, int y) {
        JsonObject result = new JsonObject();
        result.addProperty("model", model);
        if (x != 0) result.addProperty("x", x);
        if (y != 0) result.addProperty("y", y);
        return result;
    }

    private static JsonObject syntheticRandomizedStandardAxisBlockState() {
        JsonArray x = new JsonArray();
        x.add(syntheticVariant("minecraft:block/deepslate", 90, 90));
        x.add(syntheticVariant("minecraft:block/deepslate_mirrored", 90, 90));
        x.add(syntheticVariant("minecraft:block/deepslate", 90, 90));
        x.add(syntheticVariant("minecraft:block/deepslate_mirrored", 90, 90));
        JsonArray y = new JsonArray();
        y.add(syntheticVariant("minecraft:block/deepslate", 0, 0));
        y.add(syntheticVariant("minecraft:block/deepslate_mirrored", 0, 0));
        y.add(syntheticVariant("minecraft:block/deepslate", 0, 180));
        y.add(syntheticVariant("minecraft:block/deepslate_mirrored", 0, 180));
        JsonArray z = new JsonArray();
        z.add(syntheticVariant("minecraft:block/deepslate", 90, 0));
        z.add(syntheticVariant("minecraft:block/deepslate_mirrored", 90, 0));
        z.add(syntheticVariant("minecraft:block/deepslate", 90, 180));
        z.add(syntheticVariant("minecraft:block/deepslate_mirrored", 90, 180));
        return syntheticAxisBlockState(x, y, z);
    }

    private static void assertAxisPolicyRejected(GameTestHelper helper, Identifier parent,
            JsonObject blockState, String label) {
        boolean rejected = false;
        try {
            AxisModelContract.uvPolicy(parent, blockState);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Axis model policy accepted " + label + ": " + blockState);
    }

    private static NibaruMaterialProfile representativeAxisProfile(AxisUvPolicy policy) {
        Identifier parent = switch (policy) {
            case STANDARD_ROTATED -> id("minecraft:crimson_stem");
            case HORIZONTAL_ROTATED -> id("minecraft:oak_log");
            case DIRECT_UV_LOCKED -> id("minecraft:bamboo_block");
        };
        return NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.canonicalParentId().equals(parent))
                .findFirst().orElseThrow();
    }

    private static String axisTexture(String path) {
        return path.contains(":") ? path : "minecraft:block/" + path;
    }

    private static java.util.Set<String> normalVerticalModelIds(Identifier shape) {
        String base = AxisModelContract.GENERATED_NAMESPACE + ":block/" + shape.getPath();
        return java.util.Set.of(base, base + "_double");
    }

    private static java.util.Set<String> normalStepModelIds(Identifier shape) {
        String base = AxisModelContract.GENERATED_NAMESPACE + ":block/" + shape.getPath();
        return java.util.Set.of(base, base + "_top", base + "_double");
    }

    private static void assertAxisSelectorModelsResolve(GameTestHelper helper,
            java.util.Map<String, AxisModelContract.VariantSelection> selectors,
            java.util.Map<String, JsonObject> generatedModels, java.util.Set<String> normalModels,
            AxisUvPolicy policy, String geometry) {
        for (var entry : selectors.entrySet()) {
            AxisModelContract.VariantSelection selection = entry.getValue();
            JsonObject encoded = selection.json();
            helper.assertTrue(!encoded.has("uvlock") || !encoded.get("uvlock").getAsBoolean(),
                    policy + " " + geometry + " selector uses forbidden blockstate uvlock=true: "
                            + entry.getKey() + " -> " + encoded);
            helper.assertTrue(encoded.get("model").getAsString().equals(selection.model()),
                    policy + " " + geometry + " selector JSON changed its model reference: " + encoded);
            boolean generated = generatedModels.containsKey(selection.model());
            boolean normal = normalModels.contains(selection.model());
            helper.assertTrue(generated,
                    policy + " " + geometry + " selector references a missing model: "
                            + entry.getKey() + " -> " + selection.model());
            helper.assertTrue(!normal,
                    policy + " " + geometry + " placed-state selector reused item-only normal CNM model: "
                            + entry.getKey() + " -> " + selection.model());
            helper.assertTrue(AxisModelContract.generatedModelPath(selection.model()).endsWith(".json"),
                    "Generated model reference did not map to a JSON resource path: " + selection.model());
        }
    }

    private static void assertGeneratedAxisTextures(GameTestHelper helper,
            java.util.Map<String, JsonObject> models, NibaruMaterialProfile profile, String label) {
        var expected = AxisModelContract.semanticTextures(profile);
        for (var entry : models.entrySet()) {
            JsonObject textures = entry.getValue().getAsJsonObject("textures");
            for (var texture : expected.entrySet()) {
                helper.assertTrue(textures.has(texture.getKey())
                                && textures.get(texture.getKey()).getAsString().equals(texture.getValue()),
                        label + " model " + entry.getKey() + " changed semantic texture "
                                + texture.getKey() + ": " + textures);
            }
            if (!profile.textureRoles().bottom().equals(profile.textureRoles().top())) {
                String malformed = axisTexture(profile.textureRoles().bottom());
                boolean retainedMalformed = textures.entrySet().stream()
                        .anyMatch(texture -> texture.getValue().getAsString().equals(malformed));
                helper.assertTrue(!retainedMalformed,
                        label + " model retained malformed bottom texture " + malformed + ": " + entry.getKey());
            }
        }
    }

    private static JsonObject selectedModel(
            java.util.Map<String, AxisModelContract.VariantSelection> selectors,
            java.util.Map<String, JsonObject> models, String selectorKey) {
        AxisModelContract.VariantSelection selection = selectors.get(selectorKey);
        if (selection == null || !models.containsKey(selection.model())) {
            throw new IllegalStateException("Selector did not resolve to a generated model: "
                    + selectorKey + " -> " + selection);
        }
        return models.get(selection.model());
    }

    private static void assertColumnFacePolicy(
            GameTestHelper helper, JsonObject model, boolean horizontal, String label) {
        JsonObject faces = firstElementFaces(model);
        for (Direction face : Direction.values()) {
            String texture = switch (face) {
                case UP -> "#top";
                case DOWN -> "#bottom";
                default -> "#side";
            };
            int rotation = horizontal && face == Direction.UP ? 180 : 0;
            assertModelFace(helper, faces, face, texture, rotation, label);
        }
    }

    private static void assertDirectFacePolicy(
            GameTestHelper helper, JsonObject model, Direction.Axis axis, String label) {
        JsonObject faces = firstElementFaces(model);
        for (Direction face : Direction.values()) {
            String texture;
            if (face.getAxis() != axis) {
                texture = "#side";
            } else {
                texture = switch (face) {
                    case EAST, UP, SOUTH -> "#top";
                    case WEST, DOWN, NORTH -> "#bottom";
                };
            }
            int rotation = switch (axis) {
                case X -> face.getAxis() == Direction.Axis.X ? 0 : 90;
                case Y -> 0;
                case Z -> face.getAxis() == Direction.Axis.X ? 90 : 0;
            };
            assertModelFace(helper, faces, face, texture, rotation, label);
        }
    }

    private static JsonObject firstElementFaces(JsonObject model) {
        JsonArray elements = model.getAsJsonArray("elements");
        if (elements == null || elements.isEmpty()) {
            throw new IllegalStateException("Axis model has no elements: " + model);
        }
        return elements.get(0).getAsJsonObject().getAsJsonObject("faces");
    }

    private static void assertModelFace(GameTestHelper helper, JsonObject faces, Direction face,
            String texture, int rotation, String label) {
        JsonObject encoded = faces.getAsJsonObject(face.getSerializedName());
        int actualRotation = encoded.has("rotation") ? encoded.get("rotation").getAsInt() : 0;
        helper.assertTrue(encoded.get("texture").getAsString().equals(texture)
                        && actualRotation == rotation,
                label + " face contract changed for " + face + ": expected texture=" + texture
                        + ", rotation=" + rotation + ", actual=" + encoded);
    }

    private static void assertVerticalAxisGeometry(GameTestHelper helper, AxisUvPolicy policy,
            Identifier shape, java.util.Map<String, AxisModelContract.VariantSelection> selectors,
            java.util.Map<String, JsonObject> models) {
        for (Direction facing : AxisModelContract.horizontalDirections()) {
            for (boolean doubled : new boolean[]{false, true}) {
                for (Direction.Axis axis : AxisModelContract.materialAxes()) {
                    String key = AxisModelContract.verticalVariantKey(facing, doubled, axis);
                    AxisModelContract.VariantSelection selection = selectors.get(key);
                    assertMaterialTransform(helper, policy, axis, selection, "Vertical " + key);
                    JsonObject model = models.get(selection.model());
                    helper.assertTrue(model != null,
                            policy + " Vertical geometry selector did not resolve: " + key + " -> "
                                    + selection.model());
                    java.util.Set<java.util.Set<Direction>> expected = doubled
                            ? axisSignatures(new Direction[]{})
                            : axisSignatures(new Direction[]{facing});
                    java.util.Set<java.util.Set<Direction>> actual = worldAxisSignatures(model, selection, axis);
                    helper.assertTrue(actual.equals(expected),
                            policy + " Vertical inverse geometry changed for " + shape + " " + key
                                    + ": expected=" + expected + ", actual=" + actual);
                }
            }
        }
    }

    private static void assertStepAxisGeometry(GameTestHelper helper, AxisUvPolicy policy,
            Identifier shape, java.util.Map<String, AxisModelContract.VariantSelection> selectors,
            java.util.Map<String, JsonObject> models) {
        for (Direction facing : AxisModelContract.horizontalDirections()) {
            for (SlabType type : new SlabType[]{SlabType.BOTTOM, SlabType.TOP, SlabType.DOUBLE}) {
                for (Direction.Axis axis : AxisModelContract.materialAxes()) {
                    String key = AxisModelContract.stepVariantKey(facing, type, axis);
                    AxisModelContract.VariantSelection selection = selectors.get(key);
                    assertMaterialTransform(helper, policy, axis, selection, "Step " + key);
                    JsonObject model = models.get(selection.model());
                    helper.assertTrue(model != null,
                            policy + " Step geometry selector did not resolve: " + key + " -> "
                                    + selection.model());
                    java.util.Set<java.util.Set<Direction>> expected;
                    if (type == SlabType.DOUBLE) {
                        expected = axisSignatures(
                                new Direction[]{Direction.UP, facing},
                                new Direction[]{Direction.DOWN, facing.getOpposite()});
                    } else {
                        expected = axisSignatures(new Direction[]{
                                type == SlabType.BOTTOM ? Direction.DOWN : Direction.UP, facing});
                    }
                    java.util.Set<java.util.Set<Direction>> actual = worldAxisSignatures(model, selection, axis);
                    helper.assertTrue(actual.equals(expected),
                            policy + " Step inverse geometry changed for " + shape + " " + key
                                    + ": expected=" + expected + ", actual=" + actual);
                }
            }
        }
    }

    private static void assertMaterialTransform(GameTestHelper helper, AxisUvPolicy policy,
            Direction.Axis axis, AxisModelContract.VariantSelection selection, String label) {
        int expectedX = policy != AxisUvPolicy.DIRECT_UV_LOCKED && axis != Direction.Axis.Y ? 90 : 0;
        int expectedY = policy != AxisUvPolicy.DIRECT_UV_LOCKED && axis == Direction.Axis.X ? 90 : 0;
        helper.assertTrue(selection.x() == expectedX && selection.y() == expectedY,
                label + " uses the wrong material transform: expected x=" + expectedX + ", y=" + expectedY
                        + ", actual=" + selection);
    }

    private static java.util.Set<java.util.Set<Direction>> worldAxisSignatures(JsonObject model,
            AxisModelContract.VariantSelection selection, Direction.Axis axis) {
        var result = new java.util.LinkedHashSet<java.util.Set<Direction>>();
        for (java.util.Set<Direction> modelSignature : modelAxisSignatures(model)) {
            var worldSignature = java.util.EnumSet.noneOf(Direction.class);
            for (Direction direction : modelSignature) {
                worldSignature.add(selection.x() == 0 && selection.y() == 0
                        ? direction : forwardMaterialRotation(direction, axis));
            }
            result.add(java.util.Set.copyOf(worldSignature));
        }
        return java.util.Set.copyOf(result);
    }

    private static Direction forwardMaterialRotation(Direction modelDirection, Direction.Axis axis) {
        return AxisModelContract.materialRotation(axis).rotate(modelDirection);
    }

    private static java.util.Set<java.util.Set<Direction>> modelAxisSignatures(JsonObject model) {
        var result = new java.util.LinkedHashSet<java.util.Set<Direction>>();
        for (JsonElement encoded : model.getAsJsonArray("elements")) {
            JsonObject element = encoded.getAsJsonObject();
            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");
            var signature = java.util.EnumSet.noneOf(Direction.class);
            addAxisHalfSpace(signature, from.get(0).getAsInt(), to.get(0).getAsInt(),
                    Direction.WEST, Direction.EAST);
            addAxisHalfSpace(signature, from.get(1).getAsInt(), to.get(1).getAsInt(),
                    Direction.DOWN, Direction.UP);
            addAxisHalfSpace(signature, from.get(2).getAsInt(), to.get(2).getAsInt(),
                    Direction.NORTH, Direction.SOUTH);
            result.add(java.util.Set.copyOf(signature));
        }
        return java.util.Set.copyOf(result);
    }

    private static void addAxisHalfSpace(java.util.Set<Direction> signature, int from, int to,
            Direction negative, Direction positive) {
        if (from == 0 && to == 8) {
            signature.add(negative);
        } else if (from == 8 && to == 16) {
            signature.add(positive);
        } else if (from != 0 || to != 16) {
            throw new IllegalStateException("Non-canonical axis cuboid interval [" + from + "," + to + "]");
        }
    }

    private static java.util.Set<java.util.Set<Direction>> axisSignatures(Direction[]... cuboids) {
        var result = new java.util.LinkedHashSet<java.util.Set<Direction>>();
        for (Direction[] cuboid : cuboids) {
            var signature = java.util.EnumSet.noneOf(Direction.class);
            java.util.Collections.addAll(signature, cuboid);
            result.add(java.util.Set.copyOf(signature));
        }
        return java.util.Set.copyOf(result);
    }

    private static java.util.Set<Identifier> expectedMaterialAxisParentIds() {
        var expected = new java.util.LinkedHashSet<>(expectedStrippableMaterialAxisParentIds());
        for (String wood : java.util.List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "pale_oak", "mangrove", "cherry")) {
            addMinecraftIds(expected, "stripped_" + wood + "_log", "stripped_" + wood + "_wood");
        }
        for (String fungus : java.util.List.of("crimson", "warped")) {
            addMinecraftIds(expected, "stripped_" + fungus + "_stem", "stripped_" + fungus + "_hyphae");
        }
        addMinecraftIds(expected, "stripped_bamboo_block", "basalt", "polished_basalt", "bone_block",
                "deepslate", "hay_block", "muddy_mangrove_roots", "quartz_pillar", "ochre_froglight",
                "verdant_froglight", "pearlescent_froglight", "purpur_pillar");
        return java.util.Set.copyOf(expected);
    }

    private static java.util.Set<Identifier> expectedStrippableMaterialAxisParentIds() {
        var expected = new java.util.LinkedHashSet<Identifier>();
        for (String wood : java.util.List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "pale_oak", "mangrove", "cherry")) {
            addMinecraftIds(expected, wood + "_log", wood + "_wood");
        }
        for (String fungus : java.util.List.of("crimson", "warped")) {
            addMinecraftIds(expected, fungus + "_stem", fungus + "_hyphae");
        }
        addMinecraftIds(expected, "bamboo_block");
        return java.util.Set.copyOf(expected);
    }

    private static void addMinecraftIds(java.util.Set<Identifier> target, String... paths) {
        for (String path : paths) target.add(id("minecraft:" + path));
    }

    private static Identifier expectedMaterialAxisDerivedId(NibaruMaterialProfile profile,
            DerivedGeometrySupport.Geometry geometry) {
        Identifier parent = profile.canonicalParentId();
        return switch (geometry) {
            case VERTICAL_SLAB -> id("clutternomore:more_slabs_stairs_and_walls/vertical_"
                    + parent.getPath() + "_slab");
            case STEP -> id("clutternomore:more_slabs_stairs_and_walls/" + parent.getPath() + "_step");
            case LAYER -> CnmTerrainCompat.layerId(profile);
        };
    }

    private static void assertStateExceptFacing(GameTestHelper helper, BlockState expected, BlockState actual,
            net.minecraft.world.level.block.state.properties.Property<?> facing, String label) {
        expected.getProperties().stream().filter(property -> property != facing).forEach(property ->
                helper.assertTrue(expected.getValue(property).equals(actual.getValue(property)),
                        label + " changed non-facing property " + property.getName()));
    }

    private static void assertSharedState(GameTestHelper helper, BlockState expected, BlockState actual, String label) {
        expected.getProperties().stream().filter(actual::hasProperty).forEach(property ->
                helper.assertTrue(expected.getValue(property).equals(actual.getValue(property)),
                        label + " changed " + property.getName()));
    }

    private static boolean randomlyTicks(Block block) {
        try {
            Class<?> type = block.getClass();
            while (type != null) {
                try {
                    Method method = type.getDeclaredMethod("isRandomlyTicking", BlockState.class);
                    method.setAccessible(true);
                    return (boolean) method.invoke(block, block.defaultBlockState());
                } catch (NoSuchMethodException ignored) {
                    type = type.getSuperclass();
                }
            }
            throw new IllegalStateException("Missing isRandomlyTicking");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static java.util.Optional<NibaruMaterialProfile> providerProfile(Item item) {
        Block block = Block.byItem(item);
        var nativeProfile = NibaruMaterialProfiles.fromBlock(block);
        return nativeProfile.isPresent() ? nativeProfile
                : NibaruProviderAdapter.runtimeBinding(block).map(NibaruProviderAdapter.RuntimeBinding::profile);
    }

    private static void appendExpectedSegment(java.util.List<Item> result, java.util.List<Item> component,
            NibaruMaterialProfile profile) {
        appendIfPresent(result, component, profile.canonicalParent().asItem());
        profile.effectiveSlabSource().map(Block::asItem).ifPresent(item -> appendIfPresent(result, component, item));
        profile.effectiveStairSource().map(Block::asItem).ifPresent(item -> appendIfPresent(result, component, item));
        profile.nativeWall().map(Block::asItem).ifPresent(item -> appendIfPresent(result, component, item));
        NibaruProviderAdapter.derived(profile, DerivedGeometrySupport.Geometry.VERTICAL_SLAB).map(Block::asItem)
                .ifPresent(item -> appendIfPresent(result, component, item));
        NibaruProviderAdapter.derived(profile, DerivedGeometrySupport.Geometry.STEP).map(Block::asItem)
                .ifPresent(item -> appendIfPresent(result, component, item));
        NibaruProviderAdapter.derived(profile, BgeGeometryRole.CORNER).map(Block::asItem)
                .ifPresent(item -> appendIfPresent(result, component, item));
        NibaruProviderAdapter.derived(profile, BgeGeometryRole.QUARTER_COLUMN).map(Block::asItem)
                .ifPresent(item -> appendIfPresent(result, component, item));
        NibaruProviderAdapter.derived(profile, DerivedGeometrySupport.Geometry.LAYER).map(Block::asItem)
                .ifPresent(item -> appendIfPresent(result, component, item));
    }

    private static Item layerItem(NibaruMaterialProfile profile) {
        return BuiltInRegistries.ITEM.getValue(CnmTerrainCompat.layerId(profile));
    }

    private static Item cornerItem(NibaruMaterialProfile profile) {
        return BuiltInRegistries.ITEM.getValue(CnmTerrainCompat.cornerId(profile));
    }

    private static Item columnItem(NibaruMaterialProfile profile) {
        return BuiltInRegistries.ITEM.getValue(CnmTerrainCompat.quarterColumnId(profile));
    }

    private static void appendIfPresent(java.util.List<Item> result, java.util.List<Item> component, Item item) {
        if (component.contains(item) && !result.contains(item)) result.add(item);
    }

    private static void assertShapePresent(GameTestHelper helper, java.util.List<Item> shapes, String id) {
        helper.assertTrue(shapes.contains(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))),
                "Missing canonical dirt shape " + id + ": " + ids(shapes));
    }

    private static void assertShapeAbsent(GameTestHelper helper, java.util.List<Item> shapes, String id) {
        helper.assertTrue(!shapes.contains(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))),
                "Migration-only dirt shape is still presented: " + id + ": " + ids(shapes));
    }

    private static java.util.List<Identifier> ids(java.util.List<Item> shapes) {
        return shapes.stream().map(BuiltInRegistries.ITEM::getKey).toList();
    }

    private static void updateDistance(GameTestHelper helper, BlockPos relativePos) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        BlockState state = helper.getLevel().getBlockState(absolutePos);
        helper.getLevel().setBlock(absolutePos,
                LeafSemantics.updateDistanceFromLogs(state, helper.getLevel(), absolutePos), 3);
    }

    private static void assertDistance(GameTestHelper helper, BlockPos pos, int expected) {
        int actual = helper.getBlockState(pos).getValue(BlockStateProperties.DISTANCE);
        helper.assertTrue(actual == expected, "Expected distance " + expected + " at " + pos + ", got " + actual);
    }

    private static void assertNonSemanticProperties(GameTestHelper helper, BlockState expected, BlockState actual,
            String label) {
        expected.getProperties().stream()
                .filter(property -> property != BlockStateProperties.DISTANCE
                        && property != BlockStateProperties.PERSISTENT)
                .forEach(property -> helper.assertTrue(expected.getValue(property).equals(actual.getValue(property)),
                        label + " property changed: " + property.getName()));
    }

    private static BlockState nonpersistent(BlockState state) {
        return state.setValue(BlockStateProperties.PERSISTENT, false)
                .setValue(BlockStateProperties.DISTANCE, 7);
    }

    private static Block block(Identifier id) {
        return BuiltInRegistries.BLOCK.getValue(id);
    }

    private static Identifier id(String value) {
        return Identifier.parse(value);
    }

    private static boolean invokePointedDripstoneCanGrow(GameTestHelper helper, BlockPos pos)
            throws ReflectiveOperationException {
        Method method = net.minecraft.world.level.block.PointedDripstoneBlock.class
                .getDeclaredMethod("canGrow", net.minecraft.world.level.LevelReader.class, BlockPos.class);
        method.setAccessible(true);
        return (boolean) method.invoke(Blocks.POINTED_DRIPSTONE, helper.getLevel(), helper.absolutePos(pos));
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
