package dev.resivore.polytoneleadrenderingfix.mixin;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Accesses Minecraft 26.2's package-private factory without copying RenderType internals. */
@Mixin(RenderType.class)
public interface RenderTypeAccessor {
    @Invoker("create")
    static RenderType polytoneLeadRenderingFix$create(String name, RenderSetup setup) {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
