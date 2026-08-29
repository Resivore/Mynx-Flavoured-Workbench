package dev.resivore.dragonbound.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record DragonboundConfig(
        int channelTicks,
        double movementTolerance,
        boolean cancelOnMovement,
        boolean cancelOnDamage,
        boolean restrictMounted,
        boolean restrictFalling,
        boolean restrictLava,
        boolean restrictElytra,
        boolean allowCrossDimension,
        int staffCooldownTicks) {
    public static final int CONFIG_VERSION = 1;
    public static final int DEFAULT_CHANNEL_TICKS = 40;
    public static final double DEFAULT_MOVEMENT_TOLERANCE = 0.25D;
    public static final int DEFAULT_STAFF_COOLDOWN_TICKS = 1_200;

    public static DragonboundConfig defaults() {
        return new DragonboundConfig(
                DEFAULT_CHANNEL_TICKS,
                DEFAULT_MOVEMENT_TOLERANCE,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                DEFAULT_STAFF_COOLDOWN_TICKS);
    }

    public static DragonboundConfig fromJson(JsonObject json) {
        DragonboundConfig defaults = defaults();
        return new DragonboundConfig(
                boundedInt(json, "channelTicks", defaults.channelTicks(), 1, 12_000),
                boundedDouble(json, "movementTolerance", defaults.movementTolerance(), 0.0D, 16.0D),
                bool(json, "cancelOnMovement", defaults.cancelOnMovement()),
                bool(json, "cancelOnDamage", defaults.cancelOnDamage()),
                bool(json, "restrictMounted", defaults.restrictMounted()),
                bool(json, "restrictFalling", defaults.restrictFalling()),
                bool(json, "restrictLava", defaults.restrictLava()),
                bool(json, "restrictElytra", defaults.restrictElytra()),
                bool(json, "allowCrossDimension", defaults.allowCrossDimension()),
                boundedInt(json, "staffCooldownTicks", defaults.staffCooldownTicks(), 0, 72_000));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("configVersion", CONFIG_VERSION);
        json.addProperty("channelTicks", channelTicks);
        json.addProperty("movementTolerance", movementTolerance);
        json.addProperty("cancelOnMovement", cancelOnMovement);
        json.addProperty("cancelOnDamage", cancelOnDamage);
        json.addProperty("restrictMounted", restrictMounted);
        json.addProperty("restrictFalling", restrictFalling);
        json.addProperty("restrictLava", restrictLava);
        json.addProperty("restrictElytra", restrictElytra);
        json.addProperty("allowCrossDimension", allowCrossDimension);
        json.addProperty("staffCooldownTicks", staffCooldownTicks);
        return json;
    }

    private static int boundedInt(JsonObject json, String key, int fallback, int minimum, int maximum) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        try {
            int value = element.getAsInt();
            return value >= minimum && value <= maximum ? value : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double boundedDouble(JsonObject json, String key, double fallback, double minimum, double maximum) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        try {
            double value = element.getAsDouble();
            return Double.isFinite(value) && value >= minimum && value <= maximum ? value : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            return fallback;
        }
        return element.getAsBoolean();
    }
}
