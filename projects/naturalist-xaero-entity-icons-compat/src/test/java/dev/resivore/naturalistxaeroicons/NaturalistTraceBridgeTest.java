package dev.resivore.naturalistxaeroicons;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.minecraft.client.model.geom.ModelPart;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelPartRenderTrace;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

class NaturalistTraceBridgeTest {
    @Test void naturalistAdapterResolvesItsRecordedOriginalTrace() {
        ModelPart original = part(Map.of());
        ModelPart adapter = NaturalistIconAdapter.build(part(Map.of("selected", original)), original, original, original,
                presentation(), false, false, List.of("selected"), true);
        ModelRenderTrace trace = trace();
        trace.addVisibleModelPart(original, 0xFF123456);

        ModelPartRenderTrace resolved = NaturalistIconAdapter.resolveTrace(trace, adapter);
        assertNotNull(resolved);
        assertSame(original, resolved.modelPart);
        assertEquals(0xFF123456, resolved.color);
    }

    @Test void nonNaturalistAndEmfStyleAdaptersRemainUntouched() {
        ModelRenderTrace trace = trace();
        ModelPart ordinary = part(Map.of());
        ModelPart emfStyleAdapter = part(Map.of("adapter", part(Map.of())));
        trace.addVisibleModelPart(ordinary, 0xFF102030);

        assertNull(NaturalistIconAdapter.resolveTrace(trace, ordinary));
        assertSame(ordinary, trace.getModelPartRenderInfo(ordinary).modelPart);
        assertNull(NaturalistIconAdapter.resolveTrace(trace, emfStyleAdapter));
        assertNull(trace.getModelPartRenderInfo(emfStyleAdapter));
    }

    @Test void missingOrUntracedNaturalistMappingsFailClosed() {
        ModelPart original = part(Map.of());
        ModelPart adapter = NaturalistIconAdapter.build(part(Map.of("selected", original)), original, original, original,
                presentation(), false, false, List.of("selected"), true);
        ModelRenderTrace trace = trace();

        assertNull(NaturalistIconAdapter.resolveTrace(trace, adapter));
        assertNull(NaturalistIconAdapter.resolveTrace(trace, part(Map.of())));
    }

    @Test void c21StarfishModelRootResolutionFailsClosedOnTheObsoleteSecondRootHop() throws Exception {
        ModelPart body = part(Map.of());
        ModelPart authoredRoot = part(Map.of("body", body, "legs", part(Map.of())));
        Method follow = NaturalistModelContracts.class.getDeclaredMethod("follow", ModelPart.class, List.class);
        follow.setAccessible(true);

        assertSame(authoredRoot, follow.invoke(null, authoredRoot, List.of()));
        assertSame(body, follow.invoke(null, authoredRoot, List.of("body")));
        assertNull(follow.invoke(null, authoredRoot, List.of("root", "body")));
    }

    @Test void c23UsesTheLiveBodyTraceAsCenterWithoutReducingTheSyntheticAssemblies() {
        ModelPart body = part(Map.of());
        ModelPart legs = part(Map.of());
        ModelPart source = part(Map.of("body", body, "legs", legs));
        ModelPart selectedAssembly = part(Map.of("body", part(Map.of()), "legs", part(Map.of())));
        var contract = NaturalistModelContracts.contractsForId("starfish").getFirst();
        var resolved = new NaturalistModelContracts.ResolvedContract(contract, source, selectedAssembly, body);

        assertTrue(contract.useTraceAsRenderCenter());
        assertNotSame(source, selectedAssembly);
        assertTrue(selectedAssembly.hasChild("body"));
        assertTrue(selectedAssembly.hasChild("legs"));
        assertSame(body, resolved.renderCenter());
        assertNotSame(selectedAssembly, resolved.renderCenter());
        for (String id : List.of("desert_scorpion", "jungle_scorpion")) {
            var scorpion = NaturalistModelContracts.contractsForId(id).getFirst();
            var scorpionResolved = new NaturalistModelContracts.ResolvedContract(scorpion, source, selectedAssembly, body);
            assertEquals(List.of(), scorpion.path(), id);
            assertEquals(List.of("body"), scorpion.tracePath(), id);
            assertTrue(scorpion.useTraceAsRenderCenter(), id);
            assertSame(body, scorpionResolved.renderCenter(), id);
            assertNotSame(selectedAssembly, scorpionResolved.renderCenter(), id);
        }
    }

