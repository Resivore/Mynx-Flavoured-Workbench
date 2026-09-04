package com.yungnickyoung.minecraft.ribbits.client.model;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.entity.WanderingRibbitEntity;
import net.minecraft.resources.Identifier;

/** Client-only resource binding for the approved private Wandering Ribbit visuals. */
public final class WanderingRibbitModel extends GeoModel<WanderingRibbitEntity> {
    private static final Identifier MODEL = RibbitsCommon.id("wandering_ribbit");
    private static final Identifier TEXTURE = RibbitsCommon.id("textures/entity/wandering_ribbit.png");
    private static final Identifier ANIMATIONS = RibbitsCommon.id("ribbit");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(WanderingRibbitEntity animatable) {
        return ANIMATIONS;
    }
}
