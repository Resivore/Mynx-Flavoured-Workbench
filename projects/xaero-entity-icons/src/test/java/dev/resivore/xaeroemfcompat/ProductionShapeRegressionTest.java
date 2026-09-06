package dev.resivore.xaeroemfcompat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.ModelPartUtil;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.cubeCount;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.cubePart;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.cubePaths;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.emptyPart;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.traced;
import static dev.resivore.xaeroemfcompat.RelocatedHeadFailureMechanismTest.transformedEmpty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Project-owned approximations of the production C4 rejection shapes.  These
 * deliberately contain no copied resource-pack geometry.
 */
class ProductionShapeRegressionTest {

    @Test
    void directCanonicalGeometryCasesAreDetachedAndSubmitVertices() {
        for (String entity : List.of("wolf", "bat", "parrot", "witch", "ravager")) {
            ModelPart vanillaRoot = SheepModel.createBodyLayer().bakeRoot();
            ModelPart vanillaHead = vanillaRoot.getChild("head");
            ModelPart canonical = new ModelPart(
                    List.copyOf(ModelPartUtil.getCubes(vanillaHead)),
                    Map.of("nose", cubePart(Map.of("hat", cubePart(Map.of()))),
                            "body", cubePart(Map.of()), "wing", cubePart(Map.of())));
            canonical.setInitialPose(canonical.storePose());
            ModelPart root = entity.equals("ravager")
                    ? emptyPart(Map.of("neck", emptyPart(Map.of("head", canonical))))
                    : emptyPart(Map.of("head", canonical));
            ModelPart retainedRoot = entity.equals("ravager")
                    ? emptyPart(Map.of("neck", emptyPart(Map.of("head", vanillaHead))))
                    : vanillaRoot;

            var result = EmfIconPartResolver.resolveRelocatedHead(
                    root, canonical, retainedRoot, traced(canonical, 0xFF102030), true).orElseThrow();
            assertSame(canonical, result.tracedHead(), entity);
            assertFalse(cubePaths(result.renderAdapter()).stream()
                    .anyMatch(path -> path.contains("body") || path.contains("wing")), entity);
            assertTrue(submittedVertices(result.renderAdapter()) > 0, entity);
        }
    }

    @Test
    void c4KnownGoodRelocatedShapesRetainTheirHeadOnlyBoundary() {
        for (String entity : List.of("axolotl", "sniffer")) {
            ModelPart vanillaRoot = SheepModel.createBodyLayer().bakeRoot();
            ModelPart canonical = transformedEmpty(vanillaRoot.getChild("head"), Map.of());
            ModelPart relocatedHead = cubePart(Map.of());
            ModelPart root = emptyPart(Map.of(
                    "head", canonical,
                    "body", emptyPart(Map.of("EMF_head2", relocatedHead, "EMF_body", cubePart(Map.of())))));

            var result = EmfIconPartResolver.resolveRelocatedHead(
                    root, canonical, vanillaRoot, traced(relocatedHead, 0xFF708090), true).orElseThrow();
            assertSame(relocatedHead, result.geometryRoot(), entity);
            assertFalse(cubePaths(result.renderAdapter()).stream()
                    .anyMatch(path -> path.contains("EMF_body")), entity);
        }
    }

    private static int submittedVertices(ModelPart adapter) {
        AtomicInteger vertices = new AtomicInteger();
        Object[] holder = new Object[1];
        holder[0] = Proxy.newProxyInstance(
                ProductionShapeRegressionTest.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("addVertex")) {
                        vertices.incrementAndGet();
                    }
                    if (method.getReturnType().isInstance(holder[0])) {
                        return holder[0];
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType().isPrimitive()) {
                        return 0;
                    }
                    return null;
                });
        assertTrue(EmfIconPartResolver.renderAdapter(
                adapter, new PoseStack(), (VertexConsumer) holder[0], 0, 0, 0xFFFFFFFF));
        return vertices.get();
    }
}
