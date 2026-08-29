package dev.resivore.inventorycrafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Dependency-free model of the audited menu contract.
 *
 * <p>This deliberately models slot ownership and transitions rather than any
 * Minecraft implementation class. The Canary implementation can change its
 * mixin mechanics without weakening the externally observable contract.</p>
 */
final class InventoryCraftingContractFixture {
    static final int RESULT_MENU_ID = 0;
    static final int ORDINARY_START = 9;
    static final int STORAGE_END_EXCLUSIVE = 63;
    static final int HOTBAR_END_EXCLUSIVE = 72;
    static final int OFFHAND_MENU_ID = 72;
    static final int TRASH_MENU_ID = 73;
    static final int APPENDED_CRAFT_START = 74;
    static final int PRE_TRINKETS_END_EXCLUSIVE = 79;
    static final int CRAFTING_ENTITY_SLOT_START = 500;
    static final int CRAFTING_ENTITY_SLOT_END_EXCLUSIVE = 509;
    static final int NON_EQUIPMENT_SIZE = 63;

    private InventoryCraftingContractFixture() {
    }

    enum Side {
        CLIENT,
        SERVER
    }

    enum Owner {
        RESULT,
        CRAFT,
        PLAYER,
        TRINKET
    }

    enum Role {
        RESULT,
        CRAFT_INPUT,
        ARMOR,
        ORDINARY_STORAGE,
        HOTBAR,
        OFFHAND,
        TRASH,
        TRINKET
    }

    static final class SlotDescriptor {
        private final int index;
        private final Object container;
        private final Owner owner;
        private final Role role;
        private final int containerSlot;

        SlotDescriptor(int index, Object container, Owner owner, Role role, int containerSlot) {
            this.index = index;
            this.container = Objects.requireNonNull(container);
            this.owner = Objects.requireNonNull(owner);
            this.role = Objects.requireNonNull(role);
            this.containerSlot = containerSlot;
        }

        int index() {
            return index;
        }

        Object container() {
            return container;
        }

        Owner owner() {
            return owner;
        }

        Role role() {
            return role;
        }

        int containerSlot() {
            return containerSlot;
        }

        String stableSignature() {
            return index + ":" + owner + ":" + role + ":" + containerSlot;
        }
    }

    static final class TargetInventoryMenu {
        private static final int[] LEGACY_LOGICAL_CELLS = {0, 1, 3, 4};
        private static final int[] APPENDED_LOGICAL_CELLS = {2, 5, 6, 7, 8};

        private final Side side;
        private final Object resultContainer = new Object();
        private final Object craftingContainer = new Object();
        private final Object playerInventory = new Object();
        private final Object trinketContainer = new Object();
        private final List<SlotDescriptor> slots = new ArrayList<>();
        private final List<String> lastSlots = new ArrayList<>();
        private final List<String> remoteSlots = new ArrayList<>();
        private final TransientGrid craftingGrid = new TransientGrid();
        private int trinketSlotStart;

        private TargetInventoryMenu(Side side, int trinketCount) {
            this.side = Objects.requireNonNull(side);
            if (trinketCount < 0) {
                throw new IllegalArgumentException("trinketCount must be non-negative");
            }

            addSlot(resultContainer, Owner.RESULT, Role.RESULT, 0);
            for (int logicalCell : LEGACY_LOGICAL_CELLS) {
                addSlot(craftingContainer, Owner.CRAFT, Role.CRAFT_INPUT, logicalCell);
            }

            // Inventory Extended's accepted equipment and ordinary prefix.
            int[] armorBackingSlots = {66, 65, 64, 63};
            for (int backingSlot : armorBackingSlots) {
                addSlot(playerInventory, Owner.PLAYER, Role.ARMOR, backingSlot);
            }
            for (int backingSlot = 9; backingSlot < NON_EQUIPMENT_SIZE; backingSlot++) {
                addSlot(playerInventory, Owner.PLAYER, Role.ORDINARY_STORAGE, backingSlot);
            }
            for (int backingSlot = 0; backingSlot < 9; backingSlot++) {
                addSlot(playerInventory, Owner.PLAYER, Role.HOTBAR, backingSlot);
            }
            addSlot(playerInventory, Owner.PLAYER, Role.OFFHAND, 67);
            addSlot(playerInventory, Owner.PLAYER, Role.TRASH, 68);

            for (int logicalCell : APPENDED_LOGICAL_CELLS) {
                addSlot(craftingContainer, Owner.CRAFT, Role.CRAFT_INPUT, logicalCell);
            }

            if (slots.size() != PRE_TRINKETS_END_EXCLUSIVE) {
                throw new IllegalStateException("static prefix drifted to " + slots.size());
            }
            trinketSlotStart = slots.size();
            appendTrinkets(trinketCount);
            assertAligned();
        }

