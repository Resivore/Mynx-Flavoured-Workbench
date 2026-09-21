package dev.resivore.blockfamilies.cnm.catalog;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.BAR_CHAIN;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.BUILDING_ACCESSORY;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.DISPLAY_FIXTURE;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.FENCE_GATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Literal C8 contract for the Minecraft 26.2 and Enderscape 3.0.2 display/accessory audit. */
final class EnderscapeDisplayFixtureFamiliesTest {
    private static final String MINECRAFT_JAR_PROPERTY = "minecraftReferenceJar";
    private static final String ENDERSCAPE_JAR_PROPERTY = "enderscapeReferenceJar";
    private static final String BLOCK_ITEM_ID_DESCRIPTOR = "Lnet/minecraft/references/BlockItemId;";
    private static final String BLOCK_RESOURCE_KEY_DESCRIPTOR =
            "Lnet/minecraft/resources/ResourceKey;";

    private static final Pattern MODERN_ITEM_ASSET = Pattern.compile(
            "^assets/([^/]+)/items/(.+)\\.json$");
    private static final Pattern LEGACY_ITEM_MODEL_ASSET = Pattern.compile(
            "^assets/([^/]+)/models/item/(.+)\\.json$");

    private static final List<ExpectedFamily> DISPLAY_FIXTURES = List.of(
            family("cnm/display_fixture/oak", DISPLAY_FIXTURE,
                    "minecraft:oak_sign", "minecraft:oak_hanging_sign", "minecraft:oak_shelf"),
            family("cnm/display_fixture/spruce", DISPLAY_FIXTURE,
                    "minecraft:spruce_sign", "minecraft:spruce_hanging_sign", "minecraft:spruce_shelf"),
            family("cnm/display_fixture/birch", DISPLAY_FIXTURE,
                    "minecraft:birch_sign", "minecraft:birch_hanging_sign", "minecraft:birch_shelf"),
            family("cnm/display_fixture/jungle", DISPLAY_FIXTURE,
                    "minecraft:jungle_sign", "minecraft:jungle_hanging_sign", "minecraft:jungle_shelf"),
            family("cnm/display_fixture/acacia", DISPLAY_FIXTURE,
                    "minecraft:acacia_sign", "minecraft:acacia_hanging_sign", "minecraft:acacia_shelf"),
            family("cnm/display_fixture/dark_oak", DISPLAY_FIXTURE,
                    "minecraft:dark_oak_sign", "minecraft:dark_oak_hanging_sign", "minecraft:dark_oak_shelf"),
            family("cnm/display_fixture/mangrove", DISPLAY_FIXTURE,
                    "minecraft:mangrove_sign", "minecraft:mangrove_hanging_sign", "minecraft:mangrove_shelf"),
            family("cnm/display_fixture/cherry", DISPLAY_FIXTURE,
                    "minecraft:cherry_sign", "minecraft:cherry_hanging_sign", "minecraft:cherry_shelf"),
            family("cnm/display_fixture/pale_oak", DISPLAY_FIXTURE,
                    "minecraft:pale_oak_sign", "minecraft:pale_oak_hanging_sign", "minecraft:pale_oak_shelf"),
            family("cnm/display_fixture/bamboo", DISPLAY_FIXTURE,
                    "minecraft:bamboo_sign", "minecraft:bamboo_hanging_sign", "minecraft:bamboo_shelf"),
            family("cnm/display_fixture/crimson", DISPLAY_FIXTURE,
                    "minecraft:crimson_sign", "minecraft:crimson_hanging_sign", "minecraft:crimson_shelf"),
            family("cnm/display_fixture/warped", DISPLAY_FIXTURE,
                    "minecraft:warped_sign", "minecraft:warped_hanging_sign", "minecraft:warped_shelf"),
            family("cnm/display_fixture/enderscape_veiled", DISPLAY_FIXTURE,
                    "enderscape:veiled_sign", "enderscape:veiled_hanging_sign", "enderscape:veiled_shelf"),
            family("cnm/display_fixture/enderscape_celestial", DISPLAY_FIXTURE,
                    "enderscape:celestial_sign", "enderscape:celestial_hanging_sign", "enderscape:celestial_shelf"),
            family("cnm/display_fixture/enderscape_murublight", DISPLAY_FIXTURE,
                    "enderscape:murublight_sign", "enderscape:murublight_hanging_sign",
                    "enderscape:murublight_shelf"));

