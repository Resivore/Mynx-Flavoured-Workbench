package games.twinhead.moreslabsstairsandwalls.mixin;

import games.twinhead.moreslabsstairsandwalls.block.spreadable.PodzolGeometryConversion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

@Mixin(TreeDecorator.Context.class)
public abstract class TreeDecoratorGeneratorMixin {

    @Shadow @Final private WorldGenLevel level;

    @Shadow @Final private BiConsumer<BlockPos, BlockState> decorationSetter;
    @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
    private void replace(BlockPos pos, BlockState state, CallbackInfo ci){
        if (state.is(Blocks.PODZOL)){
            BlockState replacement = PodzolGeometryConversion.convert(level.getBlockState(pos));
            if (replacement != null) {
                this.decorationSetter.accept(pos, replacement);
                ci.cancel();
            }
        }
    }

}
