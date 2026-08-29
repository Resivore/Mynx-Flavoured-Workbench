package dev.resivore.dragonbound.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.Set;

final class DragonboundConfigMigration {
    private static final int LEGACY_DEFAULT_CHANNEL_TICKS = 80;
    private static final Set<String> LEGACY_DEFAULT_KEYS = Set.of(
            "channelTicks",
            "movementTolerance",
            "cancelOnMovement",
            "cancelOnDamage",
            "restrictMounted",
            "restrictFalling",
            "restrictLava",
            "restrictElytra",
            "allowCrossDimension",
            "staffCooldownTicks");

    private DragonboundConfigMigration() {
    }

    static Result apply(JsonObject source) {
        if (!isExactUnversionedLegacyDefault(source)) {
            return new Result(source, false);
        }

        JsonObject migrated = source.deepCopy();
        migrated.addProperty("configVersion", DragonboundConfig.CONFIG_VERSION);
        migrated.addProperty("channelTicks", DragonboundConfig.DEFAULT_CHANNEL_TICKS);
        return new Result(migrated, true);
    }

    private static boolean isExactUnversionedLegacyDefault(JsonObject source) {
        return source.keySet().equals(LEGACY_DEFAULT_KEYS)
                && numberEquals(source, "channelTicks", BigDecimal.valueOf(LEGACY_DEFAULT_CHANNEL_TICKS))
                && numberEquals(source, "movementTolerance", BigDecimal.valueOf(0.25D))
                && boolEquals(source, "cancelOnMovement", true)
                && boolEquals(source, "cancelOnDamage", true)
                && boolEquals(source, "restrictMounted", true)
                && boolEquals(source, "restrictFalling", true)
                && boolEquals(source, "restrictLava", true)
                && boolEquals(source, "restrictElytra", true)
                && boolEquals(source, "allowCrossDimension", true)
                && numberEquals(source, "staffCooldownTicks", BigDecimal.valueOf(1_200));
    }

    private static boolean numberEquals(JsonObject source, String key, BigDecimal expected) {
        JsonElement element = source.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return false;
        }
        try {
            return element.getAsBigDecimal().compareTo(expected) == 0;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean boolEquals(JsonObject source, String key, boolean expected) {
        JsonElement element = source.get(key);
        return element != null
                && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isBoolean()
                && element.getAsBoolean() == expected;
    }

    record Result(JsonObject json, boolean changed) {
    }
}
