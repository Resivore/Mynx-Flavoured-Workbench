package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.CrabBabyModel;
import com.crispytwig.naturalist.client.model.CrabModel;
import com.crispytwig.naturalist.client.renderer.layers.CrabItemLayer;
import com.crispytwig.naturalist.server.entity.mob.Crab;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class CrabRenderer extends NaturalistMobRenderer<Crab> {
    public CrabRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(context.bakeLayer(CrabModel.LAYER_LOCATION)), new CrabBabyModel(context.bakeLayer(CrabBabyModel.LAYER_LOCATION)), 0.3F, 0.3F);
        this.addLayer(new CrabItemLayer(this, context.getEntityRenderDispatcher().getItemInHandRenderer()));
    }
}
