package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonElement;
import dev.tazer.clutternomore.ClutterNoMore;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Writes BGE-generated client resources to both of CNM's real resource paths.
 *
 * <p>CNM's {@code AssetGenerator.write} mirrors only the {@code clutternomore} namespace.
 * BGE owns blocks in {@code cnm_terrain_slabs_compat}, so putting their JSON only in CNM's
 * in-memory pack leaves the runtime-generated pack without their blockstates, models, and item
 * definitions on a later client reload. Keep the two stores byte-equivalent and preserve each
 * resource's actual namespace.</p>
 */
final class BgeGeneratedResourceWriter {
    private BgeGeneratedResourceWriter() {}

    static void write(Identifier id, JsonElement json) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(json, "json");
        ClutterNoMore.RESOURCES.addJson(PackType.CLIENT_RESOURCES, id, json);
        // Generation also occurs while server-side GameTests build their logical resource view.
        // CNM's client config class is not safe to load there, and no client runtime pack exists.
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT
                || !runtimeAssetGenerationEnabled()) return;

        Path relative = Path.of(id.getPath());
        Path target = ClutterNoMore.pack.resolve("assets").resolve(id.getNamespace()).resolve(relative);
        Path parent = target.getParent();
        if (parent == null) throw new IllegalArgumentException("Generated resource has no parent: " + id);
        try {
            ClutterNoMore.writeFile(parent, target,
                    dev.tazer.clutternomore.common.data.CNMPackResources.serializeJson(json));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot mirror generated BGE resource " + id, exception);
        }
    }

    /** CNM deliberately keeps its config library as a non-transitive runtime dependency. */
    private static boolean runtimeAssetGenerationEnabled() {
        try {
            Class<?> clientClass = Class.forName("dev.tazer.clutternomore.ClutterNoMoreClient");
            Object config = clientClass.getField("CLIENT_CONFIG").get(null);
            Object tracked = config.getClass().getField("RUNTIME_ASSET_GENERATION").get(config);
            Object value = tracked.getClass().getMethod("value").invoke(tracked);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read CNM runtime asset-generation setting", exception);
        }
    }
}
