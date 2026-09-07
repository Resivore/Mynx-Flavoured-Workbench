package tempeststudios.quickstacknearby;

import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuickStackMoveEngine {
    private QuickStackMoveEngine() {
    }

    public static Result moveMatchingItems(Container source, int firstSourceSlot, int exclusiveLastSourceSlot, List<Target> targets) {
        return moveMatchingItems(source, firstSourceSlot, exclusiveLastSourceSlot, targets, SourceRules.EMPTY);
    }

    public static Result moveMatchingItems(
            Container source,
            int firstSourceSlot,
            int exclusiveLastSourceSlot,
            List<Target> targets,
            SourceRules sourceRules
    ) {
        if (targets.isEmpty()) {
            return Result.empty();
        }

        int movedItems = 0;
        int sourceStacksTouched = 0;
        Set<Container> touchedContainers = Collections.newSetFromMap(new IdentityHashMap<>());
        int lastSourceSlot = Math.min(exclusiveLastSourceSlot, source.getContainerSize());
        SourceRules rules = sourceRules == null ? SourceRules.EMPTY : sourceRules;

        for (int sourceSlot = Math.max(0, firstSourceSlot); sourceSlot < lastSourceSlot; sourceSlot++) {
            if (rules.isLocked(sourceSlot)) {
                continue;
            }

            ItemStack sourceStack = source.getItem(sourceSlot);
            if (sourceStack.isEmpty()) {
                continue;
            }

            int movableCount = rules.movableCount(sourceSlot, sourceStack.getCount());
            if (movableCount <= 0) {
                continue;
            }

            StackKey sourceKey = StackKey.of(sourceStack);
            ItemStack movingStack = sourceStack.copyWithCount(movableCount);
            int movedFromStack = 0;
            for (Target target : targets) {
                if (!target.accepts(sourceKey)) {
                    continue;
                }

                int movedIntoTarget = insertIntoTarget(movingStack, target.container());
                if (movedIntoTarget > 0) {
                    movedFromStack += movedIntoTarget;
                    touchedContainers.add(target.container());
                }
                if (movingStack.isEmpty()) {
                    break;
                }
            }

            if (movedFromStack > 0) {
                movedItems += movedFromStack;
                sourceStacksTouched++;
                sourceStack.shrink(movedFromStack);
                if (sourceStack.isEmpty()) {
                    source.setItem(sourceSlot, ItemStack.EMPTY);
                }
            }
        }

        if (movedItems > 0) {
            source.setChanged();
            for (Container container : touchedContainers) {
                container.setChanged();
            }
        }

        return new Result(movedItems, sourceStacksTouched, touchedContainers.size());
    }

    public static Set<StackKey> acceptedTypes(Container container) {
        LinkedHashSet<StackKey> acceptedTypes = new LinkedHashSet<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                acceptedTypes.add(StackKey.of(stack));
            }
        }
        return Collections.unmodifiableSet(acceptedTypes);
    }

    public static SelfTestResult runSelfTest() {
        SimpleContainer source = new SimpleContainer(36);
        source.setItem(9, new ItemStack(Items.COBBLESTONE, 32));
        source.setItem(10, new ItemStack(Items.DIRT, 16));
        source.setItem(11, new ItemStack(Items.STICK, 8));

        SimpleContainer stoneChest = new SimpleContainer(9);
        stoneChest.setItem(0, new ItemStack(Items.COBBLESTONE, 40));

        SimpleContainer dirtChest = new SimpleContainer(9);
        dirtChest.setItem(0, new ItemStack(Items.DIRT, 60));

        SimpleContainer emptyChest = new SimpleContainer(9);

        Result result = moveMatchingItems(
                source,
                9,
                36,
                List.of(
                        Target.fromCurrentContents(stoneChest),
                        Target.fromCurrentContents(dirtChest),
                        Target.fromCurrentContents(emptyChest)
                )
        );

        boolean passed = result.itemsMoved() == 48
                && result.sourceStacksTouched() == 2
                && result.targetContainersTouched() == 2
                && source.getItem(9).isEmpty()
                && source.getItem(10).isEmpty()
                && source.getItem(11).getCount() == 8
                && stoneChest.getItem(0).getCount() == 64
                && stoneChest.getItem(1).getCount() == 8
                && dirtChest.getItem(0).getCount() == 64
                && dirtChest.getItem(1).getCount() == 12
                && emptyChest.isEmpty();

        return new SelfTestResult(passed && runSourceRulesSelfTest() && runCompoundContainerSelfTest(), result);
    }

    private static boolean runSourceRulesSelfTest() {
        SimpleContainer source = new SimpleContainer(36);
        source.setItem(9, new ItemStack(Items.COBBLESTONE, 32));
        source.setItem(10, new ItemStack(Items.DIRT, 16));

        SimpleContainer stoneChest = new SimpleContainer(9);
        stoneChest.setItem(0, new ItemStack(Items.COBBLESTONE, 40));

        SimpleContainer dirtChest = new SimpleContainer(9);
        dirtChest.setItem(0, new ItemStack(Items.DIRT, 40));

        Result result = moveMatchingItems(
                source,
                9,
                36,
                List.of(
                        Target.fromCurrentContents(stoneChest),
                        Target.fromCurrentContents(dirtChest)
                ),
                SourceRules.fromPayloadRules(List.of(
                        new QuickStackRequestPayload.SlotRule(9, false, 4),
                        new QuickStackRequestPayload.SlotRule(10, true, 0)
                ))
        );

        return result.itemsMoved() == 28
                && result.sourceStacksTouched() == 1
                && result.targetContainersTouched() == 1
                && source.getItem(9).getCount() == 4
                && source.getItem(10).getCount() == 16
                && stoneChest.getItem(0).getCount() == 64
                && stoneChest.getItem(1).getCount() == 4
                && dirtChest.getItem(0).getCount() == 40;
    }

    private static boolean runCompoundContainerSelfTest() {
        SimpleContainer source = new SimpleContainer(36);
        source.setItem(9, new ItemStack(Items.COBBLESTONE, 32));

        SimpleContainer leftChest = new SimpleContainer(27);
        for (int slot = 0; slot < leftChest.getContainerSize(); slot++) {
            leftChest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        SimpleContainer rightChest = new SimpleContainer(27);
        CompoundContainer doubleChest = new CompoundContainer(leftChest, rightChest);

        Result result = moveMatchingItems(
                source,
                9,
                36,
                List.of(Target.fromCurrentContents(doubleChest))
        );

        return result.itemsMoved() == 32
                && result.sourceStacksTouched() == 1
                && result.targetContainersTouched() == 1
                && source.getItem(9).isEmpty()
                && leftChest.getItem(leftChest.getContainerSize() - 1).getCount() == 64
                && rightChest.getItem(0).getCount() == 32;
    }

    private static int insertIntoTarget(ItemStack sourceStack, Container target) {
        int moved = insertIntoExistingStacks(sourceStack, target);
        if (!sourceStack.isEmpty()) {
            moved += insertIntoEmptySlots(sourceStack, target);
        }
        return moved;
    }

    private static int insertIntoExistingStacks(ItemStack sourceStack, Container target) {
        int moved = 0;
        for (int slot = 0; slot < target.getContainerSize() && !sourceStack.isEmpty(); slot++) {
            ItemStack targetStack = target.getItem(slot);
            if (targetStack.isEmpty() || !InventoryCompat.sameItemAndComponents(sourceStack, targetStack)) {
                continue;
            }
            if (!target.canPlaceItem(slot, sourceStack)) {
                continue;
            }

            int maxCount = Math.min(targetStack.getMaxStackSize(), InventoryCompat.maxStackSize(target, targetStack));
            int room = maxCount - targetStack.getCount();
            if (room <= 0) {
                continue;
            }

            int amount = Math.min(room, sourceStack.getCount());
            targetStack.grow(amount);
            sourceStack.shrink(amount);
            moved += amount;
        }
        return moved;
    }

    private static int insertIntoEmptySlots(ItemStack sourceStack, Container target) {
        int moved = 0;
        for (int slot = 0; slot < target.getContainerSize() && !sourceStack.isEmpty(); slot++) {
            ItemStack targetStack = target.getItem(slot);
            if (!targetStack.isEmpty() || !target.canPlaceItem(slot, sourceStack)) {
                continue;
            }

            int maxCount = Math.min(sourceStack.getMaxStackSize(), InventoryCompat.maxStackSize(target, sourceStack));
            int amount = Math.min(maxCount, sourceStack.getCount());
            target.setItem(slot, sourceStack.copyWithCount(amount));
            sourceStack.shrink(amount);
            moved += amount;
        }
        return moved;
    }

    public record Target(Container container, Set<StackKey> acceptedTypes) {
        public static Target fromCurrentContents(Container container) {
            return new Target(container, QuickStackMoveEngine.acceptedTypes(container));
        }

        public boolean accepts(StackKey key) {
            return acceptedTypes.contains(key);
        }
    }

    public record StackKey(Object identity) {
        public static StackKey of(ItemStack stack) {
            return new StackKey(InventoryCompat.stackIdentity(stack));
        }
    }

    public record Result(int itemsMoved, int sourceStacksTouched, int targetContainersTouched) {
        public static Result empty() {
            return new Result(0, 0, 0);
        }
    }

    public record SourceRules(Map<Integer, SlotRule> slotRules) {
        public static final SourceRules EMPTY = new SourceRules(Map.of());

        public SourceRules {
            slotRules = Map.copyOf(slotRules);
        }

        public static SourceRules fromPayloadRules(List<QuickStackRequestPayload.SlotRule> payloadRules) {
            if (payloadRules == null || payloadRules.isEmpty()) {
                return EMPTY;
            }

            Map<Integer, SlotRule> rules = new HashMap<>();
            for (QuickStackRequestPayload.SlotRule payloadRule : payloadRules) {
                if (payloadRule == null) {
                    continue;
                }
                SlotRule rule = new SlotRule(payloadRule.locked(), Math.max(0, payloadRule.keepCount()));
                if (!rule.isEmpty()) {
                    rules.put(payloadRule.slotIndex(), rule);
                }
            }
            return rules.isEmpty() ? EMPTY : new SourceRules(rules);
        }

        public boolean isLocked(int slotIndex) {
            SlotRule rule = slotRules.get(slotIndex);
            return rule != null && rule.locked();
        }

        public int movableCount(int slotIndex, int stackCount) {
            SlotRule rule = slotRules.get(slotIndex);
            if (rule == null) {
                return stackCount;
            }
            if (rule.locked()) {
                return 0;
            }
            return Math.max(0, stackCount - Math.max(0, rule.keepCount()));
        }
    }

    public record SlotRule(boolean locked, int keepCount) {
        public boolean isEmpty() {
            return !locked && keepCount <= 0;
        }
    }

    public record SelfTestResult(boolean passed, Result result) {
    }
}
