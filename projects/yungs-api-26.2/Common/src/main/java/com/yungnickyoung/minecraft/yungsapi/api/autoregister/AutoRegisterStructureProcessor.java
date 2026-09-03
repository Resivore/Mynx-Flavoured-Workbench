package com.yungnickyoung.minecraft.yungsapi.api.autoregister;

import com.mojang.serialization.MapCodec;
import com.yungnickyoung.minecraft.yungsapi.autoregister.AutoRegisterEntry;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;

import java.util.function.Supplier;

/**
 * Wrapper for registering structure processor codecs with AutoRegister.
 * Minecraft 26.2 registers processor {@link MapCodec}s directly instead of
 * using the former generic StructureProcessorType wrapper.
 */
public class AutoRegisterStructureProcessor extends AutoRegisterEntry<MapCodec<? extends StructureProcessor>> {
    public static AutoRegisterStructureProcessor of(Supplier<MapCodec<? extends StructureProcessor>> codecSupplier) {
        return new AutoRegisterStructureProcessor(codecSupplier);
    }

    private AutoRegisterStructureProcessor(Supplier<MapCodec<? extends StructureProcessor>> codecSupplier) {
        super(codecSupplier);
    }
}
