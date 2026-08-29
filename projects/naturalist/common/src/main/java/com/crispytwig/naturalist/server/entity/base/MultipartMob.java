package com.crispytwig.naturalist.server.entity.base;

import com.crispytwig.naturalist.server.entity.util.MobPart;

public interface MultipartMob {
    MobPart[] getParts();

    float getSegmentPitchOffset(int index, float partialTick);

    float getSegmentYawOffset(int index, float partialTick);
}
