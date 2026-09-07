package tempeststudios.quickstacknearby;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuickStackRuleStore {
    private static final int CURRENT_VERSION = 1;
    private static final String UNKNOWN_NAMESPACE = "unknown";
    private static final int MAX_KEEP_COUNT = 64;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static QuickStackRuleStore instance;

    private final Path saveFile;
    private RuleConfig config = new RuleConfig();

    private QuickStackRuleStore() {
        Path modDir = TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.QUICK_STACK_NEARBY);
        this.saveFile = modDir.resolve("quick_stack_rules.json");
        try {
            Files.createDirectories(modDir);
        } catch (IOException e) {
            QuickStackNearby.LOGGER.error("Failed to create Quick Stack Nearby rule directory", e);
        }
        load();
    }

    public static QuickStackRuleStore getInstance() {
        if (instance == null) {
            instance = new QuickStackRuleStore();
        }
        return instance;
    }

    public SlotRules playerRules() {
        return currentWorldRules().playerRules;
    }

    public List<QuickStackRequestPayload.SlotRule> payloadRules() {
        SlotRules rules = playerRules();
        List<QuickStackRequestPayload.SlotRule> payloadRules = new ArrayList<>();
        for (Map.Entry<Integer, SlotRule> entry : rules.slotRules.entrySet()) {
            Integer slotIndex = entry.getKey();
            SlotRule rule = entry.getValue();
            if (slotIndex == null || rule == null || rule.isEmpty()) {
                continue;
            }
            payloadRules.add(new QuickStackRequestPayload.SlotRule(
                    slotIndex,
                    rule.locked,
                    Math.max(0, rule.keepCount)
            ));
        }
        return payloadRules;
    }

    public void save() {
        normalize();
        try {
            writeJsonAtomically(saveFile, config);
        } catch (Exception e) {
            QuickStackNearby.LOGGER.error("Failed to save Quick Stack Nearby rules", e);
        }
    }

    private void load() {
        if (!Files.exists(saveFile)) {
            normalize();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(saveFile, StandardCharsets.UTF_8)) {
            RuleConfig loaded = GSON.fromJson(reader, RuleConfig.class);
            if (loaded != null) {
                config = loaded;
            }
        } catch (Exception e) {
            QuickStackNearby.LOGGER.error("Failed to load Quick Stack Nearby rules", e);
            Path backup = backupFile(saveFile);
            if (Files.exists(backup)) {
                try (Reader reader = Files.newBufferedReader(backup, StandardCharsets.UTF_8)) {
                    RuleConfig loaded = GSON.fromJson(reader, RuleConfig.class);
                    if (loaded != null) {
                        config = loaded;
                        normalize();
                        return;
                    }
                } catch (Exception backupError) {
                    QuickStackNearby.LOGGER.error("Failed to load Quick Stack Nearby rule backup", backupError);
                }
            }
            config = new RuleConfig();
        }
        normalize();
    }

    private WorldRuleConfig currentWorldRules() {
        normalize();
        String namespace = currentNamespace();
        WorldRuleConfig rules = config.worldRules.computeIfAbsent(namespace, ignored -> new WorldRuleConfig());
        rules.normalize();
        return rules;
    }

    private void normalize() {
        if (config == null) {
            config = new RuleConfig();
        }
        if (config.version <= 0) {
            config.version = CURRENT_VERSION;
        }
        if (config.worldRules == null) {
            config.worldRules = new LinkedHashMap<>();
        }
        config.worldRules.entrySet().removeIf(entry ->
                entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null);
        config.worldRules.values().forEach(WorldRuleConfig::normalize);
        config.version = CURRENT_VERSION;
    }

    private static String currentNamespace() {
        String namespace = QuickStackTrackingNamespace.current(Minecraft.getInstance());
        if (namespace == null || namespace.isBlank()) {
            return UNKNOWN_NAMESPACE;
        }
        return TempestStudiosData.sanitize(namespace);
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

    private static final class RuleConfig {
        int version = CURRENT_VERSION;
        Map<String, WorldRuleConfig> worldRules = new LinkedHashMap<>();
    }

    private static final class WorldRuleConfig {
        SlotRules playerRules = new SlotRules();

        void normalize() {
            if (playerRules == null) {
                playerRules = new SlotRules();
            }
            playerRules.normalize();
        }
    }

    public static final class SlotRules {
        public Map<Integer, SlotRule> slotRules = new LinkedHashMap<>();

        public void normalize() {
            if (slotRules == null) {
                slotRules = new LinkedHashMap<>();
            }
            slotRules.entrySet().removeIf(entry ->
                    entry.getKey() == null || entry.getKey() < 0 || entry.getValue() == null);
            slotRules.values().forEach(SlotRule::normalize);
            slotRules.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }

        public SlotRule ruleFor(int slotIndex) {
            SlotRule rule = slotRules.get(slotIndex);
            return rule == null ? SlotRule.EMPTY : rule;
        }

        public SlotRule mutableRuleFor(int slotIndex) {
            SlotRule rule = slotRules.computeIfAbsent(slotIndex, ignored -> new SlotRule());
            rule.normalize();
            return rule;
        }

        public void cleanupSlotRule(int slotIndex) {
            SlotRule rule = slotRules.get(slotIndex);
            if (rule != null) {
                rule.normalize();
                if (rule.isEmpty()) {
                    slotRules.remove(slotIndex);
                }
            }
        }

        public void clear() {
            slotRules.clear();
        }
    }

    public static final class SlotRule {
        static final SlotRule EMPTY = new SlotRule();

        public boolean locked = false;
        public int keepCount = 0;

        SlotRule copy() {
            SlotRule copy = new SlotRule();
            copy.locked = locked;
            copy.keepCount = keepCount;
            return copy;
        }

        void normalize() {
            keepCount = Math.max(0, Math.min(MAX_KEEP_COUNT, keepCount));
        }

        public boolean hasKeepCount() {
            return keepCount > 0;
        }

        public boolean isEmpty() {
            return !locked && keepCount <= 0;
        }
    }
}
