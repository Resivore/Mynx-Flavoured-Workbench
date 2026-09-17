package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.resivore.villagerwork.ShearingToolMarker;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/** The marker only supplies synchronized ownership to a villager render layer; it has no world model. */
public final class ShearingToolMarkerRenderer extends EntityRenderer<ShearingToolMarker, EntityRenderState> {
    public ShearingToolMarkerRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public EntityRenderState createRenderState() { return new EntityRenderState(); }
    @Override public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                 CameraRenderState camera) { }
}