    private static final List<ExpectedFamily> ENDERSCAPE_FENCE_GATES = List.of(
            family("cnm/fence_gate/enderscape_veiled", FENCE_GATE,
                    "enderscape:veiled_fence", "enderscape:veiled_fence_gate"),
            family("cnm/fence_gate/enderscape_celestial", FENCE_GATE,
                    "enderscape:celestial_fence", "enderscape:celestial_fence_gate"),
            family("cnm/fence_gate/enderscape_murublight", FENCE_GATE,
                    "enderscape:murublight_fence", "enderscape:murublight_fence_gate"));

    private static final List<ExpectedFamily> ENDERSCAPE_BUILDING_ACCESSORIES = List.of(
            family("cnm/building_accessory/enderscape_veiled", BUILDING_ACCESSORY,
                    "enderscape:veiled_button", "enderscape:veiled_pressure_plate"),
            family("cnm/building_accessory/enderscape_celestial", BUILDING_ACCESSORY,
                    "enderscape:celestial_button", "enderscape:celestial_pressure_plate"),
            family("cnm/building_accessory/enderscape_murublight", BUILDING_ACCESSORY,
                    "enderscape:murublight_button", "enderscape:murublight_pressure_plate"),
            family("cnm/building_accessory/enderscape_polished_end_stone", BUILDING_ACCESSORY,
                    "enderscape:polished_end_stone_button",
                    "enderscape:polished_end_stone_pressure_plate"),
            family("cnm/building_accessory/enderscape_polished_mirestone", BUILDING_ACCESSORY,
                    "enderscape:polished_mirestone_button",
                    "enderscape:polished_mirestone_pressure_plate"),
            family("cnm/building_accessory/enderscape_polished_veradite", BUILDING_ACCESSORY,
                    "enderscape:polished_veradite_button",
                    "enderscape:polished_veradite_pressure_plate"),
            family("cnm/building_accessory/enderscape_polished_kurodite", BUILDING_ACCESSORY,
                    "enderscape:polished_kurodite_button",
                    "enderscape:polished_kurodite_pressure_plate"));

    private static final List<ExpectedFamily> ENDERSCAPE_BAR_CHAINS = List.of(
            family("cnm/bar_chain/enderscape_shadoline", BAR_CHAIN,
                    "enderscape:shadoline_bars", "enderscape:shadoline_chain"));

    private static final Set<String> WALL_ONLY_BLOCKS = Set.of(
            "minecraft:oak_wall_sign", "minecraft:oak_wall_hanging_sign",
            "minecraft:spruce_wall_sign", "minecraft:spruce_wall_hanging_sign",
            "minecraft:birch_wall_sign", "minecraft:birch_wall_hanging_sign",
            "minecraft:jungle_wall_sign", "minecraft:jungle_wall_hanging_sign",
            "minecraft:acacia_wall_sign", "minecraft:acacia_wall_hanging_sign",
            "minecraft:dark_oak_wall_sign", "minecraft:dark_oak_wall_hanging_sign",
            "minecraft:mangrove_wall_sign", "minecraft:mangrove_wall_hanging_sign",
            "minecraft:cherry_wall_sign", "minecraft:cherry_wall_hanging_sign",
            "minecraft:pale_oak_wall_sign", "minecraft:pale_oak_wall_hanging_sign",
            "minecraft:bamboo_wall_sign", "minecraft:bamboo_wall_hanging_sign",
            "minecraft:crimson_wall_sign", "minecraft:crimson_wall_hanging_sign",
            "minecraft:warped_wall_sign", "minecraft:warped_wall_hanging_sign",
            "enderscape:veiled_wall_sign", "enderscape:veiled_wall_hanging_sign",
            "enderscape:celestial_wall_sign", "enderscape:celestial_wall_hanging_sign",
            "enderscape:murublight_wall_sign", "enderscape:murublight_wall_hanging_sign");

