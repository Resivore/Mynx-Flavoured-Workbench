package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.LionBabyModel;
import com.crispytwig.naturalist.client.model.LionModel;
import com.crispytwig.naturalist.server.entity.mob.Lion;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class LionRenderer extends NaturalistMobRenderer<Lion> {
    public LionRenderer(EntityRendererProvider.Context context) {
        super(context, new LionModel(context.bakeLayer(LionModel.LAYER_LOCATION)), new LionBabyModel(context.bakeLayer(LionBabyModel.LAYER_LOCATION)), 1.1F);
    }
}
