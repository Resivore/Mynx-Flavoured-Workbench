package com.yungnickyoung.minecraft.ribbits.entity;

import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionResultContractTest {
    @Test
    void minecraftTwentySixSuccessPreservesOldSidedSuccessSemantics() {
        assertTrue(InteractionResult.SUCCESS.consumesAction());
        assertTrue(InteractionResult.CONSUME.consumesAction());
        assertEquals(InteractionResult.SwingSource.CLIENT,
                ((InteractionResult.Success) InteractionResult.SUCCESS).swingSource());
        assertEquals(InteractionResult.SwingSource.NONE,
                ((InteractionResult.Success) InteractionResult.CONSUME).swingSource());
    }
}
