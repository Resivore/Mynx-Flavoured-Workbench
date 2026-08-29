package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.DragonflyModel;
import com.crispytwig.naturalist.server.entity.mob.Dragonfly;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class DragonflyRenderer extends NaturalistMobRenderer<Dragonfly> {
    public DragonflyRenderer(EntityRendererProvider.@NotNull Context context) {
        super(context, new DragonflyModel(context.bakeLayer(DragonflyModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Dragonfly entity) {
        return entity.getVariantTexture();
    }
}