    @Test void c27UsesTheLiveTopJawCenterWhileKeepingTheCompleteAdultWhaleFaceAssembly() {
        ModelPart topJaw = part(Map.of());
        ModelPart bottomJaw = part(Map.of());
        ModelPart skullRot = part(Map.of("topJaw", topJaw, "bottomJaw", bottomJaw));
        var contract = NaturalistModelContracts.contractsForId("whale").getFirst();
        var resolved = new NaturalistModelContracts.ResolvedContract(contract, skullRot, skullRot, topJaw);

        assertEquals(List.of("body", "skullRot"), contract.path());
        assertEquals(List.of("body", "skullRot", "topJaw"), contract.tracePath());
        assertTrue(contract.useTraceAsRenderCenter());
        assertSame(skullRot, resolved.selected());
        assertTrue(resolved.selected().hasChild("topJaw"));
        assertTrue(resolved.selected().hasChild("bottomJaw"));
        assertSame(topJaw, resolved.renderCenter());
        assertNotSame(resolved.selected(), resolved.renderCenter());
        assertEquals(.30F, contract.presentation().scale());
        assertEquals(1.5708F, contract.presentation().yRotation());

        var baby = NaturalistModelContracts.contractsForId("whale").get(1);
        assertEquals(List.of("body", "skull"), baby.path());
        assertEquals(List.of("body", "skull"), baby.tracePath());
        assertEquals(.60F, baby.presentation().scale());
        assertEquals(.7854F, baby.presentation().yRotation());
    }

    @Test void explicitDescendantTraceCanRenderAnExactRootContract() {
        ModelPart tracedBody = part(Map.of());
        ModelPart originalRoot = part(Map.of("body", tracedBody));
        ModelPart adapter = NaturalistIconAdapter.build(originalRoot, originalRoot, originalRoot, tracedBody,
                presentation(), false, false, List.of(), true);
        ModelRenderTrace trace = trace();
        trace.addVisibleModelPart(tracedBody, 0xFFAABBCC);

        assertNotNull(adapter);
        assertSame(tracedBody, NaturalistIconAdapter.resolveTrace(trace, adapter).modelPart);
    }

    @Test void neutralRootCopyKeepsPositionButDropsOnlyDynamicRotation() {
        ModelPart root = part(Map.of("selected", part(Map.of())));
        root.x = 3.0F; root.y = 5.0F; root.z = 7.0F;
        root.xRot = .25F; root.yRot = .5F; root.zRot = .75F;
        ModelPart selected = root.getChild("selected");
        ModelPart adapter = NaturalistIconAdapter.build(root, selected, selected, selected,
                presentation(), true, false, List.of("selected"), true);
        ModelPart copiedRoot = adapter.getChild("naturalist_contract");

        assertEquals(3.0F, copiedRoot.x);
        assertEquals(5.0F, copiedRoot.y);
        assertEquals(7.0F, copiedRoot.z);
        assertEquals(0.0F, copiedRoot.xRot);
        assertEquals(0.0F, copiedRoot.yRot);
        assertEquals(0.0F, copiedRoot.zRot);
    }

    @Test void detachedContractBuildCopiesOnlyItsExplicitSourceSubtree() {
        ModelPart selected = part(Map.of("feature", part(Map.of())));
        selected.x = 4.0F;
        ModelPart adapter = NaturalistIconAdapter.build(part(Map.of("selected", selected)), selected, selected, selected,
                presentation(), false, false, List.of("selected"), false);

        ModelPart detached = adapter.getChild("naturalist_contract");
        assertNotSame(selected, detached);
        assertEquals(4.0F, detached.x);
        assertNotSame(selected.getChild("feature"), detached.getChild("feature"));
    }

