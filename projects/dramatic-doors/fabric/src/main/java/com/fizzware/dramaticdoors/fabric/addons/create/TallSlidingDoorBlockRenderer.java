/*package com.fizzware.dramaticdoors.fabric.addons.create;

import com.fizzware.dramaticdoors.blocks.TallCreateSlidingDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallDoorBlock;
import com.fizzware.dramaticdoors.state.properties.TripleBlockPart;
import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.Vec3;

public class TallSlidingDoorBlockRenderer implements BlockEntityRenderer<TallFabricCreateSlidingDoorBlockEntity>
{		
	public TallSlidingDoorBlockRenderer(Context context) {}
	
	@Override
	public void render(TallFabricCreateSlidingDoorBlockEntity be, float partialTicks, PoseStack posestack, MultiBufferSource buffer, int light, int overlay) {
		BlockState blockState = be.getBlockState();
		if (!be.shouldRenderSpecial(blockState)) {
			return;
		}

		Direction facing = blockState.getValue(TallDoorBlock.FACING);
		Direction movementDirection = facing.getClockWise();

		if (blockState.getValue(TallDoorBlock.HINGE) == DoorHingeSide.LEFT)
			movementDirection = movementDirection.getOpposite();

		float value = be.getAnimation().getValue(partialTicks);
		float value2 = Mth.clamp(value * 10, 0, 1);

		VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
		Vec3 offset = Vec3.atLowerCornerOf(movementDirection.getNormal()).scale(value * value * 13 / 16f).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(value2 * 1 / 32f));

		if (((TallCreateSlidingDoorBlock) blockState.getBlock()).isFoldingDoor()) {
			Couple<PartialModel> partials = DDPartialModels.TALL_FOLDING_DOORS.get(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));

			boolean flip = blockState.getValue(TallDoorBlock.HINGE) == DoorHingeSide.RIGHT;
			for (boolean left : Iterate.trueAndFalse) {
				SuperByteBuffer partial = CachedBufferer.partial(partials.get(left ^ flip), blockState);
				float f = flip ? -1 : 1;

				partial.translate(0, -1 / 512f, 0).translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(value2 * 1 / 32f));
				partial.rotateCentered(Direction.UP, Mth.DEG_TO_RAD * AngleHelper.horizontalAngle(facing.getClockWise()));

				if (flip) {
					partial.translate(0, 0, 1);
				}
				partial.rotateY(91 * f * value * value);

				if (!left) {
					partial.translate(0, 0, f / 2f).rotateY(-181 * f * value * value);
				}
				if (flip) {
					partial.translate(0, 0, -1 / 2f);
				}
				partial.translate(0, 1, 0);
				partial.light(light).renderInto(posestack, vb);
			}

			return;
		}
		for (TripleBlockPart third : TripleBlockPart.values()) {
			float offsetY = 0;
			if (third == TripleBlockPart.MIDDLE) {
				offsetY = 1;
			}
			if (third == TripleBlockPart.UPPER) {
				offsetY = 2;
			}
			CachedBufferer.block(blockState.setValue(TallDoorBlock.OPEN, false).setValue(TallDoorBlock.THIRD, third)).translate(0, offsetY > 0 ? offsetY - (1 / 512F) : 0, 0).translate(offset).light(light).renderInto(posestack, vb);
		}
	}
}
*/