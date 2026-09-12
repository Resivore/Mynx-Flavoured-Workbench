package dev.resivore.slotreservations.gametest;

import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.ShulkerContents;
import dev.resivore.slotreservations.ShulkerHostFingerprint;
import dev.resivore.slotreservations.ShulkerPanelActions;
import dev.resivore.slotreservations.ShulkerHostResolver;
import dev.resivore.slotreservations.ShulkerSelectionTracker;
import dev.resivore.slotreservations.network.ReservationActionPayload;
import dev.resivore.slotreservations.network.ShulkerHostLocator;
import dev.resivore.slotreservations.network.ShulkerPanelContentActionPayload;
import dev.resivore.slotreservations.network.ShulkerPanelReservationActionPayload;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ShulkerPanelGameTests implements CustomTestMethodInvoker {
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos position = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(position.getX() + .5, position.getY(), position.getZ() + .5);
        return player;
    }

    private static String fingerprint(ServerPlayer player, int hostSlot) {
        return ShulkerHostFingerprint.of(player.containerMenu.getSlot(hostSlot).getItem(), player.registryAccess());
    }

    private static ShulkerPanelContentActionPayload primaryAction(
            ServerPlayer player, ShulkerHostLocator host, int internalSlot, String fingerprint
    ) {
        return new ShulkerPanelContentActionPayload(player.containerMenu.containerId, host, internalSlot,
                ShulkerPanelContentActionPayload.Click.PRIMARY, fingerprint);
    }

    private static ItemStack namedStone(int count) {
        ItemStack stack = new ItemStack(Items.STONE, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Synchronizer exact stack"));
        return stack;
    }

    private static ItemStack panelHost(ItemStack... contents) {
        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        var slots = net.minecraft.core.NonNullList.withSize(ReservationData.SLOT_COUNT, ItemStack.EMPTY);
        for (int slot = 0; slot < contents.length; slot++) slots.set(slot, contents[slot]);
        ShulkerContents.replace(host, slots);
        return host;
    }

    private static void synchronizeBaseline(AbstractContainerMenu menu, RecordingSynchronizer synchronizer) {
        menu.broadcastChanges();
        synchronizer.clear();
    }

    /** Real 26.2 RemoteSlot comparison semantics for the recording ContainerSynchronizer. */
    private static final class RecordingRemoteSlot implements RemoteSlot {
        private ItemStack stack = ItemStack.EMPTY;

        @Override public void force(ItemStack stack) { this.stack = stack.copy(); }

        @Override public void receive(HashedStack stack) {
            throw new AssertionError("The server synchronizer must not receive a client hash in this test");
        }

        @Override public boolean matches(ItemStack stack) {
            return ItemStack.matches(this.stack, stack);
        }
    }

    /** Captures the exact real menu seam that becomes clientbound in a connected 26.2 player. */
    private static final class RecordingSynchronizer implements ContainerSynchronizer {
        private final List<ItemStack> carriedChanges = new ArrayList<>();
        private final List<ItemStack> fullStateCarries = new ArrayList<>();

        @Override
        public void sendInitialData(AbstractContainerMenu menu, List<ItemStack> slots,
                                    ItemStack carried, int[] dataSlots) {
            fullStateCarries.add(carried.copy());
        }

        @Override public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {}

        @Override public void sendCarriedChange(AbstractContainerMenu menu, ItemStack carried) {
            carriedChanges.add(carried.copy());
        }

        @Override public void sendDataChange(AbstractContainerMenu menu, int property, int value) {}

        @Override public RemoteSlot createSlot() { return new RecordingRemoteSlot(); }

        void clear() {
            carriedChanges.clear();
            fullStateCarries.clear();
        }
    }

    @GameTest(maxTicks = 40)
    public void localFingerprintIsContextFreeComponentExactAndStillBindsTheServerHost(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 2, 1);
        helper.setBlock(position, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(position, ChestBlockEntity.class);
        ServerPlayer player = player(helper);
        player.containerMenu = ChestMenu.threeRows(6, player.getInventory(), chest);

        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        host.set(DataComponents.CUSTOM_NAME, Component.literal("Canonical host"));
        host.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE, 3))));
        String ordinaryContext = ShulkerHostFingerprint.of(host, player.registryAccess());
        String equalClientOrServerCopy = ShulkerHostFingerprint.of(host.copy(), player.registryAccess());
        helper.assertTrue(ordinaryContext.matches("[0-9a-f]{64}"),
                "Local fingerprinting did not produce the SHA-256 contract value");
        helper.assertTrue(ordinaryContext.equals(equalClientOrServerCopy),
                "Equal component-exact stacks did not produce one deterministic client/server fingerprint");

        ItemStack componentChanged = host.copy();
        componentChanged.set(DataComponents.CUSTOM_NAME, Component.literal("Different component"));
        helper.assertTrue(!ordinaryContext.equals(ShulkerHostFingerprint.of(componentChanged, player.registryAccess())),
                "A materially different component did not change the host fingerprint");

        chest.setItem(0, host);
        helper.assertTrue(ShulkerHostResolver.resolve(player, player.containerMenu.containerId,
                        ShulkerHostLocator.menuSlot(0), ordinaryContext).isPresent(),
                "The server rejected its matching component-exact host fingerprint");
        chest.setItem(0, componentChanged);
        helper.assertTrue(ShulkerHostResolver.resolve(player, player.containerMenu.containerId,
                        ShulkerHostLocator.menuSlot(0), ordinaryContext).isEmpty(),
                "The server accepted a stale fingerprint after the host components changed");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void allBoundaryCellsUseExactPhysicalIndicesAndPreserveComponents(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 2, 1);
        helper.setBlock(position, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(position, ChestBlockEntity.class);
        ServerPlayer player = player(helper);
        player.containerMenu = ChestMenu.threeRows(7, player.getInventory(), chest);
        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        host.set(DataComponents.CUSTOM_NAME, Component.literal("Panel host"));
        host.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE, 12))));
        chest.setItem(0, host);

        for (int internal : new int[]{0, 8, 9, 17, 18, 26}) {
            ReservationActionPayload.Source source;
            if (internal == 0) source = ReservationActionPayload.Source.SLOT_STACK;
            else {
                player.containerMenu.setCarried(new ItemStack(Items.DIRT));
                source = ReservationActionPayload.Source.CARRIED_STACK;
            }
            var action = new ShulkerPanelReservationActionPayload(player.containerMenu.containerId,
                    ShulkerHostLocator.menuSlot(0), internal, source, fingerprint(player, 0));
            helper.assertTrue(ShulkerPanelActions.handleReservation(player, action),
                    "Panel reservation rejected boundary cell " + internal);
            helper.assertTrue(ReservationStore.getData(chest.getItem(0)).get(internal).isPresent(),
                    "Panel reservation landed on the wrong internal cell");
        }
        helper.assertTrue(chest.getItem(0).getHoverName().getString().equals("Panel host"),
                "Panel mutation lost the host's custom name");
        helper.assertTrue(ShulkerContents.copy(chest.getItem(0)).get(0).getCount() == 12,
                "Panel reservation mutation changed physical contents");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void panelContentClicksAreServerAuthoritativeAndFingerprintBound(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 2, 1);
        helper.setBlock(position, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(position, ChestBlockEntity.class);
        ServerPlayer player = player(helper);
        player.containerMenu = ChestMenu.threeRows(8, player.getInventory(), chest);
        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        ItemStack named = new ItemStack(Items.STONE, 5);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Exact"));
        host.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(named)));
        chest.setItem(0, host);
        String fingerprint = fingerprint(player, 0);

        var stale = new ShulkerPanelContentActionPayload(player.containerMenu.containerId,
                ShulkerHostLocator.menuSlot(0), 0, ShulkerPanelContentActionPayload.Click.PRIMARY,
                "0".repeat(64));
        helper.assertTrue(!ShulkerPanelActions.handleContent(player, stale), "A stale fingerprint mutated the host");
        helper.assertTrue(player.containerMenu.getCarried().isEmpty(), "Rejected action changed the cursor");

        var half = new ShulkerPanelContentActionPayload(player.containerMenu.containerId,
                ShulkerHostLocator.menuSlot(0), 0, ShulkerPanelContentActionPayload.Click.SECONDARY, fingerprint);
        helper.assertTrue(ShulkerPanelActions.handleContent(player, half), "Secondary extraction was rejected");
        helper.assertTrue(player.containerMenu.getCarried().getCount() == 3,
                "Secondary extraction did not use ceiling-half semantics");
        helper.assertTrue(ShulkerContents.copy(chest.getItem(0)).get(0).getCount() == 2,
                "Secondary extraction changed the wrong physical count");
        helper.assertTrue(player.containerMenu.getCarried().getHoverName().getString().equals("Exact"),
                "Extracted stack lost exact components");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void panelPrimaryActionsSynchronizeTheActualCarriedStackThroughTheMenuSeam(
            GameTestHelper helper
    ) {
        BlockPos position = new BlockPos(1, 2, 1);
        helper.setBlock(position, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(position, ChestBlockEntity.class);
        ServerPlayer player = player(helper);
        player.containerMenu = ChestMenu.threeRows(14, player.getInventory(), chest);
        RecordingSynchronizer synchronizer = new RecordingSynchronizer();
        player.containerMenu.setSynchronizer(synchronizer);
        synchronizer.clear();
        ShulkerHostLocator locator = ShulkerHostLocator.menuSlot(0);

        // A: extracting a component-rich stack produces the exact clientbound carried stack.
        ItemStack exact = namedStone(5);
        chest.setItem(0, panelHost(exact));
        synchronizeBaseline(player.containerMenu, synchronizer);
        helper.assertTrue(ShulkerPanelActions.handleContent(player,
                        primaryAction(player, locator, 0, fingerprint(player, 0))),
                "Primary extraction was rejected");
        helper.assertTrue(ShulkerContents.copy(chest.getItem(0)).get(0).isEmpty()
                        && ItemStack.isSameItemSameComponents(player.containerMenu.getCarried(), exact)
                        && player.containerMenu.getCarried().getCount() == 5,
                "Primary extraction did not establish the exact authoritative carried stack");
        helper.assertTrue(synchronizer.carriedChanges.size() == 1
                        && ItemStack.isSameItemSameComponents(synchronizer.carriedChanges.getFirst(), exact)
                        && synchronizer.carriedChanges.getFirst().getCount() == 5,
                "Primary extraction did not emit the exact clientbound carried-stack update");

        // B: reinsertion clears the authoritative cursor and sends its empty clientbound state.
        synchronizer.clear();
        helper.assertTrue(ShulkerPanelActions.handleContent(player,
                        primaryAction(player, locator, 0, fingerprint(player, 0))),
                "Primary reinsertion was rejected");
        helper.assertTrue(ShulkerContents.copy(chest.getItem(0)).get(0).getCount() == 5
                        && player.containerMenu.getCarried().isEmpty()
                        && synchronizer.carriedChanges.size() == 1
                        && synchronizer.carriedChanges.getFirst().isEmpty(),
                "Primary reinsertion did not synchronize the empty cursor");

        // C: an exact capacity remainder is both authoritative and clientbound.
        chest.setItem(0, panelHost(namedStone(63)));
        player.containerMenu.setCarried(namedStone(5));
        synchronizeBaseline(player.containerMenu, synchronizer);
        helper.assertTrue(ShulkerPanelActions.handleContent(player,
                        primaryAction(player, locator, 0, fingerprint(player, 0))),
                "Partial primary insertion was rejected");
        helper.assertTrue(ShulkerContents.copy(chest.getItem(0)).get(0).getCount() == 64
                        && player.containerMenu.getCarried().getCount() == 4
                        && synchronizer.carriedChanges.size() == 1
                        && synchronizer.carriedChanges.getFirst().getCount() == 4
                        && ItemStack.isSameItemSameComponents(synchronizer.carriedChanges.getFirst(), namedStone(4)),
                "Partial primary insertion did not synchronize its exact component-rich remainder");

        // D: a reservation mismatch is a no-op for both the host and the carried synchronization.
        ItemStack mismatchedHost = panelHost(ItemStack.EMPTY);
        ReservationStore.setData(mismatchedHost, ReservationData.EMPTY.with(0, new ItemStack(Items.DIRT)));
        chest.setItem(0, mismatchedHost);
        player.containerMenu.setCarried(namedStone(3));
        synchronizeBaseline(player.containerMenu, synchronizer);
        ItemStack beforeRejectedHost = chest.getItem(0).copy();
        helper.assertTrue(!ShulkerPanelActions.handleContent(player,
                        primaryAction(player, locator, 0, fingerprint(player, 0))),
                "Reservation-mismatched primary insertion was admitted");
        helper.assertTrue(ItemStack.isSameItemSameComponents(chest.getItem(0), beforeRejectedHost)
                        && player.containerMenu.getCarried().getCount() == 3
                        && synchronizer.carriedChanges.isEmpty(),
                "Rejected insertion mutated a host/cursor or emitted a bogus carried update");

        // E: the second click returns the item only through the first click's genuine carried state.
        chest.setItem(0, panelHost(namedStone(5)));
        player.containerMenu.setCarried(ItemStack.EMPTY);
        synchronizeBaseline(player.containerMenu, synchronizer);
        helper.assertTrue(ShulkerPanelActions.handleContent(player,
                        primaryAction(player, locator, 0, fingerprint(player, 0))),
                "Repeated-click first extraction was rejected");
        helper.assertTrue(!synchronizer.carriedChanges.isEmpty()
                        && !synchronizer.carriedChanges.getFirst().isEmpty(),
                "Repeated-click first extraction did not send a non-empty client cursor");
        synchronizer.clear();
        helper.assertTrue(ShulkerPanelActions.handleContent(player,
                        primaryAction(player, locator, 0, fingerprint(player, 0))),
                "Repeated-click second insertion was rejected");
        helper.assertTrue(ShulkerContents.copy(chest.getItem(0)).get(0).getCount() == 5
                        && player.containerMenu.getCarried().isEmpty()
                        && synchronizer.carriedChanges.size() == 1
                        && synchronizer.carriedChanges.getFirst().isEmpty(),
                "Repeated-click regression returned an item without synchronizing its actual cursor lifecycle");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void creativeInventoryPanelActionsUseTheNativeFullContentCursorSynchronization(
            GameTestHelper helper
    ) {
        ServerPlayer player = player(helper);
        player.getAbilities().instabuild = true;
        player.containerMenu = player.inventoryMenu;
        int physicalPlayerSlot = 9;
        Slot hostSlot = player.containerMenu.slots.stream()
                .filter(slot -> slot.container == player.getInventory()
                        && slot.getContainerSlot() == physicalPlayerSlot)
                .findFirst().orElseThrow();
        int visibleMenuSlot = player.containerMenu.slots.indexOf(hostSlot);
        player.getInventory().setItem(physicalPlayerSlot, panelHost(namedStone(5)));
        RecordingSynchronizer synchronizer = new RecordingSynchronizer();
        player.containerMenu.setSynchronizer(synchronizer);
        synchronizer.clear();

        ShulkerHostLocator locator = ShulkerHostLocator.playerInventory(visibleMenuSlot, physicalPlayerSlot);
        helper.assertTrue(ShulkerPanelActions.handleContent(player,
                        primaryAction(player, locator, 0,
                                ShulkerHostFingerprint.of(hostSlot.getItem(), player.registryAccess()))),
                "Creative inventory panel extraction was rejected");
        helper.assertTrue(player.containerMenu.getCarried().getCount() == 5
                        && synchronizer.carriedChanges.isEmpty()
                        && synchronizer.fullStateCarries.size() == 1
                        && synchronizer.fullStateCarries.getFirst().getCount() == 5
                        && ItemStack.isSameItemSameComponents(synchronizer.fullStateCarries.getFirst(), namedStone(5)),
                "Creative inventory extraction did not use the full native menu state carrying its real cursor");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void panelRejectsReplacementMulticountNestingAndOutOfRangeRequests(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 2, 1);
        helper.setBlock(position, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(position, ChestBlockEntity.class);
        ServerPlayer player = player(helper);
        player.containerMenu = ChestMenu.threeRows(9, player.getInventory(), chest);
        chest.setItem(0, new ItemStack(Blocks.SHULKER_BOX));
        String original = fingerprint(player, 0);
        chest.getItem(0).setCount(2);
        player.containerMenu.setCarried(new ItemStack(Items.STONE));
        var multicount = new ShulkerPanelContentActionPayload(player.containerMenu.containerId,
                ShulkerHostLocator.menuSlot(0), 0, ShulkerPanelContentActionPayload.Click.PRIMARY, original);
        helper.assertTrue(!ShulkerPanelActions.handleContent(player, multicount), "A multi-count host was admitted");

        chest.setItem(0, new ItemStack(Blocks.SHULKER_BOX));
        String fresh = fingerprint(player, 0);
        player.containerMenu.setCarried(new ItemStack(Blocks.SHULKER_BOX));
        var nesting = new ShulkerPanelContentActionPayload(player.containerMenu.containerId,
                ShulkerHostLocator.menuSlot(0), 26, ShulkerPanelContentActionPayload.Click.PRIMARY, fresh);
        helper.assertTrue(!ShulkerPanelActions.handleContent(player, nesting), "Shulker nesting was admitted");
        var outOfRange = new ShulkerPanelContentActionPayload(player.containerMenu.containerId,
                ShulkerHostLocator.menuSlot(0), 27, ShulkerPanelContentActionPayload.Click.PRIMARY, fresh);
        helper.assertTrue(!ShulkerPanelActions.handleContent(player, outOfRange), "Out-of-range cell was admitted");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nativeSecondaryClickSeamOwnsBothQuickInsertionDirectionsExactlyOnce(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 2, 1);
        helper.setBlock(position, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(position, ChestBlockEntity.class);
        ServerPlayer player = player(helper);
        player.containerMenu = ChestMenu.threeRows(10, player.getInventory(), chest);

        chest.setItem(0, new ItemStack(Items.STONE, 10));
        player.containerMenu.setCarried(new ItemStack(Blocks.SHULKER_BOX));
        player.containerMenu.clicked(0, 1, ContainerInput.PICKUP, player);
        helper.assertTrue(chest.getItem(0).isEmpty(), "Cursor-shulker insertion left an unexpected remainder");
        helper.assertTrue(ShulkerContents.copy(player.containerMenu.getCarried()).get(0).getCount() == 10,
                "One secondary click did not produce exactly one conserved insertion");

        chest.setItem(1, new ItemStack(Blocks.SHULKER_BOX));
        player.containerMenu.setCarried(new ItemStack(Items.DIRT, 7));
        player.containerMenu.clicked(1, 1, ContainerInput.PICKUP, player);
        helper.assertTrue(player.containerMenu.getCarried().isEmpty(), "Item-cursor insertion left a remainder");
        helper.assertTrue(ShulkerContents.copy(chest.getItem(1)).get(0).getCount() == 7,
                "The opposite insertion direction was not owned exactly once");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void exactSelectionMigratesFromHostSlotToCursorAndExtractsOnlyThatCell(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 2, 1);
        helper.setBlock(position, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(position, ChestBlockEntity.class);
        ServerPlayer player = player(helper);
        player.containerMenu = ChestMenu.threeRows(11, player.getInventory(), chest);
        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        var contents = net.minecraft.core.NonNullList.withSize(ReservationData.SLOT_COUNT, ItemStack.EMPTY);
        contents.set(5, new ItemStack(Items.DIAMOND, 4));
        contents.set(20, new ItemStack(Items.DIRT, 2));
        ShulkerContents.replace(host, contents);
        ReservationStore.setData(host, ReservationData.EMPTY.with(5, new ItemStack(Items.DIAMOND)));
        chest.setItem(0, host);

        ShulkerHostResolver.ResolvedHost resolved = ShulkerHostResolver
                .resolveMenuSlot(player, player.containerMenu, player.containerMenu.getSlot(0)).orElseThrow();
        String fingerprint = fingerprint(player, 0);
        helper.assertTrue(ShulkerSelectionTracker.select(player, resolved,
                ShulkerHostLocator.menuSlot(0), 5, fingerprint), "Valid selection was rejected");
        player.containerMenu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(ShulkerSelectionTracker.validate(player) != null
                        && ShulkerSelectionTracker.validate(player).kind()
                        == ShulkerSelectionTracker.HostKind.CARRIED_CURSOR,
                "Selection did not follow the exact host slot-to-cursor transaction");

        // Slot 27 is the first real player-inventory slot in a three-row chest menu.
        player.containerMenu.clicked(27, 1, ContainerInput.PICKUP, player);
        helper.assertTrue(player.containerMenu.getSlot(27).getItem().getCount() == 4
                        && player.containerMenu.getSlot(27).getItem().is(Items.DIAMOND),
                "Selected physical stack did not enter the exact empty player-inventory target slot");
        helper.assertTrue(ShulkerContents.copy(player.containerMenu.getCarried()).get(5).isEmpty()
                        && ShulkerContents.copy(player.containerMenu.getCarried()).get(20).getCount() == 2,
                "Extraction changed an unselected internal slot");
        helper.assertTrue(ReservationStore.getData(player.containerMenu.getCarried()).matches(
                        5, new ItemStack(Items.DIAMOND)),
                "Full extraction cleared the selected slot's reservation");
        helper.assertTrue(ShulkerSelectionTracker.validate(player) != null
                        && ShulkerSelectionTracker.validate(player).internalSlot() == 20,
                "A fully removed selected stack did not advance to the next occupied cell");

        player.containerMenu.clicked(28, 1, ContainerInput.PICKUP, player);
        helper.assertTrue(player.containerMenu.getSlot(28).getItem().getCount() == 2
                        && player.containerMenu.getSlot(28).getItem().is(Items.DIRT)
                        && ShulkerContents.copy(player.containerMenu.getCarried()).get(20).isEmpty()
                        && ShulkerSelectionTracker.get(player).isEmpty(),
                "Removing the final occupied cell did not clear carried selection exactly");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nativeCarriedExtractionDefaultsBackwardsAndRespectsCapacityAndReservations(
            GameTestHelper helper
    ) {
        BlockPos position = new BlockPos(1, 2, 1);
        helper.setBlock(position, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(position, ChestBlockEntity.class);
        ServerPlayer player = player(helper);
        player.containerMenu = ChestMenu.threeRows(12, player.getInventory(), chest);

        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        var contents = net.minecraft.core.NonNullList.withSize(ReservationData.SLOT_COUNT, ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.STONE, 2));
        contents.set(8, new ItemStack(Items.DIAMOND, 6));
        contents.set(20, new ItemStack(Items.DIRT, 7));
        ShulkerContents.replace(host, contents);
        chest.setItem(0, host);
        var resolved = ShulkerHostResolver.resolveMenuSlot(player, player.containerMenu,
                player.containerMenu.getSlot(0)).orElseThrow();
        helper.assertTrue(ShulkerSelectionTracker.select(player, resolved,
                ShulkerHostLocator.menuSlot(0), 0, fingerprint(player, 0)), "Selection setup was rejected");
        player.containerMenu.clicked(0, 0, ContainerInput.PICKUP, player);
        // Model the packet-order edge: the native click is authoritative even if no carried record survived.
        ShulkerSelectionTracker.clear(player);

        Slot limitedTarget = new Slot(player.getInventory(), 9, 0, 0) {
            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 3;
            }
        };
        player.containerMenu.slots.set(27, limitedTarget);
        player.containerMenu.clicked(27, 1, ContainerInput.PICKUP, player);
        helper.assertTrue(player.containerMenu.getSlot(27).getItem().getCount() == 3
                        && player.containerMenu.getSlot(27).getItem().is(Items.DIRT)
                        && ShulkerContents.copy(player.containerMenu.getCarried()).get(20).getCount() == 4
                        && ShulkerSelectionTracker.validate(player) != null
                        && ShulkerSelectionTracker.validate(player).internalSlot() == 20,
                "Backmost default extraction did not preserve its capacity-limited remainder");

        ReservationStore.setData(chest, ReservationData.EMPTY.with(2, new ItemStack(Items.STONE)));
        player.containerMenu.clicked(2, 1, ContainerInput.PICKUP, player);
        helper.assertTrue(chest.getItem(2).isEmpty()
                        && ShulkerContents.copy(player.containerMenu.getCarried()).get(20).getCount() == 4,
                "A reservation-mismatched native extraction mutated either source or target");

        player.containerMenu.clicked(28, 1, ContainerInput.PICKUP, player);
        helper.assertTrue(player.containerMenu.getSlot(28).getItem().getCount() == 4
                        && player.containerMenu.getSlot(28).getItem().is(Items.DIRT)
                        && ShulkerContents.copy(player.containerMenu.getCarried()).get(20).isEmpty()
                        && ShulkerSelectionTracker.validate(player).internalSlot() == 8,
                "After the backmost stack is empty, the next default must be the previous occupied gap-aware cell");

        player.containerMenu.clicked(29, 1, ContainerInput.PICKUP, player);
        helper.assertTrue(player.containerMenu.getSlot(29).getItem().getCount() == 6
                        && player.containerMenu.getSlot(29).getItem().is(Items.DIAMOND)
                        && ShulkerContents.copy(player.containerMenu.getCarried()).get(8).isEmpty()
                        && ShulkerSelectionTracker.validate(player).internalSlot() == 0,
                "Repeated default extraction must continue backward through occupied internal cells");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void panelQuickMoveUsesLivePlayerSlotsAndConservesExactRemainders(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 2, 1);
        helper.setBlock(position, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(position, ChestBlockEntity.class);
        ServerPlayer player = player(helper);
        player.containerMenu = ChestMenu.threeRows(13, player.getInventory(), chest);

        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        var contents = net.minecraft.core.NonNullList.withSize(ReservationData.SLOT_COUNT, ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.STONE, 5));
        ShulkerContents.replace(host, contents);
        ReservationStore.setData(host, ReservationData.EMPTY.with(0, new ItemStack(Items.DIRT)));
        chest.setItem(0, host);
        Slot firstPlayerSlot = player.containerMenu.getSlot(27);
        var full = new ShulkerPanelContentActionPayload(player.containerMenu.containerId,
                ShulkerHostLocator.menuSlot(0), 0, ShulkerPanelContentActionPayload.Click.QUICK_MOVE,
                fingerprint(player, 0));
        helper.assertTrue(ShulkerPanelActions.handleContent(player, full), "Full quick move was rejected");
        helper.assertTrue(firstPlayerSlot.getItem().is(Items.STONE) && firstPlayerSlot.getItem().getCount() == 5,
                "Quick move did not use the live player-inventory menu slot");
        helper.assertTrue(ShulkerContents.copy(chest.getItem(0)).get(0).isEmpty(),
                "Full quick move retained source contents");
        helper.assertTrue(ReservationStore.getData(chest.getItem(0)).matches(0, new ItemStack(Items.DIRT)),
                "Quick move cleared the source reservation");

        for (Slot slot : player.containerMenu.slots) {
            if (slot.container == player.getInventory()) slot.set(new ItemStack(Items.DIAMOND, 64));
        }
        firstPlayerSlot.set(new ItemStack(Items.STONE, 62));
        host = new ItemStack(Blocks.SHULKER_BOX);
        contents = net.minecraft.core.NonNullList.withSize(ReservationData.SLOT_COUNT, ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.STONE, 5));
        ShulkerContents.replace(host, contents);
        chest.setItem(0, host);
        int before = firstPlayerSlot.getItem().getCount() + ShulkerContents.copy(host).get(0).getCount();
        var partial = new ShulkerPanelContentActionPayload(player.containerMenu.containerId,
                ShulkerHostLocator.menuSlot(0), 0, ShulkerPanelContentActionPayload.Click.QUICK_MOVE,
                fingerprint(player, 0));
        helper.assertTrue(ShulkerPanelActions.handleContent(player, partial), "Partial quick move was rejected");
        int remainder = ShulkerContents.copy(chest.getItem(0)).get(0).getCount();
        helper.assertTrue(firstPlayerSlot.getItem().getCount() == 64 && remainder == 3,
                "Partial quick move did not retain the exact source remainder");
        helper.assertTrue(before == firstPlayerSlot.getItem().getCount() + remainder,
                "Partial quick move did not conserve the total item count");

        host = new ItemStack(Blocks.SHULKER_BOX);
        contents = net.minecraft.core.NonNullList.withSize(ReservationData.SLOT_COUNT, ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.STONE, 5));
        ShulkerContents.replace(host, contents);
        chest.setItem(0, host);
        var blocked = new ShulkerPanelContentActionPayload(player.containerMenu.containerId,
                ShulkerHostLocator.menuSlot(0), 0, ShulkerPanelContentActionPayload.Click.QUICK_MOVE,
                fingerprint(player, 0));
        helper.assertTrue(!ShulkerPanelActions.handleContent(player, blocked),
                "Full player inventory mutated a panel source");
        helper.assertTrue(ShulkerContents.copy(chest.getItem(0)).get(0).getCount() == 5,
                "No-op quick move changed source contents");
        helper.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
