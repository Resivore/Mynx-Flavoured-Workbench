package net.penumbra.enderscape;

import dev.aero.cnmterraincompat.fixture.ExternalFixtureRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Test-only optional-provider entrypoint with the production Enderscape binary name. */
public final class Enderscape implements ModInitializer {
    /**
     * The retained provider classes initialise their registries through this helper.  The
     * GameTest fixture deliberately owns the entrypoint, so retain the provider's tiny static
     * API surface without loading its complete mod bootstrap and dependency graph.
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("enderscape", path);
    }

    public static SoundEvent registerSoundEvent(String path) {
        Identifier id = id(path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static Holder.Reference<SoundEvent> registerSoundEventHolder(String path) {
        Identifier id = id(path);
        return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id,
                SoundEvent.createVariableRangeEvent(id));
    }

    @Override public void onInitialize() { ExternalFixtureRegistry.registerEnderscape(); }
}
