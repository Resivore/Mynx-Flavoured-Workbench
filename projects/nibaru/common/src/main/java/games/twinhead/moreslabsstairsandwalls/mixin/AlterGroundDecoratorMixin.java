package games.twinhead.moreslabsstairsandwalls.mixin;

import games.twinhead.moreslabsstairsandwalls.block.spreadable.PodzolGeometryConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.AlterGroundDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AlterGroundDecorator.class)
public abstract class AlterGroundDecoratorMixin {
    @Inject(method = "placeBlockAt", at = @At("HEAD"), cancellable = true)
    private void moreSlabsStairsAndWalls$replaceGeometry(
            TreeDecorator.Context context, BlockPos origin, CallbackInfo ci) {
        for (int offset = 2; offset >= -3; --offset) {
            BlockPos target = origin.above(offset);
            BlockState replacement = PodzolGeometryConversion.convert(
                    context.level().getBlockState(target));
            if (replacement != null) {
                context.setBlock(target, replacement);
                ci.cancel();
                return;
            }
        }
    }
}
