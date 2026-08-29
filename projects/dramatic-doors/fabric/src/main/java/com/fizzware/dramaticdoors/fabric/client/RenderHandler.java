package com.fizzware.dramaticdoors.fabric.client;

import net.fabricmc.api.ClientModInitializer;

public class RenderHandler implements ClientModInitializer
{
	@Override
	public void onInitializeClient() {
		// Minecraft 26.2 derives block render layers from model materials. The
		// former Fabric BlockRenderLayerMap API no longer exists.
		/*if (FabricUtils.INSTANCE.isModLoaded("create")) {
	    	DDPartialModels.putFoldingDoor(DDNames.TALL_CREATE_ANDESITE, "create/tall_andesite_door");
	    	DDPartialModels.putFoldingDoor(DDNames.TALL_CREATE_COPPER, "create/tall_copper_door");
			BlockEntityRenderers.register(CreateFabricCompat.TALL_SLIDING_DOOR_BLOCK_ENTITY, TallSlidingDoorBlockRenderer::new);
		}*/
	}
}