    @Test void adapterFrameOffsetIsLocalToTheCopiedIcon() {
        ModelPart selected = part(Map.of());
        NaturalistModelContracts.Presentation framed =
                new NaturalistModelContracts.Presentation(.21F, 0.0F, 1.5708F, 0.0F, -4.0F);
        ModelPart adapter = NaturalistIconAdapter.build(part(Map.of("selected", selected)), selected, selected, selected,
                framed, false, false, List.of("selected"), false);

        assertEquals(-4.0F, adapter.y);
        assertEquals(0.0F, selected.y);
        assertEquals(.21F, adapter.xScale);
    }

    @Test void normalizedRootAssemblyDropsOnlyTheCopiedGameplayRootFrame() {
        ModelPart selected = part(Map.of("body", part(Map.of())));
        selected.x = 4.0F; selected.y = 24.0F; selected.z = -3.0F;
        selected.xRot = .2F; selected.yRot = .4F; selected.zRot = .6F;
        ModelPart adapter = NaturalistIconAdapter.build(selected, selected, selected, selected,
                presentation(), false, true, List.of(), false);
        ModelPart normalized = adapter.getChild("naturalist_contract");

        assertEquals(0.0F, normalized.x);
        assertEquals(0.0F, normalized.y);
        assertEquals(0.0F, normalized.z);
        assertEquals(0.0F, normalized.xRot);
        assertEquals(1.0F, normalized.xScale);
        assertNotSame(selected.getChild("body"), normalized.getChild("body"));
    }

    @Test void bridgeUsesTheClosedContractPathRatherThanTreeDiscovery() throws Exception {
        Path module = Path.of(System.getProperty("projectRoot"));
        String source = Files.readString(module.resolve("src/main/java/dev/resivore/naturalistxaeroicons/NaturalistIconAdapter.java"));
        assertTrue(source.contains("ancestors(modelRoot, source, sourcePath)"));
        assertFalse(source.contains("private static boolean find("));
    }

    @Test void c21StarfishDiagnosticsObserveXaeroSeamsWithoutCompetingForC9sRedirect() throws Exception {
        Path module = Path.of(System.getProperty("projectRoot"));
        String config = Files.readString(module.resolve("src/main/resources/naturalist_xaero_entity_icons_compat.mixins.json"));
        assertTrue(config.contains("ModelRenderTraceMixin"));
        assertTrue(config.contains("RadarIconCreatorMixin"));
        assertTrue(config.contains("RadarIconModelFormPrerendererMixin"));
        assertTrue(config.contains("RadarIconModelPartPrerendererMixin"));
        assertTrue(config.contains("RadarIconEntityCacheMixin"));
        String manager = Files.readString(module.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/mixin/RadarIconManagerMixin.java"));
        assertTrue(manager.contains("@ModifyVariable"));
        assertTrue(manager.contains("observeStarfishCacheBeforeXaeroEmfRetry"));
        assertFalse(manager.contains("@Redirect"));

        Path c9 = module.getParent().resolve("xaero-entity-icons/src/main/java/dev/resivore/xaeroemfcompat/mixin/RadarIconModelPartPrerendererMixin.java");
        String c9Source = Files.readString(c9);
        assertEquals(1, occurrences(c9Source, "ModelRenderTrace;getModelPartRenderInfo"));
        assertEquals(1, occurrences(c9Source, "xaeroEmfEntityIconCompat$resolveAdapterTrace"));
        assertTrue(c9Source.contains("require = 1"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(needle, index)) >= 0; index += needle.length()) count++;
        return count;
    }

    private static ModelPart part(Map<String, ModelPart> children) {
        return new ModelPart(List.of(), children);
    }

    private static NaturalistModelContracts.Presentation presentation() {
        return new NaturalistModelContracts.Presentation(1.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    private static ModelRenderTrace trace() {
        return new ModelRenderTrace(null, Map.of(), null, false, false, false,
                null, null, null, null, 0xFFFFFFFF);
    }
}
