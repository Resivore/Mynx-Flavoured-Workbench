package dev.resivore.bgebushyleaves.client;

import dev.resivore.bgebushyleaves.geometry.PatchFoliagePlan;
import dev.resivore.bgebushyleaves.geometry.PatchFrame;
import dev.resivore.bgebushyleaves.geometry.Rect16;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for copying captured FRAPI appearance into a renderer-owned output quad. */
final class PatchLocalFoliageEmissionTest {
    @Test
    void emitsCardsThroughPublicQuadAccessorsInsteadOfRendererBulkCopy() {
        SourceQuad sourceState = SourceQuad.decorative();
        QuadView source = proxy(QuadView.class, new SourceHandler(sourceState));
        List<EmittedQuad> emitted = new ArrayList<>();
        QuadEmitter output = proxy(QuadEmitter.class, new OutputHandler(new EmittedQuad(), emitted));
        PatchFrame frame = new PatchFrame(Direction.UP, 16, Direction.Axis.X,
                new Rect16(0, 16, 0, 16), Direction.Axis.Z, Direction.UP);
        PatchFoliagePlan.Card card = PatchFoliagePlan.plan(frame, 0xBEEFL).getFirst();

        PatchLocalFoliage.emitCard(source, output, frame, card, false);

        assertEquals(1, emitted.size());
        EmittedQuad cardQuad = emitted.getFirst();
        assertEquals(sourceState.atlas, cardQuad.atlas);
        assertEquals(sourceState.chunkLayer, cardQuad.chunkLayer);
        assertTrue(cardQuad.itemRenderTypeCopied);
        assertEquals(sourceState.emissive, cardQuad.emissive);
        assertEquals(sourceState.diffuseShade, cardQuad.diffuseShade);
        assertEquals(sourceState.ambientOcclusion, cardQuad.ambientOcclusion);
        assertTrue(cardQuad.foilTypeCopied);
        assertEquals(sourceState.shadeMode, cardQuad.shadeMode);
        assertEquals(sourceState.animated, cardQuad.animated);
        assertEquals(sourceState.tintIndex, cardQuad.tintIndex);
        assertEquals(sourceState.tag, cardQuad.tag);
        assertEquals(Direction.UP, cardQuad.nominalFace);
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
        List<EmittedQuad> emitted = new ArrayList<>();
        QuadEmitter output = proxy(QuadEmitter.class, new OutputHandler(new EmittedQuad(), emitted));
        PatchFrame frame = new PatchFrame(Direction.UP, 16, Direction.Axis.X,
                new Rect16(0, 16, 0, 16), Direction.Axis.Z, Direction.UP);
        PatchFoliagePlan.Card card = PatchFoliagePlan.plan(frame, 0xBEEFL).getFirst();

        PatchLocalFoliage.emitCard(source, output, frame, card, false);

        assertEquals(1, emitted.size());
        assertEquals(QuadAtlas.BLOCK, emitted.getFirst().atlas);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static final class SourceHandler implements InvocationHandler {
        private final SourceQuad state;
        private final boolean atlasUnavailable;

        private SourceHandler(SourceQuad state) { this(state, false); }

        private SourceHandler(SourceQuad state, boolean atlasUnavailable) {
            this.state = state;
            this.atlasUnavailable = atlasUnavailable;
        }

        @Override public Object invoke(Object proxy, Method method, Object[] arguments) {
            int vertex = arguments != null && arguments.length == 1 && arguments[0] instanceof Integer
                    ? (Integer) arguments[0] : -1;
            return switch (method.getName()) {
                case "u" -> state.u[vertex];
                case "v" -> state.v[vertex];
                case "color" -> state.color[vertex];
                case "lightmap" -> state.lightmap[vertex];
                case "atlas" -> {
                    if (atlasUnavailable) throw new AssertionError("wrapped source atlas must not be read");
                    yield state.atlas;
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
                case "toString" -> "SourceQuad";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                default -> defaultValue(method.getReturnType());
            };
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
                case "itemRenderType": state.itemRenderTypeCopied = true; return proxy;
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
        final QuadAtlas atlas = QuadAtlas.BLOCK;
        final ChunkSectionLayer chunkLayer = ChunkSectionLayer.TRANSLUCENT;
        final boolean emissive = true;
        final boolean diffuseShade = false;
        final TriState ambientOcclusion = TriState.FALSE;
        final ShadeMode shadeMode = ShadeMode.ENHANCED;
        final boolean animated = true;
        final int tintIndex = 7;
        final int tag = 42;

        static SourceQuad decorative() { return new SourceQuad(); }
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
        boolean itemRenderTypeCopied;
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
            copy.itemRenderTypeCopied = itemRenderTypeCopied;
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
