package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ModComponents;
import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

final class ReservationVisualRendererTest {
    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); initializeComponentsAfterBootstrap();
        bind(Items.POISONOUS_POTATO); bind(Items.STONE); bind(Blocks.SHULKER_BOX.asItem());
    }

    @Test void exactGhostAndMarkerStatesRemainStable() {
        ItemStack template = new ItemStack(Items.POISONOUS_POTATO);
        assertEquals(ReservationVisualRenderer.SlotVisualState.EMPTY_RESERVED,
                ReservationVisualRenderer.state(ItemStack.EMPTY, Optional.of(template)));
        assertEquals(ReservationVisualRenderer.SlotVisualState.OCCUPIED_RESERVED,
                ReservationVisualRenderer.state(new ItemStack(Items.STONE), Optional.of(template)));
        assertEquals(ReservationVisualRenderer.SlotVisualState.UNRESERVED,
                ReservationVisualRenderer.state(ItemStack.EMPTY, Optional.empty()));
        assertEquals(0x59, ReservationVisualRenderer.GHOST_ALPHA_8);
        assertEquals(0x59FFFFFF, ReservationVisualRenderer.GHOST_ALPHA_ONLY_COLOR);
        assertEquals(21, ReservationVisualRenderer.literalZeroX(10, 6));
        assertEquals(19, ReservationVisualRenderer.literalZeroY(10));
    }

    @Test void nativePanelPlansAllTwentySevenCellsIncludingEveryEmptyKind() {
        ItemStack shulker = new ItemStack(Blocks.SHULKER_BOX);
        ReservationStore.setData(shulker, ReservationData.EMPTY.with(8, new ItemStack(Items.STONE)));
        NonNullList<ItemStack> physical = NonNullList.withSize(27, ItemStack.EMPTY);
        physical.set(9, new ItemStack(Items.POISONOUS_POTATO, 3));
        var plan = ShulkerPanelOverlay.plan(shulker, physical);
        assertEquals(27, plan.size());
        assertEquals(ReservationVisualRenderer.SlotVisualState.EMPTY_RESERVED, plan.get(8).state());
        assertEquals(ReservationVisualRenderer.SlotVisualState.UNRESERVED, plan.get(9).state());
        assertEquals(ReservationVisualRenderer.SlotVisualState.UNRESERVED, plan.get(26).state());
        assertTrue(plan.get(8).physical().isEmpty());
        assertEquals(3, plan.get(9).physical().getCount());
    }

    @Test void ghostAlphaScopeRestoresAfterNestingAndFailure() {
        assertEquals(GhostItemRenderScope.OPAQUE_ALPHA, GhostItemRenderScope.activeAlpha());
        GhostItemRenderScope.withAlpha(0x59, () -> {
            assertEquals(0x59, GhostItemRenderScope.activeAlpha());
            GhostItemRenderScope.withAlpha(0x40, () -> assertEquals(0x40, GhostItemRenderScope.activeAlpha()));
        });
        assertEquals(GhostItemRenderScope.OPAQUE_ALPHA, GhostItemRenderScope.activeAlpha());
        assertThrows(IllegalStateException.class, () -> GhostItemRenderScope.withAlpha(0x59, () -> {
            throw new IllegalStateException("fixture");
        }));
        assertEquals(GhostItemRenderScope.OPAQUE_ALPHA, GhostItemRenderScope.activeAlpha());
    }

    private static void bind(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }
    private static void initializeComponentsAfterBootstrap() throws Exception {
        var registry = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE;
        var frozen = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
        frozen.setAccessible(true);
        boolean wasFrozen = frozen.getBoolean(registry);
        frozen.setBoolean(registry, false);
        try { ModComponents.initialize(); } finally { frozen.setBoolean(registry, wasFrozen); }
    }
}
