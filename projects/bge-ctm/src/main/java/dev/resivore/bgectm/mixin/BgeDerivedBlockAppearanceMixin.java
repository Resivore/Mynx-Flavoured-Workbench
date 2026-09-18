package dev.resivore.bgectm.mixin;

import dev.resivore.bgectm.CanonicalAppearanceResolver;
import net.fabricmc.fabric.api.block.v1.FabricBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Adds one binding-driven appearance implementation at the Block base. It is inert for unbound
 * blocks and therefore reaches special or future BGE carriers without a Java-class target list.
 */
@Mixin(Block.class)
abstract class BgeDerivedBlockAppearanceMixin implements FabricBlock {
    @Override
    public BlockState getAppearance(BlockState state, BlockAndLightGetter view, BlockPos pos,
            Direction side, @Nullable BlockState sourceState, @Nullable BlockPos sourcePos) {
        return CanonicalAppearanceResolver.resolve(state, view, pos, side, sourceState, sourcePos);
    }
}
