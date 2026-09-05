package dev.resivore.quickstacknearbycompat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityScopeTest {
    @Test
    void runtimeMetadataUsesFlexibleCapabilityAndOptionalProviderPredicates() throws Exception {
        Path root = projectRoot();
        String metadata = Files.readString(root.resolve("src/main/resources/fabric.mod.json"));
        String build = Files.readString(root.resolve("build.gradle"));

        assertTrue(metadata.contains("\"quick-stack-nearby\": \">=0.4.0\""));
        String depends = metadata.substring(metadata.indexOf("\"depends\""), metadata.indexOf("\"suggests\""));
        assertFalse(depends.contains("container_slot_reservations"));
        assertFalse(depends.contains("inventoryextended"));
        assertFalse(depends.contains("inventorysearch"));
        assertFalse(depends.contains("clutternomore"));
        assertFalse(depends.contains("cnm_terrain_slabs_compat"));
        assertFalse(depends.contains("more_slabs_stairs_and_walls"));
        for (String optional : List.of(
                "container_slot_reservations",
                "inventorysearch",
                "inventoryextended",
                "clutternomore",
                "cnm_terrain_slabs_compat",
                "more_slabs_stairs_and_walls")) {
            assertTrue(metadata.contains("\"" + optional + "\": \"*\""),
                    "Missing flexible optional predicate for " + optional);
        }
        assertTrue(build.contains("compileOnly(\"maven.modrinth:quick-stack-nearby:"));
        assertTrue(build.contains("inventorySearchReference(\"maven.modrinth:"));
        assertTrue(build.contains("compileOnly files(csrReferenceJar)"));
        assertFalse(build.contains("inventoryextended"));
    }

    @Test
    void exactShapeMapApiIsGatedAndIsolatedWithoutParallelFamilyInference() throws Exception {
        Path sourceRoot = projectRoot().resolve("src/main/java");
        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertEquals(18, javaFiles.size());

        StringBuilder sources = new StringBuilder();
        for (Path javaFile : javaFiles) {
            String source = Files.readString(javaFile, StandardCharsets.UTF_8);
            sources.append(source);
            if (!javaFile.getFileName().toString().equals("CnmShapeMapApi.java")) {
                assertFalse(source.contains("dev.tazer.clutternomore.common.shape_map.ShapeMap"),
                        "Optional ShapeMap API leaked outside its post-gate class: " + javaFile);
            }
        }
        String source = sources.toString();
        assertFalse(source.contains("import net.inventoryextended"));
        assertFalse(source.contains("import tempeststudios.inventorysort"));
        assertTrue(source.contains("FabricLoader.getInstance()"));
        assertTrue(source.contains("isModLoaded(CNM_MOD_ID)"));
        assertTrue(source.contains("ShapeMap.inSameShapeSet(first, second)"));
        assertFalse(source.contains("games.twinhead.moreslabsstairsandwalls.api.material"));
        assertFalse(source.contains("canonicalParent"));
        assertFalse(source.contains("ShapeMap.getParent"));
        assertFalse(source.contains("getPath()"));
        assertFalse(source.contains("Identifier.parse"));
        String shapeAffinity = Files.readString(sourceRoot.resolve(
                "dev/resivore/quickstacknearbycompat/core/ShapeMapTargetAffinity.java"));
        assertFalse(shapeAffinity.contains("isSameItemSameComponents"));
        assertFalse(shapeAffinity.contains("insertIntoExistingStacks"));
        assertFalse(shapeAffinity.contains("insertIntoEmptySlots"));
    }

    @Test
    void csrApiLinkageIsOptionalGatedAndReadOnly() throws Exception {
        Path sourceRoot = projectRoot().resolve("src/main/java");
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path javaFile : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(javaFile, StandardCharsets.UTF_8);
                if (!javaFile.getFileName().toString().equals("CsrReservationApi.java")) {
                    assertFalse(source.contains("dev.resivore.slotreservations"),
                            "Optional CSR API leaked outside its post-gate class: " + javaFile);
                }
            }
        }

        String resolver = Files.readString(sourceRoot.resolve(
                "dev/resivore/quickstacknearbycompat/core/CsrReservationResolver.java"));
        String api = Files.readString(sourceRoot.resolve(
                "dev/resivore/quickstacknearbycompat/core/CsrReservationApi.java"));
        String integration = Files.readString(sourceRoot.resolve(
                "dev/resivore/quickstacknearbycompat/core/CsrQuickStackIntegration.java"));
        assertTrue(resolver.contains("isModLoaded(CSR_MOD_ID)"));
        assertTrue(resolver.contains("catch (LinkageError"));
        assertTrue(api.contains("ContainerSlotReservationsApi.classify(container, slot, incoming)"));
        assertFalse(api.contains("ReservationStore"));
        assertFalse(api.contains("setData"));
        assertFalse(api.contains("setItem"));
        assertTrue(integration.contains("Construct the QSN key only after CSR has confirmed"));
        assertTrue(integration.contains("target.getMaxStackSize(sourceStack)"));

        String serviceMixin = Files.readString(sourceRoot.resolve(
                "dev/resivore/quickstacknearbycompat/mixin/QuickStackServiceMixin.java"));
        assertTrue(serviceMixin.contains("@WrapMethod"));
        assertTrue(serviceMixin.contains("quickStackNearbyCompat$scopeReservationDiscovery"));
        assertTrue(serviceMixin.contains(
                "scanContainer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;"));
        assertTrue(serviceMixin.contains(
                "QuickStackMoveEngine;acceptedTypes(Lnet/minecraft/world/Container;)Ljava/util/Set;"));
        assertTrue(serviceMixin.contains("augmentDiscoveredAcceptedTypes"));
        assertFalse(serviceMixin.contains("CsrQuickStackIntegration.augmentTargets("),
                "CSR affinity must happen before QSN's empty accepted-types prefilter, not after discovery");
        assertFalse(integration.contains("BlockPos"));
        assertFalse(integration.contains("ServerLevel"));
        assertFalse(integration.contains("betweenClosed"));

        String insertionMixin = Files.readString(sourceRoot.resolve(
                "dev/resivore/quickstacknearbycompat/mixin/QuickStackMoveEngineMixin.java"));
        assertTrue(insertionMixin.contains(
                "insertIntoEmptySlots(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/Container;)I"));
        assertTrue(insertionMixin.contains("require = 1"));
        assertTrue(insertionMixin.contains("if (moved.isPresent())"));
    }

    @Test
    void mixinSplitKeepsCommonAndClientHooksExplicit() throws Exception {
        Path resources = projectRoot().resolve("src/main/resources");
        String common = Files.readString(resources.resolve("quick_stack_nearby_compat.mixins.json"));
        String client = Files.readString(resources.resolve("quick_stack_nearby_compat.client.mixins.json"));

        assertTrue(common.contains("\"QuickStackMoveEngineMixin\""));
        assertTrue(common.contains("\"QuickStackServiceMixin\""));
        assertFalse(common.contains("InventoryCompatMixin"));
        assertFalse(Files.exists(projectRoot().resolve(
                "src/main/java/dev/resivore/quickstacknearbycompat/mixin/InventoryCompatMixin.java")));
        assertFalse(common.contains("StackKey"));
        assertFalse(common.contains("RulesScreen"));
        assertTrue(client.contains("\"QuickStackRuleStoreMixin\""));
        assertTrue(client.contains("\"QuickStackRulesScreenMixin\""));
        assertTrue(client.contains("\"QuickStackButtonSlotBridgeMixin\""));
        assertTrue(client.contains("\"InventoryScreenButtonSlotsMixin\""));
        assertFalse(common.contains("QuickStackButtonSlotBridgeMixin"));
        assertFalse(common.contains("InventoryScreenButtonSlotsMixin"));

        String classificationMixin = Files.readString(projectRoot().resolve(
                "src/main/java/dev/resivore/quickstacknearbycompat/mixin/client/InventoryScreenButtonSlotsMixin.java"
        ));
        assertTrue(classificationMixin.contains("@Pseudo"));
        assertTrue(classificationMixin.contains(
                "targets = \"tempeststudios.inventorysort.api.InventoryScreenButtonSlots\""
        ));
        assertTrue(classificationMixin.contains("at = @At(\"RETURN\")"));
        assertTrue(classificationMixin.contains("require = 1"));
        assertTrue(classificationMixin.contains(
                "private static void quickStackNearbyCompat$allowExtendedPlayerInventorySearch"
        ));
        assertTrue(classificationMixin.contains("screen instanceof InventoryScreen"));
        assertFalse(classificationMixin.contains("new InventorySortIconButton"));
        assertFalse(classificationMixin.contains("addRenderableWidget"));
    }

    @Test
    void shelfRuleUsesOnlyTheCommonVanillaTypeAtQsnDiscoveryReturn() throws Exception {
        Path root = projectRoot();
        String exclusions = Files.readString(root.resolve(
                "src/main/java/dev/resivore/quickstacknearbycompat/core/QsnDestinationExclusions.java"));
        String mixin = Files.readString(root.resolve(
                "src/main/java/dev/resivore/quickstacknearbycompat/mixin/QuickStackServiceMixin.java"));

        assertTrue(exclusions.contains("instanceof ShelfBlockEntity"));
        assertFalse(exclusions.contains("Blocks."));
        assertFalse(exclusions.contains("BuiltInRegistries"));
        assertFalse(exclusions.contains("ResourceLocation"));
        assertFalse(exclusions.matches("(?s).*(?:OAK|SPRUCE|BIRCH|JUNGLE|ACACIA|CHERRY|DARK_OAK|PALE_OAK|MANGROVE|BAMBOO|CRIMSON|WARPED)_SHELF.*"));
        assertTrue(mixin.contains("method = \"nearbyTargets(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/List;\""));
        assertTrue(mixin.contains("at = @At(\"RETURN\")"));
        assertTrue(mixin.contains("QsnDestinationExclusions.filterVanillaShelves(targets)"));
    }

    private static Path projectRoot() {
        return Path.of(System.getProperty("projectRoot"));
    }
}
