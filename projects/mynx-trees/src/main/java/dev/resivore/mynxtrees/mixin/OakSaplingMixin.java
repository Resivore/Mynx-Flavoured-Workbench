package dev.resivore.mynxtrees.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.resivore.mynxtrees.MossyOakGeneration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SaplingBlock.class)
abstract class OakSaplingMixin {
    @WrapMethod(method = "advanceTree")
    private void mynx$growSapling(ServerLevel level, BlockPos pos, BlockState state, RandomSource random,
                                  Operation<Void> original) {
        var sapling = state.is(Blocks.OAK_SAPLING) ? new MossyOakGeneration.OakSapling(level, pos.immutable()) : null;
        if (sapling == null && !MossyOakGeneration.SAPLING.isBound()) {
            original.call(level, pos, state, random);
            return;
        }
        ScopedValue.where(MossyOakGeneration.SAPLING, sapling).run(() -> original.call(level, pos, state, random));
    }
}
