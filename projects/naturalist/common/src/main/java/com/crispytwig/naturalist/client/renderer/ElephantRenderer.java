package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.ElephantBabyModel;
import com.crispytwig.naturalist.client.model.ElephantModel;
import com.crispytwig.naturalist.client.renderer.layers.BannerLayer;
import com.crispytwig.naturalist.client.renderer.layers.SeatedRiderLayer;
import com.crispytwig.naturalist.server.entity.mob.Elephant;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class ElephantRenderer extends NaturalistMobRenderer<Elephant> {
    private static final float BANNER_X = 15.0F;
    private static final float BANNER_Y = -15.5F;
    private static final float BANNER_Z = -6.0F;
    private static final float BANNER_SCALE = 0.7F;

    public ElephantRenderer(EntityRendererProvider.Context context) {
        super(context, new ElephantModel(context.bakeLayer(ElephantModel.LAYER_LOCATION)), new ElephantBabyModel(context.bakeLayer(ElephantBabyModel.LAYER_LOCATION)), 1.5F);
        this.addLayer(new SeatedRiderLayer<>(this));
        this.addLayer(new BannerLayer(this, context, BANNER_X, BANNER_Y, BANNER_Z, BANNER_SCALE));
    }
}
