package dev.resivore.mapmarkerextension;

import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;

public final class MinecraftTestBootstrap {
    private static boolean initialized;

    private MinecraftTestBootstrap() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var vanillaData = VanillaRegistries.createLookup();
        HolderLookup.Provider registries = HolderLookup.Provider.create(Stream.concat(
            builtIns.listRegistries(),
            vanillaData.listRegistries().filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())
        ));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
            .forEach(pending -> pending.apply());
        initialized = true;
    }
}
