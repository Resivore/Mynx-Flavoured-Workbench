package dev.resivore.mapmarkerextension.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.resivore.mapmarkerextension.MinecraftTestBootstrap;
import dev.resivore.mapmarkerextension.core.ExternalNativeMapIdentities;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class MapMarkerItemModelCompositionTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.initialize();
    }

    @Test
    void ribbitsMarkerKeyedWrapperCanBeInsideOrOutsideMme() throws Exception {
        ItemStack ribbitMap = ribbitMap();

        AtomicInteger innerRibbitsCalls = new AtomicInteger();
        AtomicInteger innerTerminalCalls = new AtomicInteger();
        ItemModel mmeOutside = mmeWrapper(new MarkerKeyedWrapper(
            recordingModel(innerTerminalCalls),
            innerRibbitsCalls
        ));
        update(mmeOutside, ribbitMap);
        assertEquals(1, innerRibbitsCalls.get());
        assertEquals(0, innerTerminalCalls.get());

        AtomicInteger outerRibbitsCalls = new AtomicInteger();
        AtomicInteger outerTerminalCalls = new AtomicInteger();
        ItemModel ribbitsOutside = new MarkerKeyedWrapper(
            mmeWrapper(recordingModel(outerTerminalCalls)),
            outerRibbitsCalls
        );
        update(ribbitsOutside, ribbitMap);
        assertEquals(1, outerRibbitsCalls.get());
        assertEquals(0, outerTerminalCalls.get());
    }

    @Test
    void eitherWrapperOrderDelegatesAnUnownedFilledMapExactlyOnce() throws Exception {
        ItemStack ordinaryMap = new ItemStack(Items.FILLED_MAP);

        AtomicInteger mmeOutsideTerminalCalls = new AtomicInteger();
        update(mmeWrapper(new MarkerKeyedWrapper(
            recordingModel(mmeOutsideTerminalCalls),
            new AtomicInteger()
        )), ordinaryMap);
        assertEquals(1, mmeOutsideTerminalCalls.get());

        AtomicInteger ribbitsOutsideTerminalCalls = new AtomicInteger();
        update(new MarkerKeyedWrapper(
            mmeWrapper(recordingModel(ribbitsOutsideTerminalCalls)),
            new AtomicInteger()
        ), ordinaryMap);
        assertEquals(1, ribbitsOutsideTerminalCalls.get());
    }

    private static MapMarkerItemModel mmeWrapper(ItemModel wrapped)
        throws ReflectiveOperationException {
        Constructor<MapMarkerItemModel> constructor = MapMarkerItemModel.class
            .getDeclaredConstructor(ItemModel.class);
        constructor.setAccessible(true);
        return constructor.newInstance(wrapped);
    }

    private static ItemModel recordingModel(AtomicInteger calls) {
        return (renderState, stack, resolver, displayContext, level, owner, seed) ->
            calls.incrementAndGet();
    }

    private static void update(ItemModel model, ItemStack stack) {
        model.update(null, stack, null, ItemDisplayContext.GUI, null, null, 0);
    }

    private static ItemStack ribbitMap() {
        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        stack.set(DataComponents.MAP_ID, new MapId(91));
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(
            ExternalNativeMapIdentities.RIBBIT_VILLAGE.stackMarkerKey(),
            true
        );
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        return stack;
    }

    private static final class MarkerKeyedWrapper implements ItemModel {
        private final ItemModel wrapped;
        private final AtomicInteger matches;

        private MarkerKeyedWrapper(ItemModel wrapped, AtomicInteger matches) {
            this.wrapped = wrapped;
            this.matches = matches;
        }

        @Override
        public void update(
            ItemStackRenderState renderState,
            ItemStack stack,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            ClientLevel level,
            ItemOwner owner,
            int seed
        ) {
            if (ExternalNativeMapIdentities.find(stack).isPresent()) {
                matches.incrementAndGet();
                return;
            }
            wrapped.update(renderState, stack, resolver, displayContext, level, owner, seed);
        }
    }
}
