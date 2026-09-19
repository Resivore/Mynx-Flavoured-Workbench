package dev.resivore.bgectm.continuity;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Exact Continuity 3.0.1/Fabric Renderer 14.1.3 non-geometric emission contract. */
final class OverlayRenderStateTest {
    @Test
    void projectedEmissionRetainsEveryNativeContinuityOverlayAttribute() {
        Set<String> components = Arrays.stream(OverlayRenderState.class.getRecordComponents())
                .map(RecordComponent::getName).collect(Collectors.toSet());

        assertEquals(Set.of("tint", "atlas", "animated", "chunkLayer", "itemRenderType",
                "ambientOcclusion"), components);
        assertEquals(Sheets.cutoutBlockItemSheet(),
                OverlayRenderState.itemRenderType(ChunkSectionLayer.SOLID));
        assertEquals(Sheets.translucentBlockItemSheet(),
                OverlayRenderState.itemRenderType(ChunkSectionLayer.TRANSLUCENT));
    }

    @Test
    void controlledFabricQuadApiHasNoHiddenMaterialDecalOrDepthBiasState() {
        Set<String> methods = Arrays.stream(QuadView.class.getMethods())
                .map(method -> method.getName().toLowerCase()).collect(Collectors.toSet());

        assertFalse(methods.stream().anyMatch(name -> name.contains("material")
                        || name.contains("blend") || name.contains("decal")
                        || name.contains("depth") || name.contains("polygonoffset")),
                "Fabric 14.1.3 unexpectedly gained a hidden overlay/depth material seam");
    }
}
