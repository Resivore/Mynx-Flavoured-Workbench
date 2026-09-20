package dev.resivore.bgebushyleaves.client;

import dev.resivore.bgebushyleaves.geometry.BlockSpaceFoliagePlan;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for capturing and emitting a renderer-neutral owned appearance. */
final class PatchLocalFoliageEmissionTest {
    @Test
    void emitsCardsThroughPublicQuadAccessorsInsteadOfRendererBulkCopy() {
        SourceQuad sourceState = SourceQuad.decorative();
        QuadView source = proxy(QuadView.class, new SourceHandler(sourceState));
        CanonicalFoliageAppearance.Snapshot snapshot = CanonicalFoliageAppearance.Snapshot.capture(source);
        List<EmittedQuad> emitted = new ArrayList<>();
        QuadEmitter output = proxy(QuadEmitter.class, new OutputHandler(new EmittedQuad(), emitted));
        BlockSpaceFoliage.emitCard(snapshot, output, card(), false);

        assertEquals(1, emitted.size());
        EmittedQuad cardQuad = emitted.getFirst();
        assertEquals(QuadAtlas.BLOCK, cardQuad.atlas);
        assertEquals(sourceState.chunkLayer, cardQuad.chunkLayer);
        assertEquals(sourceState.emissive, cardQuad.emissive);
        assertEquals(sourceState.diffuseShade, cardQuad.diffuseShade);
        assertEquals(sourceState.ambientOcclusion, cardQuad.ambientOcclusion);
        assertTrue(cardQuad.foilTypeCopied);
        assertEquals(sourceState.shadeMode, cardQuad.shadeMode);
        assertEquals(sourceState.animated, cardQuad.animated);
        assertEquals(sourceState.tintIndex, cardQuad.tintIndex);
        assertEquals(sourceState.tag, cardQuad.tag);
        assertEquals(Direction.SOUTH, cardQuad.nominalFace);
        assertNull(cardQuad.cullFace);
        for (int vertex = 0; vertex < 4; vertex++) {
            assertEquals(sourceState.u[vertex], cardQuad.u[vertex]);
            assertEquals(sourceState.v[vertex], cardQuad.v[vertex]);
            assertEquals(sourceState.color[vertex], cardQuad.color[vertex]);
            assertEquals(sourceState.lightmap[vertex], cardQuad.lightmap[vertex]);
            assertTrue(cardQuad.hasNormal[vertex]);
        }
        assertFalse(cardQuad.bulkCopyAttempted);
    }

    @Test
    void emitsBlockAtlasWhenWrappedSourceHasNoAtlasBackingData() {
        SourceQuad sourceState = SourceQuad.decorative();
        QuadView source = proxy(QuadView.class, new SourceHandler(sourceState, true));
        CanonicalFoliageAppearance.Snapshot snapshot = CanonicalFoliageAppearance.Snapshot.capture(source);
        List<EmittedQuad> emitted = new ArrayList<>();
        QuadEmitter output = proxy(QuadEmitter.class, new OutputHandler(new EmittedQuad(), emitted));
        BlockSpaceFoliage.emitCard(snapshot, output, card(), false);

        assertEquals(1, emitted.size());
        assertEquals(QuadAtlas.BLOCK, emitted.getFirst().atlas);
    }

