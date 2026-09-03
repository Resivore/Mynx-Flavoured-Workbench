package com.yungnickyoung.minecraft.ribbits;

import com.yungnickyoung.minecraft.ribbits.module.ConfigModule;
import com.yungnickyoung.minecraft.ribbits.module.LootFunctionModule;
import com.yungnickyoung.minecraft.ribbits.module.NetworkModule;
import com.yungnickyoung.minecraft.ribbits.module.RecipeModule;
import com.yungnickyoung.minecraft.yungsapi.api.YungAutoRegister;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RibbitsCommon {
    public static final String MOD_ID = "ribbits";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final String MC_VERSION_STRING = "26_2";


    public static void init() {
        RecipeModule.init();
        LootFunctionModule.init();
        YungAutoRegister.scanPackageForAnnotations("com.yungnickyoung.minecraft.ribbits");
        ConfigModule.init();
        NetworkModule.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
