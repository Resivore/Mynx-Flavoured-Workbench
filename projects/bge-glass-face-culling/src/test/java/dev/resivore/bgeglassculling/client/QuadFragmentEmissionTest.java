package dev.resivore.bgeglassculling.client;

import dev.resivore.bgeglassculling.geometry.Rect16;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QuadFragmentEmissionTest {
    @Test
    void partialCropPreservesWindingUvAndAllCopiedQuadMetadata() {
        QuadState sourceState = QuadState.eastFace();
        MutableQuadView source = proxy(MutableQuadView.class, new QuadHandler(sourceState, null));
        List<QuadState> emitted = new ArrayList<>();
        QuadEmitter output = proxy(QuadEmitter.class,
                new QuadHandler(new QuadState(), emitted));

        GlassQuadClipper.emitWithCullRegions(source, output,
                List.of(new Rect16(0, 8, 0, 16)));

        assertEquals(1, emitted.size());
        QuadState fragment = emitted.getFirst();
        assertEquals(sourceState.orientation(), fragment.orientation());
        assertEquals(List.of(8.0F, 8.0F, 16.0F, 16.0F), floats(fragment.u));
        assertEquals(List.of(0.0F, 16.0F, 16.0F, 0.0F), floats(fragment.v));
        assertEquals(7, fragment.tintIndex);
        assertEquals(42, fragment.tag);
        assertTrue(fragment.emissive && fragment.animated);
        assertEquals(QuadAtlas.BLOCK, fragment.atlas);
        assertEquals(ChunkSectionLayer.TRANSLUCENT, fragment.chunkLayer);
        assertEquals(TriState.FALSE, fragment.ambientOcclusion);
        assertEquals(ShadeMode.ENHANCED, fragment.shadeMode);
        for (int vertex = 0; vertex < 4; vertex++) {
            assertEquals(sourceState.color[vertex], fragment.color[vertex]);
            assertEquals(sourceState.lightmap[vertex], fragment.lightmap[vertex]);
            assertEquals(1.0F, fragment.normalX[vertex]);
            assertEquals(0.0F, fragment.normalY[vertex]);
            assertEquals(0.0F, fragment.normalZ[vertex]);
        }
    }

    @Test
    void completeCropEmitsNoQuad() {
        MutableQuadView source = proxy(MutableQuadView.class,
                new QuadHandler(QuadState.eastFace(), null));
        List<QuadState> emitted = new ArrayList<>();
        QuadEmitter output = proxy(QuadEmitter.class,
                new QuadHandler(new QuadState(), emitted));
        GlassQuadClipper.emitWithCullRegions(source, output,
                List.of(new Rect16(0, 16, 0, 16)));
        assertEquals(List.of(), emitted);
    }

    private static List<Float> floats(float[] values) {
        List<Float> result = new ArrayList<>();
        for (float value : values) result.add(value);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static final class QuadHandler implements InvocationHandler {
        private QuadState state;
        private final List<QuadState> emitted;

        private QuadHandler(QuadState state, List<QuadState> emitted) {
            this.state = state;
            this.emitted = emitted;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            String name = method.getName();
            int vertex = arguments != null && arguments.length > 0 && arguments[0] instanceof Integer
                    ? (Integer) arguments[0] : -1;
            switch (name) {
                case "x": return state.x[vertex];
                case "y": return state.y[vertex];
                case "z": return state.z[vertex];
                case "u": return state.u[vertex];
                case "v": return state.v[vertex];
                case "color":
                    if (arguments.length == 1) return state.color[vertex];
                    state.color[vertex] = (Integer) arguments[1]; return proxy;
                case "lightmap":
                    if (arguments.length == 1) return state.lightmap[vertex];
                    state.lightmap[vertex] = (Integer) arguments[1]; return proxy;
                case "hasNormal": return state.hasNormal[vertex];
                case "normalX": return state.normalX[vertex];
                case "normalY": return state.normalY[vertex];
                case "normalZ": return state.normalZ[vertex];
                case "lightFace": return state.lightFace;
                case "nominalFace": return state.nominalFace;
                case "cullFace": return state.cullFace;
                case "atlas": return state.atlas;
                case "chunkLayer": return state.chunkLayer;
                case "emissive": return state.emissive;
                case "diffuseShade": return state.diffuseShade;
                case "ambientOcclusion": return state.ambientOcclusion;
                case "shadeMode": return state.shadeMode;
                case "animated": return state.animated;
                case "tintIndex": return state.tintIndex;
                case "tag": return state.tag;
                case "copyFrom":
                    state = ((QuadHandler) Proxy.getInvocationHandler(arguments[0])).state.copy();
                    return proxy;
                case "pos":
                    state.x[vertex] = (Float) arguments[1];
                    state.y[vertex] = (Float) arguments[2];
                    state.z[vertex] = (Float) arguments[3];
                    return proxy;
                case "uv":
                    state.u[vertex] = (Float) arguments[1];
                    state.v[vertex] = (Float) arguments[2];
                    return proxy;
                case "normal":
                    state.hasNormal[vertex] = true;
                    state.normalX[vertex] = (Float) arguments[1];
                    state.normalY[vertex] = (Float) arguments[2];
                    state.normalZ[vertex] = (Float) arguments[3];
                    return proxy;
                case "emit":
                    emitted.add(state.copy());
                    return proxy;
                case "pushTransform", "popTransform": return null;
                case "toString": return "QuadProxy";
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == arguments[0];
                default:
                    if (method.getReturnType().isInstance(proxy)) return proxy;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == float.class) return 0.0F;
                    return null;
            }
        }
    }

    private static final class QuadState {
        final float[] x = new float[4];
        final float[] y = new float[4];
        final float[] z = new float[4];
        final float[] u = new float[4];
        final float[] v = new float[4];
        final int[] color = new int[4];
        final int[] lightmap = new int[4];
        final boolean[] hasNormal = new boolean[4];
        final float[] normalX = new float[4];
        final float[] normalY = new float[4];
        final float[] normalZ = new float[4];
        Direction lightFace;
        Direction nominalFace;
        Direction cullFace;
        QuadAtlas atlas;
        ChunkSectionLayer chunkLayer;
        boolean emissive;
        boolean diffuseShade;
        TriState ambientOcclusion;
        ShadeMode shadeMode;
        boolean animated;
        int tintIndex;
        int tag;

        static QuadState eastFace() {
            QuadState state = new QuadState();
            state.x[0] = state.x[1] = state.x[2] = state.x[3] = 1.0F;
            state.y[0] = state.y[1] = 0.0F;
            state.y[2] = state.y[3] = 1.0F;
            state.z[0] = state.z[3] = 0.0F;
            state.z[1] = state.z[2] = 1.0F;
            state.u[0] = state.u[1] = 0.0F;
            state.u[2] = state.u[3] = 16.0F;
            state.v[0] = state.v[3] = 0.0F;
            state.v[1] = state.v[2] = 16.0F;
            for (int vertex = 0; vertex < 4; vertex++) {
                state.color[vertex] = 0xffffffff;
                state.lightmap[vertex] = 0x00100010;
                state.hasNormal[vertex] = true;
                state.normalX[vertex] = 1.0F;
            }
            state.lightFace = Direction.EAST;
            state.nominalFace = Direction.EAST;
            state.cullFace = Direction.EAST;
            state.atlas = QuadAtlas.BLOCK;
            state.chunkLayer = ChunkSectionLayer.TRANSLUCENT;
            state.emissive = true;
            state.diffuseShade = false;
            state.ambientOcclusion = TriState.FALSE;
            state.shadeMode = ShadeMode.ENHANCED;
            state.animated = true;
            state.tintIndex = 7;
            state.tag = 42;
            return state;
        }

        QuadState copy() {
            QuadState copy = new QuadState();
            System.arraycopy(x, 0, copy.x, 0, 4);
            System.arraycopy(y, 0, copy.y, 0, 4);
            System.arraycopy(z, 0, copy.z, 0, 4);
            System.arraycopy(u, 0, copy.u, 0, 4);
            System.arraycopy(v, 0, copy.v, 0, 4);
            System.arraycopy(color, 0, copy.color, 0, 4);
            System.arraycopy(lightmap, 0, copy.lightmap, 0, 4);
            System.arraycopy(hasNormal, 0, copy.hasNormal, 0, 4);
            System.arraycopy(normalX, 0, copy.normalX, 0, 4);
            System.arraycopy(normalY, 0, copy.normalY, 0, 4);
            System.arraycopy(normalZ, 0, copy.normalZ, 0, 4);
            copy.lightFace = lightFace;
            copy.nominalFace = nominalFace;
            copy.cullFace = cullFace;
            copy.atlas = atlas;
            copy.chunkLayer = chunkLayer;
            copy.emissive = emissive;
            copy.diffuseShade = diffuseShade;
            copy.ambientOcclusion = ambientOcclusion;
            copy.shadeMode = shadeMode;
            copy.animated = animated;
            copy.tintIndex = tintIndex;
            copy.tag = tag;
            return copy;
        }

        float orientation() {
            float signed = 0.0F;
            for (int vertex = 0; vertex < 4; vertex++) {
                int next = (vertex + 1) % 4;
                signed += y[vertex] * z[next] - y[next] * z[vertex];
            }
            return Math.signum(signed);
        }
    }
}