    private static final List<String> OTHER_PROVIDER_JAR_PROPERTIES = List.of(
            "cnmUpstreamReferenceJar",
            "macawsDoorsReferenceJar",
            "macawsPathsReferenceJar",
            "aurorasLanternsReferenceJar",
            "ribbitsReferenceJar",
            "bbbReferenceJar",
            "lithostitchedReferenceJar",
            "trimPatcherReferenceJar",
            "yaclReferenceJar",
            "macawsTrapdoorsReferenceJar",
            "macawsWindowsReferenceJar",
            "dramaticDoorsReferenceJar",
            "nibaruReferenceJar",
            "cnmIntegrationReferenceJar",
            "qsnReferenceJar",
            "qsnCompatReferenceJar");

    @Test
    void displayFixtureCatalogIsExactlyTwelveMinecraftAndThreeEnderscapeTriples() {
        assertExpectedFamilies(DISPLAY_FIXTURES, AuditedShapeFamilies.families(DISPLAY_FIXTURE));
        for (AuditedShapeFamily actual : AuditedShapeFamilies.families(DISPLAY_FIXTURE)) {
            assertEquals(3, actual.members().size(), actual.key().toString());
            assertEquals(actual.canonicalParent(), actual.members().getFirst(), actual.key().toString());
            assertTrue(actual.canonicalParent().getPath().endsWith("_sign"), actual.key().toString());
            assertFalse(actual.canonicalParent().getPath().endsWith("_hanging_sign"), actual.key().toString());
        }
    }

    @Test
    void enderscapeUsesTheExistingFenceAccessoryAndBarChainArchitecturesExactly() {
        assertExpectedFamilies(ENDERSCAPE_FENCE_GATES, enderscapeFamilies(FENCE_GATE));
        assertExpectedFamilies(ENDERSCAPE_BUILDING_ACCESSORIES,
                enderscapeFamilies(BUILDING_ACCESSORY));
        assertExpectedFamilies(ENDERSCAPE_BAR_CHAINS, enderscapeFamilies(BAR_CHAIN));

        List<ExpectedFamily> expectedEnderscape = new ArrayList<>();
        expectedEnderscape.addAll(DISPLAY_FIXTURES.stream()
                .filter(family -> family.members().getFirst().getNamespace().equals("enderscape"))
                .toList());
        expectedEnderscape.addAll(ENDERSCAPE_FENCE_GATES);
        expectedEnderscape.addAll(ENDERSCAPE_BAR_CHAINS);
        expectedEnderscape.addAll(ENDERSCAPE_BUILDING_ACCESSORIES);

        Set<Identifier> expectedMembers = new LinkedHashSet<>();
        expectedEnderscape.forEach(family -> expectedMembers.addAll(family.members()));
        Set<Identifier> actualMembers = new LinkedHashSet<>();
        AuditedShapeFamilies.families().stream()
                .flatMap(family -> family.members().stream())
                .filter(member -> member.getNamespace().equals("enderscape"))
                .forEach(actualMembers::add);
        assertEquals(expectedMembers, actualMembers,
                "Only the approved literal Enderscape members may enter the catalog");
    }

