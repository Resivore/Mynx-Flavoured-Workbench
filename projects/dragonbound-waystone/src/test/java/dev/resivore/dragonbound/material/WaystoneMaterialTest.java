package dev.resivore.dragonbound.material;

import dev.resivore.dragonbound.DragonboundContent;
import dev.resivore.dragonbound.block.DragonboundWaystoneBlockEntity;
import dev.resivore.dragonbound.recipe.WaystoneMaterialRecipe;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.storage.TagValueInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WaystoneMaterialTest {
    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!BuiltInRegistries.ITEM.containsKey(DragonboundContent.WAYSTONE_ID)) {
            DragonboundContent.register();
        }
        var builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var vanillaData = VanillaRegistries.createLookup();
        registries = HolderLookup.Provider.create(
                java.util.stream.Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries().filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
    }

    @Test
    void componentlessWaystoneRetainsEndStoneBricksAsItsEffectiveMaterial() {
        assertEquals(
                Identifier.withDefaultNamespace("end_stone_bricks"),
                WaystoneMaterial.effectiveBlockId(new ItemStack(DragonboundContent.WAYSTONE_ITEM)));
    }

    @Test
    void opaqueFullBlockMatchesTheTwoInputShapelessRecipeAndWritesItsIdentity() {
        WaystoneMaterialRecipe recipe = WaystoneMaterialRecipe.INSTANCE;
        CraftingInput input = input(
                new ItemStack(Items.STONE),
                new ItemStack(DragonboundContent.WAYSTONE_ITEM));

        assertTrue(recipe.matches(input, null));
        ItemStack result = recipe.assemble(input);
        assertEquals(1, result.getCount());
        assertEquals(Identifier.withDefaultNamespace("stone"), result.get(WaystoneMaterial.BLOCK_ID));
    }

    @Test
    void warpedHyphaeCraftingAndPlacedVisualSyncRetainOnlyTheDonorIdentity() {
        WaystoneMaterialRecipe recipe = WaystoneMaterialRecipe.INSTANCE;
        ItemStack materialized = recipe.assemble(input(
                new ItemStack(DragonboundContent.WAYSTONE_ITEM), new ItemStack(Items.WARPED_HYPHAE)));
        assertEquals(Identifier.withDefaultNamespace("warped_hyphae"), materialized.get(WaystoneMaterial.BLOCK_ID));

        DragonboundWaystoneBlockEntity serverEntity = new DragonboundWaystoneBlockEntity(
                BlockPos.ZERO,
                DragonboundContent.WAYSTONE.defaultBlockState());
        serverEntity.setPlacedStack(materialized);
        CompoundTag updateTag = serverEntity.getUpdateTag(registries);

        assertTrue(updateTag.contains("visual_material"));
        assertFalse(updateTag.contains("placed_stack"));

        DragonboundWaystoneBlockEntity clientEntity = new DragonboundWaystoneBlockEntity(
                BlockPos.ZERO,
                DragonboundContent.WAYSTONE.defaultBlockState());
        clientEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, updateTag));

        assertEquals(Identifier.withDefaultNamespace("warped_hyphae"), clientEntity.visualMaterialId().orElseThrow());
        assertTrue(clientEntity.copyPlacedStack().isEmpty());

        CompoundTag ineligibleUpdateTag = new CompoundTag();
        ineligibleUpdateTag.putString("visual_material", "minecraft:glass");
        clientEntity.loadWithComponents(TagValueInput.create(
                ProblemReporter.DISCARDING, registries, ineligibleUpdateTag));

        assertTrue(clientEntity.visualMaterialId().isEmpty());
    }

    @Test
    void componentlessPlacedWaystoneSynchronizesNoVisualMaterialAndFallsBackToTheBaseModel() {
        DragonboundWaystoneBlockEntity serverEntity = new DragonboundWaystoneBlockEntity(
                BlockPos.ZERO,
                DragonboundContent.WAYSTONE.defaultBlockState());
        serverEntity.setPlacedStack(new ItemStack(DragonboundContent.WAYSTONE_ITEM));

        CompoundTag updateTag = serverEntity.getUpdateTag(registries);
        assertFalse(updateTag.contains("visual_material"));

        DragonboundWaystoneBlockEntity clientEntity = new DragonboundWaystoneBlockEntity(
                BlockPos.ZERO,
                DragonboundContent.WAYSTONE.defaultBlockState());
        clientEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, updateTag));

        assertTrue(clientEntity.visualMaterialId().isEmpty());
    }

    @Test
    void matchingIsShapelessButRequiresExactlyOneWaystoneAndOneDonor() {
        WaystoneMaterialRecipe recipe = WaystoneMaterialRecipe.INSTANCE;

        assertTrue(recipe.matches(input(
                new ItemStack(DragonboundContent.WAYSTONE_ITEM), new ItemStack(Items.SANDSTONE)), null));
        assertFalse(recipe.matches(input(
                new ItemStack(DragonboundContent.WAYSTONE_ITEM), new ItemStack(Items.STONE),
                new ItemStack(Items.STICK)), null));
        assertFalse(recipe.matches(input(
                new ItemStack(DragonboundContent.WAYSTONE_ITEM), new ItemStack(Items.STONE),
                new ItemStack(Items.SANDSTONE)), null));
    }

    @Test
    void transparentNonFullAndTintDependentDonorsFailClosed() {
        WaystoneMaterialRecipe recipe = WaystoneMaterialRecipe.INSTANCE;

        assertFalse(recipe.matches(input(new ItemStack(DragonboundContent.WAYSTONE_ITEM), new ItemStack(Items.GLASS)), null));
        assertFalse(recipe.matches(input(new ItemStack(DragonboundContent.WAYSTONE_ITEM), new ItemStack(Items.STONE_SLAB)), null));
        assertFalse(recipe.matches(input(new ItemStack(DragonboundContent.WAYSTONE_ITEM), new ItemStack(Items.GRASS_BLOCK)), null));
    }

    @Test
    void recraftingReplacesRatherThanAccumulatesMaterialIdentity() {
        WaystoneMaterialRecipe recipe = WaystoneMaterialRecipe.INSTANCE;
        ItemStack previouslyMaterialized = new ItemStack(DragonboundContent.WAYSTONE_ITEM);
        previouslyMaterialized.set(WaystoneMaterial.BLOCK_ID, Identifier.withDefaultNamespace("stone"));

        ItemStack result = recipe.assemble(input(previouslyMaterialized, new ItemStack(Items.SANDSTONE)));

        assertEquals(Identifier.withDefaultNamespace("sandstone"), result.get(WaystoneMaterial.BLOCK_ID));
    }

    @Test
    void unresolvedMaterialIdentityFallsBackWithoutMakingTheStackInvalid() {
        ItemStack malformed = new ItemStack(DragonboundContent.WAYSTONE_ITEM);
        malformed.set(WaystoneMaterial.BLOCK_ID, Identifier.fromNamespaceAndPath("removed_mod", "gone"));

        assertTrue(WaystoneMaterial.selectedBlockId(malformed).isEmpty());
        assertEquals(Identifier.withDefaultNamespace("end_stone_bricks"), WaystoneMaterial.effectiveBlockId(malformed));
    }

    @Test
    void placedStackCopyPreservesTheExactMaterialComponentForBreakAndLossProtection() throws IOException {
        ItemStack materialized = new ItemStack(DragonboundContent.WAYSTONE_ITEM);
        materialized.set(WaystoneMaterial.BLOCK_ID, Identifier.withDefaultNamespace("oak_log"));
        DragonboundWaystoneBlockEntity blockEntity = new DragonboundWaystoneBlockEntity(
                BlockPos.ZERO,
                DragonboundContent.WAYSTONE.defaultBlockState());

        blockEntity.setPlacedStack(materialized);
        ItemStack returned = blockEntity.copyPlacedStack();

        assertEquals(Identifier.withDefaultNamespace("oak_log"), returned.get(WaystoneMaterial.BLOCK_ID));
        assertEquals(1, returned.getCount());
        String protection = Files.readString(Path.of(
                "src/main/java/dev/resivore/dragonbound/block/WaystoneLossProtection.java"));
        assertTrue(protection.contains("returnedStack.copyWithCount(1)"));
    }

    @Test
    void broadDefaultEligibilityKeepsDirectionalBuildingBlocksAndRejectsNoModelSubstitutes() {
        assertTrue(WaystoneMaterial.isEligibleBlock(Blocks.WARPED_HYPHAE));
        assertTrue(WaystoneMaterial.isEligibleBlock(Blocks.STONE));
        assertTrue(WaystoneMaterial.isEligibleBlock(Blocks.SANDSTONE));
        assertTrue(WaystoneMaterial.isEligibleBlock(Blocks.OAK_LOG));
        assertTrue(WaystoneMaterial.isEligibleBlock(Blocks.FURNACE));
        assertTrue(WaystoneMaterial.isEligibleBlock(Blocks.GLAZED_TERRACOTTA.white()));
        assertFalse(WaystoneMaterial.isEligibleBlock(Blocks.GLASS));
        assertFalse(WaystoneMaterial.isEligibleBlock(Blocks.STONE_SLAB));
    }

    private static CraftingInput input(ItemStack... stacks) {
        return CraftingInput.of(stacks.length, 1, List.of(stacks));
    }
}
