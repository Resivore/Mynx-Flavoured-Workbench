package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.CaterpillarModel;
import com.crispytwig.naturalist.server.entity.mob.Caterpillar;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class CaterpillarRenderer extends NaturalistMobRenderer<Caterpillar> {
    public CaterpillarRenderer(EntityRendererProvider.Context context) {
        super(context, new CaterpillarModel(context.bakeLayer(CaterpillarModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Caterpillar entity) {
        return entity.getVariantTexture();
    }
}
