package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.client.model.DuckBabyModel;
import com.crispytwig.naturalist.client.model.DuckModel;
import com.crispytwig.naturalist.client.renderer.layers.DyeLayer;
import com.crispytwig.naturalist.server.entity.mob.Duck;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class DuckRenderer extends NaturalistMobRenderer<Duck> {
    private static final Identifier DUCK = Naturalist.location("textures/entity/duck/duck.png");
    private static final Identifier DUCK_BABY = Naturalist.location("textures/entity/duck/duck_baby.png");
    private static final Identifier QUESO = Naturalist.location("textures/entity/duck/queso.png");

    public DuckRenderer(EntityRendererProvider.Context context) {
        super(context, new DuckModel(context.bakeLayer(DuckModel.LAYER_LOCATION)), new DuckBabyModel(context.bakeLayer(DuckBabyModel.LAYER_LOCATION)), 0.3F);
        this.addLayer(new DyeLayer<>(this, "duck"));
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Duck entity) {
        if (entity.hasNonDefaultVariant()) {
            return entity.isBaby() ? entity.getVariantBabyTexture() : entity.getVariantTexture();
        }
        if (entity.isBaby()) {
            return DUCK_BABY;
        }
        if (entity.hasCustomName() && entity.getName().getString().equalsIgnoreCase("Queso")) {
            return QUESO;
        }
        return DUCK;
    }
}
