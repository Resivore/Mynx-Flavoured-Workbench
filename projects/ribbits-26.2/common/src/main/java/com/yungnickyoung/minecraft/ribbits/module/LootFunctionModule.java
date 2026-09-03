package com.yungnickyoung.minecraft.ribbits.module;

import com.mojang.serialization.MapCodec;
import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.world.loot.RibbitVillageExplorerResultFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

/** Common-side registration for the exact Ribbit explorer-map result finalizer. */
public final class LootFunctionModule {
    public static final String RIBBIT_VILLAGE_EXPLORER_RESULT_ID =
            "finalize_ribbit_village_explorer_result";
    public static final MapCodec<? extends LootItemFunction> RIBBIT_VILLAGE_EXPLORER_RESULT =
            RibbitVillageExplorerResultFunction.CODEC;

    private static boolean initialized;

    private LootFunctionModule() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        Registry.register(
                BuiltInRegistries.LOOT_FUNCTION_TYPE,
                RibbitsCommon.id(RIBBIT_VILLAGE_EXPLORER_RESULT_ID),
                RIBBIT_VILLAGE_EXPLORER_RESULT
        );
        initialized = true;
    }
}
