package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.StarfishModel;
import com.crispytwig.naturalist.server.entity.mob.Starfish;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class StarfishRenderer extends NaturalistMobRenderer<Starfish> {
    public StarfishRenderer(EntityRendererProvider.Context context) {
        super(context, new StarfishModel(context.bakeLayer(StarfishModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Starfish entity) {
        return entity.getVariantTexture();
    }
}