    @Test
    void capturesCompleteAppearanceDuringIterationThenEmitsAfterEverySourceViewExpires() {
        SourceQuad ordinaryState = SourceQuad.ordinary();
        SourceQuad decorativeState = SourceQuad.decorative();
        SourceHandler ordinaryHandler = new SourceHandler(ordinaryState);
        SourceHandler decorativeHandler = new SourceHandler(decorativeState);
        QuadView ordinary = proxy(QuadView.class, ordinaryHandler);
        QuadView decorative = proxy(QuadView.class, decorativeHandler);
        MeshView mesh = new ExpiringMesh(List.of(new ExpiringQuad(ordinary, ordinaryHandler),
                new ExpiringQuad(decorative, decorativeHandler)));

        CanonicalFoliageAppearance appearance = CanonicalFoliageAppearance.sample(mesh).orElseThrow();
        CanonicalFoliageAppearance.Snapshot selected = appearance.choose(0L);
        assertTrue(ordinaryHandler.invalidated);
        assertTrue(decorativeHandler.invalidated);
        // The non-cull candidate remains preferred after both renderer callback lifetimes end.
        assertEquals(decorativeState.tintIndex, selected.tintIndex());

        List<EmittedQuad> emitted = new ArrayList<>();
        QuadEmitter output = proxy(QuadEmitter.class, new OutputHandler(new EmittedQuad(), emitted));
        BlockSpaceFoliage.emitCard(selected, output, card(), false);

        assertEquals(1, emitted.size());
        EmittedQuad cardQuad = emitted.getFirst();
        assertEquals(QuadAtlas.BLOCK, cardQuad.atlas);
        assertEquals(decorativeState.chunkLayer, cardQuad.chunkLayer);
        assertEquals(decorativeState.emissive, cardQuad.emissive);
        assertEquals(decorativeState.diffuseShade, cardQuad.diffuseShade);
        assertEquals(decorativeState.ambientOcclusion, cardQuad.ambientOcclusion);
        assertEquals(decorativeState.shadeMode, cardQuad.shadeMode);
        assertEquals(decorativeState.animated, cardQuad.animated);
        assertEquals(decorativeState.tintIndex, cardQuad.tintIndex);
        assertEquals(decorativeState.tag, cardQuad.tag);
        assertEquals(0, ordinaryHandler.accessesAfterInvalidation);
        assertEquals(0, decorativeHandler.accessesAfterInvalidation);
        assertFalse(Arrays.stream(CanonicalFoliageAppearance.class.getDeclaredFields())
                .anyMatch(field -> QuadView.class.isAssignableFrom(field.getType())
                        || field.getGenericType().getTypeName().contains(QuadView.class.getName())));
        assertFalse(Arrays.stream(CanonicalFoliageAppearance.Snapshot.class.getDeclaredFields())
                .anyMatch(field -> QuadView.class.isAssignableFrom(field.getType())
                        || field.getGenericType().getTypeName().contains(QuadView.class.getName())));
    }

