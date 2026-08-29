package dev.resivore.radialslotcycler.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.resivore.radialslotcycler.RadialSlotCycler;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

final class RadialClientConfig {
    private static final String FILE_NAME = "radial-slot-cycler.json";
    private static final String DEFAULT_FILE = """
            {
              "interactionMode": "hold"
            }
            """;

    private RadialClientConfig() {}

    static RadialInteractionController.Mode loadMode() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(
                        path,
                        DEFAULT_FILE,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                String value = root.has("interactionMode")
                        ? root.get("interactionMode").getAsString()
                        : "hold";
                return switch (value.toLowerCase(Locale.ROOT)) {
                    case "toggle" -> RadialInteractionController.Mode.TOGGLE;
                    default -> RadialInteractionController.Mode.HOLD;
                };
            }
        } catch (IOException | RuntimeException exception) {
            RadialSlotCycler.LOGGER.warn(
                    "Could not read {}; using hold mode", path, exception);
            return RadialInteractionController.Mode.HOLD;
        }
    }
}
