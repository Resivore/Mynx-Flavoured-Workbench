package dev.resivore.slotreservations;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Optional, string-linked bridge for the exact audited Double Barrels physical-owner contract. */
public final class DoubleBarrelBridge {
    static final String ACCESS_CLASS = "com.mozko.doublebarrels.DoubleBarrelAccess";
    static final String WRAPPER_CLASS = "com.mozko.doublebarrels.DoubleBarrelInventory";
    private static final int PHYSICAL_SIZE = 27;
    private static final int COMBINED_SIZE = 54;

    private static final ReferenceQueue<Container> STALE_WRAPPERS = new ReferenceQueue<>();
    private static final Map<IdentityWeakReference, OwnerPair> WRAPPERS = new HashMap<>();

    private DoubleBarrelBridge() {
    }

    public static Resolution resolve(Container container, int slot) {
        if (container.getClass().getName().equals(WRAPPER_CLASS)) {
            if (slot < 0 || slot >= COMBINED_SIZE) {
                return Resolution.handledEmpty();
            }
            OwnerPair attached = attached(container);
            if (attached == null || !stillCurrent(attached)) {
                return Resolution.handledEmpty();
            }
            return Resolution.handled(attached.slot(slot));
        }

        if (!(container instanceof BarrelBlockEntity barrel)) {
            return Resolution.unhandled();
        }
        Inspection inspection = inspect(barrel);
        return switch (inspection.state()) {
            case NOT_APPLICABLE, SINGLE -> Resolution.unhandled();
            case INVALID -> Resolution.handledEmpty();
            case CONNECTED -> slot >= 0 && slot < COMBINED_SIZE
                    ? Resolution.handled(inspection.pair().slot(slot))
                    : Resolution.handledEmpty();
        };
    }

    /** Called at the audited DoubleBarrelAccess#getCombinedInventory return seam. */
    public static void associate(BarrelBlockEntity source, Container combined) {
        if (!combined.getClass().getName().equals(WRAPPER_CLASS)) {
            return;
        }
        Inspection inspection = inspect(source);
        if (inspection.state() != State.CONNECTED || !wrapperMatches(combined, inspection.pair())) {
            return;
        }
        synchronized (WRAPPERS) {
            expungeStaleWrappers();
            WRAPPERS.put(new IdentityWeakReference(combined, STALE_WRAPPERS), inspection.pair());
        }
    }

    static Inspection inspect(BarrelBlockEntity barrel) {
        if (barrel.getType() != BlockEntityTypes.BARREL
                || !BlockEntityTypes.BARREL.isValid(barrel.getBlockState())) {
            return Inspection.invalid();
        }
        try {
            ClassLoader loader = barrel.getClass().getClassLoader();
            Class<?> access = Class.forName(ACCESS_CLASS, false, loader);
            if (!access.isInstance(barrel)) {
                return Inspection.notApplicable();
            }
            Method isConnected = access.getMethod("isConnected");
            Method isMain = access.getMethod("isMainBarrel");
            Method connectionPos = access.getMethod("getConnectionPos");
            Method physicalItems = access.getMethod("doublebarrels$getItems");
            if (!(boolean) isConnected.invoke(barrel)) {
                return Inspection.single();
            }

            Object rawPos = connectionPos.invoke(barrel);
            Level level = barrel.getLevel();
            if (!(rawPos instanceof BlockPos partnerPos) || level == null) {
                return Inspection.invalid();
            }
            BlockEntity rawPartner = level.getBlockEntity(partnerPos);
            if (!(rawPartner instanceof BarrelBlockEntity partner)
                    || partner == barrel
                    || partner.getLevel() != level
                    || !access.isInstance(partner)
                    || !(boolean) isConnected.invoke(partner)
                    || !barrel.getBlockPos().equals(connectionPos.invoke(partner))) {
                return Inspection.invalid();
            }

            boolean barrelMain = (boolean) isMain.invoke(barrel);
            boolean partnerMain = (boolean) isMain.invoke(partner);
            if (barrelMain == partnerMain) {
                return Inspection.invalid();
            }
            List<ItemStack> barrelItems = exactPhysicalItems(physicalItems.invoke(barrel));
            List<ItemStack> partnerItems = exactPhysicalItems(physicalItems.invoke(partner));
            if (barrelItems == null || partnerItems == null || barrelItems == partnerItems) {
                return Inspection.invalid();
            }
            PhysicalOwner first = barrelMain
                    ? new PhysicalOwner(barrel, barrelItems)
                    : new PhysicalOwner(partner, partnerItems);
            PhysicalOwner second = barrelMain
                    ? new PhysicalOwner(partner, partnerItems)
                    : new PhysicalOwner(barrel, barrelItems);
            return Inspection.connected(new OwnerPair(first, second));
        } catch (ClassNotFoundException missingOptionalMod) {
            return Inspection.notApplicable();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException changedContract) {
            return Inspection.invalid();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ItemStack> exactPhysicalItems(Object value) {
        if (!(value instanceof List<?> list) || list.size() != PHYSICAL_SIZE) {
            return null;
        }
        for (Object element : list) {
            if (!(element instanceof ItemStack)) {
                return null;
            }
        }
        return (List<ItemStack>) list;
    }

    private static boolean wrapperMatches(Container wrapper, OwnerPair pair) {
        try {
            if (wrapper.getContainerSize() != COMBINED_SIZE
                    || declaresIdentityMethod(wrapper.getClass(), "equals", Object.class)
                    || declaresIdentityMethod(wrapper.getClass(), "hashCode")) {
                return false;
            }
            Field first = wrapper.getClass().getDeclaredField("first");
            Field second = wrapper.getClass().getDeclaredField("second");
            Field firstSize = wrapper.getClass().getDeclaredField("firstSize");
            Field totalSize = wrapper.getClass().getDeclaredField("totalSize");
            if (!first.trySetAccessible() || !second.trySetAccessible()
                    || !firstSize.trySetAccessible() || !totalSize.trySetAccessible()) {
                return false;
            }
            return first.get(wrapper) == pair.first().items()
                    && second.get(wrapper) == pair.second().items()
                    && firstSize.getInt(wrapper) == PHYSICAL_SIZE
                    && totalSize.getInt(wrapper) == COMBINED_SIZE;
        } catch (ReflectiveOperationException | RuntimeException changedContract) {
            return false;
        }
    }

    private static boolean declaresIdentityMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            type.getDeclaredMethod(name, parameters);
            return true;
        } catch (NoSuchMethodException expected) {
            return false;
        }
    }