        static TargetInventoryMenu create(Side side, int trinketCount) {
            return new TargetInventoryMenu(side, trinketCount);
        }

        Side side() {
            return side;
        }

        Object craftingContainer() {
            return craftingContainer;
        }

        Object playerInventory() {
            return playerInventory;
        }

        SlotDescriptor slot(int menuId) {
            return slots.get(menuId);
        }

        List<SlotDescriptor> slots() {
            return Collections.unmodifiableList(slots);
        }

        List<String> lastSlots() {
            return Collections.unmodifiableList(lastSlots);
        }

        List<String> remoteSlots() {
            return Collections.unmodifiableList(remoteSlots);
        }

        int trinketSlotStart() {
            return trinketSlotStart;
        }

        int gridWidth() {
            return 3;
        }

        int gridHeight() {
            return 3;
        }

        TransientGrid craftingGrid() {
            return craftingGrid;
        }

        List<SlotDescriptor> inputGridSlots() {
            List<SlotDescriptor> ordered = new ArrayList<>(9);
            for (int logicalCell = 0; logicalCell < 9; logicalCell++) {
                final int cell = logicalCell;
                SlotDescriptor slot = slots.stream()
                        .filter(candidate -> candidate.container() == craftingContainer)
                        .filter(candidate -> candidate.containerSlot() == cell)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("missing craft cell " + cell));
                ordered.add(slot);
            }
            return List.copyOf(ordered);
        }

        List<SlotDescriptor> craftingSlotsByMenuOrder() {
            return slots.stream()
                    .filter(slot -> slot.container() == craftingContainer)
                    .toList();
        }

        Optional<SlotDescriptor> craftingSlotForLogicalCell(int logicalCell) {
            return slots.stream()
                    .filter(slot -> slot.container() == craftingContainer)
                    .filter(slot -> slot.containerSlot() == logicalCell)
                    .findFirst();
        }

        void markSnapshots(int menuId, String last, String remote) {
            lastSlots.set(menuId, Objects.requireNonNull(last));
            remoteSlots.set(menuId, Objects.requireNonNull(remote));
        }

        void rebuildTrinketSuffix(int newCount) {
            if (newCount < 0) {
                throw new IllegalArgumentException("newCount must be non-negative");
            }
            if (slots.subList(trinketSlotStart, slots.size()).stream()
                    .anyMatch(slot -> slot.owner() != Owner.TRINKET)) {
                throw new IllegalStateException("foreign slot found inside dynamic Trinkets suffix");
            }

            slots.subList(trinketSlotStart, slots.size()).clear();
            lastSlots.subList(trinketSlotStart, lastSlots.size()).clear();
            remoteSlots.subList(trinketSlotStart, remoteSlots.size()).clear();
            appendTrinkets(newCount);
            assertAligned();
        }

        String deterministicFingerprint() {
            String slotFingerprint = slots.stream()
                    .map(SlotDescriptor::stableSignature)
                    .reduce((left, right) -> left + "|" + right)
                    .orElse("");
            String inputs = inputGridSlots().stream()
                    .map(slot -> Integer.toString(slot.index()))
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
            return "3x3;trinkets=" + trinketSlotStart + ";inputs=" + inputs + ";slots=" + slotFingerprint;
        }

        private void appendTrinkets(int count) {
            for (int logicalSlot = 0; logicalSlot < count; logicalSlot++) {
                addSlot(trinketContainer, Owner.TRINKET, Role.TRINKET, logicalSlot);
            }
        }

