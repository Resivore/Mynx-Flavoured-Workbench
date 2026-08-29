package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.DesertScorpionModel;
import com.crispytwig.naturalist.server.entity.mob.DesertScorpion;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class DesertScorpionRenderer extends NaturalistMobRenderer<DesertScorpion> {
    public DesertScorpionRenderer(EntityRendererProvider.Context context) {
        super(context, new DesertScorpionModel(context.bakeLayer(DesertScorpionModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull DesertScorpion entity) {
        return entity.getVariantTexture();
    }
}
