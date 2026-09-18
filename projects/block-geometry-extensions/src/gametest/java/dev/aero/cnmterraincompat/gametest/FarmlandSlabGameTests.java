package dev.aero.cnmterraincompat.gametest;

import com.mojang.datafixers.util.Pair;
import dev.aero.cnmterraincompat.CnmTerrainCompat;
import dev.aero.cnmterraincompat.FarmlandSlabBlock;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import dev.aero.cnmterraincompat.mixin.HoeItemAccessor;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Runtime contracts for the C71 Farmland Slab plus C72's lower-water hydration extension. */
public final class FarmlandSlabGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void actualHoeRegistryHasExactDynamicBgeParity(GameTestHelper helper) {
        Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> tillables =
                HoeItemAccessor.bge$getTillables();
        Set<Block> expectedParents = rules().stream()
                .map(HoeRule::vanillaSource)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Block> actualParents = new LinkedHashSet<>();

        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            if (profile.nativeSlab().isEmpty()) continue;
            boolean parentTillable = tillables.containsKey(profile.canonicalParent());
            boolean slabTillable = tillables.containsKey(profile.nativeSlab().orElseThrow());
            helper.assertTrue(parentTillable == slabTillable,
                    "HoeItem/BGE slab eligibility drifted for " + profile.canonicalParentId());
            if (parentTillable) actualParents.add(profile.canonicalParent());
        }

        helper.assertTrue(actualParents.equals(expectedParents),
                "Expected the exact five 26.2 HoeItem profile sources, actual="
                        + ids(actualParents) + ", expected=" + ids(expectedParents));
        helper.assertTrue(!tillables.containsKey(slab(ModBlocks.PODZOL))
                        && !tillables.containsKey(slab(ModBlocks.MYCELIUM)),
                "Class-sharing Podzol or Mycelium slab became tillable");
        helper.assertTrue(tillables.containsKey(CnmTerrainCompat.DIRT_SLAB)
                        && tillables.containsKey(CnmTerrainCompat.GRASS_SLAB),
                "Legacy horizontal Dirt/Grass compatibility identities are not tillable");

        ServerPlayer player = survivalPlayer(helper);
        BlockPos vanillaPos = new BlockPos(1, 1, 1);
        BlockPos slabPos = new BlockPos(3, 1, 1);
        for (HoeRule rule : rules()) {
            Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> vanilla =
                    requireRule(tillables, rule.vanillaSource());
            Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> bge =
                    requireRule(tillables, rule.slabSource());
            for (Direction face : List.of(Direction.UP, Direction.DOWN)) {
                for (boolean obstructed : List.of(false, true)) {
                    helper.setBlock(vanillaPos, rule.vanillaSource());
                    helper.setBlock(slabPos, slabState(rule.slabSource(), SlabType.BOTTOM, false));
                    helper.setBlock(vanillaPos.above(), obstructed ? Blocks.STONE : Blocks.AIR);
                    helper.setBlock(slabPos.above(), obstructed ? Blocks.STONE : Blocks.AIR);
                    UseOnContext vanillaContext = context(helper, player, new ItemStack(Items.IRON_HOE),
                            vanillaPos, face);
                    UseOnContext slabContext = context(helper, player, new ItemStack(Items.IRON_HOE),
                            slabPos, face);
                    helper.assertTrue(vanilla.getFirst().test(vanillaContext)
                                    == bge.getFirst().test(slabContext),
                            "Slab predicate differs from live HoeItem predicate for " + rule.name()
                                    + " face=" + face + " obstructed=" + obstructed);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void exactFiveOutcomesPreserveEverySlabType(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        BlockPos pos = new BlockPos(2, 2, 2);
        boolean oldDrops = helper.getLevel().getGameRules().get(GameRules.BLOCK_DROPS);
        try {
            helper.getLevel().getGameRules().set(GameRules.BLOCK_DROPS, true,
                    helper.getLevel().getServer());
            for (SlabType type : SlabType.values()) {
                for (HoeRule rule : rules()) {
                    helper.killAllEntitiesOfClass(ItemEntity.class);
                    helper.setBlock(pos.above(), Blocks.AIR);
                    helper.setBlock(pos, slabState(rule.slabSource(), type, false));
                    ItemStack hoe = new ItemStack(Items.IRON_HOE);

                    InteractionResult first = useTool(helper, player, hoe, pos, Direction.UP);
                    BlockState firstState = helper.getBlockState(pos);
                    Block expectedFirst = rule.firstResult() == FirstResult.FARMLAND
                            ? CnmTerrainCompat.FARMLAND_SLAB : canonicalDirtSlab();
                    helper.assertTrue(first.consumesAction()
                                    && firstState.is(expectedFirst)
                                    && firstState.getValue(BlockStateProperties.SLAB_TYPE) == type
                                    && hoe.getDamageValue() == 1,
                            rule.name() + " did not produce its exact first result for " + type
                                    + ": result=" + first + ", state=" + firstState
                                    + ", damage=" + hoe.getDamageValue());
                    if (firstState.is(CnmTerrainCompat.FARMLAND_SLAB)) {
                        helper.assertTrue(firstState.getValue(FarmlandSlabBlock.MOISTURE) == 0,
                                rule.name() + " created hydrated Farmland Slab");
                    }

                    if (rule.dropsHangingRoots()) {
                        helper.assertItemEntityCountIs(Items.HANGING_ROOTS, pos, 2.0, 1);
                    } else {
                        helper.assertItemEntityCountIs(Items.HANGING_ROOTS, pos, 2.0, 0);
                    }

                    if (rule.firstResult() == FirstResult.DIRT) {
                        helper.killAllEntitiesOfClass(ItemEntity.class);
                        InteractionResult second = useTool(helper, player, hoe, pos, Direction.UP);
                        BlockState secondState = helper.getBlockState(pos);
                        helper.assertTrue(second.consumesAction()
                                        && secondState.is(CnmTerrainCompat.FARMLAND_SLAB)
                                        && secondState.getValue(FarmlandSlabBlock.TYPE) == type
                                        && secondState.getValue(FarmlandSlabBlock.MOISTURE) == 0
                                        && hoe.getDamageValue() == 2,
                                rule.name() + " did not follow Dirt with Farmland for " + type);
                        helper.assertItemEntityCountIs(Items.HANGING_ROOTS, pos, 2.0, 0);
                    }
                }
            }
        } finally {
            helper.getLevel().getGameRules().set(GameRules.BLOCK_DROPS, oldDrops,
                    helper.getLevel().getServer());
            helper.killAllEntitiesOfClass(ItemEntity.class);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void podzolMyceliumStayNegativeAndLegacyAliasesTill(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        BlockPos pos = new BlockPos(2, 2, 2);
        for (SlabType type : SlabType.values()) {
            for (Block negative : List.of(slab(ModBlocks.PODZOL), slab(ModBlocks.MYCELIUM))) {
                BlockState source = slabState(negative, type, false);
                helper.setBlock(pos, source);
                helper.setBlock(pos.above(), Blocks.AIR);
                ItemStack hoe = new ItemStack(Items.IRON_HOE);
                InteractionResult result = useTool(helper, player, hoe, pos, Direction.UP);
                helper.assertTrue(!result.consumesAction()
                                && helper.getBlockState(pos).equals(source)
                                && hoe.getDamageValue() == 0,
                        BuiltInRegistries.BLOCK.getKey(negative)
                                + " incorrectly tilled for " + type);
            }

            for (Block alias : List.of(CnmTerrainCompat.DIRT_SLAB, CnmTerrainCompat.GRASS_SLAB)) {
                helper.setBlock(pos, slabState(alias, type, false));
                helper.setBlock(pos.above(), Blocks.AIR);
                ItemStack hoe = new ItemStack(Items.IRON_HOE);
                InteractionResult result = useTool(helper, player, hoe, pos, Direction.UP);
                BlockState farmland = helper.getBlockState(pos);
                helper.assertTrue(result.consumesAction()
                                && farmland.is(CnmTerrainCompat.FARMLAND_SLAB)
                                && farmland.getValue(FarmlandSlabBlock.TYPE) == type
                                && farmland.getValue(FarmlandSlabBlock.MOISTURE) == 0
                                && hoe.getDamageValue() == 1,
                        "Legacy alias did not till to canonical Farmland Slab: "
                                + BuiltInRegistries.BLOCK.getKey(alias) + " " + type
                                + ", result=" + result + ", state=" + farmland
                                + ", damage=" + hoe.getDamageValue());
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void faceAirAndRootedExceptionMatchVanilla(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        BlockPos pos = new BlockPos(2, 2, 2);
        List<HoeRule> airGated = new java.util.ArrayList<>(rules().stream()
                .filter(candidate -> !candidate.dropsHangingRoots()).toList());
        airGated.add(new HoeRule("Legacy Dirt alias", Blocks.DIRT, CnmTerrainCompat.DIRT_SLAB,
                FirstResult.FARMLAND, false));
        airGated.add(new HoeRule("Legacy Grass alias", Blocks.GRASS_BLOCK, CnmTerrainCompat.GRASS_SLAB,
                FirstResult.FARMLAND, false));
        for (HoeRule rule : airGated) {
            BlockState source = slabState(rule.slabSource(), SlabType.BOTTOM, false);
            helper.setBlock(pos.above(), Blocks.AIR);
            helper.setBlock(pos, source);
            ItemStack downHoe = new ItemStack(Items.IRON_HOE);
            InteractionResult down = useTool(helper, player, downHoe, pos, Direction.DOWN);
            helper.assertTrue(!down.consumesAction()
                            && helper.getBlockState(pos).equals(source)
                            && downHoe.getDamageValue() == 0,
                    rule.name() + " accepted the forbidden DOWN face");

            helper.setBlock(pos.above(), Blocks.STONE);
            helper.getLevel().setBlock(helper.absolutePos(pos), source, Block.UPDATE_CLIENTS);
            helper.assertTrue(helper.getBlockState(pos).equals(source),
                    rule.name() + " covered-source fixture changed before HoeItem evaluated it");
            ItemStack coveredHoe = new ItemStack(Items.IRON_HOE);
            InteractionResult covered = useTool(helper, player, coveredHoe, pos, Direction.UP);
            helper.assertTrue(!covered.consumesAction()
                            && helper.getBlockState(pos).equals(source)
                            && coveredHoe.getDamageValue() == 0,
                    rule.name() + " tilled without exact air above: result=" + covered
                            + ", state=" + helper.getBlockState(pos)
                            + ", damage=" + coveredHoe.getDamageValue());
        }

        HoeRule rooted = rules().stream().filter(HoeRule::dropsHangingRoots).findFirst().orElseThrow();
        for (SlabType type : SlabType.values()) {
            helper.setBlock(pos, slabState(rooted.slabSource(), type, false));
            helper.setBlock(pos.above(), Blocks.STONE);
            ItemStack hoe = new ItemStack(Items.IRON_HOE);
            InteractionResult result = useTool(helper, player, hoe, pos, Direction.DOWN);
            BlockState dirt = helper.getBlockState(pos);
            helper.assertTrue(result.consumesAction()
                            && dirt.is(canonicalDirtSlab())
                            && dirt.getValue(SlabBlock.TYPE) == type
                            && hoe.getDamageValue() == 1,
                    "Rooted Dirt lost its unconditional predicate for " + type);
            helper.killAllEntitiesOfClass(ItemEntity.class);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void everyWaterloggedTillableSourceRejectsWithoutDeletingWater(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        BlockPos pos = new BlockPos(2, 2, 2);
        List<Block> sources = new java.util.ArrayList<>(rules().stream()
                .map(HoeRule::slabSource).toList());
        sources.add(CnmTerrainCompat.DIRT_SLAB);
        sources.add(CnmTerrainCompat.GRASS_SLAB);

        for (SlabType type : SlabType.values()) {
            for (Block sourceBlock : sources) {
                BlockState source = slabState(sourceBlock, type, true);
                helper.setBlock(pos, source);
                helper.setBlock(pos.above(), Blocks.AIR);
                ItemStack hoe = new ItemStack(Items.IRON_HOE);
                InteractionResult result = useTool(helper, player, hoe, pos, Direction.UP);
                BlockState actual = helper.getBlockState(pos);
                helper.assertTrue(!result.consumesAction()
                                && actual.equals(source)
                                && actual.getValue(SlabBlock.WATERLOGGED)
                                && hoe.getDamageValue() == 0,
                        "Waterlogged source lost water or tilled: "
                                + BuiltInRegistries.BLOCK.getKey(sourceBlock) + " " + type);
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40, skyAccess = true)
    public void moistureHydrationRainDryingAndCropRetention(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WeatherSnapshot weather = WeatherSnapshot.capture(level);
        BlockPos pos = new BlockPos(5, 2, 5);
        try {
            helper.setBiome(Biomes.PLAINS);
            forceDry(level);

            for (SlabType type : SlabType.values()) {
                BlockPos directlyBelow = pos.offset(0, -1, 0);
                BlockPos lowerBoundary = pos.offset(4, -1, 0);
                BlockPos lowerOutsideBoundary = pos.offset(5, -1, 0);
                BlockPos twoBelow = pos.offset(0, -2, 0);

                helper.setBlock(pos, farmland(type, 0));
                helper.setBlock(directlyBelow, Blocks.WATER);
                helper.randomTick(pos);
                helper.assertTrue(helper.getBlockState(pos).getValue(FarmlandSlabBlock.MOISTURE) == 7,
                        "Water directly one block below did not hydrate " + type + " Farmland Slab");
                helper.setBlock(directlyBelow, Blocks.AIR);

                helper.setBlock(pos, farmland(type, 0));
                helper.setBlock(lowerBoundary, Blocks.WATER);
                helper.randomTick(pos);
                helper.assertTrue(helper.getBlockState(pos).getValue(FarmlandSlabBlock.MOISTURE) == 7,
                        "Water at the inclusive four-block/y-1 boundary did not hydrate " + type);
                helper.setBlock(lowerBoundary, Blocks.AIR);

                helper.setBlock(pos, farmland(type, 1));
                helper.setBlock(lowerOutsideBoundary, Blocks.WATER);
                helper.randomTick(pos);
                helper.assertTrue(helper.getBlockState(pos).getValue(FarmlandSlabBlock.MOISTURE) == 0,
                        "Water five blocks horizontally away at y-1 hydrated " + type);
                helper.setBlock(lowerOutsideBoundary, Blocks.AIR);

                helper.setBlock(pos, farmland(type, 1));
                helper.setBlock(twoBelow, Blocks.WATER);
                helper.randomTick(pos);
                helper.assertTrue(helper.getBlockState(pos).getValue(FarmlandSlabBlock.MOISTURE) == 0,
                        "Water two blocks below hydrated " + type);
                helper.setBlock(twoBelow, Blocks.AIR);

                for (int waterY : List.of(0, 1)) {
                    BlockPos existingLevelWater = pos.offset(1, waterY, 0);
                    helper.setBlock(pos, farmland(type, 0));
                    helper.setBlock(existingLevelWater, Blocks.WATER);
                    helper.randomTick(pos);
                    helper.assertTrue(helper.getBlockState(pos).getValue(FarmlandSlabBlock.MOISTURE) == 7,
                            "Existing y" + (waterY == 0 ? "" : "+1")
                                    + " water did not hydrate " + type);
                    helper.setBlock(existingLevelWater, Blocks.AIR);
                }

                helper.setBlock(pos, farmland(type, 7));
                helper.setBlock(directlyBelow, Blocks.WATER);
                helper.randomTick(pos);
                helper.setBlock(directlyBelow, Blocks.AIR);
                helper.randomTick(pos);
                helper.assertTrue(helper.getBlockState(pos).is(CnmTerrainCompat.FARMLAND_SLAB)
                                && helper.getBlockState(pos).getValue(FarmlandSlabBlock.MOISTURE) == 6,
                        "Removing qualifying lower water did not restore normal drying for " + type);
                helper.setBlock(pos, farmland(type, 0));
                helper.randomTick(pos);
                assertDirt(helper, pos, type,
                        "Removing qualifying lower water did not restore reversion for " + type);
            }

            helper.setBlock(pos, farmland(SlabType.TOP, 0));
            forceRain(level);
            helper.assertTrue(level.isRainingAt(helper.absolutePos(pos).above()),
                    "Rain test position is not receiving rain");
            helper.randomTick(pos);
            helper.assertTrue(helper.getBlockState(pos).getValue(FarmlandSlabBlock.MOISTURE) == 7,
                    "Rain did not hydrate Farmland Slab to seven");

            forceDry(level);
            helper.setBlock(pos, farmland(SlabType.TOP, 7));
            helper.randomTick(pos);
            BlockState drying = helper.getBlockState(pos);
            helper.assertTrue(drying.is(CnmTerrainCompat.FARMLAND_SLAB)
                            && drying.getValue(FarmlandSlabBlock.MOISTURE) == 6
                            && drying.getValue(FarmlandSlabBlock.TYPE) == SlabType.TOP,
                    "Dry Farmland Slab did not lose exactly one moisture level");

            helper.setBlock(pos, farmland(SlabType.TOP, 0));
            helper.randomTick(pos);
            assertDirt(helper, pos, SlabType.TOP,
                    "Dry zero-moisture Farmland Slab did not revert on its later tick");

            helper.setBlock(pos, farmland(SlabType.DOUBLE, 1));
            BlockPos absoluteAbove = helper.absolutePos(pos).above();
            level.setBlock(absoluteAbove, Blocks.WHEAT.defaultBlockState(),
                    Block.UPDATE_CLIENTS);
            BlockState forcedWheat = level.getBlockState(absoluteAbove);
            helper.assertTrue(forcedWheat.is(Blocks.WHEAT)
                            && forcedWheat.is(BlockTags.MAINTAINS_FARMLAND),
                    "Force-placed Wheat is not an actual MAINTAINS_FARMLAND state: " + forcedWheat);
            BlockState deferredCropSupport = helper.getBlockState(pos);
            helper.assertTrue(!deferredCropSupport.is(BlockTags.SUPPORTS_CROPS)
                            && !deferredCropSupport.is(BlockTags.GROWS_CROPS),
                    "Crop placement/growth support must remain deferred to Slab Decorations");

            // Force the tagged crop immediately before the zero-moisture random tick so
            // this canary proves Farmland's crop-retention rule without claiming the
            // deferred crop-placement/growth integration.
            level.setBlock(helper.absolutePos(pos), farmland(SlabType.DOUBLE, 0),
                    Block.UPDATE_CLIENTS);
            level.setBlock(absoluteAbove, Blocks.WHEAT.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertTrue(level.getBlockState(absoluteAbove).is(Blocks.WHEAT)
                            && level.getBlockState(absoluteAbove).is(BlockTags.MAINTAINS_FARMLAND),
                    "Force-placed Wheat was absent before the dry crop-retention tick");
            helper.randomTick(pos);
            helper.assertTrue(helper.getBlockState(pos).is(CnmTerrainCompat.FARMLAND_SLAB)
                            && helper.getBlockState(pos).getValue(FarmlandSlabBlock.TYPE)
                            == SlabType.DOUBLE,
                    "Tagged crop did not prevent dry Farmland Slab reversion");

            // Wheat correctly identifies the lifecycle contract above, but it cannot remain
            // placed until the deferred SUPPORTS_CROPS work exists. Use a stable member of
            // the same MAINTAINS_FARMLAND tag for the two-tick retention assertion.
            level.setBlock(helper.absolutePos(pos), farmland(SlabType.DOUBLE, 1),
                    Block.UPDATE_CLIENTS);
            level.setBlock(absoluteAbove, Blocks.OAK_FENCE_GATE.defaultBlockState(),
                    Block.UPDATE_CLIENTS);
            assertMaintainer(helper, pos, Blocks.OAK_FENCE_GATE, "before moisture 1 -> 0");
            helper.randomTick(pos);
            BlockState cropDrying = helper.getBlockState(pos);
            helper.assertTrue(cropDrying.is(CnmTerrainCompat.FARMLAND_SLAB)
                            && cropDrying.getValue(FarmlandSlabBlock.MOISTURE) == 0
                            && cropDrying.getValue(FarmlandSlabBlock.TYPE) == SlabType.DOUBLE,
                    "MAINTAINS_FARMLAND block prevented drying instead of only preventing reversion");
            assertMaintainer(helper, pos, Blocks.OAK_FENCE_GATE, "before zero-moisture retention");
            helper.randomTick(pos);
            helper.assertTrue(helper.getBlockState(pos).is(CnmTerrainCompat.FARMLAND_SLAB)
                            && helper.getBlockState(pos).getValue(FarmlandSlabBlock.TYPE) == SlabType.DOUBLE,
                    "Stable MAINTAINS_FARMLAND block did not retain dry Farmland Slab");
        } finally {
            weather.restore(level);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void obstructionAndTramplingRevertToTypedCanonicalDirt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        int obstructionIndex = 0;
        for (SlabType type : SlabType.values()) {
            BlockPos obstructionPos = pos.offset(obstructionIndex++ * 2, 0, 0);
            helper.setBlock(obstructionPos, farmland(type, 4));
            helper.setBlock(obstructionPos.above(), Blocks.STONE);
            helper.assertTrue(helper.getLevel().getBlockTicks().hasScheduledTick(
                            helper.absolutePos(obstructionPos), CnmTerrainCompat.FARMLAND_SLAB),
                    "Solid upper neighbor did not schedule the one-tick Farmland Slab check for " + type);
            helper.tickBlock(obstructionPos);
            assertDirt(helper, obstructionPos, type,
                    "Scheduled solid-obstruction check did not revert Farmland Slab");
        }

        helper.setBlock(pos.above(), Blocks.AIR);
        ServerPlayer player = survivalPlayer(helper);
        for (SlabType type : SlabType.values()) {
            BlockState state = farmland(type, 3);
            helper.setBlock(pos, state);
            CnmTerrainCompat.FARMLAND_SLAB.fallOn(helper.getLevel(), state,
                    helper.absolutePos(pos), player, 2.0);
            assertDirt(helper, pos, type, "Player trampling did not preserve Dirt slab type");
        }

        boolean oldMobGriefing = helper.getLevel().getGameRules().get(GameRules.MOB_GRIEFING);
        try {
            var cow = helper.spawn(EntityTypes.COW, pos.above());
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, false,
                    helper.getLevel().getServer());
            BlockState protectedState = farmland(SlabType.BOTTOM, 0);
            helper.setBlock(pos, protectedState);
            CnmTerrainCompat.FARMLAND_SLAB.fallOn(helper.getLevel(), protectedState,
                    helper.absolutePos(pos), cow, 2.0);
            helper.assertTrue(helper.getBlockState(pos).equals(protectedState),
                    "Mob trampled Farmland Slab while MOB_GRIEFING was false");

            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, true,
                    helper.getLevel().getServer());
            helper.setBlock(pos, protectedState);
            CnmTerrainCompat.FARMLAND_SLAB.fallOn(helper.getLevel(), protectedState,
                    helper.absolutePos(pos), cow, 2.0);
            assertDirt(helper, pos, SlabType.BOTTOM,
                    "Large mob did not trample with MOB_GRIEFING enabled");

            var chicken = helper.spawn(EntityTypes.CHICKEN, pos.above());
            BlockState sizeProtected = farmland(SlabType.TOP, 0);
            helper.setBlock(pos, sizeProtected);
            CnmTerrainCompat.FARMLAND_SLAB.fallOn(helper.getLevel(), sizeProtected,
                    helper.absolutePos(pos), chicken, 2.0);
            helper.assertTrue(helper.getBlockState(pos).equals(sizeProtected),
                    "Sub-threshold living entity trampled Farmland Slab");
        } finally {
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, oldMobGriefing,
                    helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void stateShapesAndTypedDirtDropsAreExact(GameTestHelper helper) {
        Block farmlandBlock = CnmTerrainCompat.FARMLAND_SLAB;
        BlockState defaultState = farmlandBlock.defaultBlockState();
        helper.assertTrue(defaultState.getProperties().size() == 2
                        && defaultState.hasProperty(FarmlandSlabBlock.TYPE)
                        && defaultState.hasProperty(FarmlandSlabBlock.MOISTURE)
                        && !defaultState.hasProperty(BlockStateProperties.WATERLOGGED)
                        && defaultState.getValue(FarmlandSlabBlock.TYPE) == SlabType.BOTTOM
                        && defaultState.getValue(FarmlandSlabBlock.MOISTURE) == 0
                        && FarmlandSlabBlock.MOISTURE.getPossibleValues().size() == 8
                        && farmlandBlock.getStateDefinition().getPossibleStates().size() == 24,
                "Farmland Slab state definition is not exactly type + moisture[0..7]");

        ServerPlayer player = survivalPlayer(helper);
        BlockPos pos = new BlockPos(2, 2, 2);
        for (SlabType type : SlabType.values()) {
            BlockState state = farmland(type, 0);
            helper.setBlock(pos, state);
            AABB outline = state.getShape(helper.getLevel(), helper.absolutePos(pos),
                    CollisionContext.empty()).bounds();
            AABB collision = state.getCollisionShape(helper.getLevel(), helper.absolutePos(pos),
                    CollisionContext.empty()).bounds();
            double expectedMinY = type == SlabType.TOP ? 7.0 / 16.0 : 0.0;
            double expectedMaxY = type == SlabType.BOTTOM ? 7.0 / 16.0 : 15.0 / 16.0;
            helper.assertTrue(outline.minX == 0.0 && outline.maxX == 1.0
                            && outline.minZ == 0.0 && outline.maxZ == 1.0
                            && outline.minY == expectedMinY && outline.maxY == expectedMaxY
                            && collision.minX == 0.0 && collision.maxX == 1.0
                            && collision.minZ == 0.0 && collision.maxZ == 1.0
                            && collision.minY == expectedMinY && collision.maxY == expectedMaxY,
                    "Farmland geometry changed for " + type + ": outline=" + outline
                            + ", collision=" + collision);

            List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(pos),
                    null, player, ItemStack.EMPTY);
            int expectedCount = type == SlabType.DOUBLE ? 2 : 1;
            helper.assertTrue(drops.size() == 1
                            && drops.getFirst().is(canonicalDirtSlab().asItem())
                            && drops.getFirst().getCount() == expectedCount,
                    "Farmland Slab returned wrong canonical Dirt slab drops for " + type + ": " + drops);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void farmlandHasNoItemProfileOrDerivedGeometry(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.getValue(CnmTerrainCompat.FARMLAND_SLAB_ID)
                        == CnmTerrainCompat.FARMLAND_SLAB
                        && BuiltInRegistries.BLOCK.getKey(CnmTerrainCompat.FARMLAND_SLAB)
                        .equals(CnmTerrainCompat.FARMLAND_SLAB_ID),
                "Farmland Slab registry identity is not its one public canonical ID");
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(CnmTerrainCompat.FARMLAND_SLAB_ID)
                        && CnmTerrainCompat.FARMLAND_SLAB.asItem() == Items.AIR,
                "Farmland Slab unexpectedly registered a BlockItem");
        helper.assertTrue(NibaruMaterialProfiles.fromBlock(CnmTerrainCompat.FARMLAND_SLAB).isEmpty()
                        && NibaruMaterialProfiles.fromBlock(Blocks.FARMLAND).isEmpty()
                        && NibaruProviderAdapter.runtimeBinding(CnmTerrainCompat.FARMLAND_SLAB).isEmpty(),
                "Vanilla or slab Farmland leaked into the material profile/derived-geometry catalog");

        Set<Identifier> farmlandGeometry = new LinkedHashSet<>();
        Set<String> bgeNamespaces = Set.of(CnmTerrainCompat.MOD_ID,
                "more_slabs_stairs_and_walls", "clutternomore");
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null && bgeNamespaces.contains(id.getNamespace())
                    && id.getPath().contains("farmland")) {
                farmlandGeometry.add(id);
            }
        }
        helper.assertTrue(farmlandGeometry.equals(Set.of(CnmTerrainCompat.FARMLAND_SLAB_ID)),
                "Farmland gained unsupported stairs/walls/vertical/step/layer/corner/column geometry: "
                        + farmlandGeometry);
        helper.assertTrue(ShapeMap.getShapes(Items.DIRT).stream()
                        .map(BuiltInRegistries.ITEM::getKey)
                        .noneMatch(id -> id != null && id.getPath().contains("farmland")),
                "Farmland leaked into the Dirt creative ShapeMap");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void dirtPathShovelAndCanary70StoneContractsRemainExact(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        BlockPos pos = new BlockPos(2, 2, 2);
        for (SlabType type : SlabType.values()) {
            helper.setBlock(pos, slabState(canonicalDirtSlab(), type, false));
            helper.setBlock(pos.above(), Blocks.AIR);
            ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
            InteractionResult result = useTool(helper, player, shovel, pos, Direction.UP);
            BlockState path = helper.getBlockState(pos);
            helper.assertTrue(result.consumesAction()
                            && path.is(canonicalPathSlab())
                            && path.getValue(SlabBlock.TYPE) == type
                            && !path.getValue(SlabBlock.WATERLOGGED)
                            && shovel.getDamageValue() == 1,
                    "Existing Dirt -> Dirt Path shovel contract changed for " + type
                            + ": result=" + result + ", state=" + path
                            + ", damage=" + shovel.getDamageValue());
        }

        NibaruMaterialProfile stone = NibaruMaterialProfiles.fromBlock(Blocks.STONE_SLAB).orElseThrow();
        helper.assertTrue(stone.canonicalParent() == Blocks.STONE
                        && stone.nativeSlab().isEmpty()
                        && stone.effectiveSlabSource().orElseThrow() == Blocks.STONE_SLAB,
                "Canary 70 Stone exact-vanilla-slab profile contract regressed");
        helper.succeed();
    }

    private static List<HoeRule> rules() {
        return List.of(
                new HoeRule("Grass Block", Blocks.GRASS_BLOCK, slab(ModBlocks.GRASS_BLOCK),
                        FirstResult.FARMLAND, false),
                new HoeRule("Dirt Path", Blocks.DIRT_PATH, slab(ModBlocks.DIRT_PATH),
                        FirstResult.FARMLAND, false),
                new HoeRule("Dirt", Blocks.DIRT, slab(ModBlocks.DIRT),
                        FirstResult.FARMLAND, false),
                new HoeRule("Coarse Dirt", Blocks.COARSE_DIRT, slab(ModBlocks.COARSE_DIRT),
                        FirstResult.DIRT, false),
                new HoeRule("Rooted Dirt", Blocks.ROOTED_DIRT, slab(ModBlocks.ROOTED_DIRT),
                        FirstResult.DIRT, true));
    }

    private static Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> requireRule(
            Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> rules, Block source) {
        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> rule = rules.get(source);
        if (rule == null) {
            throw new IllegalStateException("Missing HoeItem rule for "
                    + BuiltInRegistries.BLOCK.getKey(source));
        }
        return rule;
    }

    private static Block slab(ModBlocks family) {
        return family.getBlock(ModBlocks.BlockType.SLAB);
    }

    private static Block canonicalDirtSlab() {
        return slab(ModBlocks.DIRT);
    }

    private static Block canonicalPathSlab() {
        return slab(ModBlocks.DIRT_PATH);
    }

    private static BlockState slabState(Block block, SlabType type, boolean waterlogged) {
        return block.defaultBlockState()
                .setValue(SlabBlock.TYPE, type)
                .setValue(SlabBlock.WATERLOGGED, waterlogged);
    }

    private static BlockState farmland(SlabType type, int moisture) {
        return CnmTerrainCompat.FARMLAND_SLAB.defaultBlockState()
                .setValue(FarmlandSlabBlock.TYPE, type)
                .setValue(FarmlandSlabBlock.MOISTURE, moisture);
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static InteractionResult useTool(GameTestHelper helper, ServerPlayer player,
            ItemStack stack, BlockPos relativePos, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player.gameMode.useItemOn(player, helper.getLevel(), stack,
                InteractionHand.MAIN_HAND, hit(helper, relativePos, face));
    }

    private static UseOnContext context(GameTestHelper helper, ServerPlayer player,
            ItemStack stack, BlockPos relativePos, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                hit(helper, relativePos, face));
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos relativePos, Direction face) {
        BlockPos absolute = helper.absolutePos(relativePos);
        Vec3 location = Vec3.atCenterOf(absolute).add(
                face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return new BlockHitResult(location, face, absolute, false);
    }

    private static void assertDirt(GameTestHelper helper, BlockPos pos, SlabType type, String message) {
        BlockState dirt = helper.getBlockState(pos);
        helper.assertTrue(dirt.is(canonicalDirtSlab())
                        && dirt.getValue(SlabBlock.TYPE) == type
                        && !dirt.getValue(SlabBlock.WATERLOGGED),
                message + ": " + dirt);
    }

    private static void assertMaintainer(GameTestHelper helper, BlockPos farmlandPos,
            Block expected, String phase) {
        BlockState above = helper.getBlockState(farmlandPos.above());
        helper.assertTrue(above.is(expected) && above.is(BlockTags.MAINTAINS_FARMLAND),
                "Expected tagged maintainer " + BuiltInRegistries.BLOCK.getKey(expected)
                        + " " + phase + ", actual=" + above
                        + ", maintains=" + above.is(BlockTags.MAINTAINS_FARMLAND));
    }

    private static void forceDry(ServerLevel level) {
        WeatherData weather = level.getWeatherData();
        weather.setClearWeatherTime(6000);
        weather.setRainTime(0);
        weather.setThunderTime(0);
        weather.setRaining(false);
        weather.setThundering(false);
        level.setRainLevel(0.0F);
        level.setThunderLevel(0.0F);
    }

    private static void forceRain(ServerLevel level) {
        WeatherData weather = level.getWeatherData();
        weather.setClearWeatherTime(0);
        weather.setRainTime(6000);
        weather.setThunderTime(0);
        weather.setRaining(true);
        weather.setThundering(false);
        level.setRainLevel(1.0F);
        level.setThunderLevel(0.0F);
    }

    private static Set<Identifier> ids(Set<Block> blocks) {
        return blocks.stream().map(BuiltInRegistries.BLOCK::getKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }

    private enum FirstResult {
        FARMLAND,
        DIRT
    }

    private record HoeRule(String name, Block vanillaSource, Block slabSource,
            FirstResult firstResult, boolean dropsHangingRoots) {}

    private record WeatherSnapshot(int clearWeatherTime, int rainTime, int thunderTime,
            boolean raining, boolean thundering, float rainLevel, float thunderLevel) {
        static WeatherSnapshot capture(ServerLevel level) {
            WeatherData weather = level.getWeatherData();
            return new WeatherSnapshot(weather.getClearWeatherTime(), weather.getRainTime(),
                    weather.getThunderTime(), weather.isRaining(), weather.isThundering(),
                    level.getRainLevel(1.0F), level.getThunderLevel(1.0F));
        }

        void restore(ServerLevel level) {
            WeatherData weather = level.getWeatherData();
            weather.setClearWeatherTime(clearWeatherTime);
            weather.setRainTime(rainTime);
            weather.setThunderTime(thunderTime);
            weather.setRaining(raining);
            weather.setThundering(thundering);
            level.setRainLevel(rainLevel);
            level.setThunderLevel(thunderLevel);
        }
    }
}
