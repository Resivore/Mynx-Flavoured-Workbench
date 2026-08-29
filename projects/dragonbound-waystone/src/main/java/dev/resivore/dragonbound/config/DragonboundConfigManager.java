package dev.resivore.dragonbound.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.resivore.dragonbound.DragonboundWaystone;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DragonboundConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("dragonbound-waystone.json");

    private static volatile DragonboundConfig current = DragonboundConfig.defaults();

    private DragonboundConfigManager() {
    }

    public static DragonboundConfig get() {
        return current;
    }

    public static synchronized DragonboundConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            current = DragonboundConfig.defaults();
            persistDefaults();
            return current;
        }

        try {
            JsonObject source;
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    throw new IOException("root value is not a JSON object");
                }
                source = parsed.getAsJsonObject();
            }

            DragonboundConfigMigration.Result migration = DragonboundConfigMigration.apply(source);
            current = DragonboundConfig.fromJson(migration.json());
            if (migration.changed()) {
                persistCurrent();
            }
        } catch (IOException | RuntimeException exception) {
            current = DragonboundConfig.defaults();
            DragonboundWaystone.LOGGER.error(
                    "Could not read {}; using server defaults without overwriting the file",
                    CONFIG_PATH,
                    exception);
        }
        return current;
    }

    public static synchronized DragonboundConfig reload() {
        return load();
    }

    private static void persistDefaults() {
        persistCurrent();
    }

    private static void persistCurrent() {
        JsonObject json = current.toJson();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException exception) {
            DragonboundWaystone.LOGGER.error("Could not write config at {}", CONFIG_PATH, exception);
        }
    }
}
