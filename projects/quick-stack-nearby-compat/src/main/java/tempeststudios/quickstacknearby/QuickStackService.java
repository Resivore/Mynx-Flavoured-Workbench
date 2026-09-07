package tempeststudios.quickstacknearby;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class QuickStackService {
    private QuickStackService() {
    }

    public static QuickStackMoveEngine.Result quickStack(ServerPlayer player) {
        return quickStack(player, QuickStackMoveEngine.SourceRules.EMPTY);
    }

    public static QuickStackMoveEngine.Result quickStack(
            ServerPlayer player,
            QuickStackMoveEngine.SourceRules sourceRules
    ) {
        List<QuickStackMoveEngine.Target> targets = nearbyTargets(player);
        QuickStackMoveEngine.Result result = QuickStackMoveEngine.moveMatchingItems(
                player.getInventory(),
                Inventory.getSelectionSize(),
                Inventory.INVENTORY_SIZE,
                targets,
                sourceRules
        );

        if (result.itemsMoved() > 0) {
            player.containerMenu.broadcastChanges();
        }
        PlayerFeedbackCompat.displayClientMessage(player, messageFor(result, targets.size()));
        return result;
    }

    private static List<QuickStackMoveEngine.Target> nearbyTargets(ServerPlayer player) {
        ServerLevel level = ServerPlayerCompat.serverLevel(player);
        BlockPos center = player.blockPosition();
        List<ScannedContainer> scannedContainers = new ArrayList<>();
        Set<BlockPos> scannedPositions = new HashSet<>();
        QuickStackServerConfig config = QuickStackServerConfig.getInstance();
        int horizontalRadius = config.horizontalRadius();
        int verticalRadius = config.verticalRadius();

        BlockPos min = center.offset(-horizontalRadius, -verticalRadius, -horizontalRadius);
        BlockPos max = center.offset(horizontalRadius, verticalRadius, horizontalRadius);
        for (BlockPos candidate : BlockPos.betweenClosed(min, max)) {
            BlockPos position = candidate.immutable();
            if (!scannedPositions.add(position)) {
                continue;
            }
            if (!level.isLoaded(position)) {
                continue;
            }

            ScannedContainer scannedContainer = scanContainer(level, player, center, position);
            if (scannedContainer == null) {
                continue;
            }
            scannedPositions.addAll(scannedContainer.positions());
            if (!scannedContainer.acceptedTypes().isEmpty()) {
                scannedContainers.add(scannedContainer);
            }
        }

        scannedContainers.sort(Comparator.comparingDouble(ScannedContainer::distance));

        List<QuickStackMoveEngine.Target> targets = new ArrayList<>(scannedContainers.size());
        for (ScannedContainer scannedContainer : scannedContainers) {
            targets.add(new QuickStackMoveEngine.Target(scannedContainer.container(), scannedContainer.acceptedTypes()));
        }
        return targets;
    }

    private static ScannedContainer scanContainer(
            ServerLevel level,
            ServerPlayer player,
            BlockPos center,
            BlockPos position
    ) {
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (!(blockEntity instanceof Container container)) {
            return null;
        }

        BlockState state = level.getBlockState(position);
        List<BlockPos> positions = List.of(position);
        if (state.getBlock() instanceof ChestBlock chestBlock && state.hasProperty(ChestBlock.TYPE)) {
            ChestType chestType = state.getValue(ChestBlock.TYPE);
            if (chestType != ChestType.SINGLE) {
                BlockPos connectedPosition = position.relative(ChestBlock.getConnectedDirection(state)).immutable();
                if (level.isLoaded(connectedPosition)) {
                    BlockState connectedState = level.getBlockState(connectedPosition);
                    if (connectedState.getBlock() == state.getBlock()
                            && connectedState.hasProperty(ChestBlock.TYPE)
                            && connectedState.getValue(ChestBlock.TYPE) == chestType.getOpposite()) {
                        Container combinedContainer = ChestBlock.getContainer(chestBlock, state, level, position, false);
                        if (combinedContainer == null) {
                            return null;
                        }
                        container = combinedContainer;
                        positions = sortedPositions(position, connectedPosition);
                    }
                }
            }
        }

        if (!canUseContainer(container, level, positions, player)) {
            return null;
        }

        Set<QuickStackMoveEngine.StackKey> acceptedTypes = QuickStackMoveEngine.acceptedTypes(container);
        return new ScannedContainer(container, acceptedTypes, positions, distanceToContainer(center, positions));
    }

    private static boolean canUseContainer(
            Container container,
            ServerLevel level,
            List<BlockPos> positions,
            ServerPlayer player
    ) {
        if (container instanceof BaseContainerBlockEntity baseContainer && !baseContainer.canOpen(player)) {
            return false;
        }
        if (!container.stillValid(player)) {
            return false;
        }
        for (BlockPos position : positions) {
            if (!player.mayInteract(level, position)) {
                return false;
            }
        }
        return true;
    }

    private static List<BlockPos> sortedPositions(BlockPos first, BlockPos second) {
        if (Comparator
                .comparingLong((BlockPos position) -> position.getX())
                .thenComparingLong(position -> position.getY())
                .thenComparingLong(position -> position.getZ())
                .compare(first, second) <= 0) {
            return List.of(first, second);
        }
        return List.of(second, first);
    }

    private static double distanceToContainer(BlockPos center, List<BlockPos> positions) {
        double distance = Double.MAX_VALUE;
        for (BlockPos position : positions) {
            distance = Math.min(distance, center.distSqr(position));
        }
        return distance;
    }

    private static Component messageFor(QuickStackMoveEngine.Result result, int targetCount) {
        if (targetCount == 0) {
            return Component.literal("No nearby containers with matching item stacks.");
        }
        if (result.itemsMoved() == 0) {
            return Component.literal("No matching inventory items to quick stack.");
        }

        String itemLabel = result.itemsMoved() == 1 ? "item" : "items";
        String containerLabel = result.targetContainersTouched() == 1 ? "container" : "containers";
        return Component.literal("Quick stacked "
                + result.itemsMoved()
                + " "
                + itemLabel
                + " into "
                + result.targetContainersTouched()
                + " "
                + containerLabel
                + ".");
    }

    private record ScannedContainer(
            Container container,
            Set<QuickStackMoveEngine.StackKey> acceptedTypes,
            List<BlockPos> positions,
            double distance
    ) {
    }
}
