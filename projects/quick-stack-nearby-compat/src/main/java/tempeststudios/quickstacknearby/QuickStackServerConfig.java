package tempeststudios.quickstacknearby;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class QuickStackServerConfig {
    public static final int DEFAULT_HORIZONTAL_RADIUS = 8;
    public static final int DEFAULT_VERTICAL_RADIUS = 4;
    public static final int MIN_HORIZONTAL_RADIUS = 0;
    public static final int MAX_HORIZONTAL_RADIUS = 32;
    public static final int MIN_VERTICAL_RADIUS = 0;
    public static final int MAX_VERTICAL_RADIUS = 16;

    private static final int CURRENT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static QuickStackServerConfig instance;

    private final Path saveFile;
    private ConfigData config = new ConfigData();

    private QuickStackServerConfig() {
        Path modDir = TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.QUICK_STACK_NEARBY);
        this.saveFile = modDir.resolve("server_config.json");
        try {
            Files.createDirectories(modDir);
        } catch (IOException e) {
            QuickStackNearby.LOGGER.error("Failed to create Quick Stack Nearby data directory", e);
        }
        load();
    }

    public static synchronized QuickStackServerConfig getInstance() {
        if (instance == null) {
            instance = new QuickStackServerConfig();
        }
        return instance;
    }

    public synchronized int horizontalRadius() {
        normalize();
        return config.horizontalRadius;
    }

    public synchronized int verticalRadius() {
        normalize();
        return config.verticalRadius;
    }

    public synchronized void setRange(int horizontalRadius, int verticalRadius) {
        normalize();
        config.horizontalRadius = clamp(horizontalRadius, MIN_HORIZONTAL_RADIUS, MAX_HORIZONTAL_RADIUS);
        config.verticalRadius = clamp(verticalRadius, MIN_VERTICAL_RADIUS, MAX_VERTICAL_RADIUS);
        save();
    }

    public synchronized void reload() {
        load();
    }

    private void save() {
        normalize();
        try {
            writeJsonAtomically(saveFile, config);
        } catch (IOException e) {
            QuickStackNearby.LOGGER.error("Failed to save Quick Stack Nearby server config", e);
        }
    }

    private void load() {
        if (!Files.exists(saveFile)) {
            config = new ConfigData();
            normalize();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(saveFile, StandardCharsets.UTF_8)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) {
                config = loaded;
                normalize();
                return;
            }
        } catch (Exception e) {
            QuickStackNearby.LOGGER.error("Failed to load Quick Stack Nearby server config", e);
            Path backup = backupFile(saveFile);
            if (Files.exists(backup)) {
                try (Reader reader = Files.newBufferedReader(backup, StandardCharsets.UTF_8)) {
                    ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                    if (loaded != null) {
                        config = loaded;
                        normalize();
                        return;
                    }
                } catch (Exception backupError) {
                    QuickStackNearby.LOGGER.error("Failed to load Quick Stack Nearby server config backup", backupError);
                }
            }
        }

        config = new ConfigData();
        normalize();
        save();
    }

    private void normalize() {
        if (config == null) {
            config = new ConfigData();
        }
        config.version = CURRENT_VERSION;
        if (config.horizontalRadius == null) {
            config.horizontalRadius = DEFAULT_HORIZONTAL_RADIUS;
        }
        if (config.verticalRadius == null) {
            config.verticalRadius = DEFAULT_VERTICAL_RADIUS;
        }
        config.horizontalRadius = clamp(config.horizontalRadius, MIN_HORIZONTAL_RADIUS, MAX_HORIZONTAL_RADIUS);
        config.verticalRadius = clamp(config.verticalRadius, MIN_VERTICAL_RADIUS, MAX_VERTICAL_RADIUS);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void writeJsonAtomically(Path target, Object data) throws IOException {
        Files.createDirectories(target.getParent());
        Path tempFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            if (Files.exists(target)) {
                Files.copy(target, backupFile(target), StandardCopyOption.REPLACE_EXISTING);
            }
            moveIntoPlace(tempFile, target);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    private static void moveIntoPlace(Path tempFile, Path target) throws IOException {
        try {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path backupFile(Path target) {
        return target.resolveSibling(target.getFileName().toString() + ".bak");
    }

    private static final class ConfigData {
        Integer version = CURRENT_VERSION;
        Integer horizontalRadius = DEFAULT_HORIZONTAL_RADIUS;
        Integer verticalRadius = DEFAULT_VERTICAL_RADIUS;
    }
}
