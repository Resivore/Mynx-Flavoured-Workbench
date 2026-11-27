package dev.resivore.villagerwork.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** Asset capability v1. No Ribbits classes, release identifiers, or protected payload are embedded. */
public final class RibbitsRodContract {
    public static final String MODEL_PATH = "assets/ribbits/geckolib/models/fisherman_ribbit.geo.json";
    public static final String TEXTURE_PATH = "assets/ribbits/textures/entity/ribbit.png";
    private static final String STRUCTURE_SHA256 =
            "6b73080c17b60e62307606b507540d4ccc61579bb037bf93bf7ce548f281d52c";
    private static final String TEXTURE_SHA256 =
            "ba7c0a98b163bb69c266d988c4a0f1b9a9cbd8c6c9368f1b110b3a985adb4332";
    private static final Set<String> HIDDEN_CUBES = Set.of("main", "body", "left_arm",
            "right_arm", "right_leg", "left_leg", "fishing_rod_2", "fishing_rod_3");

    private RibbitsRodContract() {}

    public record Result(boolean compatible, String detail) {}

    public static Result check(byte[] model, byte[] texture) {
        if (model == null || texture == null) {
            return new Result(false, "missing Fisherman model or ribbit texture");
        }
        try {
            JsonObject root = JsonParser.parseString(new String(model, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (JsonElement entry : root.getAsJsonArray("minecraft:geometry")) {
                for (JsonElement element : entry.getAsJsonObject().getAsJsonArray("bones")) {
                    JsonObject bone = element.getAsJsonObject();
                    if (HIDDEN_CUBES.contains(bone.get("name").getAsString())) bone.remove("cubes");
                }
            }
            String structure = sha256(canonical(root).getBytes(StandardCharsets.UTF_8));
            if (!STRUCTURE_SHA256.equals(structure)) {
                return new Result(false, "Fisherman geometry contract v1 changed (structure=" + structure
                        + "); inspect bone hierarchy, transforms, visible cubes/UVs and physical tip");
            }
            if (!TEXTURE_SHA256.equals(sha256(texture))) {
                return new Result(false, "ribbit texture contract v1 changed; verify rod UV artwork");
            }
            return new Result(true, "Fisherman asset contract v1 satisfied");
        } catch (RuntimeException error) {
            return new Result(false, "malformed Fisherman geometry: " + error.getClass().getSimpleName());
        }
    }

    // Ignore JSON key order/whitespace and numeric spelling, but retain array order and unknown
    // fields. In particular, unknown bones fail closed instead of leaking their geometry.
    private static String canonical(JsonElement value) {
        if (value.isJsonObject()) {
            return value.getAsJsonObject().keySet().stream().sorted()
                    .map(key -> new com.google.gson.JsonPrimitive(key) + ":"
                            + canonical(value.getAsJsonObject().get(key)))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (value.isJsonArray()) {
            return StreamSupport.stream(value.getAsJsonArray().spliterator(), false)
                    .map(RibbitsRodContract::canonical).collect(Collectors.joining(",", "[", "]"));
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return value.getAsBigDecimal().stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }
}
