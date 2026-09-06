package com.crispytwig.naturalist.startup;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class PrivateResourcePreservationTest {
    private static final Path ROOT = Path.of(System.getProperty("naturalist.stagedResources"));
    private int ingredients, entityTypes, soundPaths;

    // Compare the entire private corpus. Every difference must be one of the
    // demonstrated leaf-level migrations; patterns, results and conditions survive.
    @Test
    void onlyRequiredSchemaLeavesAndSoundPathsChange() throws Exception {
        int recipes = 0, predicates = 0, unchanged = 0;
        try (var zip = new ZipFile(System.getProperty("naturalist.originalJar")); var files = Files.walk(ROOT)) {
            for (var file : files.filter(Files::isRegularFile).toList()) {
                String name = ROOT.relativize(file).toString().replace('\\', '/');
                if (name.startsWith("data/") || name.equals("assets/naturalist/sounds.json")) continue;
                var entry = zip.getEntry(name);
                assertNotNull(entry, name);
                byte[] original;
                try (var input = zip.getInputStream(entry)) { original = input.readAllBytes(); }
                byte[] staged = Files.readAllBytes(file);
                if (name.equals("naturalist.fieldguide.mixins.json")) {
                    var before = JsonParser.parseString(new String(original, StandardCharsets.UTF_8)).getAsJsonObject();
                    var after = JsonParser.parseString(new String(staged, StandardCharsets.UTF_8)).getAsJsonObject();
                    assertEquals("com.crispytwig.naturalist.fabric.compat.FieldGuideMixinPlugin",
                            after.remove("plugin").getAsString());
                    assertEquals(before, after);
                } else if (name.equals("naturalist.mixins.json")) {
                    var before = JsonParser.parseString(new String(original, StandardCharsets.UTF_8)).getAsJsonObject();
                    var after = JsonParser.parseString(new String(staged, StandardCharsets.UTF_8)).getAsJsonObject();
                    var expected = before.deepCopy();
                    expected.getAsJsonArray("mixins").add("CreativeItemStackIdentityMixin");
                    assertEquals(expected, after);
                } else {
                    assertArrayEquals(original, staged, name);
                    unchanged++;
                }
            }
        }
        assertEquals(0, recipes);
        assertEquals(0, ingredients);
        assertEquals(0, predicates);
        assertEquals(0, entityTypes);
        assertEquals(0, soundPaths);
        assertTrue(unchanged > 1300);
    }

    private void compare(JsonElement before, JsonElement after, String file, String path) {
        if (before.equals(after)) return;
        String location = file + path;
        if ((file.startsWith("data/naturalist/recipe/") || file.startsWith("data/minecraft/recipe/")) &&
                path.matches("/(ingredient|ingredients/[0-9]+|key/.)") &&
                before.isJsonObject() && after.isJsonPrimitive()) {
            var object = before.getAsJsonObject();
            assertEquals(1, object.size(), location);
            assertTrue(object.has("item") || object.has("tag"), location);
            String expected = object.has("item") ? object.get("item").getAsString() : "#" + object.get("tag").getAsString();
            assertEquals(expected, after.getAsString(), location);
            ingredients++;
        } else if (before.isJsonObject() && after.isJsonObject()) {
            var left = before.getAsJsonObject();
            var right = after.getAsJsonObject();
            Set<String> leftKeys = new HashSet<>(left.keySet()), rightKeys = new HashSet<>(right.keySet());
            if (file.startsWith("data/naturalist/") && leftKeys.contains("type") && rightKeys.contains("minecraft:entity_type")) {
                assertTrue(path.endsWith("/predicate") || path.endsWith("/entity") || path.endsWith("/vehicle")
                        || path.endsWith("/source_entity"), location);
                assertEquals(left.get("type"), right.get("minecraft:entity_type"), location);
                leftKeys.remove("type");
                rightKeys.remove("minecraft:entity_type");
                entityTypes++;
            }
            assertEquals(leftKeys, rightKeys, location);
            for (String key : leftKeys) compare(left.get(key), right.get(key), file, path + "/" + key);
        } else if (before.isJsonArray() && after.isJsonArray()) {
            assertEquals(before.getAsJsonArray().size(), after.getAsJsonArray().size(), location);
            for (int i = 0; i < before.getAsJsonArray().size(); i++) {
                compare(before.getAsJsonArray().get(i), after.getAsJsonArray().get(i), file, path + "/" + i);
            }
        } else if (file.equals("assets/naturalist/sounds.json") &&
                (path.endsWith("/name") || path.matches(".*/sounds/[0-9]+"))) {
            assertTrue(before.getAsString().matches("minecraft:entity/(pufferfish/flop[1-4]|horse/eat[1-5])"), location);
            assertEquals(before.getAsString().replace("minecraft:entity/", "minecraft:mob/"), after.getAsString(), location);
            soundPaths++;
        } else {
            fail("Unintended private resource change at " + location);
        }
    }

    @Test
    void everySoundFileAndEventResolvesAgainstPrivateAssetsOrThe26_2Index() throws Exception {
        Path indexPath = Path.of(System.getProperty("naturalist.assetIndex"));
        var objects = JsonParser.parseString(Files.readString(indexPath)).getAsJsonObject().getAsJsonObject("objects");
        String hash = objects.getAsJsonObject("minecraft/sounds.json").get("hash").getAsString();
        byte[] vanillaBytes = Files.readAllBytes(indexPath.getParent().getParent().resolve("objects/" + hash.substring(0, 2) + "/" + hash));
        assertEquals(hash, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(vanillaBytes)));
        var vanillaEvents = JsonParser.parseString(new String(vanillaBytes, StandardCharsets.UTF_8)).getAsJsonObject();
        var events = JsonParser.parseString(Files.readString(ROOT.resolve("assets/naturalist/sounds.json"))).getAsJsonObject();
        int checked = 0;
        for (var event : events.asMap().values()) {
            for (var sound : event.getAsJsonObject().getAsJsonArray("sounds")) {
                JsonObject object = sound.isJsonObject() ? sound.getAsJsonObject() : null;
                String name = object == null ? sound.getAsString() : object.get("name").getAsString();
                String[] id = name.contains(":") ? name.split(":", 2) : new String[]{"minecraft", name};
                assertTrue(Set.of("minecraft", "naturalist").contains(id[0]), name);
                if (object != null && object.has("type") && object.get("type").getAsString().equals("event")) {
                    assertTrue((id[0].equals("minecraft") ? vanillaEvents : events).has(id[1]), name);
                } else if (id[0].equals("minecraft")) {
                    assertTrue(objects.has("minecraft/sounds/" + id[1] + ".ogg"), name);
                } else {
                    assertTrue(Files.isRegularFile(ROOT.resolve("assets/naturalist/sounds/" + id[1] + ".ogg")), name);
                }
                checked++;
            }
        }
        assertEquals(648, checked);
    }
}
