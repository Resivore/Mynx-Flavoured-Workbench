package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.TigerBabyModel;
import com.crispytwig.naturalist.client.model.TigerModel;
import com.crispytwig.naturalist.server.entity.mob.Tiger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class TigerRenderer extends NaturalistMobRenderer<Tiger> {
    public TigerRenderer(EntityRendererProvider.Context context) {
        super(context, new TigerModel(context.bakeLayer(TigerModel.LAYER_LOCATION)), new TigerBabyModel(context.bakeLayer(TigerBabyModel.LAYER_LOCATION)), 1.1F);
    }
}
