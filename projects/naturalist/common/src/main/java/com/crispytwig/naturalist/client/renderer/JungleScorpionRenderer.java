package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.JungleScorpionModel;
import com.crispytwig.naturalist.server.entity.mob.JungleScorpion;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class JungleScorpionRenderer extends NaturalistMobRenderer<JungleScorpion> {
    public JungleScorpionRenderer(EntityRendererProvider.Context context) {
        super(context, new JungleScorpionModel(context.bakeLayer(JungleScorpionModel.LAYER_LOCATION)), 0.7F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull JungleScorpion entity) {
        return entity.getVariantTexture();
    }
}
