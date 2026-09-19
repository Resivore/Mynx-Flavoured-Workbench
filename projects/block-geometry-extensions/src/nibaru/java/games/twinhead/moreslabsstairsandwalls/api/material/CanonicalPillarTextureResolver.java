package games.twinhead.moreslabsstairsandwalls.api.material;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;

/**
 * Resolves the side/end contract from the canonical block model instead of treating a registry
 * path as a texture name. Vanilla logs happen to use their block path for the side texture;
 * Purpur Pillar does not, so both must flow through this one resolver.
 */
final class CanonicalPillarTextureResolver {
    private static final String CLIENT_ARCHIVE_PROPERTY = "bge.minecraftClientJar";
    private CanonicalPillarTextureResolver() {}

    static Optional<NibaruMaterialProfile.TextureRoles> resolve(Block canonical) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(canonical);
        if (id == null || id.equals(BuiltInRegistries.BLOCK.getDefaultKey())) return Optional.empty();
        try {
            Map<String, String> textures = textureVariables(model(id), new LinkedHashMap<>());
            String side = normalizeMinecraftPath(resolveVariable(textures, "side"));
            String end = normalizeMinecraftPath(firstResolved(textures, "end", "top"));
            if (side == null || end == null) return Optional.empty();
            return Optional.of(new NibaruMaterialProfile.TextureRoles(side, end, end, "", side));
        } catch (RuntimeException exception) {
            // Client resources are not guaranteed to be exposed by every dedicated-server
            // classpath. The existing declared role remains a safe fallback there; the normal
            // client/build classpaths resolve and verify the canonical contract below.
            return Optional.empty();
        }
    }

    private static JsonObject model(Identifier id) {
        String path = id.getPath().startsWith("block/") ? id.getPath().substring("block/".length())
                : id.getPath();
        String resource = "assets/" + id.getNamespace() + "/models/block/" + path + ".json";
        InputStream stream = resourceStream(resource);
        if (stream == null) throw new IllegalArgumentException("Missing canonical pillar model " + resource);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot read canonical pillar model " + resource, exception);
        }
    }

    /**
     * The dedicated-server classpath intentionally omits client model JSON.  The build writer
     * supplies the exact matching Minecraft client archive as an explicit Gradle input; runtime
     * resolution continues to use its normal resource classpath.
     */
    private static InputStream resourceStream(String resource) {
        InputStream classpath = CanonicalPillarTextureResolver.class.getClassLoader()
                .getResourceAsStream(resource);
        if (classpath != null) return classpath;
        String configuredArchive = System.getProperty(CLIENT_ARCHIVE_PROPERTY, "");
        if (configuredArchive.isBlank()) return null;
        Path archive = Path.of(configuredArchive).toAbsolutePath().normalize();
        if (!Files.isRegularFile(archive)) return null;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var entry = zip.getEntry(resource);
            if (entry == null) return null;
            try (InputStream input = zip.getInputStream(entry)) {
                return new ByteArrayInputStream(input.readAllBytes());
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot read canonical pillar model " + resource
                    + " from " + archive, exception);
        }
    }

    private static Map<String, String> textureVariables(JsonObject model, Map<String, String> inherited) {
        if (model.has("parent")) {
            Identifier parent = Identifier.parse(model.get("parent").getAsString());
            inherited = textureVariables(model(parent), inherited);
        }
        Map<String, String> result = new LinkedHashMap<>(inherited);
        if (model.has("textures") && model.get("textures").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : model.getAsJsonObject("textures").entrySet()) {
                if (entry.getValue().isJsonPrimitive()) result.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return result;
    }

    private static String firstResolved(Map<String, String> textures, String... names) {
        for (String name : names) {
            String value = resolveVariable(textures, name);
            if (value != null) return value;
        }
        return null;
    }

    private static String resolveVariable(Map<String, String> textures, String name) {
        String value = textures.get(name);
        int guard = 0;
        while (value != null && value.startsWith("#") && guard++ < 32) {
            value = textures.get(value.substring(1));
        }
        return value == null || value.startsWith("#") ? null : value;
    }

    /** Profile texture roles retain their established compact Minecraft-path representation. */
    private static String normalizeMinecraftPath(String value) {
        return value != null && value.startsWith("minecraft:block/")
                ? value.substring("minecraft:block/".length()) : value;
    }
}
