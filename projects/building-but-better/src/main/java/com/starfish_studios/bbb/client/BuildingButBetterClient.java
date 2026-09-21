package com.starfish_studios.bbb.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.starfish_studios.bbb.compat.EnderscapeWoodIntegration;
import com.starfish_studios.bbb.item.DescriptionBlockItem;
import com.starfish_studios.bbb.registry.BBBContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class BuildingButBetterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EnderscapeWoodIntegration.requireProviderReady();
        DescriptionBlockItem.installControlKeyCheck(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            return InputConstants.isKeyDown(minecraft.getWindow(), InputConstants.KEY_LCONTROL)
                    || InputConstants.isKeyDown(minecraft.getWindow(), InputConstants.KEY_RCONTROL);
        });

        // Tint index 0 is unused by the lattice templates; index 1 is foliage.
        BlockColorRegistry.register(
                List.of(BlockTintSources.constant(0xFFFFFFFF), BlockTintSources.foliage()),
                BBBContent.LATTICES.toArray(Block[]::new)
        );
        // Minecraft 26.2 derives section layers from sprite transparency. The
        // staging verifier therefore checks the retained sprites' binary alpha
        // instead of adding unsupported model render_type metadata.
    }
}
