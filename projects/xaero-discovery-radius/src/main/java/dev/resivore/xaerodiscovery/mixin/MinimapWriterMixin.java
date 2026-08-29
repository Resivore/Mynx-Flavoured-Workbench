package dev.resivore.xaerodiscovery.mixin;

import dev.resivore.xaerodiscovery.DiscoveryService;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.minimap.write.MinimapWriter;

@Mixin(value = MinimapWriter.class, remap = false)
abstract class MinimapWriterMixin {
    @Redirect(
            method = "writeTile(Lxaero/common/minimap/MinimapProcessor;DDDLnet/minecraft/client/multiplayer/ClientLevel;"
                    + "Lxaero/common/minimap/region/MinimapChunk;Lxaero/common/minimap/region/MinimapChunk;"
                    + "Lxaero/common/minimap/region/MinimapChunk;Lxaero/common/minimap/region/MinimapChunk;"
                    + "Lxaero/common/minimap/region/MinimapChunk;IIIIZZ)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getChunk("
                            + "IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)"
                            + "Lnet/minecraft/world/level/chunk/ChunkAccess;",
                    remap = true
            ),
            require = 1,
            allow = 1,
            remap = false
    )
    private ChunkAccess xaeroDiscoveryRadius$gateMinimapTerrainRead(
            ClientLevel world,
            int chunkX,
            int chunkZ,
            ChunkStatus status,
            boolean create
    ) {
        if (!DiscoveryService.mayReadLiveChunk(world, chunkX, chunkZ)) {
            return null;
        }
        return world.getChunk(chunkX, chunkZ, status, create);
    }
}
