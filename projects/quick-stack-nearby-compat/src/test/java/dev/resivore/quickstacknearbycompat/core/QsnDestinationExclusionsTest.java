package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QsnDestinationExclusionsTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void everyVanillaShelfVariantUsesTheCommonRejectedBlockEntityIdentity() throws Exception {
        List<ShelfBlock> shelfBlocks = vanillaShelfBlocks();
        assertEquals(12, shelfBlocks.size(), "Unexpected Minecraft 26.2 vanilla shelf variant count");

        List<QuickStackMoveEngine.Target> targets = new ArrayList<>();
        for (ShelfBlock shelfBlock : shelfBlocks) {
            BlockEntity blockEntity = shelfBlock.newBlockEntity(BlockPos.ZERO, shelfBlock.defaultBlockState());
            ShelfBlockEntity shelf = assertInstanceOf(ShelfBlockEntity.class, blockEntity);
            assertInstanceOf(Container.class, shelf);
            targets.add(target(shelf));
        }

        assertTrue(QsnDestinationExclusions.filterVanillaShelves(targets).isEmpty());
    }

    @Test
    void ordinaryChestRemainsEligible() {
        ChestBlockEntity chest = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        List<QuickStackMoveEngine.Target> targets = new ArrayList<>(List.of(target(chest)));

        assertSame(targets, QsnDestinationExclusions.filterVanillaShelves(targets));
    }

    @Test
    void barrelRemainsEligible() {
        BarrelBlockEntity barrel = new BarrelBlockEntity(BlockPos.ZERO, Blocks.BARREL.defaultBlockState());
        List<QuickStackMoveEngine.Target> targets = new ArrayList<>(List.of(target(barrel)));

        assertSame(targets, QsnDestinationExclusions.filterVanillaShelves(targets));
    }

    @Test
    void filteringPreservesTheExistingOrderOfOrdinaryTargets() {
        ChestBlockEntity firstChest = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        ShelfBlockEntity shelf = new ShelfBlockEntity(BlockPos.ZERO, Blocks.OAK_SHELF.defaultBlockState());
        BarrelBlockEntity barrel = new BarrelBlockEntity(BlockPos.ZERO, Blocks.BARREL.defaultBlockState());
        ChestBlockEntity secondChest = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        QuickStackMoveEngine.Target first = target(firstChest);
        QuickStackMoveEngine.Target excluded = target(shelf);
        QuickStackMoveEngine.Target second = target(barrel);
        QuickStackMoveEngine.Target third = target(secondChest);

        List<QuickStackMoveEngine.Target> filtered = QsnDestinationExclusions.filterVanillaShelves(
                List.of(first, excluded, second, third));

        assertEquals(List.of(first, second, third), filtered);
    }

    private static List<ShelfBlock> vanillaShelfBlocks() throws IllegalAccessException {
        List<ShelfBlock> shelves = new ArrayList<>();
        for (Field field : Blocks.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !Block.class.isAssignableFrom(field.getType())) {
                continue;
            }
            Object block = field.get(null);
            if (block instanceof ShelfBlock shelf) {
                shelves.add(shelf);
            }
        }
        return shelves;
    }

    private static QuickStackMoveEngine.Target target(Container container) {
        return new QuickStackMoveEngine.Target(container, Set.of());
    }
}
