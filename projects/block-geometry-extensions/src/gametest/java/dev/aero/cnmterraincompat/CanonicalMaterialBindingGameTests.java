package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Canonical identity, closure, projection, topology, and real placement regressions for C73. */
public final class CanonicalMaterialBindingGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void everyOwnedBlockIsClassifiedAndEveryNormalFamilyHasNinePrimaryRoles(
            GameTestHelper helper) {
        BgeMaterialBindings.requireValid();
        helper.assertTrue(BgeMaterialBindings.exemptions().isEmpty(),
                "The current BGE catalog unexpectedly contains non-material exemptions: "
                        + BgeMaterialBindings.exemptions());
        for (Block owned : BgeMaterialBindings.ownedBlocks()) {
            helper.assertTrue(BgeMaterialBindings.fromBlock(owned).isPresent(),
                    "BGE-owned block has no canonical classification: " + owned);
        }

        int profiles = 0;
        int primary = 0;
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            profiles++;
            for (BgeMaterialBindings.Role role : BgeMaterialBindings.Role.values()) {
                List<BgeMaterialBindings.Binding> owners = BgeMaterialBindings.all().stream()
                        .filter(binding -> binding.materialProfile().orElse(null) == profile
                                && binding.role() == role
                                && binding.ownership() == BgeMaterialBindings.Ownership.PRIMARY)
                        .toList();
                helper.assertTrue(owners.size() == 1,
                        "Expected one primary " + role + " for " + profile.canonicalParentId()
                                + ", found " + owners.size());
                helper.assertTrue(owners.getFirst().canonicalMaterial() == profile.canonicalParent()
                                && owners.getFirst().membership()
                                        == BgeMaterialBindings.CatalogMembership.NORMAL_CATALOG
                                && owners.getFirst().additionalCatalogGenerationExpected(),
                        "Invalid normal-catalog owner for " + profile.canonicalParentId() + " " + role);
                primary++;
            }
        }
        helper.assertTrue(BgeMaterialBindings.exclusions().isEmpty(),
                "No current normal family needs a technical role exclusion: "
                        + BgeMaterialBindings.exclusions());
        helper.assertTrue(primary == profiles * BgeMaterialBindings.Role.values().length,
                "Nine-role closure count mismatch: profiles=" + profiles + ", primary=" + primary);
        System.out.println("CANONICAL_BINDING_CLOSURE|profiles=" + profiles
                + "|primary=" + primary + "|owned=" + BgeMaterialBindings.ownedBlocks().size()
                + "|aliases=3|special=1|exemptions=0");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void retainedDirtGrassIdentitiesAndFarmlandAreExplicit(GameTestHelper helper) {
        assertBinding(helper, CnmTerrainCompat.DIRT_SLAB, Blocks.DIRT,
                BgeMaterialBindings.Role.HORIZONTAL_SLAB, BgeMaterialBindings.Ownership.RETAINED_ALIAS);
        assertBinding(helper, CnmTerrainCompat.GRASS_SLAB, Blocks.GRASS_BLOCK,
                BgeMaterialBindings.Role.HORIZONTAL_SLAB, BgeMaterialBindings.Ownership.RETAINED_ALIAS);
        assertBinding(helper, CnmTerrainCompat.DIRT_VERTICAL_SLAB, Blocks.DIRT,
                BgeMaterialBindings.Role.VERTICAL_SLAB, BgeMaterialBindings.Ownership.RETAINED_ALIAS);
        assertBinding(helper, CnmTerrainCompat.GRASS_VERTICAL_SLAB, Blocks.GRASS_BLOCK,
                BgeMaterialBindings.Role.VERTICAL_SLAB, BgeMaterialBindings.Ownership.PRIMARY);

        BgeMaterialBindings.Binding farmland = BgeMaterialBindings.fromBlock(
                CnmTerrainCompat.FARMLAND_SLAB).orElseThrow();
        BlockState source = CnmTerrainCompat.FARMLAND_SLAB.defaultBlockState()
                .setValue(FarmlandSlabBlock.MOISTURE, 6)
                .setValue(FarmlandSlabBlock.TYPE, SlabType.TOP);
        BlockState projected = farmland.canonicalState(source).orElseThrow();
        helper.assertTrue(farmland.canonicalMaterial() == Blocks.FARMLAND
                        && farmland.materialProfile().isEmpty()
                        && farmland.ownership() == BgeMaterialBindings.Ownership.SPECIAL
                        && farmland.membership()
                                == BgeMaterialBindings.CatalogMembership.SPECIAL_CANONICAL_BOUND
                        && !farmland.additionalCatalogGenerationExpected()
                        && !farmland.fullOccupancyNormalizationEnabled()
                        && !farmland.normallyObtainable()
                        && farmland.limitationReason().isPresent()
                        && projected.is(Blocks.FARMLAND)
                        && projected.getValue(BlockStateProperties.MOISTURE) == 6
                        && !projected.hasProperty(BlockStateProperties.SLAB_TYPE),
                "Farmland special canonical projection/classification is incomplete: " + farmland);

        assertBounds(helper, farmland, SlabType.BOTTOM, new BgeMaterialBindings.Bounds(0, 0, 0, 16, 7, 16));
        assertBounds(helper, farmland, SlabType.TOP, new BgeMaterialBindings.Bounds(0, 7, 0, 16, 15, 16));
        assertBounds(helper, farmland, SlabType.DOUBLE, new BgeMaterialBindings.Bounds(0, 0, 0, 16, 15, 16));
        helper.assertTrue(CnmTerrainCompat.FARMLAND_SLAB.asItem() == net.minecraft.world.item.Items.AIR,
                "Farmland special binding gained a BlockItem");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void catalogWideSlabProjectionAndOccupancyAreDefined(GameTestHelper helper) {
        int horizontal = 0;
        int vertical = 0;
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            BgeMaterialBindings.Binding slab = primary(profile,
                    BgeMaterialBindings.Role.HORIZONTAL_SLAB);
            BlockState slabState = slab.physicalBlock().defaultBlockState()
                    .setValue(SlabBlock.TYPE, SlabType.DOUBLE);
            helper.assertTrue(slab.topology().isFullOccupancy(slabState)
                            && slab.canonicalState(slabState).orElseThrow().is(profile.canonicalParent()),
                    "Horizontal projection/occupancy failed for " + profile.canonicalParentId());
            horizontal++;

            BgeMaterialBindings.Binding verticalSlab = primary(profile,
                    BgeMaterialBindings.Role.VERTICAL_SLAB);
            BlockState verticalState = verticalSlab.physicalBlock().defaultBlockState()
                    .setValue(VerticalSlabBlock.DOUBLE, true);
            helper.assertTrue(verticalSlab.topology().isFullOccupancy(verticalState)
                            && verticalSlab.canonicalState(verticalState).orElseThrow()
                                    .is(profile.canonicalParent()),
                    "Vertical projection/occupancy failed for " + profile.canonicalParentId());
            vertical++;
        }
        helper.assertTrue(horizontal == NibaruMaterialProfiles.all().size()
                        && vertical == horizontal,
                "Catalog-wide normalization matrix did not cover every profile");
        System.out.println("CANONICAL_PROJECTION_MATRIX|horizontal=" + horizontal
                + "|vertical=" + vertical + "|properties=axis,pattern,distance,persistent,snowy,waterlogged");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void horizontalCompletionUsesRealBlockItemPathAndPreservesMaterialState(
            GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        BlockPos target = new BlockPos(3, 2, 3);

        // Exact accepted-C70 effective vanilla source plus reverse order.
        completeHorizontal(helper, player, target, Blocks.STONE_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM), Direction.UP, Blocks.STONE,
                state -> true, "Stone bottom+top");
        completeHorizontal(helper, player, target, Blocks.STONE_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.TOP), Direction.DOWN, Blocks.STONE,
                state -> true, "Stone top+bottom");

        NibaruMaterialProfile oakLog = profile(Blocks.OAK_LOG);
        Block oakLogSlab = primary(oakLog, BgeMaterialBindings.Role.HORIZONTAL_SLAB).physicalBlock();
        completeHorizontal(helper, player, target, oakLogSlab.defaultBlockState()
                        .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                        .setValue(BlockStateProperties.AXIS, Direction.Axis.X),
                Direction.UP, Blocks.OAK_LOG,
                state -> state.getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
                "Oak Log axis");

        NibaruMaterialProfile leaves = profile(Blocks.OAK_LEAVES);
        Block leavesSlab = primary(leaves, BgeMaterialBindings.Role.HORIZONTAL_SLAB).physicalBlock();
        completeHorizontal(helper, player, target, leavesSlab.defaultBlockState()
                        .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                        .setValue(BlockStateProperties.DISTANCE, 4)
                        .setValue(BlockStateProperties.PERSISTENT, true),
                Direction.UP, Blocks.OAK_LEAVES,
                state -> state.getValue(BlockStateProperties.PERSISTENT), "Oak Leaves persistent");

        NibaruMaterialProfile grass = profile(Blocks.GRASS_BLOCK);
        Block grassSlab = primary(grass, BgeMaterialBindings.Role.HORIZONTAL_SLAB).physicalBlock();
        completeHorizontal(helper, player, target, grassSlab.defaultBlockState()
                        .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                        .setValue(BlockStateProperties.SNOWY, true),
                Direction.UP, Blocks.GRASS_BLOCK,
                state -> state.getValue(BlockStateProperties.SNOWY), "Grass snowy state",
                () -> helper.setBlock(target.above(), Blocks.SNOW));
        helper.setBlock(target.above(), Blocks.AIR);

        Block whiteGlazed = Blocks.GLAZED_TERRACOTTA.pick(DyeColor.WHITE);
        NibaruMaterialProfile glazed = profile(whiteGlazed);
        Block glazedSlab = primary(glazed, BgeMaterialBindings.Role.HORIZONTAL_SLAB).physicalBlock();
        completeHorizontal(helper, player, target, glazedSlab.defaultBlockState()
                        .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST),
                Direction.UP, whiteGlazed,
                state -> state.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.WEST,
                "Glazed orientation");

        ExternalMaterialFamilies.Binding external = ExternalMaterialFamilies.all().getFirst();
        completeHorizontal(helper, player, target, external.slab().defaultBlockState()
                        .setValue(SlabBlock.TYPE, SlabType.BOTTOM),
                Direction.UP, external.source(), state -> true, "External provider");

        // A placement that does not complete remains a slab and consumes exactly one item.
        helper.setBlock(target.below(), Blocks.STONE);
        helper.setBlock(target, Blocks.AIR);
        ItemStack partialStack = new ItemStack(Blocks.STONE_SLAB, 1);
        InteractionResult partialResult = place(helper, player, partialStack, target.below(),
                Direction.UP, 0.5, 1.0, 0.5);
        helper.assertTrue(partialResult.consumesAction()
                        && helper.getBlockState(target).is(Blocks.STONE_SLAB)
                        && helper.getBlockState(target).getValue(SlabBlock.TYPE) != SlabType.DOUBLE
                        && partialStack.isEmpty(),
                "Non-completing horizontal placement changed identity or economy");

        // Historical full derived states remain valid/settable and are not migrated in-place.
        BlockState historical = Blocks.STONE_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        helper.setBlock(target, historical);
        helper.assertTrue(helper.getBlockState(target).equals(historical),
                "Historical horizontal DOUBLE state was rewritten outside placement");

        // Different material and an invalid face cannot replace/normalize the existing slab.
        helper.setBlock(target, Blocks.STONE_SLAB.defaultBlockState());
        helper.setBlock(target.above(), Blocks.BEDROCK);
        ItemStack wrongMaterial = new ItemStack(Blocks.OAK_SLAB, 1);
        place(helper, player, wrongMaterial, target, Direction.UP, 0.5, 1.0, 0.5);
        helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE_SLAB)
                        && helper.getBlockState(target).getValue(SlabBlock.TYPE) != SlabType.DOUBLE
                        && wrongMaterial.getCount() == 1,
                "Incompatible horizontal material normalized or consumed an item");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void verticalCompletionUsesRealCnmPathAndPreservesMaterialState(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        BlockPos target = new BlockPos(3, 2, 3);

        completeVertical(helper, player, target, profile(Blocks.STONE), Blocks.STONE,
                state -> true, "Stone");
        completeVertical(helper, player, target, profile(Blocks.OAK_LOG), Blocks.OAK_LOG,
                state -> state.getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
                "Oak Log axis", state -> state.setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        completeVertical(helper, player, target, profile(Blocks.OAK_LEAVES), Blocks.OAK_LEAVES,
                state -> state.getValue(BlockStateProperties.PERSISTENT),
                "Oak Leaves persistent", state -> state
                        .setValue(BlockStateProperties.DISTANCE, 3)
                        .setValue(BlockStateProperties.PERSISTENT, true));
        Block whiteGlazed = Blocks.GLAZED_TERRACOTTA.pick(DyeColor.WHITE);
        completeVertical(helper, player, target, profile(whiteGlazed),
                whiteGlazed,
                state -> state.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.WEST,
                "Glazed pattern", state -> state.setValue(
                        GlazedPatternState.PATTERN_FACING, Direction.WEST));
        ExternalMaterialFamilies.Binding external = ExternalMaterialFamilies.all().getFirst();
        completeVertical(helper, player, target, external.profile(), external.source(),
                state -> true, "External provider");

        BgeMaterialBindings.Binding stoneBinding = primary(profile(Blocks.STONE),
                BgeMaterialBindings.Role.VERTICAL_SLAB);
        Block verticalStone = stoneBinding.physicalBlock();
        BlockState historical = verticalStone.defaultBlockState()
                .setValue(VerticalSlabBlock.DOUBLE, true);
        helper.setBlock(target, historical);
        helper.assertTrue(helper.getBlockState(target).equals(historical),
                "Historical Vertical Slab double state was rewritten outside placement");

        helper.setBlock(target, verticalStone.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.NORTH));
        helper.setBlock(target.above(), Blocks.BEDROCK);
        Block oakVertical = primary(profile(Blocks.OAK_PLANKS),
                BgeMaterialBindings.Role.VERTICAL_SLAB).physicalBlock();
        ItemStack incompatible = new ItemStack(oakVertical, 1);
        place(helper, player, incompatible, target, Direction.UP, 0.5, 0.5, 0.75);
        helper.assertTrue(helper.getBlockState(target).is(verticalStone)
                        && !helper.getBlockState(target).getValue(VerticalSlabBlock.DOUBLE)
                        && incompatible.getCount() == 1,
                "Incompatible Vertical Slab normalized or consumed an item");

        ItemStack occupiedFace = new ItemStack(verticalStone, 1);
        place(helper, player, occupiedFace, target, Direction.UP, 0.5, 0.5, 0.25);
        helper.assertTrue(helper.getBlockState(target).is(verticalStone)
                        && !helper.getBlockState(target).getValue(VerticalSlabBlock.DOUBLE)
                        && occupiedFace.getCount() == 1,
                "Occupied-face Vertical Slab placement falsely completed");
        helper.succeed();
    }

    private static void completeHorizontal(GameTestHelper helper, ServerPlayer player, BlockPos target,
            BlockState existing, Direction face, Block canonical,
            java.util.function.Predicate<BlockState> materialAssertion, String label) {
        completeHorizontal(helper, player, target, existing, face, canonical, materialAssertion, label, () -> {});
    }

    private static void completeHorizontal(GameTestHelper helper, ServerPlayer player, BlockPos target,
            BlockState existing, Direction face, Block canonical,
            java.util.function.Predicate<BlockState> materialAssertion, String label,
            Runnable afterExistingPlaced) {
        helper.setBlock(target, existing);
        afterExistingPlaced.run();
        ItemStack stack = new ItemStack(existing.getBlock(), 1);
        double localY = face == Direction.DOWN ? 0.0 : 1.0;
        InteractionResult result = place(helper, player, stack, target, face, 0.5, localY, 0.5);
        BlockState completed = helper.getBlockState(target);
        helper.assertTrue(result.consumesAction() && completed.is(canonical)
                        && materialAssertion.test(completed) && stack.isEmpty(),
                label + " did not place canonical state or consume exactly one item: state="
                        + completed + ", count=" + stack.getCount() + ", result=" + result);
    }

    private static void completeVertical(GameTestHelper helper, ServerPlayer player, BlockPos target,
            NibaruMaterialProfile profile, Block canonical,
            java.util.function.Predicate<BlockState> materialAssertion, String label) {
        completeVertical(helper, player, target, profile, canonical, materialAssertion, label,
                java.util.function.UnaryOperator.identity());
    }

    private static void completeVertical(GameTestHelper helper, ServerPlayer player, BlockPos target,
            NibaruMaterialProfile profile, Block canonical,
            java.util.function.Predicate<BlockState> materialAssertion, String label,
            java.util.function.UnaryOperator<BlockState> decorate) {
        Block vertical = primary(profile, BgeMaterialBindings.Role.VERTICAL_SLAB).physicalBlock();
        BlockState existing = decorate.apply(vertical.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.NORTH)
                .setValue(VerticalSlabBlock.DOUBLE, false));
        helper.setBlock(target, existing);
        ItemStack stack = new ItemStack(vertical, 1);
        InteractionResult result = place(helper, player, stack, target,
                Direction.UP, 0.5, 0.5, 0.75);
        BlockState completed = helper.getBlockState(target);
        helper.assertTrue(result.consumesAction() && completed.is(canonical)
                        && materialAssertion.test(completed) && stack.isEmpty(),
                label + " Vertical Slab did not place canonical state or consume exactly one item: state="
                        + completed + ", count=" + stack.getCount() + ", result=" + result);
    }

    private static void assertBinding(GameTestHelper helper, Block physical, Block canonical,
            BgeMaterialBindings.Role role, BgeMaterialBindings.Ownership ownership) {
        BgeMaterialBindings.Binding binding = BgeMaterialBindings.fromBlock(physical).orElseThrow();
        helper.assertTrue(binding.canonicalMaterial() == canonical
                        && binding.role() == role && binding.ownership() == ownership,
                "Wrong canonical binding for " + physical + ": " + binding);
    }

    private static void assertBounds(GameTestHelper helper, BgeMaterialBindings.Binding binding,
            SlabType type, BgeMaterialBindings.Bounds expected) {
        BlockState state = binding.physicalBlock().defaultBlockState()
                .setValue(FarmlandSlabBlock.TYPE, type);
        helper.assertTrue(binding.topology().bounds(state).orElseThrow().equals(expected)
                        && !binding.topology().isFullOccupancy(state),
                "Wrong Farmland Slab topology for " + type + ": "
                        + binding.topology().bounds(state));
    }

    private static BgeMaterialBindings.Binding primary(NibaruMaterialProfile profile,
            BgeMaterialBindings.Role role) {
        return BgeMaterialBindings.all().stream()
                .filter(binding -> binding.materialProfile().orElse(null) == profile
                        && binding.role() == role
                        && binding.ownership() == BgeMaterialBindings.Ownership.PRIMARY)
                .findFirst().orElseThrow();
    }

    private static NibaruMaterialProfile profile(Block block) {
        return NibaruMaterialProfiles.fromBlock(block).orElseThrow();
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static InteractionResult place(GameTestHelper helper, ServerPlayer player, ItemStack stack,
            BlockPos relativePos, Direction face, double localX, double localY, double localZ) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(relativePos);
        BlockHitResult hit = new BlockHitResult(new Vec3(absolute.getX() + localX,
                absolute.getY() + localY, absolute.getZ() + localZ), face, absolute, false);
        return player.gameMode.useItemOn(player, helper.getLevel(), stack,
                InteractionHand.MAIN_HAND, hit);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
