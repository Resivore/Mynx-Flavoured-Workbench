package com.yungnickyoung.minecraft.ribbits.mixin.mixins.world;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.data.RibbitData;
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import com.yungnickyoung.minecraft.ribbits.mixin.mixins.accessor.StructurePieceInvoker;
import com.yungnickyoung.minecraft.ribbits.module.EntityTypeModule;
import com.yungnickyoung.minecraft.ribbits.module.RibbitInstrumentModule;
import com.yungnickyoung.minecraft.ribbits.module.RibbitProfessionModule;
import com.yungnickyoung.minecraft.ribbits.module.RibbitUmbrellaTypeModule;
import com.yungnickyoung.minecraft.ribbits.world.structure.SwampHutPhaseC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.SwampHutPiece;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Narrow changes to the procedural hut's one-time residents and interior map barrel. */
@Mixin(SwampHutPiece.class)
public abstract class SwampHutPieceMixin {
    private static final String POST_PROCESS = "postProcess("
            + "Lnet/minecraft/world/level/WorldGenLevel;"
            + "Lnet/minecraft/world/level/StructureManager;"
            + "Lnet/minecraft/world/level/chunk/ChunkGenerator;"
            + "Lnet/minecraft/util/RandomSource;"
            + "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;"
            + "Lnet/minecraft/world/level/ChunkPos;"
            + "Lnet/minecraft/core/BlockPos;)V";
    private static final String SPAWN_CAT = "spawnCat("
            + "Lnet/minecraft/world/level/ServerLevelAccessor;"
            + "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)V";

    private static final ResourceKey<LootTable> MAP_BARREL_LOOT_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE, RibbitsCommon.id("chests/swamp_hut_map"));

    @ModifyArgs(
            method = POST_PROCESS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/structures/SwampHutPiece;"
                            + "getWorldPos(III)Lnet/minecraft/core/BlockPos$MutableBlockPos;",
                    ordinal = 0
            ),
            require = 1,
            allow = 1
    )
    private void ribbits$moveInitialResidentToSorcererPosition(Args args) {
        requireCoordinates(args, 2, 2, 5, "initial Witch");
        args.set(0, SwampHutPhaseC.SORCERER_LOCAL.getX());
        args.set(1, SwampHutPhaseC.SORCERER_LOCAL.getY());
        args.set(2, SwampHutPhaseC.SORCERER_LOCAL.getZ());
    }

    @Redirect(
            method = POST_PROCESS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType;create("
                            + "Lnet/minecraft/world/level/Level;"
                            + "Lnet/minecraft/world/entity/EntitySpawnReason;)"
                            + "Lnet/minecraft/world/entity/Entity;",
                    ordinal = 0
            ),
            require = 1,
            allow = 1
    )
    private Entity ribbits$replaceInitialWitch(
            EntityType<?> entityType,
            Level entityLevel,
            EntitySpawnReason spawnReason,
            WorldGenLevel generationLevel,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos
    ) {
        if (entityType != EntityTypes.WITCH || spawnReason != EntitySpawnReason.STRUCTURE
                || entityLevel != generationLevel.getLevel()) {
            throw new IllegalStateException("Phase C initial-resident hook no longer targets the exact structure Witch");
        }

        BlockPos spawnPos = structurePiece().ribbits$invokeGetWorldPos(
                SwampHutPhaseC.SORCERER_LOCAL.getX(),
                SwampHutPhaseC.SORCERER_LOCAL.getY(),
                SwampHutPhaseC.SORCERER_LOCAL.getZ()).immutable();
        if (!chunkBB.isInside(spawnPos)) {
            throw new IllegalStateException("Phase C Sorcerer escaped the vanilla generation chunk guard");
        }

        RibbitEntity ribbit = EntityTypeModule.RIBBIT.get().create(entityLevel, spawnReason);
        if (ribbit == null) {
            throw new IllegalStateException("Phase C could not create the hut Sorcerer");
        }
        ribbit.setPersistenceRequired();
        ribbit.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                0.0F, 0.0F);
        ribbit.finalizeSpawn(generationLevel,
                generationLevel.getCurrentDifficultyAt(spawnPos), spawnReason, null);
        ribbit.setRibbitData(new RibbitData(
                RibbitProfessionModule.SORCERER,
                RibbitUmbrellaTypeModule.UMBRELLA_1,
                RibbitInstrumentModule.NONE));
        ribbit.reassessGoals();
        // ServerLevelAccessor#addFreshEntityWithPassengers is void in 26.2. A newly created
        // Ribbit has no passengers, so use the underlying boolean insertion path and fail loudly.
        if (!generationLevel.addFreshEntity(ribbit)) {
            throw new IllegalStateException("Phase C could not add the hut Sorcerer");
        }

        // postProcess immediately CHECKCASTs this result to Witch. Null preserves its existing
        // null branch while preventing vanilla from finalizing or adding an initial Witch.
        return null;
    }

    @ModifyArgs(
            method = SPAWN_CAT,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/structures/SwampHutPiece;"
                            + "getWorldPos(III)Lnet/minecraft/core/BlockPos$MutableBlockPos;",
                    ordinal = 0
            ),
            require = 1,
            allow = 1
    )
    private void ribbits$moveInitialCat(Args args) {
        requireCoordinates(args, 2, 2, 5, "initial Cat");
        args.set(2, SwampHutPhaseC.CAT_LOCAL.getZ());
    }

    @Inject(
            method = POST_PROCESS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/structures/SwampHutPiece;"
                            + "spawnCat(Lnet/minecraft/world/level/ServerLevelAccessor;"
                            + "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            ),
            require = 1,
            allow = 1
    )
    private void ribbits$placeMapBarrel(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos,
            CallbackInfo ci
    ) {
        BlockPos pos = structurePiece().ribbits$invokeGetWorldPos(
                SwampHutPhaseC.BARREL_LOCAL.getX(),
                SwampHutPhaseC.BARREL_LOCAL.getY(),
                SwampHutPhaseC.BARREL_LOCAL.getZ()).immutable();
        if (!chunkBB.isInside(pos) || level.getBlockState(pos).is(Blocks.BARREL)) {
            // The latter case includes an unresolved keyed barrel and a permanently resolved one.
            // Never place again or restore a cleared loot key on structure-generation re-entry.
            return;
        }

        BlockState localState = Blocks.BARREL.defaultBlockState()
                .setValue(BarrelBlock.FACING, Direction.NORTH)
                .setValue(BarrelBlock.OPEN, false);
        structurePiece().ribbits$invokePlaceBlock(level, localState,
                SwampHutPhaseC.BARREL_LOCAL.getX(),
                SwampHutPhaseC.BARREL_LOCAL.getY(),
                SwampHutPhaseC.BARREL_LOCAL.getZ(), chunkBB);

        if (!level.getBlockState(pos).is(Blocks.BARREL)) {
            throw new IllegalStateException("Phase C hut map barrel placement did not produce a Barrel");
        }
        if (!(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) {
            throw new IllegalStateException("Phase C hut map barrel is missing its BarrelBlockEntity");
        }
        barrel.setLootTable(MAP_BARREL_LOOT_TABLE);
        barrel.setLootTableSeed(random.nextLong());
    }

    private static void requireCoordinates(Args args, int x, int y, int z, String target) {
        if ((int) args.get(0) != x || (int) args.get(1) != y || (int) args.get(2) != z) {
            throw new IllegalStateException("Phase C " + target + " coordinate drifted from ("
                    + x + "," + y + "," + z + ")");
        }
    }

    private StructurePieceInvoker structurePiece() {
        return (StructurePieceInvoker) (Object) this;
    }
}