    private static boolean stillCurrent(OwnerPair expected) {
        Inspection current = inspect(expected.first().owner());
        return current.state() == State.CONNECTED
                && current.pair().first().owner() == expected.first().owner()
                && current.pair().second().owner() == expected.second().owner()
                && current.pair().first().items() == expected.first().items()
                && current.pair().second().items() == expected.second().items();
    }

    private static OwnerPair attached(Container wrapper) {
        synchronized (WRAPPERS) {
            expungeStaleWrappers();
            return WRAPPERS.get(new IdentityWeakReference(wrapper, null));
        }
    }

    private static void expungeStaleWrappers() {
        IdentityWeakReference stale;
        while ((stale = (IdentityWeakReference) STALE_WRAPPERS.poll()) != null) {
            WRAPPERS.remove(stale);
        }
    }

    enum State {
        NOT_APPLICABLE,
        SINGLE,
        CONNECTED,
        INVALID
    }

    record Inspection(State state, OwnerPair pair) {
        static Inspection notApplicable() {
            return new Inspection(State.NOT_APPLICABLE, null);
        }

        static Inspection single() {
            return new Inspection(State.SINGLE, null);
        }

        static Inspection connected(OwnerPair pair) {
            return new Inspection(State.CONNECTED, pair);
        }

        static Inspection invalid() {
            return new Inspection(State.INVALID, null);
        }
    }

    record PhysicalOwner(BarrelBlockEntity owner, List<ItemStack> items) {
    }

    record OwnerPair(PhysicalOwner first, PhysicalOwner second) {
        PhysicalSlot slot(int combinedSlot) {
            return combinedSlot < PHYSICAL_SIZE
                    ? new PhysicalSlot(first.owner(), combinedSlot, first.items())
                    : new PhysicalSlot(second.owner(), combinedSlot - PHYSICAL_SIZE, second.items());
        }
    }

    public record PhysicalSlot(BarrelBlockEntity owner, int localSlot, List<ItemStack> physicalItems) {
    }

    public record Resolution(boolean handled, Optional<PhysicalSlot> slot) {
        static Resolution unhandled() {
            return new Resolution(false, Optional.empty());
        }

        static Resolution handled(PhysicalSlot slot) {
            return new Resolution(true, Optional.of(slot));
        }

        static Resolution handledEmpty() {
            return new Resolution(true, Optional.empty());
        }
    }

    private static final class IdentityWeakReference extends WeakReference<Container> {
        private final int identityHash;

        IdentityWeakReference(Container referent, ReferenceQueue<Container> queue) {
            super(referent, queue);
            this.identityHash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return identityHash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            return other instanceof IdentityWeakReference reference
                    && get() != null
                    && get() == reference.get();
        }
    }
}
