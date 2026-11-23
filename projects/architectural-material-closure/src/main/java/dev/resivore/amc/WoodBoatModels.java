package dev.resivore.amc;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.BoatRenderer;

final class WoodBoatModels {
    static void register() {
        for (var boat : WoodBoats.VESSELS) {
            var layer = new ModelLayerLocation(boat.texture(), "main");
            ModelLayerRegistry.registerModelLayer(layer, boat.chest() ? BoatModel::createChestBoatModel : BoatModel::createBoatModel);
            EntityRendererRegistry.register(boat.type(), context -> new BoatRenderer(context, layer));
        }
    }
}
