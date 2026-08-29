package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.ClamModel;
import com.crispytwig.naturalist.client.renderer.layers.ClamItemLayer;
import com.crispytwig.naturalist.server.entity.mob.Clam;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class ClamRenderer extends NaturalistMobRenderer<Clam> {
    public ClamRenderer(EntityRendererProvider.Context context) {
        super(context, new ClamModel(context.bakeLayer(ClamModel.LAYER_LOCATION)), 0.0F);
        this.addLayer(new ClamItemLayer(this, context.getEntityRenderDispatcher().getItemInHandRenderer()));
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Clam entity) {
        return entity.getVariantTexture();
    }
}
