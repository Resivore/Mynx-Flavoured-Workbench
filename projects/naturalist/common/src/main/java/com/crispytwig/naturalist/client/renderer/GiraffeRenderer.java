package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.GiraffeBabyModel;
import com.crispytwig.naturalist.client.model.GiraffeModel;
import com.crispytwig.naturalist.client.renderer.layers.SeatedRiderLayer;
import com.crispytwig.naturalist.server.entity.mob.Giraffe;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class GiraffeRenderer extends NaturalistMobRenderer<Giraffe> {
    public GiraffeRenderer(EntityRendererProvider.Context context) {
        super(context, new GiraffeModel(context.bakeLayer(GiraffeModel.LAYER_LOCATION)), new GiraffeBabyModel(context.bakeLayer(GiraffeBabyModel.LAYER_LOCATION)), 1.1F);
        this.addLayer(new SeatedRiderLayer<>(this));
    }
}
