package dev.resivore.dragonbound.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client-only access to the vanilla item layers created by the wrapped model. */
@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
    @Accessor("activeLayerCount")
    int dragonboundWaystone$activeLayerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] dragonboundWaystone$layers();
}
