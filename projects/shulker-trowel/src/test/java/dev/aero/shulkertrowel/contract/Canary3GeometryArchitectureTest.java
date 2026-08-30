package dev.aero.shulkertrowel.contract;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Canary3GeometryArchitectureTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void resolverUsesOnlyExactAcceptedOwnershipAndTypedRoles() throws IOException {
        String resolver = source("geometry/CnmNibaruGeometryResolver.java");
        String catalog = source("geometry/TargetGeometry.java");
        String placement = source("item/ShulkerTrowelItem.java");

        assertTrue(resolver.contains("NibaruProviderAdapter.profile(sourceBlock)"));
        assertTrue(resolver.contains("NibaruProviderAdapter.runtimeBinding(sourceBlock)"));
        assertTrue(resolver.contains("TargetGeometry.NativeRole.FULL"));
        assertTrue(resolver.contains("effectiveSlabSource()"));
        assertTrue(resolver.contains("effectiveStairSource()"));
        assertTrue(resolver.contains("nativeWall()"));
        assertTrue(catalog.contains("Geometry.VERTICAL_SLAB"));
        assertTrue(catalog.contains("Geometry.STEP"));
        assertTrue(catalog.contains("Geometry.LAYER"));
        assertTrue(resolver.contains("targetGeometry.derivedGeometry()"));
        assertTrue(catalog.contains(
                "LAYER(6, DerivedGeometrySupport.Geometry.LAYER)"));
        assertTrue(resolver.contains("blockItem.getBlock() == block"));
        assertFalse(resolver.contains("ShapeMap."));
        assertFalse(resolver.contains("import dev.tazer.clutternomore.common.shape_map.ShapeMap"));
        assertFalse(resolver.contains("BuiltInRegistries"));
        assertFalse(resolver.contains("getPath()"));
        assertFalse(resolver.contains("endsWith("));
        assertFalse(placement.contains("ShapeMap"));
    }

    @Test
    void cnmOwnsTheOnlyKeyAndOverlayWhileTrowelOwnsThePayload() throws IOException {
        String input = source("client/TrowelShapeInput.java");
        String overlay = source("client/TrowelShapeSwitcherOverlay.java");
        String inputMixin = source("mixin/client/ClutterNoMoreClientInputMixin.java");
        String renderMixin = source("mixin/client/ShapeSwitcherOverlayRenderMixin.java");

        assertTrue(input.contains("ClutterNoMoreClient.shapeKey()"));
        assertTrue(input.contains("held.is(ModItems.TROWEL)"));
        assertTrue(input.contains("ClientPlayNetworking.canSend(ChangeTrowelGeometryPayload.TYPE)"));
        assertFalse(input.contains("KeyMapping"));
        assertFalse(input.contains("KeyBindingHelper"));
        assertTrue(inputMixin.contains("at = @At(\"HEAD\")"));
        assertTrue(inputMixin.contains("if (TrowelShapeInput.tryHandle(key, action)) ci.cancel();"));

        assertTrue(overlay.contains("extends ShapeSwitcherOverlay"));
        assertTrue(overlay.contains("new ChangeTrowelGeometryPayload(selectedMode.networkId())"));
        assertTrue(overlay.contains("initialMode.selectorIndex()"));
        assertTrue(overlay.contains("TargetGeometry.fromSelectorIndex(selectedIndex)"));
        assertTrue(overlay.contains("TrowelClientModeCache.displayed"));
        assertTrue(overlay.contains("TrowelClientModeCache.record"));
        assertFalse(overlay.contains("setItemInHand"));
        assertFalse(overlay.contains("ChangeStackPayload"));
        assertTrue(renderMixin.contains("instanceof TrowelShapeSwitcherOverlay"));
        assertTrue(renderMixin.contains("return ShapeMap.transferStack(held, geometryId);"));
    }

    @Test
    void modeMutationIsPersistentValidatedAndServerAuthoritative() throws IOException {
        String state = source("geometry/TrowelGeometryState.java");
        String authority = source("network/TrowelGeometryAuthority.java");
        String common = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/aero/shulkertrowel/ShulkerTrowel.java"
        ));
        String clientTree = readTree(PROJECT_ROOT.resolve("src/main/java/dev/aero/shulkertrowel/client"));

        assertTrue(state.contains("DataComponents.CUSTOM_DATA"));
        assertTrue(state.contains("CustomData.update"));
        assertTrue(authority.contains("TargetGeometry.fromNetworkId"));
        assertTrue(authority.contains("player.isAlive()"));
        assertTrue(authority.contains("mainHand.is(ModItems.TROWEL)"));
        assertTrue(authority.contains("inventoryMenu.broadcastChanges()"));
        assertTrue(common.contains("PayloadTypeRegistry.serverboundPlay().register"));
        assertTrue(common.contains("ServerPlayNetworking.registerGlobalReceiver"));
        assertFalse(clientTree.contains("TrowelGeometryState.set"));
    }

    @Test
    void unsupportedPaletteReturnsBeforePlacementOrSoundScope() throws IOException {
        String source = source("item/ShulkerTrowelItem.java");
        int noCandidate = source.indexOf("if (selected == null) return InteractionResult.FAIL;");
        int placementStack = source.indexOf("new ItemStack(selected.placementItem(), 1)");
        int soundScope = source.indexOf("PlacementSoundBroadcastScope.open()");

        assertTrue(noCandidate >= 0 && noCandidate < placementStack && placementStack < soundScope);
    }

    @Test
    void metadataRetainsTheProvenMinimumWithoutRejectingLaterCompatibleBgeReleases() throws Exception {
        JsonObject metadata = JsonParser.parseString(Files.readString(
                PROJECT_ROOT.resolve("src/main/resources/fabric.mod.json")
        )).getAsJsonObject();
        JsonObject depends = metadata.getAsJsonObject("depends");

        assertEquals("=2.0.7+26.2", depends.get("clutternomore").getAsString());
        String nibaruRange = depends.get("more_slabs_stairs_and_walls").getAsString();
        assertEquals(">=4.2.0 <4.3.0-", nibaruRange);
        String bgeRange = depends.get("cnm_terrain_slabs_compat").getAsString();
        assertEquals(">=0.6.1-bge-canary54-layer-economy", bgeRange);

        VersionPredicate nibaruPredicate = VersionPredicate.parse(nibaruRange);
        assertTrue(nibaruPredicate.test(Version.parse(
                "4.2.0+26.2-port-canary46-bge-layer-contract"
        )));

        VersionPredicate bgePredicate = VersionPredicate.parse(bgeRange);
        assertTrue(bgePredicate.test(Version.parse("0.6.1-bge-canary54-layer-economy")));
        assertTrue(bgePredicate.test(Version.parse("0.7.0")));
        assertFalse(bgePredicate.test(Version.parse("0.6.0-bge-canary53-layer")));
        assertTrue(bgePredicate.getInterval().getMax() == null);
    }

    @Test
    void candidateBuildAndGameTestsUseExactCurrentC54AndC46Fixtures() throws IOException {
        String build = Files.readString(PROJECT_ROOT.resolve("build.gradle"));
        JsonObject runtimeDepends = JsonParser.parseString(Files.readString(
                PROJECT_ROOT.resolve("src/gametest/resources/fabric.mod.json")
        )).getAsJsonObject().getAsJsonObject("depends");

        assertTrue(build.contains(
                "more-slabs-stairs-and-walls-4.2.0+26.2-port-canary46-bge-layer-contract.jar"
        ));
        assertTrue(build.contains(
                "cnm-nibaru-integration-0.6.1-bge-canary54-layer-economy.jar"
        ));
        assertFalse(build.contains("canary43-native-directional-material-axis.jar"));
        assertFalse(build.contains("0.5.49-nibaru-cnm-canary1.39-native-directional-material-axis.jar"));
        assertEquals("=0.1.0-canary6", runtimeDepends.get("shulker_trowel").getAsString());
        assertEquals(
                "=0.6.1-bge-canary54-layer-economy",
                runtimeDepends.get("cnm_terrain_slabs_compat").getAsString()
        );
        assertEquals(
                "=4.2.0+26.2-port-canary46-bge-layer-contract",
                runtimeDepends.get("more_slabs_stairs_and_walls").getAsString()
        );
    }

    @Test
    void exactCnmBinaryStillExposesTheNarrowSwitcherSeams() throws IOException {
        Path cnmJar = Path.of(System.getProperty(
                "cnmJar",
                PROJECT_ROOT.resolve("../../originals/mods/clutternomore-2.0.7+26.2-fabric.jar")
                        .normalize()
                        .toString()
        ));
        try (JarFile jar = new JarFile(cnmJar.toFile())) {
            ClassNode client = classNode(jar, "dev/tazer/clutternomore/ClutterNoMoreClient.class");
            MethodNode onKeyInput = method(client, "onKeyInput", "(II)V");
            assertTrue((onKeyInput.access & Opcodes.ACC_STATIC) != 0,
                    "CNM shape-key entry point is no longer static");

            ClassNode overlay = classNode(
                    jar,
                    "dev/tazer/clutternomore/client/ShapeSwitcherOverlay.class"
            );
            assertEquals(0, overlay.access & Opcodes.ACC_FINAL,
                    "CNM overlay can no longer be narrowly subclassed");
            assertTrue(overlay.fields.stream().anyMatch(field ->
                            field.name.equals("shapes") && field.desc.equals("Ljava/util/List;")
                                    && (field.access & Opcodes.ACC_FINAL) != 0),
                    "CNM overlay shapes field seam changed");
            method(overlay, "changeSlot", "(I)V");
            method(overlay, "shouldStayOpenThisTick", "()Z");

            MethodNode render = method(
                    overlay,
                    "render",
                    "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"
            );
            long renderTransfers = StreamSupport.stream(render.instructions.spliterator(), false)
                    .filter(instruction -> instruction instanceof MethodInsnNode call
                            && call.getOpcode() == Opcodes.INVOKESTATIC
                            && call.owner.equals("dev/tazer/clutternomore/common/shape_map/ShapeMap")
                            && call.name.equals("transferStack")
                            && call.desc.equals("(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;"))
                    .count();
            assertEquals(2, renderTransfers,
                    "CNM overlay render transfer seam changed");
        }
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve("src/main/java/dev/aero/shulkertrowel").resolve(relativePath))
                .replace("\r\n", "\n");
    }

    private static String readTree(Path root) throws IOException {
        StringBuilder source = new StringBuilder();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                source.append(Files.readString(file)).append('\n');
            }
        }
        return source.toString();
    }

    private static ClassNode classNode(JarFile jar, String entryName) throws IOException {
        var entry = jar.getJarEntry(entryName);
        assertTrue(entry != null, "Missing CNM class " + entryName);
        ClassNode node = new ClassNode();
        try (var input = jar.getInputStream(entry)) {
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return node;
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream()
                .filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing method " + owner.name + "." + name + descriptor));
    }
}