    @Test
    void decorativeTextureInheritsOnlySampledCanonicalTintAndLeavesUntintedMeshesUntinted() {
        SourceHandler tintedShell = new SourceHandler(new SourceQuad(0, Direction.NORTH));
        SourceHandler untintedDecoration = new SourceHandler(new SourceQuad(-1, null));
        CanonicalFoliageAppearance tinted = CanonicalFoliageAppearance.sample(new ExpiringMesh(List.of(
                new ExpiringQuad(proxy(QuadView.class, tintedShell), tintedShell),
                new ExpiringQuad(proxy(QuadView.class, untintedDecoration), untintedDecoration)))).orElseThrow();
        assertEquals(0, tinted.choose(0L).tintIndex());
        List<EmittedQuad> emitted = new ArrayList<>();
        BlockSpaceFoliage.emitCard(tinted.choose(0L), proxy(QuadEmitter.class,
                new OutputHandler(new EmittedQuad(), emitted)), card(), false);
        assertEquals(0, emitted.getFirst().tintIndex);

        SourceHandler untintedShell = new SourceHandler(new SourceQuad(-1, Direction.NORTH));
        SourceHandler anotherUntintedDecoration = new SourceHandler(new SourceQuad(-1, null));
        CanonicalFoliageAppearance untinted = CanonicalFoliageAppearance.sample(new ExpiringMesh(List.of(
                new ExpiringQuad(proxy(QuadView.class, untintedShell), untintedShell),
                new ExpiringQuad(proxy(QuadView.class, anotherUntintedDecoration), anotherUntintedDecoration)))).orElseThrow();
        assertEquals(-1, untinted.choose(0L).tintIndex());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static BlockSpaceFoliagePlan.Card card() {
        return new BlockSpaceFoliagePlan.Card(0, 16, new BlockSpaceFoliagePlan.Vertex[] {
                new BlockSpaceFoliagePlan.Vertex(0, 0, 0), new BlockSpaceFoliagePlan.Vertex(16, 0, 0),
                new BlockSpaceFoliagePlan.Vertex(16, 16, 0), new BlockSpaceFoliagePlan.Vertex(0, 16, 0)});
    }

    private static final class SourceHandler implements InvocationHandler {
        private final SourceQuad state;
        private final boolean atlasUnavailable;
        private boolean invalidated;
        private int accessesAfterInvalidation;

        private SourceHandler(SourceQuad state) { this(state, false); }

        private SourceHandler(SourceQuad state, boolean atlasUnavailable) {
            this.state = state;
            this.atlasUnavailable = atlasUnavailable;
        }

        private void invalidate() { invalidated = true; }

        @Override public Object invoke(Object proxy, Method method, Object[] arguments) {
            String name = method.getName();
            if (!name.equals("toString") && !name.equals("hashCode") && !name.equals("equals") && invalidated) {
                accessesAfterInvalidation++;
                throw new AssertionError("source QuadView escaped its mesh callback: " + name);
            }
            int vertex = arguments != null && arguments.length == 1 && arguments[0] instanceof Integer
                    ? (Integer) arguments[0] : -1;
            return switch (name) {
                case "u" -> state.u[vertex];
                case "v" -> state.v[vertex];
                case "color" -> state.color[vertex];
                case "lightmap" -> state.lightmap[vertex];
                case "atlas" -> {
                    if (atlasUnavailable) throw new AssertionError("wrapped source atlas must not be read");
                    yield QuadAtlas.ITEM;
                }
                case "chunkLayer" -> state.chunkLayer;
                case "itemRenderType", "foilType" -> null;
                case "emissive" -> state.emissive;
                case "diffuseShade" -> state.diffuseShade;
                case "ambientOcclusion" -> state.ambientOcclusion;
                case "shadeMode" -> state.shadeMode;
                case "animated" -> state.animated;
                case "tintIndex" -> state.tintIndex;
                case "tag" -> state.tag;
                case "cullFace" -> state.cullFace;
                case "toString" -> "SourceQuad";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private record ExpiringQuad(QuadView quad, SourceHandler handler) {}

    private static final class ExpiringMesh implements MeshView {
        private final List<ExpiringQuad> quads;

        private ExpiringMesh(List<ExpiringQuad> quads) { this.quads = List.copyOf(quads); }

        @Override public int size() { return quads.size(); }

        @Override public void forEach(Consumer<? super QuadView> consumer) {
            for (ExpiringQuad quad : quads) {
                consumer.accept(quad.quad());
                quad.handler().invalidate();
            }
        }

        @Override public void outputTo(QuadEmitter emitter) {
            throw new AssertionError("appearance capture must use MeshView.forEach");
        }
    }

    private static final class OutputHandler implements InvocationHandler {
        private EmittedQuad state;
        private final List<EmittedQuad> emitted;

        private OutputHandler(EmittedQuad state, List<EmittedQuad> emitted) {
            this.state = state;
            this.emitted = emitted;
        }

        @Override public Object invoke(Object proxy, Method method, Object[] arguments) {
            String name = method.getName();
            int vertex = arguments != null && arguments.length > 0 && arguments[0] instanceof Integer
                    ? (Integer) arguments[0] : -1;
            switch (name) {
                case "copyFrom":
                    state.bulkCopyAttempted = true;
                    throw new AssertionError("renderer-specific bulk copy must not be used for captured quads");
                case "pos":
                    state.x[vertex] = (Float) arguments[1];
                    state.y[vertex] = (Float) arguments[2];
                    state.z[vertex] = (Float) arguments[3];
                    return proxy;
                case "uv":
                    state.u[vertex] = (Float) arguments[1];
                    state.v[vertex] = (Float) arguments[2];
                    return proxy;
                case "color":
                    state.color[vertex] = (Integer) arguments[1];
                    return proxy;
                case "lightmap":
                    state.lightmap[vertex] = (Integer) arguments[1];
                    return proxy;
                case "normal":
                    state.hasNormal[vertex] = true;
                    return proxy;
                case "atlas": state.atlas = (QuadAtlas) arguments[0]; return proxy;
                case "chunkLayer": state.chunkLayer = (ChunkSectionLayer) arguments[0]; return proxy;
                case "emissive": state.emissive = (Boolean) arguments[0]; return proxy;
                case "diffuseShade": state.diffuseShade = (Boolean) arguments[0]; return proxy;
                case "ambientOcclusion": state.ambientOcclusion = (TriState) arguments[0]; return proxy;
                case "foilType": state.foilTypeCopied = true; return proxy;
                case "shadeMode": state.shadeMode = (ShadeMode) arguments[0]; return proxy;
                case "animated": state.animated = (Boolean) arguments[0]; return proxy;
                case "tintIndex": state.tintIndex = (Integer) arguments[0]; return proxy;
                case "tag": state.tag = (Integer) arguments[0]; return proxy;
                case "nominalFace": state.nominalFace = (Direction) arguments[0]; return proxy;
                case "cullFace": state.cullFace = (Direction) arguments[0]; return proxy;
                case "emit": emitted.add(state.copy()); return null;
                case "toString": return "OutputQuad";
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == arguments[0];
                default: return defaultValue(method.getReturnType());
            }
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == float.class) return 0.0F;
        return null;
    }

    private static final class SourceQuad {
        final float[] u = {0.0F, 0.0F, 16.0F, 16.0F};
        final float[] v = {0.0F, 16.0F, 16.0F, 0.0F};
        final int[] color = {0xFFABCDEF, 0xFF012345, 0xFF56789A, 0xFFFEDCBA};
        final int[] lightmap = {1, 2, 3, 4};
        final ChunkSectionLayer chunkLayer = ChunkSectionLayer.TRANSLUCENT;
        final boolean emissive = true;
        final boolean diffuseShade = false;
        final TriState ambientOcclusion = TriState.FALSE;
        final ShadeMode shadeMode = ShadeMode.ENHANCED;
        final boolean animated = true;
        final int tintIndex;
        final int tag = 42;
        final Direction cullFace;

        private SourceQuad(int tintIndex, Direction cullFace) {
            this.tintIndex = tintIndex;
            this.cullFace = cullFace;
        }

        static SourceQuad decorative() { return new SourceQuad(7, null); }
        static SourceQuad ordinary() { return new SourceQuad(2, Direction.NORTH); }
    }

    private static final class EmittedQuad {
        final float[] x = new float[4];
        final float[] y = new float[4];
        final float[] z = new float[4];
        final float[] u = new float[4];
        final float[] v = new float[4];
        final int[] color = new int[4];
        final int[] lightmap = new int[4];
        final boolean[] hasNormal = new boolean[4];
        QuadAtlas atlas;
        ChunkSectionLayer chunkLayer;
        boolean emissive;
        boolean diffuseShade;
        TriState ambientOcclusion;
        boolean foilTypeCopied;
        ShadeMode shadeMode;
        boolean animated;
        int tintIndex;
        int tag;
        Direction nominalFace;
        Direction cullFace;
        boolean bulkCopyAttempted;

        EmittedQuad copy() {
            EmittedQuad copy = new EmittedQuad();
            System.arraycopy(x, 0, copy.x, 0, 4);
            System.arraycopy(y, 0, copy.y, 0, 4);
            System.arraycopy(z, 0, copy.z, 0, 4);
            System.arraycopy(u, 0, copy.u, 0, 4);
            System.arraycopy(v, 0, copy.v, 0, 4);
            System.arraycopy(color, 0, copy.color, 0, 4);
            System.arraycopy(lightmap, 0, copy.lightmap, 0, 4);
            System.arraycopy(hasNormal, 0, copy.hasNormal, 0, 4);
            copy.atlas = atlas;
            copy.chunkLayer = chunkLayer;
            copy.emissive = emissive;
            copy.diffuseShade = diffuseShade;
            copy.ambientOcclusion = ambientOcclusion;
            copy.foilTypeCopied = foilTypeCopied;
            copy.shadeMode = shadeMode;
            copy.animated = animated;
            copy.tintIndex = tintIndex;
            copy.tag = tag;
            copy.nominalFace = nominalFace;
            copy.cullFace = cullFace;
            copy.bulkCopyAttempted = bulkCopyAttempted;
            return copy;
        }
    }
}
