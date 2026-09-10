package dev.resivore.xaeroemfcompat;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class IconPresentationPolicyTest {
    @AfterEach
    void clearRequest() {
        IconPresentationPolicy.requestFinished();
    }

    @Test
    void beeAndRabbitUseXaerosSupportedSubOneRequestScaleOnly() {
        for (String entity : new String[]{"minecraft:bee", "minecraft:rabbit"}) {
            RadarIconCreator.Parameters original = parameters(1.0F);
            IconPresentationPolicy.requestStarted(entity, true);
            assertEquals(0.75F,
                    IconPresentationPolicy.scaleForCurrentRequest(original).scale, 0.0F, entity);

            PoseStack pose = new PoseStack();
            IconPresentationPolicy.applyUpscaleForCurrentRequest(pose);
            assertEquals(1.0F, pose.last().pose().m00(), 0.0F, entity);
            IconPresentationPolicy.requestFinished();
        }
    }

    @Test
    void ghastsApplyOneExactPresentationStageUpscaleWithoutCompoundingParameters() {
        for (String entity : new String[]{"minecraft:ghast", "minecraft:happy_ghast"}) {
            RadarIconCreator.Parameters original = parameters(1.0F);
            IconPresentationPolicy.requestStarted(entity, true);
            assertSame(original, IconPresentationPolicy.scaleForCurrentRequest(original), entity);

            PoseStack pose = new PoseStack();
            IconPresentationPolicy.applyUpscaleForCurrentRequest(pose);
            assertEquals(1.50F, pose.last().pose().m00(), 0.0F, entity);
            assertEquals(1.50F, pose.last().pose().m11(), 0.0F, entity);
            assertEquals(1.50F, pose.last().pose().m22(), 0.0F, entity);
            IconPresentationPolicy.requestFinished();
        }
    }

    @Test
    void unlistedOrUnavailablePrerenderRequestsRemainIdentity() {
        for (String entity : new String[]{"minecraft:allay", "minecraft:ghast"}) {
            RadarIconCreator.Parameters original = parameters(0.8F);
            IconPresentationPolicy.requestStarted(entity, !entity.equals("minecraft:ghast"));
            assertSame(original, IconPresentationPolicy.scaleForCurrentRequest(original), entity);
            PoseStack pose = new PoseStack();
            IconPresentationPolicy.applyUpscaleForCurrentRequest(pose);
            assertEquals(1.0F, pose.last().pose().m00(), 0.0F, entity);
            IconPresentationPolicy.requestFinished();
        }
    }

    private static RadarIconCreator.Parameters parameters(float scale) {
        return new RadarIconCreator.Parameters("variant", null, null, scale, false);
    }
}
