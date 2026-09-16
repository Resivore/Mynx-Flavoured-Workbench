package dev.aero.shulkertrowel.compat;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffhandShulkerPlacementPolicyTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void reservesVanillaAndDyedShulkersOnlyFromTheOffhand() {
        assertTrue(OffhandShulkerPlacementPolicy.blocks(
                (BlockItem) Blocks.SHULKER_BOX.asItem(), InteractionHand.OFF_HAND));
        assertTrue(OffhandShulkerPlacementPolicy.blocks(
                (BlockItem) Blocks.DYED_SHULKER_BOX.red().asItem(), InteractionHand.OFF_HAND));
        assertFalse(OffhandShulkerPlacementPolicy.blocks(
                (BlockItem) Blocks.SHULKER_BOX.asItem(), InteractionHand.MAIN_HAND));
    }

    @Test
    void leavesUnrelatedOffhandBlockItemsAlone() {
        assertFalse(OffhandShulkerPlacementPolicy.blocks(
                (BlockItem) Blocks.STONE.asItem(), InteractionHand.OFF_HAND));
    }
}
