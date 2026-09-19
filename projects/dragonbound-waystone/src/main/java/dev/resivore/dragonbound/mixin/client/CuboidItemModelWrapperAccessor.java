package dev.resivore.dragonbound.mixin.client;

import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client-only read access to the already baked Waystone item cuboids and display transforms. */
@Mixin(CuboidItemModelWrapper.class)
public interface CuboidItemModelWrapperAccessor {
    @Accessor("quads")
    QuadCollection dragonboundWaystone$quads();

    @Accessor("properties")
    ModelRenderProperties dragonboundWaystone$properties();
}
