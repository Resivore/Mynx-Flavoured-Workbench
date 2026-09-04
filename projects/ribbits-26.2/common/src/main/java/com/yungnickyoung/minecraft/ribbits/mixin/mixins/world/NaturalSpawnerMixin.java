package com.yungnickyoung.minecraft.ribbits.mixin.mixins.world;

import com.yungnickyoung.minecraft.ribbits.world.structure.SwampHutPhaseC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rejects only natural Witch candidates whose exact position belongs to a SwampHutPiece. */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    private static final ResourceKey<Structure> SWAMP_HUT = ResourceKey.create(
            Registries.STRUCTURE, Identifier.parse("minecraft:swamp_hut"));

    @Inject(
            method = "isValidSpawnPostitionForType("
                    + "Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/world/entity/MobCategory;"
                    + "Lnet/minecraft/world/level/StructureManager;"
                    + "Lnet/minecraft/world/level/chunk/ChunkGenerator;"
                    + "Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;"
                    + "Lnet/minecraft/core/BlockPos$MutableBlockPos;D)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            allow = 1
    )
    private static void ribbits$suppressNaturalWitchInsideExactHutPiece(
            ServerLevel level,
            MobCategory mobCategory,
            StructureManager structureManager,
            ChunkGenerator generator,
            MobSpawnSettings.SpawnerData spawnerData,
            BlockPos.MutableBlockPos pos,
            double nearestPlayerDistanceSqr,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (spawnerData.type() != EntityTypes.WITCH) {
            return;
        }

        Registry<Structure> structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Structure swampHut = structures.getValueOrThrow(SWAMP_HUT);
        StructureStart start = structureManager.getStructureWithPieceAt(pos, swampHut);
        if (SwampHutPhaseC.containsExactPiece(start, pos)) {
            cir.setReturnValue(false);
        }
    }
}