    @Test
    void actualBlockItemDeclarationsAndAssetsYieldOnlyTheFifteenLiteralEligibleTriples()
            throws Exception {
        try (JarFile minecraft = providerJar(MINECRAFT_JAR_PROPERTY);
             JarFile enderscape = providerJar(ENDERSCAPE_JAR_PROPERTY)) {
            Set<DisplayTriple> expectedMinecraft = expectedDisplayTriples("minecraft");
            Set<DisplayTriple> expectedEnderscape = expectedDisplayTriples("enderscape");

            Set<String> minecraftAssets = itemAssetIds(minecraft);
            Set<String> minecraftBlockItems = staticStringIds(
                    minecraft,
                    "net/minecraft/references/BlockItemIds.class",
                    BLOCK_ITEM_ID_DESCRIPTOR,
                    "minecraft");
            Set<String> enderscapeAssets = itemAssetIds(enderscape);
            Set<String> enderscapeBlockItems = staticStringIds(
                    enderscape,
                    "net/penumbra/enderscape/references/EnderscapeBlockItemIds.class",
                    BLOCK_ITEM_ID_DESCRIPTOR,
                    "enderscape");

            assertProviderDisplaySurface(
                    expectedMinecraft, minecraftAssets, minecraftBlockItems, "Minecraft 26.2");
            assertProviderDisplaySurface(
                    expectedEnderscape, enderscapeAssets, enderscapeBlockItems, "Enderscape");

            Set<DisplayTriple> completeActualSet = new LinkedHashSet<>();
            completeActualSet.addAll(discoverDisplayTriples(intersection(
                    minecraftAssets, minecraftBlockItems)));
            completeActualSet.addAll(discoverDisplayTriples(intersection(
                    enderscapeAssets, enderscapeBlockItems)));
            assertEquals(expectedDisplayTriples(null), completeActualSet,
                    "The audited providers expose an unexpected complete Sign/Hanging Sign/Shelf triple");
        }
    }

    @Test
    void noOtherCurrentProviderJarExposesACompleteDisplayFixtureTriple() throws Exception {
        Map<String, Set<DisplayTriple>> unexpectedByProvider = new HashMap<>();
        for (String property : OTHER_PROVIDER_JAR_PROPERTIES) {
            try (JarFile provider = providerJar(property)) {
                Set<DisplayTriple> discovered = discoverDisplayTriples(itemAssetIds(provider));
                if (!discovered.isEmpty()) unexpectedByProvider.put(property, discovered);
            }
        }
        assertEquals(Map.of(), unexpectedByProvider,
                "A current non-Minecraft/non-Enderscape provider exposes an unaudited display triple");
    }

    @Test
    void wallSignPlacementBlocksAreRealButHaveNoItemsAndNeverEnterShapeMap() throws Exception {
        try (JarFile minecraft = providerJar(MINECRAFT_JAR_PROPERTY);
             JarFile enderscape = providerJar(ENDERSCAPE_JAR_PROPERTY)) {
            Set<String> minecraftBlockIds = staticStringIds(
                    minecraft,
                    "net/minecraft/references/BlockIds.class",
                    BLOCK_RESOURCE_KEY_DESCRIPTOR,
                    "minecraft");
            Set<String> enderscapeBlockIds = staticStringIds(
                    enderscape,
                    "net/penumbra/enderscape/references/EnderscapeBlockIds.class",
                    BLOCK_RESOURCE_KEY_DESCRIPTOR,
                    "enderscape");
            Set<String> declaredBlocks = new HashSet<>(minecraftBlockIds);
            declaredBlocks.addAll(enderscapeBlockIds);
            assertTrue(declaredBlocks.containsAll(WALL_ONLY_BLOCKS),
                    "The literal wall-only placement blocks must remain present in provider BlockIds");

            Set<String> blockstateAssets = new HashSet<>(blockstateAssetIds(minecraft, "minecraft"));
            blockstateAssets.addAll(blockstateAssetIds(enderscape, "enderscape"));
            assertTrue(blockstateAssets.containsAll(WALL_ONLY_BLOCKS),
                    "The literal wall-only placement blocks must retain provider blockstates");

            Set<String> itemAssets = new HashSet<>(itemAssetIds(minecraft));
            itemAssets.addAll(itemAssetIds(enderscape));
            assertTrue(java.util.Collections.disjoint(WALL_ONLY_BLOCKS, itemAssets),
                    "Wall-only placement blocks unexpectedly acquired independent inventory items");

            Set<String> declaredBlockItems = new HashSet<>(staticStringIds(
                    minecraft,
                    "net/minecraft/references/BlockItemIds.class",
                    BLOCK_ITEM_ID_DESCRIPTOR,
                    "minecraft"));
            declaredBlockItems.addAll(staticStringIds(
                    enderscape,
                    "net/penumbra/enderscape/references/EnderscapeBlockItemIds.class",
                    BLOCK_ITEM_ID_DESCRIPTOR,
                    "enderscape"));
            assertTrue(java.util.Collections.disjoint(WALL_ONLY_BLOCKS, declaredBlockItems),
                    "Wall-only placement blocks unexpectedly acquired BlockItemId declarations");
        }

        assertTrue(java.util.Collections.disjoint(WALL_ONLY_BLOCKS, allCatalogMemberStrings()),
                "Wall-only sign placement blocks must never be ShapeMap members");
    }

