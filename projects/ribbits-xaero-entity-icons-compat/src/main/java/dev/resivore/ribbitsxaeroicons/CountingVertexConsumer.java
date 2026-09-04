package dev.resivore.ribbitsxaeroicons;
import com.mojang.blaze3d.vertex.VertexConsumer;
/** Counts actual submitted vertices without changing positions, colors or the destination. */
final class CountingVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private int vertices;
    CountingVertexConsumer(VertexConsumer delegate) { this.delegate = delegate; }
    int vertices() { return vertices; }
    @Override public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x,y,z); vertices++; return this;
    }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) { delegate.setColor(r,g,b,a); return this; }
    @Override public VertexConsumer setColor(int color) { delegate.setColor(color); return this; }
    @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u,v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u,v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u,v); return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x,y,z); return this; }
    @Override public VertexConsumer setLineWidth(float width) { delegate.setLineWidth(width); return this; }
}
