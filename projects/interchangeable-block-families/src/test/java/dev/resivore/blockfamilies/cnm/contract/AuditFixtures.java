package dev.resivore.blockfamilies.cnm.contract;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class AuditFixtures {
    static final String ROOT = "/audit/cnm-shapemap/";

    private AuditFixtures() {}

    static Properties contract() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = resource("audit-contract.properties")) {
            properties.load(input);
        }
        return properties;
    }

    static List<String[]> tsv(String name, int columns) throws IOException {
        String text = new String(resourceBytes(name), StandardCharsets.UTF_8);

        List<String[]> rows = new ArrayList<>();
        int lineNumber = 0;
        for (String line : text.split("\\R")) {
            lineNumber++;
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split("\\t", -1);
            if (fields.length != columns) {
                throw new AssertionError(name + ":" + lineNumber + " expected " + columns
                        + " tab-separated fields but found " + fields.length);
            }
            rows.add(fields);
        }
        return List.copyOf(rows);
    }

    static byte[] resourceBytes(String name) throws IOException {
        try (InputStream input = resource(name)) {
            return input.readAllBytes();
        }
    }

    static Map<String, Family> representativeFamilies() throws IOException {
        Map<String, Family> families = new LinkedHashMap<>();
        for (String[] row : tsv("representative-families.tsv", 5)) {
            List<String> members = List.of(row[4].split("\\|", -1));
            Family prior = families.put(row[0],
                    new Family(row[0], row[1], Integer.parseInt(row[2]), row[3], members));
            if (prior != null) throw new AssertionError("Duplicate family key " + row[0]);
        }
        return Collections.unmodifiableMap(families);
    }

    static Map<String, String> memberToFamily(Map<String, Family> families) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Family family : families.values()) {
            for (String member : family.members()) {
                String prior = result.put(member, family.key());
                if (prior != null && !prior.equals(family.key())) {
                    throw new AssertionError(member + " overlaps " + prior + " and " + family.key());
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    static Map<String, String> memberToParent(Map<String, Family> families) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Family family : families.values()) {
            for (String member : family.members()) {
                String prior = result.put(member, family.parent());
                if (prior != null && !prior.equals(family.parent())) {
                    throw new AssertionError(member + " has two canonical parents");
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    static Map<String, String> componentPatch(String encoded) {
        if (encoded.equals("-")) return Map.of();
        Map<String, String> patch = new LinkedHashMap<>();
        for (String entry : encoded.split("\\|", -1)) {
            String[] pair = entry.split("=", 2);
            if (pair.length != 2 || pair[0].isBlank()) {
                throw new AssertionError("Invalid component patch entry " + entry);
            }
            if (patch.put(pair[0], pair[1]) != null) {
                throw new AssertionError("Duplicate component patch key " + pair[0]);
            }
        }
        return Collections.unmodifiableMap(patch);
    }

    static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static InputStream resource(String name) {
        InputStream input = AuditFixtures.class.getResourceAsStream(ROOT + name);
        assertNotNull(input, "Missing audit fixture " + ROOT + name);
        return input;
    }

    record Family(String key, String category, int auditedSize, String parent, List<String> members) {}
}
