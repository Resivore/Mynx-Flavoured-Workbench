package com.crispytwig.naturalist.glowgoop;

import com.crispytwig.naturalist.fabric.platform.FabricRegistrationProvider;
import com.crispytwig.naturalist.platform.registry.DeferredHolder;
import com.crispytwig.naturalist.registry.NaturalistRegistry;
import com.crispytwig.naturalist.server.block.GlowGoopBlock;
import com.crispytwig.naturalist.server.item.GlowGoopItem;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class GlowGoopRegistrationTest {
    private static final String MOD_ID = "naturalist_glow_goop_test";
    private static RecordingGlowGoopBlock block;
    private static DeferredHolder<Item, GlowGoopItem> item;
    private static int earlyShapeCalls;

    @BeforeAll
    static void registerBlockBeforeItsItem() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        assertNull(NaturalistRegistry.GLOW_GOOP);
        var id = Identifier.fromNamespaceAndPath(MOD_ID, "glow_goop");
        var blocks = new FabricRegistrationProvider<Block>(Registries.BLOCK, MOD_ID);
        var holder = blocks.register("glow_goop", () -> new RecordingGlowGoopBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id))
                        .strength(0.5F).replaceable().noOcclusion().noCollision()
                        .lightLevel(GlowGoopBlock.LIGHT_EMISSION).sound(SoundType.HONEY_BLOCK)));
        block = holder.get();
        assertNull(NaturalistRegistry.GLOW_GOOP);
        assertEquals(6, block.getStateDefinition().getPossibleStates().size());
        assertTrue(earlyShapeCalls >= 6, "Fabric registration must initialize all six state caches");
        // Also exercise direct queries and repeat the same cache path before item registration.
        for (var state : block.getStateDefinition().getPossibleStates()) {
            assertDoesNotThrow(state::initCache);
            assertSame(Shapes.empty(), block.getShape(state, EmptyBlockGetter.INSTANCE,
                    BlockPos.ZERO, CollisionContext.empty()));
            assertSame(Shapes.empty(), state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        }
        var items = new FabricRegistrationProvider<Item>(Registries.ITEM, MOD_ID);
        item = items.register("glow_goop", () -> new GlowGoopItem(holder.get(),
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
        NaturalistRegistry.GLOW_GOOP = item;
    }

    @ParameterizedTest
    @CsvSource({"1,false", "2,false", "3,false", "1,true", "2,true", "3,true"})
    void initializedStatesKeepLiveSelectionAndPreservedProperties(int goop, boolean waterlogged) {
        var state = block.defaultBlockState().setValue(GlowGoopBlock.GOOP, goop)
                .setValue(GlowGoopBlock.WATERLOGGED, waterlogged);
        var held = new HeldItemContext();
        assertNotSame(Items.AIR, item.get());
        // The same state already underwent initCache before the holder existed.
        // Repeated switches detect cached empty/full shapes or an early AIR lookup.
        for (Item current : new Item[]{item.get(), Items.STICK, null, item.get(), null}) {
            held.item = current;
            var expected = current == item.get() ? Shapes.block() : Shapes.empty();
            assertSame(expected, block.getShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, held));
            assertSame(expected, state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, held));
        }
        assertSame(Shapes.empty(), state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        state.initCache();
        held.item = item.get();
        assertSame(Shapes.block(), state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, held));
        assertTrue(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty());
        assertEquals(RenderShape.INVISIBLE, state.getRenderShape());
        assertEquals(goop * 5, state.getLightEmission());
        assertEquals(waterlogged ? Fluids.WATER : Fluids.EMPTY, state.getFluidState().getType());
        assertEquals(!waterlogged, state.propagatesSkylightDown());
        assertFalse(state.canOcclude());
        assertTrue(state.canBeReplaced());
    }

    private static final class RecordingGlowGoopBlock extends GlowGoopBlock {
        private RecordingGlowGoopBlock(Properties properties) { super(properties); }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (NaturalistRegistry.GLOW_GOOP == null) earlyShapeCalls++;
            return super.getShape(state, level, pos, context);
        }
    }

    private static final class HeldItemContext implements CollisionContext {
        private Item item;
        @Override public boolean isHoldingItem(Item queried) {
            assertNotNull(queried);
            assertNotSame(Items.AIR, queried, "Only the registered Glow Goop item may be queried");
            assertSame(GlowGoopRegistrationTest.item.get(), queried);
            return item == queried;
        }
        @Override public boolean isDescending() { return false; }
        @Override public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) { return defaultValue; }
        @Override public boolean alwaysCollideWithFluid() { return false; }
        @Override public boolean canStandOnFluid(FluidState first, FluidState second) { return false; }
        @Override public VoxelShape getCollisionShape(BlockState state, CollisionGetter level, BlockPos pos) {
            return state.getCollisionShape(level, pos, this);
        }
    }
}
