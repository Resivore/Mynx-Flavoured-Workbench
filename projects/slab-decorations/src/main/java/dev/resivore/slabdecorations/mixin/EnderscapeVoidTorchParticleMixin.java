package dev.resivore.slabdecorations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps Void Torch's existing Void Star particle at its projected floor-torch height. */
@Pseudo
@Mixin(targets = "net.penumbra.enderscape.block.VoidTorchBlock", remap = false)
public abstract class EnderscapeVoidTorchParticleMixin {
    @WrapOperation(
            method = "animateTick",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void slabDecorations$alignVoidTorchParticle(
            Level particleLevel,
            ParticleOptions options,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            Operation<Void> original,
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random) {
        original.call(particleLevel, options, x,
                y + NibaruHorizontalSurface.visibleOffset(state, level, pos), z,
                xSpeed, ySpeed, zSpeed);
    }
}
