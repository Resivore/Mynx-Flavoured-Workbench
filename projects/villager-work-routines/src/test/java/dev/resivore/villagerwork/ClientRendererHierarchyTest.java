package dev.resivore.villagerwork;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.entity.npc.villager.Villager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the production-mapped 26.2 renderer hierarchy used by the client hook. */
class ClientRendererHierarchyTest {
    @Test
    void villagerRendererInheritsRatherThanDeclaresAddLayer() throws NoSuchMethodException {
        assertFalse(Arrays.stream(VillagerRenderer.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("addLayer")),
                "VillagerRenderer must not be shadowed as the declaring owner of addLayer");

        Method addLayer = LivingEntityRenderer.class.getDeclaredMethod("addLayer", RenderLayer.class);
        assertEquals(boolean.class, addLayer.getReturnType());
        assertTrue(addLayer.getDeclaringClass().isAssignableFrom(VillagerRenderer.class));
        assertEquals(void.class, VillagerRenderer.class.getDeclaredMethod("extractRenderState",
                Villager.class, VillagerRenderState.class, float.class).getReturnType());
    }

    @Test
    void supportedFabricCallbackAcceptsVillagerRenderLayers() throws NoSuchMethodException {
        Method register = LivingEntityRenderLayerRegistrationCallback.RegistrationHelper.class
                .getDeclaredMethod("register", RenderLayer.class);
        assertEquals(void.class, register.getReturnType());
        assertTrue(RenderLayer.class.isAssignableFrom(
                dev.resivore.villagerwork.client.VwrFishingRodLayer.class));
        assertEquals(VillagerRenderState.class,
                VillagerRenderer.class.getDeclaredMethod("createRenderState").getReturnType());
    }

    @Test
    void structuralAccessorsAndDirectGeometryPredicateMatchMappedRuntime()
            throws NoSuchFieldException, NoSuchMethodException {
        assertEquals(ModelPart.class, VillagerModel.class.getDeclaredField("arms").getType());
        assertEquals(Map.class, ModelPart.class.getDeclaredField("children").getType());
        assertEquals(boolean.class, ModelPart.class.getDeclaredMethod("isEmpty").getReturnType());
    }
}
