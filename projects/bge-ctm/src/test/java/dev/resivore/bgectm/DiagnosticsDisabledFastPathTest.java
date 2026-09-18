package dev.resivore.bgectm;

import dev.resivore.bgectm.continuity.OverlayAttemptContext;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import net.minecraft.server.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Architectural proof for the production-default diagnostic fast path. */
final class DiagnosticsDisabledFastPathTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void defaultDisabledPathCreatesNoOverlayAttemptContext() {
        assertFalse(BgeCtmDiagnostics.enabled(), "Diagnostics must be opt-in");
        OverlayAttemptContext.begin(BlockPos.ZERO, Blocks.STONE.defaultBlockState(),
                Blocks.STONE.defaultBlockState(), BlockPos.ZERO.east(),
                Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Direction.UP);
        OverlayAttemptContext.gate(false, true);
        OverlayAttemptContext.connectBlocks(true);
        assertNull(OverlayAttemptContext.current(), "Disabled diagnostics must not set ThreadLocal state");
        assertNull(OverlayAttemptContext.end(), "Disabled diagnostics must not create an Attempt");
    }
}
