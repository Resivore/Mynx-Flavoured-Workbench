package dev.resivore.slotreservations;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProductionWiringContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Path JAVA = ROOT.resolve("src/main/java/dev/resivore/slotreservations");
    private static final Pattern SUPPORTED_TYPE = Pattern.compile(
            "exactType\\(blockEntity, BlockEntityTypes\\.([A-Z_]+)\\)"
    );

    @Test
    void resolverUsesTheClosedC4PhysicalOwnerAllowlist() throws IOException {
        String resolver = source("SupportedContainerResolver.java");
        String supported = between(
                resolver,
                "private static boolean isSupported(BlockEntity blockEntity)",
                "private static boolean exactType("
        );
        Set<String> expected = Set.of(
                "CHEST",
                "TRAPPED_CHEST",
                "BARREL",
                "SHULKER_BOX",
                "DISPENSER",
                "DROPPER",
                "HOPPER",
                "FURNACE",
                "BLAST_FURNACE",
                "SMOKER",
                "BREWING_STAND",
                "CRAFTER"
        );
        Set<String> actual = new HashSet<>();
        Matcher matcher = SUPPORTED_TYPE.matcher(supported);
        int matches = 0;
        while (matcher.find()) {
            matches++;
            assertTrue(actual.add(matcher.group(1)), "The closed allowlist must not repeat a type");
        }

        assertEquals(expected, actual,
                "Only the requested persistent vanilla block-entity families may be eligible");
        assertEquals(expected.size(), matches);
        assertTrue(resolver.contains(
                "blockEntity.getType() == type && type.isValid(blockEntity.getBlockState())"),
                "Copper variants must be admitted through the exact vanilla chest type's valid blocks");
        assertFalse(resolver.contains("block == Blocks.CHEST"),
                "A single ordinary-chest block predicate would omit the vanilla copper family");

        assertTrue(resolver.contains("container instanceof PlayerEnderChestContainer enderChest"));
        assertTrue(resolver.contains("ResolvedSlot.standard(enderChest, slot)"));
        assertTrue(resolver.contains("if (!(container instanceof BlockEntity blockEntity)"),
                "Arbitrary Container implementations must fail closed");
        assertFalse(resolver.contains("EnderChestBlockEntity"),
                "Ender Chest reservations belong to the player's inventory, never the placed block");
        assertFalse(resolver.contains("RandomizableContainerBlockEntity.class.isAssignableFrom"));

        assertTrue(resolver.contains("Blocks.DYED_SHULKER_BOX.asList()"));
        assertTrue(resolver.contains("blocks.add(Blocks.SHULKER_BOX)"));
        assertTrue(resolver.contains("exactType(blockEntity, BlockEntityTypes.SHULKER_BOX)"));
    }

    @Test
    void logicalSlotsExposeStablePhysicalOwnershipAndCurrentPhysicalStacks() throws IOException {
        String resolver = source("SupportedContainerResolver.java");
        String accessor = source("mixin/CompoundContainerAccessor.java");

        assertTrue(resolver.contains("container instanceof CompoundContainer compound"));
        assertTrue(resolver.contains("containerSlotReservations$getFirst()"));
        assertTrue(resolver.contains("containerSlotReservations$getSecond()"));
        assertTrue(resolver.contains("slot < firstSize"));
        assertTrue(resolver.contains("resolvePhysical(first, slot)"));
        assertTrue(resolver.contains("resolvePhysical(second, slot - firstSize)"));
        assertTrue(accessor.contains("@Accessor(\"container1\")"));
        assertTrue(accessor.contains("@Accessor(\"container2\")"));

        assertTrue(resolver.contains("private final Container owner;"));
        assertTrue(resolver.contains("private final int localSlot;"));
        assertTrue(resolver.contains("private final int ownerSlotCount;"));
        assertTrue(resolver.contains("private final List<ItemStack> physicalItems;"));
        assertTrue(resolver.contains("int slotCount = owner.getContainerSize()"));
        assertTrue(resolver.contains("public Container owner()"));
        assertTrue(resolver.contains("public int localSlot()"));
        assertTrue(resolver.contains("public int ownerSlotCount()"));
        assertTrue(resolver.contains("public ItemStack physicalStack()"));
        assertTrue(resolver.contains(
                "physicalItems == null ? owner.getItem(localSlot) : physicalItems.get(localSlot)"));
        assertTrue(resolver.contains(
                "new ResolvedSlot(slot.owner(), slot.owner(), slot.localSlot(), 27, slot.physicalItems())"),
                "Double Barrels must retain two physical 27-slot reservation owners");
        assertTrue(resolver.contains("if (blockEntity != null && owner != blockEntity)"),
                "Block reservations must never be stored on an ephemeral wrapper");
    }

    @Test
    void nativeAdmissionIsQueriedUnderBypassThenReservationsRunPostNative() throws IOException {
        String policy = source("NativeInsertionPolicy.java");
        String api = source("api/ContainerSlotReservationsApi.java");
        String ordinarySlot = source("mixin/SlotMixin.java");
        String randomizable = source("mixin/RandomizableContainerBlockEntityMixin.java");
        String furnaceEntity = source("mixin/AbstractFurnaceBlockEntityMixin.java");
        String brewingEntity = source("mixin/BrewingStandBlockEntityMixin.java");
        String crafterEntity = source("mixin/CrafterBlockEntityMixin.java");
        String furnaceFuel = source("mixin/FurnaceFuelSlotMixin.java");
        String brewingSlots = source("mixin/BrewingStandSlotMixin.java");
        String wrappedSlot = source("menu/ReservationAwareSlot.java");
        String wrappedShulkerSlot = source("menu/ReservationAwareShulkerBoxSlot.java");
        String commonMixins = resource("container_slot_reservations.mixins.json");

        assertTrue(policy.contains("ThreadLocal<Integer> NATIVE_QUERY_DEPTH"));
        assertTrue(policy.contains("NATIVE_QUERY_DEPTH.set(previousDepth + 1)"));
        assertTrue(policy.contains("return slot.owner().canPlaceItem(slot.localSlot(), incoming)"));
        assertTrue(policy.contains("finally"));
        assertTrue(policy.contains("NATIVE_QUERY_DEPTH.remove()"));
        assertTrue(policy.contains("!nativeAllowed || isNativeQuery() || isNonInsertionQuery()"),
                "Native permission probes and non-insertion queries must bypass the CSR layer");
        assertTrue(policy.contains("return nativeAllowed;"));
        assertTrue(api.contains("NativeInsertionPolicy.nativeMayInsert(physical, incoming)"));
        assertTrue(api.contains("physical.physicalStack()"));
        assertTrue(api.contains("if (!nativeWritable)"));
        assertTrue(api.contains("return ReservationSlotClass.NON_WRITABLE;"));

        assertPostNativeHook(ordinarySlot, "mayPlace");
        // RandomizableContainerBlockEntity inherits Container's native default, whose exact
        // Minecraft 26.2 result is true; there is no concrete method there for an injector to
        // wrap. CSR materializes that inherited result, while every requested subtype with a
        // stricter native implementation is wrapped separately below.
        assertTrue(randomizable.contains("public boolean canPlaceItem(int slot, ItemStack incoming)"));
        assertTrue(randomizable.contains(
                "NativeInsertionPolicy.applyReservation(self, slot, incoming, true)"));
        assertPostNativeHook(furnaceEntity, "canPlaceItem");
        assertPostNativeHook(brewingEntity, "canPlaceItem");
        assertPostNativeHook(crafterEntity, "canPlaceItem");
        assertPostNativeHook(furnaceFuel, "mayPlace");
        assertPostNativeHook(brewingSlots, "mayPlace");
        assertTrue(furnaceEntity.contains("callbackInfo.getReturnValue()"));
        assertTrue(brewingEntity.contains("callbackInfo.getReturnValue()"));
        assertTrue(crafterEntity.contains("callbackInfo.getReturnValue()"));
        assertTrue(ordinarySlot.contains("beginNonInsertionQuery()"));
        assertTrue(ordinarySlot.contains("endNonInsertionQuery()"));

        assertTrue(furnaceFuel.contains("@Mixin(FurnaceFuelSlot.class)"));
        assertTrue(brewingSlots.contains(
                "\"net.minecraft.world.inventory.BrewingStandMenu$PotionSlot\""));
        assertTrue(brewingSlots.contains(
                "\"net.minecraft.world.inventory.BrewingStandMenu$IngredientsSlot\""));
        assertTrue(brewingSlots.contains(
                "\"net.minecraft.world.inventory.BrewingStandMenu$FuelSlot\""));
        assertTrue(wrappedSlot.contains("super.mayPlace(incoming)"));
        assertTrue(wrappedShulkerSlot.contains("super.mayPlace(incoming)"),
                "Vanilla shulker nesting exclusion must remain authoritative");

        for (String mixin : Set.of(
                "SlotMixin",
                "RandomizableContainerBlockEntityMixin",
                "AbstractFurnaceBlockEntityMixin",
                "BrewingStandBlockEntityMixin",
                "CrafterBlockEntityMixin",
                "FurnaceFuelSlotMixin",
                "BrewingStandSlotMixin"
        )) {
            assertTrue(commonMixins.contains("\"" + mixin + "\""),
                    "Missing production mixin registration for " + mixin);
        }

        String allMixins = allJavaUnder(JAVA.resolve("mixin"));
        assertFalse(allMixins.contains("method = \"setItem\""));
        assertFalse(allMixins.contains("method=\"setItem\""));
        assertFalse(allMixins.contains("@Overwrite"),
                "CSR must not replace vanilla machine writes or admission implementations");
    }

    @Test
    void enderChestUsesAPlayerOwnedHolderAndAdjacentNamespacedPersistence() throws IOException {
        String resolver = source("SupportedContainerResolver.java");
        String holder = source("EnderChestReservationHolder.java");
        String holderMixin = source("mixin/PlayerEnderChestContainerMixin.java");
        String persistence = source("mixin/PlayerEnderChestPersistenceMixin.java");
        String store = source("ReservationStore.java");
        String mixins = resource("container_slot_reservations.mixins.json");

        assertTrue(resolver.contains("container instanceof PlayerEnderChestContainer enderChest"));
        assertTrue(holder.contains("ReservationData containerSlotReservations$getReservations()"));
        assertTrue(holder.contains(
                "void containerSlotReservations$setReservations(ReservationData data)"));
        assertTrue(holderMixin.contains("@Mixin(PlayerEnderChestContainer.class)"));
        assertTrue(holderMixin.contains("implements EnderChestReservationHolder"));
        assertTrue(holderMixin.contains("ReservationData.EMPTY"));
        assertTrue(store.contains("owner instanceof EnderChestReservationHolder holder"));
        assertTrue(store.contains("holder.containerSlotReservations$getReservations()"));
        assertTrue(store.contains("holder.containerSlotReservations$setReservations(data)"));

        assertTrue(persistence.contains("@Mixin(Player.class)"));
        assertTrue(persistence.contains(
                "\"container_slot_reservations:ender_chest_reservations\""));
        assertTrue(persistence.contains("method = \"readAdditionalSaveData\""));
        assertTrue(persistence.contains(
                "PlayerEnderChestContainer;fromSlots(Lnet/minecraft/world/level/storage/ValueInput$TypedInputList;)V"));
        assertTrue(persistence.contains("method = \"addAdditionalSaveData\""));
        assertTrue(persistence.contains(
                "PlayerEnderChestContainer;storeAsSlots(Lnet/minecraft/world/level/storage/ValueOutput$TypedOutputList;)V"));
        assertEquals(2, occurrences(persistence, "shift = At.Shift.AFTER"),
                "Reservation I/O must stay adjacent to vanilla EnderItems I/O");
        assertEquals(2, occurrences(persistence, "require = 1"));
        assertTrue(persistence.contains("input.read(RESERVATION_KEY, ReservationData.CODEC)"));
        assertTrue(persistence.contains(".orElse(ReservationData.EMPTY)"),
                "Old player data must load with empty reservations");
        assertTrue(persistence.contains("output.store(RESERVATION_KEY, ReservationData.CODEC, data)"));
        assertTrue(mixins.contains("\"PlayerEnderChestContainerMixin\""));
        assertTrue(mixins.contains("\"PlayerEnderChestPersistenceMixin\""));
        assertFalse(allProductionJava().contains("EnderChestBlockEntity"));
    }

    @Test
    void doubleBarrelsBridgeIsStringOnlyIdentityBoundAndFailClosed() throws IOException {
        String bridge = source("DoubleBarrelBridge.java");
        String integration = source("mixin/DoubleBarrelIntegrationMixin.java");
        String resolver = source("SupportedContainerResolver.java");
        String metadata = resource("fabric.mod.json");
        String allJava = allProductionJava();

        assertTrue(bridge.contains(
                "ACCESS_CLASS = \"com.mozko.doublebarrels.DoubleBarrelAccess\""));
        assertTrue(bridge.contains(
                "WRAPPER_CLASS = \"com.mozko.doublebarrels.DoubleBarrelInventory\""));
        assertEquals(2, occurrences(allJava, "com.mozko.doublebarrels."),
                "Optional classes may appear only as the bridge's two audited string names");
        assertFalse(allJava.contains("import com.mozko.doublebarrels"));
        assertFalse(allJava.contains("DoubleBarrelAccess.class"));
        assertFalse(allJava.contains("DoubleBarrelInventory.class"));
        assertTrue(bridge.contains("Class.forName(ACCESS_CLASS, false, loader)"));
        assertTrue(bridge.contains("catch (ClassNotFoundException missingOptionalMod)"));
        assertTrue(bridge.contains("catch (ReflectiveOperationException | LinkageError | RuntimeException"));

        for (String method : Set.of(
                "isConnected",
                "isMainBarrel",
                "getConnectionPos",
                "doublebarrels$getItems"
        )) {
            assertTrue(bridge.contains("getMethod(\"" + method + "\")"),
                    "Missing audited Double Barrels seam " + method);
        }
        assertTrue(bridge.contains("private static final int PHYSICAL_SIZE = 27"));
        assertTrue(bridge.contains("private static final int COMBINED_SIZE = 54"));
        assertTrue(bridge.contains("combinedSlot < PHYSICAL_SIZE"));
        assertTrue(bridge.contains("new PhysicalSlot(first.owner(), combinedSlot, first.items())"));
        assertTrue(bridge.contains(
                "new PhysicalSlot(second.owner(), combinedSlot - PHYSICAL_SIZE, second.items())"));
        assertTrue(bridge.contains("ReferenceQueue<Container>"));
        assertTrue(bridge.contains("IdentityWeakReference"));
        assertTrue(bridge.contains("attached == null || !stillCurrent(attached)"));
        assertTrue(bridge.contains("return Resolution.handledEmpty()"));
        assertTrue(resolver.indexOf("DoubleBarrelBridge.resolve(container, slot)")
                        < resolver.indexOf("container instanceof CompoundContainer"),
                "The exact Double Barrels wrapper must be resolved before generic views");

        assertTrue(integration.contains("@Mixin(value = BarrelBlockEntity.class, priority = 900)"));
        assertTrue(integration.contains("method = \"getCombinedInventory()Lnet/minecraft/world/Container;\""));
        assertTrue(integration.contains("remap = false"));
        assertTrue(integration.contains("require = 0"),
                "The optional mod's absent seam must not prevent CSR from loading");
        assertFalse(integration.contains("com.mozko.doublebarrels"));

        String depends = between(metadata, "\"depends\": {", "},\n  \"suggests\"");
        String suggests = between(metadata, "\"suggests\": {", "}\n}");
        assertFalse(depends.contains("doublebarrels"),
                "Double Barrels must never become a hard production dependency");
        assertTrue(suggests.contains("\"doublebarrels\": \"*\""),
                "The exact optional Fabric id needs a flexible suggests predicate");
    }

    @Test
    void shulkerRoundTripStillUsesTheNarrowComponentSeams() throws IOException {
        String shulkerBlock = source("mixin/ShulkerBoxBlockMixin.java");
        String drops = source("ShulkerReservationDrops.java");
        String components = source("ModComponents.java");
        String commonMixins = resource("container_slot_reservations.mixins.json");

        assertTrue(commonMixins.contains("\"ShulkerBoxBlockMixin\""));
        assertTrue(shulkerBlock.contains("method = \"playerWillDestroy\""));
        assertTrue(drops.contains("drop.applyComponents(shulker.components())"));
        assertTrue(components.contains(".persistent(ReservationData.CODEC)"));
        assertTrue(components.contains(".networkSynchronized(ReservationData.STREAM_CODEC)"));
    }

    private static void assertPostNativeHook(String productionSource, String method) {
        assertTrue(productionSource.contains("method = \"" + method + "\""));
        assertTrue(productionSource.contains("at = @At(\"RETURN\")"));
        assertTrue(productionSource.contains("cancellable = true"));
        assertTrue(productionSource.contains("NativeInsertionPolicy.applyReservation("));
        assertTrue(productionSource.contains("callbackInfo.getReturnValue()"));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(JAVA.resolve(relative));
    }

    private static String resource(String relative) throws IOException {
        return Files.readString(ROOT.resolve("src/main/resources").resolve(relative));
    }

    private static String allProductionJava() throws IOException {
        return allJavaUnder(JAVA);
    }

    private static String allJavaUnder(Path directory) throws IOException {
        StringBuilder joined = new StringBuilder();
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.toString().endsWith(".java"))
                    .sorted()
                    .toList()) {
                joined.append(Files.readString(path)).append('\n');
            }
        }
        return joined.toString();
    }

    private static String between(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertTrue(start >= 0, "Missing source-contract marker: " + startMarker);
        int end = text.indexOf(endMarker, start + startMarker.length());
        assertTrue(end >= 0, "Missing source-contract marker: " + endMarker);
        return text.substring(start, end);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(needle, index)) >= 0; index += needle.length()) {
            count++;
        }
        return count;
    }
}
