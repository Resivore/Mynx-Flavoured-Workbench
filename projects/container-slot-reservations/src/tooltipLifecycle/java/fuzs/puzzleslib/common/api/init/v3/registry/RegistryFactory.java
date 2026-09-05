package fuzs.puzzleslib.common.api.init.v3.registry;
import net.minecraft.core.*;
import net.minecraft.resources.*;
import com.mojang.serialization.Lifecycle;
/** Fixture-only registry bootstrap in place of platform service discovery. */
public interface RegistryFactory {
    RegistryFactory INSTANCE = (key, id) -> new MappedRegistry(key, Lifecycle.stable());
    Registry createSynced(ResourceKey key, Identifier defaultId);
}
