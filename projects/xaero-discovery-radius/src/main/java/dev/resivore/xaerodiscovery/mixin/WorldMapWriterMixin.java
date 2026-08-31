package dev.resivore.xaerodiscovery.mixin;

import dev.resivore.xaerodiscovery.DiscoveryService;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapWriter;

@Mixin(value = MapWriter.class, remap = false)
abstract class WorldMapWriterMixin {
    @Shadow
    private int playerChunkX;

    @Shadow
    private int playerChunkZ;

    @Shadow
    private int writingLayer;

    @Inject(
            method = "getWriteDistance()I",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            allow = 1,
            remap = false
    )
    private void xaeroDiscoveryRadius$clampWriteDistance(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(DiscoveryService.clampWorldMapWriteDistance(cir.getReturnValue()));
    }

    @Redirect(
            method = "writeChunk(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Registry;IZ"
                    + "Lnet/minecraft/core/Registry;Lxaero/map/region/OverlayManager;ZZZZZ"
                    + "Lnet/minecraft/core/BlockPos$MutableBlockPos;Lxaero/map/biome/BlockTintProvider;"
                    + "IIIIIIIIILxaero/map/region/MapUpdateFastConfig;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getChunk("
                            + "IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)"
                            + "Lnet/minecraft/world/level/chunk/ChunkAccess;",
                    remap = true
            ),
            require = 1,
            allow = 1,
            remap = false
    )
    private ChunkAccess xaeroDiscoveryRadius$gateWorldMapTerrainRead(
            Level world,
            int chunkX,
            int chunkZ,
            ChunkStatus status,
            boolean create
    ) {
        if (!DiscoveryService.mayWriteWorldMapChunk(
                playerChunkX,
                playerChunkZ,
                chunkX,
                chunkZ
        )) {
            return null;
        }
        ChunkAccess chunk = world.getChunk(chunkX, chunkZ, status, create);
        if (chunk != null) {
            DiscoveryService.recordWorldMapLayer(world, playerChunkX, playerChunkZ, writingLayer);
        }
        return chunk;
    }
}
