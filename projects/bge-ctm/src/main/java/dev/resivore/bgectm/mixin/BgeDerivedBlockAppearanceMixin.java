package dev.resivore.bgectm.mixin;

import dev.aero.cnmterraincompat.BgeLayerBlock;
import dev.resivore.bgectm.CanonicalAppearanceResolver;
import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import net.fabricmc.fabric.api.block.v1.FabricBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/** Adds one shared appearance implementation to the three current BGE geometry bases. */
@Mixin({BgeLayerBlock.class, VerticalSlabBlock.class, StepBlock.class})
abstract class BgeDerivedBlockAppearanceMixin implements FabricBlock {
    @Override
    public BlockState getAppearance(BlockState state, BlockAndLightGetter view, BlockPos pos,
            Direction side, @Nullable BlockState sourceState, @Nullable BlockPos sourcePos) {
        return CanonicalAppearanceResolver.resolve(state, view, pos, side, sourceState, sourcePos);
    }
}
