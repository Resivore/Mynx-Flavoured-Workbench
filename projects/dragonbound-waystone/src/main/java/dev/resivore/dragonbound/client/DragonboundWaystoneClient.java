package dev.resivore.dragonbound.client;

import dev.resivore.dragonbound.DragonboundContent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

/** Client-only dynamic material rendering registration. */
public final class DragonboundWaystoneClient implements ClientModInitializer {
    /**
     * Rebuilds exactly the chunk section containing a Waystone whose block-entity render data
     * changed. {@link Level#setBlocksDirty(BlockPos, net.minecraft.world.level.block.state.BlockState,
     * net.minecraft.world.level.block.state.BlockState)} only reports a same-state block change.
     * In 26.2, equal range endpoints make {@link ClientLevel#setSectionRangeDirty(int, int, int,
     * int, int, int)} mark precisely one rendered section for a geometry-key-only change.
     */
    @Environment(EnvType.CLIENT)
    public static void invalidateRenderedSection(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }

        int sectionX = SectionPos.blockToSectionCoord(pos.getX());
        int sectionY = SectionPos.blockToSectionCoord(pos.getY());
        int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());
        clientLevel.setSectionRangeDirty(sectionX, sectionY, sectionZ, sectionX, sectionY, sectionZ);
    }

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(context -> {
            // Model plugins run for every model reload; do not retain atlas sprites from the
            // previous resource-pack generation.
            MaterializedWaystoneModels.clearCache();
            context.modifyBlockModelAfterBake().register((model, bakeContext) ->
                    bakeContext.state().is(DragonboundContent.WAYSTONE)
                            ? new MaterializedWaystoneBlockStateModel(model)
                            : model);
            context.modifyItemModelAfterBake().register((model, bakeContext) ->
                    bakeContext.itemId().equals(DragonboundContent.WAYSTONE_ID)
                            ? new MaterializedWaystoneItemModel(model)
                            : model);
        });
    }
}
