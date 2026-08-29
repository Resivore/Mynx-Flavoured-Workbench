package com.crispytwig.naturalist.client.model;

import com.crispytwig.naturalist.server.entity.base.MultipartMob;
import com.crispytwig.naturalist.server.entity.util.SmoothAnimationState;
import com.crispytwig.naturalist.client.renderer.NaturalistRenderState;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class NaturalistEntityModel<E extends Entity> extends EntityModel<NaturalistRenderState<E>> {
    private static final double GAIT_FACTOR = 0.65D;
    private static final double LIMB_SWING_PER_SPEED = 8.64D;
    protected static final float IDLE_FADE_SCALE = 2.5F;

    public static final double SMALL_SWIMMER_LIMB_SWING = 0.25D;
    public static final double LARGE_SWIMMER_LIMB_SWING = 0.5D;

    private final Map<AnimationDefinition, CompatibleAnimation> animations = new IdentityHashMap<>();

    protected NaturalistEntityModel(ModelPart root) {
        super(root);
    }

    protected NaturalistEntityModel(ModelPart root, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
    }

    protected String getRootPartName() {
        return "root";
    }

    @Override
    public final void setupAnim(NaturalistRenderState<E> state) {
        super.setupAnim(state);
        this.setupAnimations(state.entity, state.walkAnimationPos, state.walkAnimationSpeed,
                state.ageInTicks, state.partialTick, state.yRot, state.xRot);
    }

    protected abstract void setupAnimations(E entity, float limbSwing, float limbSwingAmount, float ageInTicks, float partialTick, float netHeadYaw, float headPitch);

    protected static void applyHeadLook(ModelPart part, float netHeadYaw, float headPitch) {
        part.xRot += headPitch * Mth.DEG_TO_RAD;
        part.yRot += netHeadYaw * Mth.DEG_TO_RAD;
    }

    protected void rotatePart(ModelPart part, float dx, float dy, float dz) {
        part.xRot += dx;
        part.yRot += dy;
        part.zRot += dz;
    }

    protected void bend(ModelPart part, MultipartMob mob, int segment, float partialTick) {
        this.rotatePart(part,
                mob.getSegmentPitchOffset(segment, partialTick) * Mth.DEG_TO_RAD,
                mob.getSegmentYawOffset(segment, partialTick) * Mth.DEG_TO_RAD,
                0.0F);
    }

    protected static float movementAnimationSpeed(LivingEntity entity, float limbSwingAmount, float baseSpeed) {
        double gaitLimbSwing = Math.min(LIMB_SWING_PER_SPEED * entity.getAttributeValue(Attributes.MOVEMENT_SPEED), 1.0D) * GAIT_FACTOR;
        return movementAnimationSpeed(entity, limbSwingAmount, baseSpeed, gaitLimbSwing);
    }

    protected static float movementAnimationSpeed(LivingEntity entity, float limbSwingAmount, float baseSpeed, double referenceLimbSwing) {
        return movementAnimationSpeed(entity, limbSwingAmount, baseSpeed, referenceLimbSwing, 0.4F);
    }

    protected static float movementAnimationSpeed(LivingEntity entity, float limbSwingAmount, float baseSpeed, double referenceLimbSwing, float minSpeed) {
        return baseSpeed * Mth.clamp(limbSwingAmount / (float) Math.max(referenceLimbSwing, 0.05D), minSpeed, 2.0F);
    }

    protected void animateSmooth(SmoothAnimationState state, AnimationDefinition definition, float ageInTicks, float partialTick) {
        this.animateSmooth(state, definition, ageInTicks, partialTick, 1.0F);
    }

    protected void animateSmooth(SmoothAnimationState state, AnimationDefinition definition, float ageInTicks, float partialTick, float speed) {
        float factor = state.factor(partialTick);
        if (factor <= SmoothAnimationState.ACTIVE_THRESHOLD) {
            return;
        }
        state.updateTime(ageInTicks, speed);
        this.animation(definition).apply(state.getAccumulatedTime(), factor);
    }

    protected void animateUnblended(SmoothAnimationState state, AnimationDefinition definition, float ageInTicks) {
        if (state.isStarted()) {
            state.updateTime(ageInTicks, 1.0F);
        }
        this.animation(definition).apply(state.getAccumulatedTime(), 1.0F);
    }

    protected void animate(AnimationState state, AnimationDefinition definition, float ageInTicks) {
        this.animation(definition).apply(state, ageInTicks);
    }

    protected void animateIdleSmooth(SmoothAnimationState state, AnimationDefinition definition, float ageInTicks, float partialTick, float limbSwingAmount) {
        this.animateIdleSmooth(state, definition, ageInTicks, partialTick, limbSwingAmount, IDLE_FADE_SCALE, 1.0F);
    }

    protected void animateIdleSmooth(SmoothAnimationState state, AnimationDefinition definition, float ageInTicks, float partialTick, float limbSwingAmount, float animationScaleFactor, float speed) {
        float factor = state.factor(partialTick) * (1.0F - Math.min(limbSwingAmount * animationScaleFactor, 1.0F));
        if (factor <= SmoothAnimationState.ACTIVE_THRESHOLD) {
            return;
        }
        state.updateTime(ageInTicks, speed);
        this.animation(definition).apply(state.getAccumulatedTime(), factor);
    }

    private CompatibleAnimation animation(AnimationDefinition definition) {
        return this.animations.computeIfAbsent(definition, this::bakeCompatibleAnimation);
    }

    private CompatibleAnimation bakeCompatibleAnimation(AnimationDefinition definition) {
        List<KeyframeAnimation> channelsByResolvedPart = definition.boneAnimations().entrySet().stream()
                .map(entry -> this.bakeChannelsForResolvedPart(definition, entry.getKey(), entry.getValue()))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new CompatibleAnimation(channelsByResolvedPart);
    }

    private KeyframeAnimation bakeChannelsForResolvedPart(
            AnimationDefinition definition, String boneName, List<AnimationChannel> channels) {
        ModelPart targetPart = this.findAnimationPart(boneName);
        if (targetPart == null) {
            return null;
        }

        AnimationDefinition resolvedDefinition = new AnimationDefinition(
                definition.lengthInSeconds(), definition.looping(), Map.of("root", channels));
        return resolvedDefinition.bake(targetPart);
    }

    private ModelPart findAnimationPart(String name) {
        return this.root().getAllParts().stream()
                .filter(part -> part.hasChild(name))
                .findFirst()
                .map(part -> part.getChild(name))
                .orElseGet(() -> name.equals(this.getRootPartName()) ? this.root() : null);
    }

    private record CompatibleAnimation(List<KeyframeAnimation> parts) {
        void apply(long accumulatedTime, float weight) {
            this.parts.forEach(animation -> animation.apply(accumulatedTime, weight));
        }

        void apply(AnimationState state, float ageInTicks) {
            this.parts.forEach(animation -> animation.apply(state, ageInTicks));
        }
    }
}
