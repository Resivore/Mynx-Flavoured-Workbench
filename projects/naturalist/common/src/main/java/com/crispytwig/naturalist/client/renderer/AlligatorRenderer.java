package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.client.model.AlligatorBabyModel;
import com.crispytwig.naturalist.client.model.AlligatorModel;
import com.crispytwig.naturalist.client.renderer.layers.GlowLayer;
import com.crispytwig.naturalist.server.entity.mob.Alligator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class AlligatorRenderer extends NaturalistMobRenderer<Alligator> {
    private static final Identifier GLOWMASK = Naturalist.location("textures/entity/alligator/alligator_glowmask.png");
    private static final Identifier BABY_GLOWMASK = Naturalist.location("textures/entity/alligator/alligator_baby_glowmask.png");

    public AlligatorRenderer(EntityRendererProvider.Context context) {
        super(context, new AlligatorModel(context.bakeLayer(AlligatorModel.LAYER_LOCATION)), new AlligatorBabyModel(context.bakeLayer(AlligatorBabyModel.LAYER_LOCATION)), 1.0F);
        this.addLayer(new GlowLayer<>(this, entity -> entity.isBaby() ? BABY_GLOWMASK : GLOWMASK));
    }
}
