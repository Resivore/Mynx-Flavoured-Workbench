package dev.resivore.dragonbound.config;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DragonboundConfigTest {
    @Test
    void defaultsMatchTheCanaryContract() {
        DragonboundConfig config = DragonboundConfig.defaults();

        assertEquals(40, config.channelTicks());
        assertEquals(0.25D, config.movementTolerance());
        assertEquals(1_200, config.staffCooldownTicks());
        assertTrue(config.cancelOnMovement());
        assertTrue(config.cancelOnDamage());
        assertTrue(config.restrictMounted());
        assertTrue(config.restrictFalling());
        assertTrue(config.restrictLava());
        assertTrue(config.restrictElytra());
        assertTrue(config.allowCrossDimension());
    }

    @Test
    void partialJsonKeepsDefaultsForAbsentFields() {
        JsonObject json = new JsonObject();
        json.addProperty("channelTicks", 100);
        json.addProperty("cancelOnMovement", false);

        DragonboundConfig config = DragonboundConfig.fromJson(json);

        assertEquals(100, config.channelTicks());
        assertFalse(config.cancelOnMovement());
        assertEquals(0.25D, config.movementTolerance());
        assertEquals(1_200, config.staffCooldownTicks());
        assertTrue(config.cancelOnDamage());
    }

    @Test
    void malformedAndOutOfRangeValuesFallBackIndividually() {
        JsonObject json = new JsonObject();
        json.addProperty("channelTicks", 0);
        json.addProperty("movementTolerance", Double.NaN);
        json.addProperty("staffCooldownTicks", 72_001);
        json.addProperty("cancelOnDamage", "not a boolean");

        DragonboundConfig config = DragonboundConfig.fromJson(json);

        assertEquals(40, config.channelTicks());
        assertEquals(0.25D, config.movementTolerance());
        assertEquals(1_200, config.staffCooldownTicks());
        assertTrue(config.cancelOnDamage());
    }

    @Test
    void serializedConfigRoundTripsEveryServerSetting() {
        DragonboundConfig expected = new DragonboundConfig(
                60, 0.125D, false, false, false, false, false, false, false, 600);

        JsonObject serialized = expected.toJson();
        DragonboundConfig actual = DragonboundConfig.fromJson(serialized);

        assertEquals(DragonboundConfig.CONFIG_VERSION, serialized.get("configVersion").getAsInt());
        assertEquals(expected, actual);
    }

    @Test
    void exactUnversionedCanaryThreeDefaultsMigrateOnceToFortyTicks() {
        JsonObject legacy = exactLegacyDefaults();

        DragonboundConfigMigration.Result result = DragonboundConfigMigration.apply(legacy);

        assertTrue(result.changed());
        assertFalse(legacy.has("configVersion"));
        assertEquals(80, legacy.get("channelTicks").getAsInt());
        assertEquals(DragonboundConfig.CONFIG_VERSION, result.json().get("configVersion").getAsInt());
        assertEquals(40, DragonboundConfig.fromJson(result.json()).channelTicks());
        assertEquals(1_200, DragonboundConfig.fromJson(result.json()).staffCooldownTicks());
    }

    @Test
    void migrationPreservesCustomPartialAndAlreadyVersionedOverrides() {
        JsonObject customFull = exactLegacyDefaults();
        customFull.addProperty("channelTicks", 60);
        DragonboundConfigMigration.Result customFullResult = DragonboundConfigMigration.apply(customFull);
        assertFalse(customFullResult.changed());
        assertEquals(60, DragonboundConfig.fromJson(customFullResult.json()).channelTicks());

        JsonObject partial = new JsonObject();
        partial.addProperty("channelTicks", 80);
        DragonboundConfigMigration.Result partialResult = DragonboundConfigMigration.apply(partial);
        assertFalse(partialResult.changed());
        assertEquals(80, DragonboundConfig.fromJson(partialResult.json()).channelTicks());

        JsonObject versioned = exactLegacyDefaults();
        versioned.addProperty("configVersion", 7);
        DragonboundConfigMigration.Result versionedResult = DragonboundConfigMigration.apply(versioned);
        assertFalse(versionedResult.changed());
        assertEquals(80, DragonboundConfig.fromJson(versionedResult.json()).channelTicks());
        assertEquals(7, versionedResult.json().get("configVersion").getAsInt());
    }

    private static JsonObject exactLegacyDefaults() {
        JsonObject json = new JsonObject();
        json.addProperty("channelTicks", 80);
        json.addProperty("movementTolerance", 0.25D);
        json.addProperty("cancelOnMovement", true);
        json.addProperty("cancelOnDamage", true);
        json.addProperty("restrictMounted", true);
        json.addProperty("restrictFalling", true);
        json.addProperty("restrictLava", true);
        json.addProperty("restrictElytra", true);
        json.addProperty("allowCrossDimension", true);
        json.addProperty("staffCooldownTicks", 1_200);
        return json;
    }
}
