package com.yungnickyoung.minecraft.ribbits.client.model;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.data.RibbitData;
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import com.yungnickyoung.minecraft.ribbits.module.DataTicketModule;
import com.yungnickyoung.minecraft.ribbits.module.RibbitInstrumentModule;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public class RibbitModel extends GeoModel<RibbitEntity> {
    private static final Identifier TEXTURE = RibbitsCommon.id("textures/entity/ribbit.png");
    private static final Identifier ANIMATIONS = RibbitsCommon.id("ribbit");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        RibbitData data = renderState.getGeckolibData(DataTicketModule.DT_RIBBIT_DATA);
        boolean playingInstrument = Boolean.TRUE.equals(
                renderState.getGeckolibData(DataTicketModule.DT_PLAYING_INSTRUMENT));
        boolean umbrellaFalling = Boolean.TRUE.equals(
                renderState.getGeckolibData(DataTicketModule.DT_UMBRELLA_FALLING));
        boolean inRain = Boolean.TRUE.equals(renderState.getGeckolibData(DataTicketModule.DT_IN_RAIN));
        boolean isPride = Boolean.TRUE.equals(
                renderState.getGeckolibData(DataTicketModule.DT_IS_PRIDE_RIBBIT));

        if (playingInstrument && data.getInstrument() != RibbitInstrumentModule.NONE) {
            return data.getInstrument().modelId();
        }

        if (umbrellaFalling || inRain) {
            return RibbitsCommon.id("umbrella/"
                    + data.getProfession().id().getPath()
                    + "/"
                    + data.getUmbrellaType().modelLocationSuffix());
        }

        if (isPride) {
            return RibbitsCommon.id("pride_ribbit");
        }

        return data.getProfession().modelLocation();
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(RibbitEntity animatable) {
        return ANIMATIONS;
    }
}
