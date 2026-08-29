package dev.resivore.xaerodiscovery.mixin;

import dev.resivore.xaerodiscovery.DiscoveryService;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.file.worldsave.WorldDataReader;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;

@Mixin(value = WorldDataReader.class, remap = false)
abstract class WorldDataReaderMixin {
    @Inject(
            method = "buildTile(Lnet/minecraft/nbt/CompoundTag;Lxaero/map/region/MapTile;"
                    + "Lxaero/map/region/MapTileChunk;IIIIIIZZLnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/core/HolderLookup;Lnet/minecraft/core/Registry;Lnet/minecraft/core/Registry;"
                    + "Lnet/minecraft/core/Registry;ZII)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            allow = 1,
            remap = false
    )
    private void xaeroDiscoveryRadius$gateSingleplayerDiskRead(
            CompoundTag chunkNbt,
            MapTile tile,
            MapTileChunk tileChunk,
            int chunkX,
            int chunkZ,
            int insideRegionX,
            int insideRegionZ,
            int caveStart,
            int caveDepth,
            boolean worldHasSkylight,
            boolean ignoreHeightmaps,
            ServerLevel serverWorld,
            HolderLookup<Block> blockLookup,
            Registry<Block> blockRegistry,
            Registry<Fluid> fluidRegistry,
            Registry<Biome> biomeRegistry,
            boolean flowers,
            int worldBottomY,
            int worldTopY,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!DiscoveryService.mayReadSingleplayerSaveChunk(serverWorld, chunkX, chunkZ)) {
            cir.setReturnValue(false);
        }
    }
}
