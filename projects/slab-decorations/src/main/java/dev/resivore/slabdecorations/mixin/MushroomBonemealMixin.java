package dev.resivore.slabdecorations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.resivore.slabdecorations.StructureGrowthTransaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/** Runs the vanilla giant-mushroom feature against a transactionally projected parent. */
@Mixin(MushroomBlock.class)
public abstract class MushroomBonemealMixin {
    @WrapMethod(method = "growMushroom")
    private boolean slabDecorations$growAgainstCanonicalParent(
            ServerLevel level, BlockPos pos, BlockState state, RandomSource random,
            Operation<Boolean> original) {
        return StructureGrowthTransaction.run(level, pos, state,
                () -> original.call(level, pos, state, random));
    }
}