    @Test
    void everyCatalogInventoryItemStillHasExactlyOneFamilyOwner() {
        Map<Identifier, Identifier> ownerByMember = new HashMap<>();
        for (AuditedShapeFamily family : AuditedShapeFamilies.families()) {
            for (Identifier member : family.members()) {
                assertNull(ownerByMember.put(member, family.key()),
                        member + " occurs in more than one audited family");
            }
        }
        assertEquals(AuditedShapeFamilies.uniqueMemberCount(), ownerByMember.size());
    }

    private static void assertProviderDisplaySurface(
            Set<DisplayTriple> expected,
            Set<String> itemAssets,
            Set<String> blockItemIds,
            String providerName
    ) {
        Set<String> expectedItems = new LinkedHashSet<>();
        expected.forEach(triple -> expectedItems.addAll(triple.members()));
        assertTrue(itemAssets.containsAll(expectedItems),
                providerName + " is missing an audited inventory-item asset");
        assertTrue(blockItemIds.containsAll(expectedItems),
                providerName + " is missing an audited BlockItemId declaration");
        assertEquals(expected, discoverDisplayTriples(intersection(itemAssets, blockItemIds)),
                providerName + " exposes an unexpected complete display-fixture triple");
    }

    /**
     * Test-only provider audit. Production never discovers families by registry-name shape; its
     * complete catalog remains the literal {@link #DISPLAY_FIXTURES} list asserted above.
     */
    private static Set<DisplayTriple> discoverDisplayTriples(Set<String> inventoryItemIds) {
        Set<DisplayTriple> result = new LinkedHashSet<>();
        for (String sign : inventoryItemIds) {
            int separator = sign.indexOf(':');
            if (separator < 1) continue;
            String namespace = sign.substring(0, separator);
            String path = sign.substring(separator + 1);
            if (!path.endsWith("_sign") || path.endsWith("_hanging_sign")) continue;
            String material = path.substring(0, path.length() - "_sign".length());
            String hangingSign = namespace + ":" + material + "_hanging_sign";
            String shelf = namespace + ":" + material + "_shelf";
            if (inventoryItemIds.contains(hangingSign) && inventoryItemIds.contains(shelf)) {
                result.add(new DisplayTriple(sign, hangingSign, shelf));
            }
        }
        return Set.copyOf(result);
    }

