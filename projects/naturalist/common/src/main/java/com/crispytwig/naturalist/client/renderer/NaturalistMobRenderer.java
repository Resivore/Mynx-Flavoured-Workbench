package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.server.entity.variant.DataDrivenVariantAnimal;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.NotNull;

import com.crispytwig.naturalist.client.model.NaturalistEntityModel;

@Environment(EnvType.CLIENT)
public abstract class NaturalistMobRenderer<T extends Mob & DataDrivenVariantAnimal> extends MobRenderer<T, NaturalistRenderState<T>, NaturalistEntityModel<T>> {
    private final NaturalistEntityModel<T> adultModel;
    private final NaturalistEntityModel<T> babyModel;
    private final float adultShadowRadius;
    private final float babyShadowRadius;

    protected NaturalistMobRenderer(EntityRendererProvider.Context context, NaturalistEntityModel<T> adultModel, NaturalistEntityModel<T> babyModel, float shadowRadius) {
        this(context, adultModel, babyModel, shadowRadius, shadowRadius / 2.0F);
    }

    protected NaturalistMobRenderer(EntityRendererProvider.Context context, NaturalistEntityModel<T> adultModel, NaturalistEntityModel<T> babyModel, float shadowRadius, float babyShadowRadius) {
        super(context, adultModel, shadowRadius);
        this.adultModel = adultModel;
        this.babyModel = babyModel;
        this.adultShadowRadius = shadowRadius;
        this.babyShadowRadius = babyShadowRadius;
    }

    protected NaturalistMobRenderer(EntityRendererProvider.Context context, NaturalistEntityModel<T> model, float shadowRadius) {
        this(context, model, model, shadowRadius, shadowRadius);
    }

    public @NotNull Identifier getTextureLocation(@NotNull T entity) {
        return entity.isBaby() ? entity.getVariantBabyTexture() : entity.getVariantTexture();
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull NaturalistRenderState<T> state) {
        return state.texture;
    }

    @Override
    public NaturalistRenderState<T> createRenderState() {
        return new NaturalistRenderState<>();
    }

    @Override
    public void extractRenderState(T entity, NaturalistRenderState<T> state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
        state.partialTick = partialTick;
        state.texture = this.getTextureLocation(entity);
    }

    @Override
    protected float getShadowRadius(NaturalistRenderState<T> state) {
        return (state.isBaby ? this.babyShadowRadius : this.adultShadowRadius) * state.scale * state.ageScale;
    }

    protected NaturalistEntityModel<T> selectModel(NaturalistRenderState<T> state) {
        return state.isBaby ? this.babyModel : this.adultModel;
    }

    @Override
    public void submit(NaturalistRenderState<T> state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.model = this.selectModel(state);
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
