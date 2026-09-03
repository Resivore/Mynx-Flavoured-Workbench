package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations;

import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride.NoneAquiferOverride;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Consumer-fixture adaptation that requires two density computations to overlap.
 * This makes a shared or exhausted beardifier cursor fail deterministically.
 */
public final class BlockingTerrainAdaptation extends EnhancedTerrainAdaptation {
    private final CyclicBarrier overlapBarrier = new CyclicBarrier(2);

    public BlockingTerrainAdaptation() {
        super(7, 4, TerrainAction.CARVE, TerrainAction.BURY, 0.0, Padding.ZERO, NoneAquiferOverride.INSTANCE);
    }

    @Override
    public EnhancedTerrainAdaptationType<?> type() {
        return EnhancedTerrainAdaptationType.CUSTOM;
    }

    @Override
    public double computeDensityFactor(int xDistance, int yDistance, int zDistance, int yDistanceToPieceBottom) {
        try {
            overlapBarrier.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("parallel density overlap was interrupted", exception);
        } catch (BrokenBarrierException | TimeoutException exception) {
            throw new AssertionError("parallel density evaluations did not both reach the adaptation", exception);
        }
        return super.computeDensityFactor(xDistance, yDistance, zDistance, yDistanceToPieceBottom);
    }
}
