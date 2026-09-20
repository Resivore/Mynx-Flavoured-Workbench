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

/** Aligns Bulb Lantern's native purification spark without changing its particle behaviour. */
@Pseudo
@Mixin(targets = "net.penumbra.enderscape.block.BulbLanternBlock", remap = false)
public abstract class EnderscapeBulbLanternParticleMixin {
    @WrapOperation(
            method = "playClientsideActivatedParticle",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void slabDecorations$alignBulbLanternParticle(
            Level particleLevel,
            ParticleOptions options,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            Operation<Void> original,
            Level level,
            BlockPos pos,
            RandomSource random) {
        BlockState state = level.getBlockState(pos);
        original.call(particleLevel, options, x,
                y + NibaruHorizontalSurface.visibleOffset(state, level, pos), z,
                xSpeed, ySpeed, zSpeed);
    }
}
