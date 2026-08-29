package com.yungnickyoung.minecraft.ribbits.module;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.world.processor.*;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegister;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegisterStructureProcessor;

@AutoRegister(RibbitsCommon.MOD_ID)
public class StructureProcessorTypeModule {
    @AutoRegister("pillar_processor")
    public static final AutoRegisterStructureProcessor PILLAR_PROCESSOR =
            AutoRegisterStructureProcessor.of(() -> PillarProcessor.CODEC);

    @AutoRegister("podzol_processor")
    public static final AutoRegisterStructureProcessor PODZOL_PROCESSOR =
            AutoRegisterStructureProcessor.of(() -> PodzolProcessor.CODEC);

    @AutoRegister("warped_nylium_processor")
    public static final AutoRegisterStructureProcessor WARPED_NYLIUM_PROCESSOR =
            AutoRegisterStructureProcessor.of(() -> WarpedNyliumProcessor.CODEC);

    @AutoRegister("block_replace_processor")
    public static final AutoRegisterStructureProcessor BLOCK_REPLACE_PROCESSOR =
            AutoRegisterStructureProcessor.of(() -> BlockReplaceProcessor.CODEC);

    @AutoRegister("lapis_block_processor")
    public static final AutoRegisterStructureProcessor LAPIS_BLOCK_PROCESSOR =
            AutoRegisterStructureProcessor.of(() -> LapisBlockProcessor.CODEC);

    @AutoRegister("brewing_stand_processor")
    public static final AutoRegisterStructureProcessor BREWING_STAND_PROCESSOR =
            AutoRegisterStructureProcessor.of(() -> BrewingStandProcessor.CODEC);
}
