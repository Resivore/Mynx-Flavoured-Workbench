package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.server.block.SnailShellBlock;
import com.crispytwig.naturalist.server.block.entity.SnailShellBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import java.util.AbstractList;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class SnailShellRenderer implements BlockEntityRenderer<SnailShellBlockEntity, SnailShellRenderer.SnailShellRenderState> {
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Set<Identifier> POT_SPRITES = Set.of(
            Identifier.withDefaultNamespace("block/flower_pot"),
            Identifier.withDefaultNamespace("block/dirt"),
            Identifier.withDefaultNamespace("block/potted_azalea_bush_side"),
            Identifier.withDefaultNamespace("block/potted_azalea_bush_top"),
            Identifier.withDefaultNamespace("block/potted_flowering_azalea_bush_side"),
            Identifier.withDefaultNamespace("block/potted_flowering_azalea_bush_top"));

    private final BlockModelResolver blockModelResolver;

    public SnailShellRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public SnailShellRenderState createRenderState() {
        return new SnailShellRenderState();
    }

    @Override
    public void extractRenderState(SnailShellBlockEntity blockEntity, SnailShellRenderState renderState,
                                   float partialTick, Vec3 cameraPosition, CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, crumblingOverlay);
        BlockState blockState = blockEntity.getBlockState();
        renderState.visible = blockState.getBlock() instanceof SnailShellBlock;
        renderState.hasPlant = false;
        if (!renderState.visible) {
            return;
        }

        renderState.rotation = blockState.getValue(SnailShellBlock.ROTATION);
        this.blockModelResolver.update(renderState.shellModel, blockState, BLOCK_DISPLAY_CONTEXT);

        Block potted = SnailShellBlock.getPottedBlock(blockEntity.getFlower());
        if (potted != null) {
            this.blockModelResolver.update(renderState.plantModel, potted.defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
            renderState.hasPlant = true;
        }
    }

    @Override
    public void submit(SnailShellRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (!renderState.visible) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-RotationSegment.convertToDegrees(renderState.rotation)));
        poseStack.translate(0.0D, -0.28125D, 0.15625D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(-0.5D, -0.21875D, -0.65625D);
        renderState.shellModel.submit(poseStack, submitNodeCollector,
                renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        renderState.shellModel.submitBreaking(poseStack, submitNodeCollector, renderState.breakProgress);
        poseStack.popPose();

        if (renderState.hasPlant) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.15D, 0.0D);
            renderState.plantModel.submit(poseStack, submitNodeCollector,
                    renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            renderState.plantModel.submitBreaking(poseStack, submitNodeCollector, renderState.breakProgress);
            poseStack.popPose();
        }
    }

    public static final class SnailShellRenderState extends BlockEntityRenderState {
        private boolean visible;
        private int rotation;
        private boolean hasPlant;
        private final TrackedBlockModelRenderState shellModel = new TrackedBlockModelRenderState();
        private final TrackedBlockModelRenderState plantModel = new PotFilteredBlockModelRenderState();
    }

    private static class TrackedBlockModelRenderState extends BlockModelRenderState {
        private List<BlockStateModelPart> modelParts = List.of();
        private Matrix4fc transformation;

        @Override
        public void clear() {
            super.clear();
            this.modelParts = List.of();
            this.transformation = null;
        }

        @Override
        public List<BlockStateModelPart> setupModel(Matrix4fc transformation, boolean translucent) {
            this.transformation = transformation;
            this.modelParts = super.setupModel(transformation, translucent);
            return this.modelParts;
        }

        private void submitBreaking(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                    CrumblingOverlay crumblingOverlay) {
            if (crumblingOverlay == null || this.modelParts.isEmpty()) {
                return;
            }

            poseStack.pushPose();
            if (this.transformation != null) {
                poseStack.mulPose(this.transformation);
            }
            submitNodeCollector.submitBreakingBlockModel(
                    poseStack, List.copyOf(this.modelParts), crumblingOverlay.progress());
            poseStack.popPose();
        }
    }

    private static final class PotFilteredBlockModelRenderState extends TrackedBlockModelRenderState {
        @Override
        public List<BlockStateModelPart> setupModel(Matrix4fc transformation, boolean translucent) {
            return new FilteringPartList(super.setupModel(transformation, translucent));
        }
    }

    private static final class FilteringPartList extends AbstractList<BlockStateModelPart> {
        private final List<BlockStateModelPart> delegate;

        private FilteringPartList(List<BlockStateModelPart> delegate) {
            this.delegate = delegate;
        }

        @Override
        public BlockStateModelPart get(int index) {
            return this.delegate.get(index);
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public void add(int index, BlockStateModelPart element) {
            this.delegate.add(index, new PotFilteredModelPart(element));
        }

        @Override
        public BlockStateModelPart set(int index, BlockStateModelPart element) {
            return this.delegate.set(index, new PotFilteredModelPart(element));
        }

        @Override
        public BlockStateModelPart remove(int index) {
            return this.delegate.remove(index);
        }

        @Override
        public void clear() {
            this.delegate.clear();
        }
    }

    private record PotFilteredModelPart(BlockStateModelPart delegate) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            return this.delegate.getQuads(direction).stream()
                    .filter(quad -> !POT_SPRITES.contains(quad.materialInfo().sprite().contents().name()))
                    .toList();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.delegate.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return this.delegate.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return this.delegate.materialFlags();
        }
    }
}
