package com.yungnickyoung.minecraft.yungsapi.module;

import com.yungnickyoung.minecraft.yungsapi.autoregister.AutoRegistrationManager;
import com.yungnickyoung.minecraft.yungsapi.autoregister.AutoRegisterField;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegisterStructureProcessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Registration of StructureProcessorTypes.
 */
public class StructureProcessorTypeModuleFabric {
    public static void processEntries() {
        AutoRegistrationManager.STRUCTURE_PROCESSOR_TYPES.stream()
                .filter(data -> !data.processed())
                .forEach(StructureProcessorTypeModuleFabric::register);
    }

    private static void register(AutoRegisterField data) {
        AutoRegisterStructureProcessor processor = (AutoRegisterStructureProcessor) data.object();
        Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, data.name(), processor.get());
        data.markProcessed();
    }
}
