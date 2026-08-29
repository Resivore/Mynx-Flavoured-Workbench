package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.MammothBabyModel;
import com.crispytwig.naturalist.client.model.MammothModel;
import com.crispytwig.naturalist.client.renderer.layers.BannerLayer;
import com.crispytwig.naturalist.client.renderer.layers.SeatedRiderLayer;
import com.crispytwig.naturalist.server.entity.mob.Elephant;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class MammothRenderer extends NaturalistMobRenderer<Elephant> {
    private static final float BANNER_X = 16.5F;
    private static final float BANNER_Y = -17.5F;
    private static final float BANNER_Z = -4.0F;
    private static final float BANNER_SCALE = 0.8F;

    public MammothRenderer(EntityRendererProvider.Context context) {
        super(context, new MammothModel(context.bakeLayer(MammothModel.LAYER_LOCATION)), new MammothBabyModel(context.bakeLayer(MammothBabyModel.LAYER_LOCATION)), 1.5F);
        this.addLayer(new SeatedRiderLayer<>(this));
        this.addLayer(new BannerLayer(this, context, BANNER_X, BANNER_Y, BANNER_Z, BANNER_SCALE));
    }
}
