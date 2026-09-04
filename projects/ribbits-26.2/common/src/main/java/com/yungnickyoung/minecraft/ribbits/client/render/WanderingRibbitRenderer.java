package com.yungnickyoung.minecraft.ribbits.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.yungnickyoung.minecraft.ribbits.client.model.WanderingRibbitModel;
import com.yungnickyoung.minecraft.ribbits.entity.WanderingRibbitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Client-only GeckoLib renderer; no common/server initializer references this class. */
public final class WanderingRibbitRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<WanderingRibbitEntity, R> {
    public WanderingRibbitRenderer(EntityRendererProvider.Context context) {
        super(context, new WanderingRibbitModel());
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<R> renderInfo, float widthScale, float heightScale) {
        float ageScale = renderInfo.renderState().ageScale;
        super.scaleModelForRender(renderInfo, widthScale * ageScale, heightScale * ageScale);
    }

    @Override
    public RenderType getRenderType(R renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    @Override
    public float getMotionAnimThreshold(WanderingRibbitEntity animatable) {
        return 0.0005F;
    }
}
