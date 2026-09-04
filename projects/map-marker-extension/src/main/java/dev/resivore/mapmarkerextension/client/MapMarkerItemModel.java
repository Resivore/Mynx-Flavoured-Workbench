package dev.resivore.mapmarkerextension.client;

import dev.resivore.mapmarkerextension.core.MapMarkerIdentity;
import dev.resivore.mapmarkerextension.core.MapMarkerItemIdentity;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBakedItemModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class MapMarkerItemModel extends WrapperBakedItemModel {
    private static final Set<Identifier> BASE_ITEM_MODELS = Set.of(
        Identifier.parse("minecraft:filled_map"),
        Identifier.parse("minecraft:papal_outpost_map"),
        Identifier.parse("minecraft:abbey_map")
    );

    private final Map<MapMarkerIdentity, ItemModel> models =
        new EnumMap<>(MapMarkerIdentity.class);
    private final Set<MapMarkerIdentity> unavailable =
        EnumSet.noneOf(MapMarkerIdentity.class);

    private MapMarkerItemModel(ItemModel wrapped) {
        super(wrapped);
    }

    public static void register() {
        ModelLoadingPlugin.register(context ->
            context.modifyItemModelAfterBake().register((model, modifierContext) ->
                BASE_ITEM_MODELS.contains(modifierContext.itemId())
                    ? new MapMarkerItemModel(model)
                    : model
            )
        );
    }

    @Override
    public void update(
        ItemStackRenderState renderState,
        ItemStack stack,
        ItemModelResolver resolver,
        ItemDisplayContext displayContext,
        ClientLevel level,
        ItemOwner owner,
        int seed
    ) {
        MapMarkerIdentity identity = MapMarkerItemIdentity.find(stack).orElse(null);
        ItemModel model = identity == null ? null : resolve(identity);
        if (model == null) {
            // Preserve the wrapped model chain so another mod's filled-map wrapper can handle
            // identities that MME does not own, regardless of modifier registration order.
            super.update(renderState, stack, resolver, displayContext, level, owner, seed);
            return;
        }
        model.update(renderState, stack, resolver, displayContext, level, owner, seed);
    }

    private ItemModel resolve(MapMarkerIdentity identity) {
        ItemModel cached = models.get(identity);
        if (cached != null || unavailable.contains(identity)) {
            return cached;
        }

        Minecraft client = Minecraft.getInstance();
        Identifier textureId = Identifier.fromNamespaceAndPath(
            MapMarkerIdentity.RESOURCE_NAMESPACE,
            "textures/map/decorations/map_sprites/" + identity.id() + ".png"
        );
        if (client.getResourceManager().getResource(textureId).isEmpty()) {
            unavailable.add(identity);
            return null;
        }

        ItemModel model = client.getModelManager().getItemModel(
            Identifier.parse(identity.filledMapItemAssetId())
        );
        if (model instanceof MissingItemModel) {
            unavailable.add(identity);
            return null;
        }
        models.put(identity, model);
        return model;
    }
}