        private void addSlot(Object container, Owner owner, Role role, int containerSlot) {
            int index = slots.size();
            slots.add(new SlotDescriptor(index, container, owner, role, containerSlot));
            lastSlots.add("EMPTY");
            remoteSlots.add("EMPTY");
        }

        private void assertAligned() {
            if (slots.size() != lastSlots.size() || slots.size() != remoteSlots.size()) {
                throw new IllegalStateException("slot snapshot lists are not aligned");
            }
            for (int index = 0; index < slots.size(); index++) {
                if (slots.get(index).index() != index) {
                    throw new IllegalStateException("stale public slot index at " + index);
                }
            }
        }
    }

    static final class CraftingTableMenu {
        private final Object resultContainer = new Object();
        private final Object craftingContainer = new Object();
        private final List<SlotDescriptor> slots = new ArrayList<>();

        CraftingTableMenu() {
            slots.add(new SlotDescriptor(0, resultContainer, Owner.RESULT, Role.RESULT, 0));
            for (int logicalCell = 0; logicalCell < 9; logicalCell++) {
                slots.add(new SlotDescriptor(logicalCell + 1, craftingContainer, Owner.CRAFT,
                        Role.CRAFT_INPUT, logicalCell));
            }
        }

        int gridWidth() {
            return 3;
        }

        int gridHeight() {
            return 3;
        }

        List<SlotDescriptor> inputGridSlots() {
            return List.copyOf(slots.subList(1, 10));
        }

        String deterministicFingerprint() {
            return slots.stream()
                    .map(SlotDescriptor::stableSignature)
                    .reduce((left, right) -> left + "|" + right)
                    .orElse("");
        }
    }

    static final class TransientGrid {
        private final String[] contents = new String[9];
        private final int[] clearVisits = new int[9];
        private boolean removalHandled;

        void put(int logicalCell, String stackIdentity) {
            requireLogicalCell(logicalCell);
            if (removalHandled) {
                throw new IllegalStateException("cannot populate a removed transient grid");
            }
            contents[logicalCell] = Objects.requireNonNull(stackIdentity);
        }

        String get(int logicalCell) {
            requireLogicalCell(logicalCell);
            return contents[logicalCell];
        }

        ClearReport clearForRemoval() {
            if (removalHandled) {
                return new ClearReport(List.of(), clearVisits.clone());
            }
            removalHandled = true;
            List<EvacuatedStack> evacuated = new ArrayList<>();
            for (int logicalCell = 0; logicalCell < contents.length; logicalCell++) {
                clearVisits[logicalCell]++;
                if (contents[logicalCell] != null) {
                    evacuated.add(new EvacuatedStack(logicalCell, contents[logicalCell]));
                    contents[logicalCell] = null;
                }
            }
            return new ClearReport(List.copyOf(evacuated), clearVisits.clone());
        }

        boolean isEmpty() {
            return Arrays.stream(contents).allMatch(Objects::isNull);
        }

        private static void requireLogicalCell(int logicalCell) {
            if (logicalCell < 0 || logicalCell >= 9) {
                throw new IndexOutOfBoundsException("logical craft cell " + logicalCell);
            }
        }
    }

    record EvacuatedStack(int logicalCell, String stackIdentity) {
    }

    record ClearReport(List<EvacuatedStack> evacuated, int[] visits) {
        ClearReport {
            evacuated = List.copyOf(evacuated);
            visits = visits.clone();
        }

        @Override
        public int[] visits() {
            return visits.clone();
        }
    }

    enum DestinationRegion {
        ORDINARY,
        STORAGE,
        HOTBAR,
        EQUIPMENT,
        OFFHAND,
        TRINKETS,
        CRAFT
    }

    record Destination(DestinationRegion region, int startInclusive, int endExclusive) {
        Destination {
            Objects.requireNonNull(region);
            if (startInclusive < 0 || endExclusive < startInclusive) {
                throw new IllegalArgumentException("invalid destination range");
            }
        }
    }

    record TransferPlan(List<Destination> destinations, boolean reverse, boolean autoEquipAttempted) {
        TransferPlan {
            destinations = List.copyOf(destinations);
        }
    }

    static final class QuickMoveRouter {
        private QuickMoveRouter() {
        }

        static TransferPlan plan(TargetInventoryMenu menu, SlotDescriptor source, boolean equippable) {
            // Craft-container identity must win before any equipment heuristic.
            if (source.container() == menu.craftingContainer()) {
                return ordinary(false);
            }
            if (source.role() == Role.RESULT) {
                return ordinary(true);
            }
            if (equippable && (source.role() == Role.ORDINARY_STORAGE || source.role() == Role.HOTBAR)) {
                return new TransferPlan(List.of(
                        new Destination(DestinationRegion.EQUIPMENT, 5, 9),
                        new Destination(DestinationRegion.OFFHAND, OFFHAND_MENU_ID, OFFHAND_MENU_ID + 1),
                        new Destination(DestinationRegion.TRINKETS, menu.trinketSlotStart(), menu.slots().size())
                ), false, true);
            }
            if (source.role() == Role.ORDINARY_STORAGE) {
                return new TransferPlan(List.of(
                        new Destination(DestinationRegion.HOTBAR, STORAGE_END_EXCLUSIVE, HOTBAR_END_EXCLUSIVE)
                ), false, false);
            }
            if (source.role() == Role.HOTBAR) {
                return new TransferPlan(List.of(
                        new Destination(DestinationRegion.STORAGE, ORDINARY_START, STORAGE_END_EXCLUSIVE)
                ), false, false);
            }
            return ordinary(false);
        }

        private static TransferPlan ordinary(boolean reverse) {
            return new TransferPlan(List.of(
                    new Destination(DestinationRegion.ORDINARY, ORDINARY_START, HOTBAR_END_EXCLUSIVE)
            ), reverse, false);
        }
    }

    enum CreativeWrapperKind {
        BASE_VISIBLE,
        BASE_HIDDEN,
        TRINKET,
        DESTROY
    }

    record CreativeWrapper(CreativeWrapperKind kind, int targetMenuId) {
    }

    record CreativeProjection(List<CreativeWrapper> wrappers) {
        CreativeProjection {
            wrappers = List.copyOf(wrappers);
        }
    }

    static final class CreativePolicy {
        private CreativePolicy() {
        }

        static boolean visible(TargetInventoryMenu menu, SlotDescriptor target) {
            return target.role() != Role.RESULT && target.container() != menu.craftingContainer();
        }

        static boolean creativeWriteAllowed(TargetInventoryMenu menu, SlotDescriptor target) {
            return target.role() != Role.RESULT && target.container() != menu.craftingContainer();
        }

        static CreativeProjection project(TargetInventoryMenu menu) {
            List<CreativeWrapper> wrappers = new ArrayList<>();
            for (int menuId = 0; menuId < menu.trinketSlotStart(); menuId++) {
                SlotDescriptor target = menu.slot(menuId);
                CreativeWrapperKind kind = visible(menu, target)
                        ? CreativeWrapperKind.BASE_VISIBLE
                        : CreativeWrapperKind.BASE_HIDDEN;
                wrappers.add(new CreativeWrapper(kind, menuId));
            }
            for (int menuId = menu.trinketSlotStart(); menuId < menu.slots().size(); menuId++) {
                wrappers.add(new CreativeWrapper(CreativeWrapperKind.TRINKET, menuId));
            }
            wrappers.add(new CreativeWrapper(CreativeWrapperKind.DESTROY, -1));
            return new CreativeProjection(wrappers);
        }
    }

    record JeiTransferPlan(List<Integer> recipeTargetMenuIds, List<Integer> sourceMenuIds) {
        JeiTransferPlan {
            recipeTargetMenuIds = List.copyOf(recipeTargetMenuIds);
            sourceMenuIds = List.copyOf(sourceMenuIds);
        }

        int actualTargetForViewPosition(int viewPosition) {
            return recipeTargetMenuIds.get(viewPosition);
        }
    }

    static final class JeiTransferPlanner {
        private JeiTransferPlanner() {
        }

        static JeiTransferPlan forInventory(TargetInventoryMenu menu) {
            List<Integer> targets = menu.inputGridSlots().stream()
                    .map(SlotDescriptor::index)
                    .toList();
            List<Integer> sources = menu.slots().stream()
                    .filter(slot -> slot.container() == menu.playerInventory())
                    .filter(slot -> slot.containerSlot() >= 0)
                    .filter(slot -> slot.containerSlot() < NON_EQUIPMENT_SIZE)
                    .map(SlotDescriptor::index)
                    .toList();
            return new JeiTransferPlan(targets, sources);
        }
    }

    record EntityCraftingSlot(int entitySlotId, int logicalCell, int menuId) {
    }

    static final class EntitySlotResolver {
        private EntitySlotResolver() {
        }

        static Optional<EntityCraftingSlot> resolve(TargetInventoryMenu menu, int entitySlotId) {
            int logicalCell = entitySlotId - CRAFTING_ENTITY_SLOT_START;
            if (logicalCell < 0 || logicalCell >= 9) {
                return Optional.empty();
            }
            SlotDescriptor target = menu.craftingSlotForLogicalCell(logicalCell).orElseThrow();
            return Optional.of(new EntityCraftingSlot(entitySlotId, logicalCell, target.index()));
        }

        static int registeredCraftingCount() {
            return CRAFTING_ENTITY_SLOT_END_EXCLUSIVE - CRAFTING_ENTITY_SLOT_START;
        }
    }

    record ClickPacket(int containerId, int stateId, int slotNumber) {
    }

    enum ClickStatus {
        APPLIED_INCREMENTALLY,
        APPLIED_WITH_FULL_RESYNC,
        REJECTED
    }

    record ClickResult(ClickStatus status, String removedStack, int newStateId) {
    }

    static final class AuthoritativeMenuState {
        private final TargetInventoryMenu layout;
        private final int containerId;
        private final Map<Integer, String> stacksByMenuId = new LinkedHashMap<>();
        private int stateId;

        AuthoritativeMenuState(TargetInventoryMenu layout, int containerId, int stateId) {
            this.layout = Objects.requireNonNull(layout);
            this.containerId = containerId;
            this.stateId = stateId;
        }

        void put(int menuId, String stackIdentity) {
            requireValidSlot(menuId);
            stacksByMenuId.put(menuId, Objects.requireNonNull(stackIdentity));
        }

        String stackAt(int menuId) {
            requireValidSlot(menuId);
            return stacksByMenuId.get(menuId);
        }

        Map<Integer, String> snapshot() {
            return Collections.unmodifiableMap(new LinkedHashMap<>(stacksByMenuId));
        }

        int stateId() {
            return stateId;
        }

        ClickResult click(ClickPacket packet) {
            if (packet.containerId() != containerId
                    || packet.slotNumber() < 0
                    || packet.slotNumber() >= layout.slots().size()) {
                return new ClickResult(ClickStatus.REJECTED, null, stateId);
            }

            boolean stale = packet.stateId() != stateId;
            String removed = stacksByMenuId.remove(packet.slotNumber());
            stateId++;
            ClickStatus status = stale
                    ? ClickStatus.APPLIED_WITH_FULL_RESYNC
                    : ClickStatus.APPLIED_INCREMENTALLY;
            return new ClickResult(status, removed, stateId);
        }

        private void requireValidSlot(int menuId) {
            if (menuId < 0 || menuId >= layout.slots().size()) {
                throw new IndexOutOfBoundsException("menu slot " + menuId);
            }
        }
    }

    static SlotDescriptor syntheticSlot(
            int menuId,
            Object container,
            Owner owner,
            Role role,
            int containerSlot
    ) {
        return new SlotDescriptor(menuId, container, owner, role, containerSlot);
    }

    static Set<Integer> destinationMenuIds(TransferPlan plan) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (Destination destination : plan.destinations()) {
            IntStream.range(destination.startInclusive(), destination.endExclusive()).forEach(ids::add);
        }
        return Set.copyOf(ids);
    }
}
