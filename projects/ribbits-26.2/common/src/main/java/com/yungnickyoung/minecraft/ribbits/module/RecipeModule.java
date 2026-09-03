package com.yungnickyoung.minecraft.ribbits.module;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.recipe.ToadstoolHeartRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class RecipeModule {
    public static final RecipeSerializer<ToadstoolHeartRecipe> TOADSTOOL_HEART = new RecipeSerializer<>(
            ToadstoolHeartRecipe.CODEC,
            ToadstoolHeartRecipe.STREAM_CODEC
    );

    private static boolean initialized;

    private RecipeModule() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                RibbitsCommon.id("toadstool_heart"),
                TOADSTOOL_HEART
        );
        initialized = true;
    }
}
