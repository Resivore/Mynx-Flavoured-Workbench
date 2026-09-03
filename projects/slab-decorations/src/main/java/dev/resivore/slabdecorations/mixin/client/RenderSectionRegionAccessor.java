package dev.resivore.slabdecorations.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the immutable render snapshot's owning world without consulting global client state. */
@Mixin(RenderSectionRegion.class)
public interface RenderSectionRegionAccessor {
    @Accessor("level")
    ClientLevel slabDecorations$getLevel();
}
