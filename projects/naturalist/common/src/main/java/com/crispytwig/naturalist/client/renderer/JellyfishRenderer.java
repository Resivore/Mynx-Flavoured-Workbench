package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.JellyfishModel;
import com.crispytwig.naturalist.server.entity.mob.Jellyfish;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class JellyfishRenderer extends NaturalistMobRenderer<Jellyfish> {
    public JellyfishRenderer(EntityRendererProvider.Context context) {
        super(context, new JellyfishModel(context.bakeLayer(JellyfishModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Jellyfish entity) {
        return entity.getVariantTexture();
    }

}
