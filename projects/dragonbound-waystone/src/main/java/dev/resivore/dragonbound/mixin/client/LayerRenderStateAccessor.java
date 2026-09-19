package dev.resivore.dragonbound.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Client-only access to the baked quads already prepared by vanilla's item-model update. */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {
    @Accessor("quads")
    List<BakedQuad> dragonboundWaystone$quads();
}
