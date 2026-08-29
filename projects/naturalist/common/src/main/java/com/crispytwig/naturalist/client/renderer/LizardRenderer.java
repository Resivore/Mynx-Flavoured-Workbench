package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.LizardModel;
import com.crispytwig.naturalist.client.renderer.layers.DyeLayer;
import com.crispytwig.naturalist.server.entity.mob.Lizard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class LizardRenderer extends NaturalistMobRenderer<Lizard> {
    public LizardRenderer(EntityRendererProvider.@NotNull Context context) {
        super(context, new LizardModel(context.bakeLayer(LizardModel.LAYER_LOCATION)), 0.4F);
        this.addLayer(new DyeLayer<>(this, "lizard"));
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Lizard entity) {
        return entity.getVariantTexture();
    }
}
