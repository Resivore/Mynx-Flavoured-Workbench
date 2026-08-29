package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.ZebraBabyModel;
import com.crispytwig.naturalist.client.model.ZebraModel;
import com.crispytwig.naturalist.server.entity.mob.Zebra;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class ZebraRenderer extends NaturalistMobRenderer<Zebra> {
    public ZebraRenderer(EntityRendererProvider.Context context) {
        super(context, new ZebraModel(context.bakeLayer(ZebraModel.LAYER_LOCATION)), new ZebraBabyModel(context.bakeLayer(ZebraBabyModel.LAYER_LOCATION)), 1.1F);
    }
}
