package com.yungnickyoung.minecraft.ribbits.module;

import com.yungnickyoung.minecraft.ribbits.chute.ChuteEquipment;
import com.yungnickyoung.minecraft.ribbits.client.supporters.SupportersJSON;

public class NetworkModule {
    public static void init() {
        ChuteEquipment.initialize();
        SupportersJSON.populateSupportersList();
    }
}