    private static Set<DisplayTriple> expectedDisplayTriples(String namespace) {
        Set<DisplayTriple> result = new LinkedHashSet<>();
        for (ExpectedFamily family : DISPLAY_FIXTURES) {
            if (namespace == null || family.members().getFirst().getNamespace().equals(namespace)) {
                result.add(new DisplayTriple(
                        family.members().get(0).toString(),
                        family.members().get(1).toString(),
                        family.members().get(2).toString()));
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> intersection(Set<String> left, Set<String> right) {
        Set<String> result = new LinkedHashSet<>(left);
        result.retainAll(right);
        return Set.copyOf(result);
    }

    private static Set<String> itemAssetIds(JarFile jar) {
        Set<String> result = new LinkedHashSet<>();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            Matcher modern = MODERN_ITEM_ASSET.matcher(entry.getName());
            Matcher legacy = LEGACY_ITEM_MODEL_ASSET.matcher(entry.getName());
            if (modern.matches()) {
                result.add(modern.group(1) + ":" + modern.group(2));
            } else if (legacy.matches()) {
                result.add(legacy.group(1) + ":" + legacy.group(2));
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> blockstateAssetIds(JarFile jar, String namespace) {
        String prefix = "assets/" + namespace + "/blockstates/";
        Set<String> result = new LinkedHashSet<>();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory() && name.startsWith(prefix) && name.endsWith(".json")) {
                result.add(namespace + ":" + name.substring(prefix.length(), name.length() - 5));
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> staticStringIds(
            JarFile jar, String classEntry, String descriptor, String namespace) throws Exception {
        JarEntry entry = jar.getJarEntry(classEntry);
        assertNotNull(entry, "Missing audited provider class " + classEntry);
        ClassNode owner = new ClassNode();
        try (InputStream input = jar.getInputStream(entry)) {
            new ClassReader(input).accept(owner, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        MethodNode clinit = owner.methods.stream()
                .filter(method -> method.name.equals("<clinit>"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(classEntry + " has no static initializer"));

        Set<String> result = new LinkedHashSet<>();
        for (AbstractInsnNode instruction : clinit.instructions) {
            if (!(instruction instanceof FieldInsnNode field)
                    || instruction.getOpcode() != Opcodes.PUTSTATIC
                    || !field.owner.equals(owner.name)
                    || !field.desc.equals(descriptor)) {
                continue;
            }
            String value = precedingStringLiteral(instruction);
            if (value != null) result.add(value.contains(":") ? value : namespace + ":" + value);
        }
        return Set.copyOf(result);
    }

    private static String precedingStringLiteral(AbstractInsnNode instruction) {
        AbstractInsnNode cursor = instruction.getPrevious();
        while (cursor != null && cursor.getOpcode() != Opcodes.PUTSTATIC) {
            if (cursor instanceof LdcInsnNode ldc && ldc.cst instanceof String value) return value;
            cursor = cursor.getPrevious();
        }
        return null;
    }

    private static List<AuditedShapeFamily> enderscapeFamilies(
            AuditedShapeFamily.Category category) {
        return AuditedShapeFamilies.families(category).stream()
                .filter(family -> family.members().stream()
                        .anyMatch(member -> member.getNamespace().equals("enderscape")))
                .toList();
    }

    private static Set<String> allCatalogMemberStrings() {
        Set<String> result = new LinkedHashSet<>();
        AuditedShapeFamilies.families().stream()
                .flatMap(family -> family.members().stream())
                .map(Identifier::toString)
                .forEach(result::add);
        return Set.copyOf(result);
    }

    private static void assertExpectedFamilies(
            List<ExpectedFamily> expected, List<AuditedShapeFamily> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            ExpectedFamily expectedFamily = expected.get(index);
            AuditedShapeFamily actualFamily = actual.get(index);
            assertEquals(expectedFamily.key(), actualFamily.key());
            assertEquals(expectedFamily.category(), actualFamily.category());
            assertEquals(expectedFamily.members(), actualFamily.members());
            assertEquals(expectedFamily.members().getFirst(), actualFamily.canonicalParent());
        }
    }

    private static ExpectedFamily family(
            String keyPath, AuditedShapeFamily.Category category, String... members) {
        List<Identifier> ids = java.util.Arrays.stream(members).map(Identifier::parse).toList();
        return new ExpectedFamily(
                Identifier.fromNamespaceAndPath("interchangeable_block_families", keyPath),
                category,
                ids);
    }

    private static JarFile providerJar(String property) throws Exception {
        String configured = System.getProperty(property);
        assertTrue(configured != null && !configured.isBlank(),
                "Missing Gradle-wired property " + property);
        Path path = Path.of(configured);
        assertTrue(Files.isRegularFile(path), "Missing exact provider artifact " + path);
        return new JarFile(path.toFile());
    }

    private record ExpectedFamily(
            Identifier key, AuditedShapeFamily.Category category, List<Identifier> members) {
    }

    private record DisplayTriple(String sign, String hangingSign, String shelf) {
        private List<String> members() {
            return List.of(sign, hangingSign, shelf);
        }
    }
}
