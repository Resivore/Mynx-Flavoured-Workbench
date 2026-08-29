package dev.resivore.xaerodiscovery.mixin;

import dev.resivore.xaerodiscovery.DiscoveryService;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapTileChunk;
import xaero.map.region.texture.LeafRegionTexture;

@Mixin(value = LeafRegionTexture.class, remap = false)
abstract class LeafRegionTextureMixin {
    @Inject(
            method = "readCacheData(IILjava/io/DataInputStream;[B[BLxaero/map/region/LeveledRegion;"
                    + "Lxaero/map/MapProcessor;IIZ)V",
            at = @At("RETURN"),
            require = 1,
            allow = 1,
            remap = false
    )
    private void xaeroDiscoveryRadius$importLegacyCache(
            int minorSaveVersion,
            int majorSaveVersion,
            DataInputStream input,
            byte[] usableBuffer,
            byte[] integerByteBuffer,
            LeveledRegion<LeafRegionTexture> inRegion,
            MapProcessor mapProcessor,
            int x,
            int y,
            boolean leafShouldAffectBranches,
            CallbackInfo ci
    ) throws IOException {
        ServerLevel serverWorld = mapProcessor.getWorldDataHandler().getWorldServer();
        if (serverWorld == null) {
            return;
        }
        LeafRegionTexture texture = (LeafRegionTexture) (Object) this;
        MapTileChunk tileChunk = texture.getTileChunk();
        File cacheFile = inRegion.getCacheFile();
        if (cacheFile != null) {
            DiscoveryService.importXaeroCache(
                    serverWorld,
                    cacheFile.toPath(),
                    tileChunk.getX(),
                    tileChunk.getZ(),
                    texture::getHeight
            );
        }
    }
}
