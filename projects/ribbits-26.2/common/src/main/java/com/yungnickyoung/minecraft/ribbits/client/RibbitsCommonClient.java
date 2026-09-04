package com.yungnickyoung.minecraft.ribbits.client;

import com.yungnickyoung.minecraft.ribbits.client.render.RibbitVillageMapItemModel;
import com.yungnickyoung.minecraft.ribbits.client.supporters.RibbitOptionsJSON;
import com.yungnickyoung.minecraft.ribbits.util.GeoIP;

public class RibbitsCommonClient {
    public static void init() {
        RibbitVillageMapItemModel.register();
        GeoIP.init();
        RibbitOptionsJSON.loadFromFile();
    }
}
