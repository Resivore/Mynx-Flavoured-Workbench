package dev.resivore.carryonpatch.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import org.chermew.grabandgo.duck.GrabCarrier;

/** Server-thread restoration from the full authoritative NBT, never the render cache. */
public final class CarryPlacement {
    private CarryPlacement() {}

    public static InteractionResult place(Player player, Level level, BlockPos pos, CompoundTag requested) {
        GrabCarrier carrier = (GrabCarrier) player;
        if (!carrier.grabandgo$isCarrying()) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        CarryState state = ((CarryStateAccess) player).carryOnPatch$state();
        CompoundTag data = carrier.grabandgo$getCarriedData();
        if (!data.equals(requested) || !CarryState.validShape(data) || !state.begin()) {
            CarryPersistence.warn(player, "Carry data cannot be placed safely; the original payload is retained.");
            return InteractionResult.FAIL;
        }
        boolean mutationStarted = false;
        boolean decoding = false;
        try {
            if (!level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)
                    || !level.getBlockState(pos).canBeReplaced()) {
                state.retryable();
                return InteractionResult.FAIL;
            }
            if (data.getStringOr("Type", "").equals("entity")) {
                CompoundTag entityData = data.getCompoundOrEmpty("EntityData").copy();
                String type = data.getStringOr("EntityTypeId", "");
                if (!type.equals(entityData.getStringOr("id", ""))) throw new IllegalArgumentException("Entity identity mismatch");
                validateEntities(entityData);
                ProblemReporter.Collector problems = new ProblemReporter.Collector();
                List<Entity> entities = new ArrayList<>();
                decoding = true;
                Entity root = EntityType.loadEntityRecursive(
                        TagValueInput.create(problems, level.registryAccess(), entityData), level,
                        new EntitySpawnRequest(EntitySpawnReason.LOAD, false), entity -> {
                            entities.add(entity); return entity;
                        });
                if (root == null || !problems.isEmpty() || entities.size() != countEntities(entityData))
                    throw new IllegalArgumentException("Incomplete entity decoding: " + problems.getReport());
                decoding = false;
                HashSet<UUID> ids = new HashSet<>();
                for (Entity entity : entities) {
                    if (!ids.add(entity.getUUID()) || level.getEntityInAnyDimension(entity.getUUID()) != null)
                        throw new IllegalArgumentException("Entity UUID conflict");
                    entity.setPos(pos.getX() + .5, pos.getY() + .05, pos.getZ() + .5);
                    if (!level.noCollision(entity)) throw new IllegalArgumentException("Entity placement blocked");
                }
                mutationStarted = true;
                // If any addition is rejected, remove only instances introduced by this attempt.
                // An exception with uncertain side effects locks retry durably instead.
                for (Entity entity : entities) {
                    if (!level.addFreshEntity(entity)) {
                        for (Entity added : entities) {
                            if (level.getEntity(added.getUUID()) == added) added.discard();
                        }
                        boolean residue = entities.stream().anyMatch(e -> level.getEntity(e.getUUID()) == e);
                        if (residue) state.uncertain(); else state.retryable();
                        CarryPersistence.warn(player, "Entity spawn rejected; payload retained. Any incomplete removal blocks retries.");
                        return InteractionResult.FAIL;
                    }
                }
            } else {
                String id = data.getStringOr("BlockId", "");
                Identifier key = Identifier.tryParse(id);
                if (key == null || !BuiltInRegistries.BLOCK.containsKey(key))
                    throw new IllegalArgumentException("Unavailable block identifier");
                CompoundTag savedState = data.getCompoundOrEmpty("BlockState");
                if (!id.equals(savedState.getStringOr("Name", ""))) throw new IllegalArgumentException("Block identity mismatch");
                var block = BuiltInRegistries.BLOCK.getValue(key);
                var blockState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, savedState);
                // Reject invalid property decoding instead of accepting NbtUtils' default fallback.
                if (!NbtUtils.writeBlockState(blockState).equals(savedState))
                    throw new IllegalArgumentException("Block state could not be decoded exactly");
                if (!(block instanceof EntityBlock entityBlock)) throw new IllegalArgumentException("Unsupported carried block");
                if (blockState.hasProperty(ChestBlock.FACING))
                    blockState = blockState.setValue(ChestBlock.FACING, player.getDirection().getOpposite());
                else if (blockState.hasProperty(BarrelBlock.FACING))
                    blockState = blockState.setValue(BarrelBlock.FACING, Direction.UP);
                CompoundTag payload = data.getCompoundOrEmpty("BlockEntityData").copy();
                Identifier blockEntityId = Identifier.tryParse(payload.getStringOr("id", ""));
                BlockEntity restored = entityBlock.newBlockEntity(pos, blockState);
                if (restored == null || blockEntityId == null
                        || !BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(blockEntityId)
                        || BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(blockEntityId) != restored.getType())
                    throw new IllegalArgumentException("Unavailable or mismatched block entity");
                payload.putInt("x", pos.getX()); payload.putInt("y", pos.getY()); payload.putInt("z", pos.getZ());
                ProblemReporter.Collector problems = new ProblemReporter.Collector();
                // Decode off-world, before any placement side effects or container inventory exists.
                decoding = true;
                restored.loadWithComponents(TagValueInput.create(problems, level.registryAccess(), payload));
                if (!problems.isEmpty()) throw new IllegalArgumentException("Block entity decoding failed: " + problems.getReport());
                decoding = false;
                var before = level.getBlockState(pos);
                mutationStarted = true;
                if (!level.setBlock(pos, blockState, 3)) {
                    if (level.getBlockState(pos).equals(before)) state.retryable(); else state.uncertain();
                    CarryPersistence.warn(player, "Block placement rejected; carried contents retained.");
                    return InteractionResult.FAIL;
                }
                level.setBlockEntity(restored);
                if (level.getBlockEntity(pos) != restored) throw new IllegalStateException("Block entity installation rejected");
                restored.setChanged();
                level.sendBlockUpdated(pos, before, blockState, 3);
            }
            // Only verified completion can release the guard and consume the synchronized payload.
            state.consumed();
            carrier.grabandgo$clearCarried();
            try {
                boolean mob = data.getStringOr("Type", "").equals("entity");
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                        mob ? net.minecraft.sounds.SoundEvents.CHICKEN_EGG
                                : level.getBlockState(pos).getSoundType().getPlaceSound(),
                        mob ? net.minecraft.sounds.SoundSource.NEUTRAL : net.minecraft.sounds.SoundSource.BLOCKS,
                        1.0f, mob ? 0.7f : 1.0f);
            } catch (Exception soundFailure) {
                // Cosmetic failure after restoration must not turn consumption into a retry.
                CarryPersistence.warn(player, "Object restored successfully, but its placement sound failed.");
            }
            return InteractionResult.SUCCESS;
        } catch (Exception failure) {
            if (mutationStarted || decoding) state.uncertain(); else state.retryable();
            CarryPersistence.warn(player, "Placement failed (" + failure.getClass().getSimpleName()
                    + ": " + failure.getMessage() + "). Payload retained."
                    + (mutationStarted ? " Partial world effects are possible; retries are blocked pending recovery."
                    : decoding ? " Decoding was incomplete; retries are blocked pending recovery." : " Retry after correcting the cause."));
            return InteractionResult.FAIL;
        }
    }

    private static void validateEntities(CompoundTag data) {
        Identifier id = Identifier.tryParse(data.getStringOr("id", ""));
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id))
            throw new IllegalArgumentException("Unavailable entity identifier");
        // Saved UUIDs are required: silently allocating a fresh one makes retries unsafe.
        if (data.getIntArray("UUID").filter(a -> a.length == 4).isEmpty())
            throw new IllegalArgumentException("Missing or invalid saved entity UUID");
        if (data.contains("Passengers")) {
            var passengers = data.getList("Passengers").orElseThrow();
            for (var child : passengers) validateEntities(child.asCompound().orElseThrow());
        }
    }

    private static int countEntities(CompoundTag data) {
        int count = 1;
        for (var child : data.getListOrEmpty("Passengers")) count += countEntities(child.asCompound().orElseThrow());
        return count;
    }
}
