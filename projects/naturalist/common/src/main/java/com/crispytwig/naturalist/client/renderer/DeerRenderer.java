package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.DeerBabyModel;
import com.crispytwig.naturalist.client.model.DeerModel;
import com.crispytwig.naturalist.server.entity.mob.Deer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class DeerRenderer extends NaturalistMobRenderer<Deer> {
    public DeerRenderer(EntityRendererProvider.Context context) {
        super(context, new DeerModel(context.bakeLayer(DeerModel.LAYER_LOCATION)), new DeerBabyModel(context.bakeLayer(DeerBabyModel.LAYER_LOCATION)), 0.8F);
    }
}
