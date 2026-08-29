package com.crispytwig.naturalist.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class NaturalistPortraitRenderState {

    public static boolean ACTIVE = false;

    public static float SCALE = 1.0F;

    private NaturalistPortraitRenderState() {
    }
}
