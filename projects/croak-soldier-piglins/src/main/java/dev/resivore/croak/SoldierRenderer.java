package dev.resivore.croak;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.PiglinRenderState;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Items;

public final class SoldierRenderer extends GeoReplacedEntityRenderer<Soldier, Piglin, SoldierRenderer.State> {

    // GeckoLib adds GeoRenderState to EntityRenderState through its client mixin.
    public static class State extends PiglinRenderState {
        public final SoldierRenderer renderer;
        private State(SoldierRenderer renderer) { this.renderer = renderer; }
        boolean aggressive, sprinting, dying, meleeSwing;
    }

    public SoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<Soldier>() {
            @Override public Identifier getModelResource(GeoRenderState state) { return SoldierPack.id("soldier"); }
            @Override public Identifier getAnimationResource(Soldier soldier) { return SoldierPack.id("soldier"); }
            @Override public Identifier getTextureResource(GeoRenderState state) { return SoldierPack.TEXTURE; }
        }, new Soldier());
        shadowRadius = 0.5f;
    }

    @Override public State createRenderState(Soldier visual, Piglin piglin) { return new State(this); }

    @Override public void extractRenderState(Piglin piglin, State state, float partialTick) {
        state.dying = piglin.isDeadOrDying();
        state.aggressive = piglin.isAggressive();
        state.sprinting = piglin.isSprinting();
        state.meleeSwing = piglin.swinging && !piglin.getItemInHand(piglin.swingingArm).is(Items.CROSSBOW);
        state.armPose = piglin.getArmPose();
        state.isConverting = piglin.isConverting();
        super.extractRenderState(piglin, state, partialTick);
    }

    @Override protected float getDeathMaxRotation(GeoRenderState state) { return 0; }
    @Override public RenderType getRenderType(State state, Identifier texture) {
        if (state.isInvisible) return super.getRenderType(state, texture);
        return RenderTypes.entityTranslucent(texture);
    }
}

