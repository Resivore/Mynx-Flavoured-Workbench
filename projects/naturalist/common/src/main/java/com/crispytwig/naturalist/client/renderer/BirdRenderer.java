package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.BirdBabyModel;
import com.crispytwig.naturalist.client.model.BirdModel;
import com.crispytwig.naturalist.client.renderer.layers.DyeLayer;
import com.crispytwig.naturalist.server.entity.mob.Bird;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class BirdRenderer extends NaturalistMobRenderer<Bird> {
    public BirdRenderer(EntityRendererProvider.Context context) {
        super(context, new BirdModel(context.bakeLayer(BirdModel.LAYER_LOCATION)), new BirdBabyModel(context.bakeLayer(BirdBabyModel.LAYER_LOCATION)), 0.3F);
        this.addLayer(new DyeLayer<>(this, "bird"));
    }
}
