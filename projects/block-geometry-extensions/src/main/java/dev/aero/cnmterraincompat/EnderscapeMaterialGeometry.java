package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.redstone.Orientation;
import net.penumbra.enderscape.block.BlisteredMagniaBlock;
import net.penumbra.enderscape.block.HasMagniaPolarity;
import net.penumbra.enderscape.block.HasMagniaPowerSignal;
import net.penumbra.enderscape.block.MagniaBlock;
import net.penumbra.enderscape.block.properties.MagniaPolarity;
import net.penumbra.enderscape.block.state.OptionalMagniaPolarityProperty;
import net.penumbra.enderscape.block.state.StateProperties;
import net.penumbra.enderscape.registry.sound.EnderscapeBlockSounds;
import net.penumbra.enderscape.util.MagniaUtil;

import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Optional Enderscape companions for the state bridges declared in
 * {@link ExternalMaterialStateBridge}.  This class is resolved only after the Enderscape
 * provider has completed; ordinary BGE startup never loads these optional API types.
 */
@SuppressWarnings("deprecation")
final class EnderscapeMaterialGeometry {
    private static final ThreadLocal<ExternalMaterialStateBridge> CONSTRUCTING_BRIDGE = new ThreadLocal<>();
    private EnderscapeMaterialGeometry() {}

    static SlabBlock slab(ExternalMaterialStateBridge bridge, Block source,
            BlockBehaviour.Properties properties) {
        return construct(bridge, () -> bridge.isMagnia() ? new MagniaSlab(bridge, properties)
                : new BlinklmpSlab(bridge, properties));
    }

    static StairBlock stairs(ExternalMaterialStateBridge bridge, Block source,
            BlockBehaviour.Properties properties) {
        return construct(bridge, () -> bridge.isMagnia() ? new MagniaStairs(bridge, source.defaultBlockState(), properties)
                : new BlinklmpStairs(bridge, source.defaultBlockState(), properties));
    }

    static WallBlock wall(ExternalMaterialStateBridge bridge, BlockBehaviour.Properties properties) {
        return construct(bridge, () -> bridge.isMagnia() ? new MagniaWall(bridge, properties) : new BlinklmpWall(bridge, properties));
    }

    static VerticalSlabBlock vertical(ExternalMaterialStateBridge bridge, BlockBehaviour.Properties properties) {
        return construct(bridge, () -> bridge.isMagnia() ? new MagniaVertical(bridge, properties)
                : new BlinklmpVertical(bridge, properties));
    }

    static StepBlock step(ExternalMaterialStateBridge bridge, BlockBehaviour.Properties properties) {
        return construct(bridge, () -> bridge.isMagnia() ? new MagniaStep(bridge, properties) : new BlinklmpStep(bridge, properties));
    }

    static BgeLayerBlock layer(ExternalMaterialStateBridge bridge, NibaruMaterialProfile profile,
            BlockBehaviour.Properties properties) {
        return construct(bridge, () -> bridge.isMagnia() ? new MagniaLayer(bridge, profile, properties)
                : new BlinklmpLayer(bridge, profile, properties));
    }

    static BgeCornerBlock corner(ExternalMaterialStateBridge bridge, NibaruMaterialProfile profile,
            BlockBehaviour.Properties properties) {
        return construct(bridge, () -> bridge.isMagnia() ? new MagniaCorner(bridge, profile, properties)
                : new BlinklmpCorner(bridge, profile, properties));
    }

    static BgeColumnBlock column(ExternalMaterialStateBridge bridge, NibaruMaterialProfile profile,
            BlockBehaviour.Properties properties) {
        return construct(bridge, () -> bridge.isMagnia() ? new MagniaColumn(bridge, profile, properties)
                : new BlinklmpColumn(bridge, profile, properties));
    }

    private static <T extends Block> T construct(ExternalMaterialStateBridge bridge, Supplier<T> constructor) {
        if (CONSTRUCTING_BRIDGE.get() != null) throw new IllegalStateException("Nested Enderscape bridge construction");
        CONSTRUCTING_BRIDGE.set(bridge);
        try {
            return constructor.get();
        } finally {
            CONSTRUCTING_BRIDGE.remove();
        }
    }

    /** Magnia carriers intentionally expose the exact optional-provider interfaces. */
    private interface MagniaCarrier extends HasMagniaPolarity, HasMagniaPowerSignal {
        ExternalMaterialStateBridge bridge();

        @Override
        default Optional<MagniaPolarity> getPolarity(BlockState state) {
            return Support.polarity(bridge(), state);
        }

        @Override
        default int getMagniaPowerSignal(BlockState state, BlockState requester) {
            return Support.magniaSignal(bridge(), state, requester);
        }
    }

    private static final class MagniaSlab extends SlabBlock implements MagniaCarrier {
        private final ExternalMaterialStateBridge bridge;
        private MagniaSlab(ExternalMaterialStateBridge bridge, Properties p) { super(p); this.bridge = bridge; reset(); }
        @Override public ExternalMaterialStateBridge bridge() { return bridge; }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); Support.add(b, Support.constructingBridge()); }
        private void reset() { registerDefaultState(Support.defaults(bridge, defaultBlockState())); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return Support.place(bridge, super.getStateForPlacement(c), c, this); }
        @Override protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess t, BlockPos p, Direction d, BlockPos n, BlockState ns, RandomSource r) { return Support.update(bridge, super.updateShape(s,l,t,p,d,n,ns,r), l,t,p,this); }
        @Override protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) { super.tick(s,l,p,r); Support.tick(bridge,s,l,p,this); }
        @Override protected void neighborChanged(BlockState s, Level l, BlockPos p, Block n, Orientation o, boolean moved) { super.neighborChanged(s,l,p,n,o,moved); Support.neighborChanged(bridge,s,l,p,this); }
        @Override protected void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean notify) { super.onPlace(s,l,p,old,notify); Support.onPlace(bridge,s,l,p); }
        @Override protected boolean hasAnalogOutputSignal(BlockState s) { return Support.analog(bridge) || super.hasAnalogOutputSignal(s); }
        @Override protected int getAnalogOutputSignal(BlockState s, Level l, BlockPos p, Direction d) { return Support.analog(bridge) ? MagniaBlock.getPower(s) : super.getAnalogOutputSignal(s,l,p,d); }
    }

    private static final class MagniaStairs extends StairBlock implements MagniaCarrier {
        private final ExternalMaterialStateBridge bridge;
        private MagniaStairs(ExternalMaterialStateBridge bridge, BlockState source, Properties p) { super(source,p); this.bridge=bridge; reset(); }
        @Override public ExternalMaterialStateBridge bridge() { return bridge; }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); Support.add(b,Support.constructingBridge()); }
        private void reset(){ registerDefaultState(Support.defaults(bridge,defaultBlockState())); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){ return Support.place(bridge,super.getStateForPlacement(c),c,this); }
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos n,BlockState ns,RandomSource r){ return Support.update(bridge,super.updateShape(s,l,t,p,d,n,ns,r),l,t,p,this); }
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){ super.tick(s,l,p,r); Support.tick(bridge,s,l,p,this); }
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,net.minecraft.world.level.redstone.Orientation o,boolean moved){ super.neighborChanged(s,l,p,n,o,moved); Support.neighborChanged(bridge,s,l,p,this); }
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean notify){ super.onPlace(s,l,p,old,notify); Support.onPlace(bridge,s,l,p); }
        @Override protected boolean hasAnalogOutputSignal(BlockState s){ return Support.analog(bridge)||super.hasAnalogOutputSignal(s); }
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){ return Support.analog(bridge)?MagniaBlock.getPower(s):super.getAnalogOutputSignal(s,l,p,d); }
    }

    private static final class MagniaWall extends WallBlock implements MagniaCarrier {
        private final ExternalMaterialStateBridge bridge;
        private MagniaWall(ExternalMaterialStateBridge bridge, Properties p){ super(p); this.bridge=bridge; reset(); }
        @Override public ExternalMaterialStateBridge bridge(){ return bridge; }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){ super.createBlockStateDefinition(b); Support.add(b,Support.constructingBridge()); }
        private void reset(){ registerDefaultState(Support.defaults(bridge,defaultBlockState())); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){ return Support.place(bridge,super.getStateForPlacement(c),c,this); }
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos n,BlockState ns,RandomSource r){ return Support.update(bridge,super.updateShape(s,l,t,p,d,n,ns,r),l,t,p,this); }
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){ super.tick(s,l,p,r); Support.tick(bridge,s,l,p,this); }
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean moved){ super.neighborChanged(s,l,p,n,o,moved); Support.neighborChanged(bridge,s,l,p,this); }
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean notify){ super.onPlace(s,l,p,old,notify); Support.onPlace(bridge,s,l,p); }
        @Override protected boolean hasAnalogOutputSignal(BlockState s){ return Support.analog(bridge)||super.hasAnalogOutputSignal(s); }
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){ return Support.analog(bridge)?MagniaBlock.getPower(s):super.getAnalogOutputSignal(s,l,p,d); }
    }

    private static final class MagniaVertical extends VerticalSlabBlock implements MagniaCarrier {
        private final ExternalMaterialStateBridge bridge;
        private MagniaVertical(ExternalMaterialStateBridge bridge, Properties p){ super(p); this.bridge=bridge; reset(); }
        @Override public ExternalMaterialStateBridge bridge(){ return bridge; }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){ super.createBlockStateDefinition(b); Support.add(b,Support.constructingBridge()); }
        private void reset(){ registerDefaultState(Support.defaults(bridge,defaultBlockState())); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){ return Support.place(bridge,super.getStateForPlacement(c),c,this); }
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos n,BlockState ns,RandomSource r){ return Support.update(bridge,super.updateShape(s,l,t,p,d,n,ns,r),l,t,p,this); }
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){ super.tick(s,l,p,r); Support.tick(bridge,s,l,p,this); }
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean moved){ super.neighborChanged(s,l,p,n,o,moved); Support.neighborChanged(bridge,s,l,p,this); }
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean notify){ super.onPlace(s,l,p,old,notify); Support.onPlace(bridge,s,l,p); }
        @Override protected boolean hasAnalogOutputSignal(BlockState s){ return Support.analog(bridge)||super.hasAnalogOutputSignal(s); }
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){ return Support.analog(bridge)?MagniaBlock.getPower(s):super.getAnalogOutputSignal(s,l,p,d); }
    }

    private static final class MagniaStep extends StepBlock implements MagniaCarrier {
        private final ExternalMaterialStateBridge bridge;
        private MagniaStep(ExternalMaterialStateBridge bridge, Properties p){ super(p); this.bridge=bridge; reset(); }
        @Override public ExternalMaterialStateBridge bridge(){ return bridge; }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){ super.createBlockStateDefinition(b); Support.add(b,Support.constructingBridge()); }
        private void reset(){ registerDefaultState(Support.defaults(bridge,defaultBlockState())); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){ return Support.place(bridge,super.getStateForPlacement(c),c,this); }
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos n,BlockState ns,RandomSource r){ return Support.update(bridge,super.updateShape(s,l,t,p,d,n,ns,r),l,t,p,this); }
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){ super.tick(s,l,p,r); Support.tick(bridge,s,l,p,this); }
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean moved){ super.neighborChanged(s,l,p,n,o,moved); Support.neighborChanged(bridge,s,l,p,this); }
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean notify){ super.onPlace(s,l,p,old,notify); Support.onPlace(bridge,s,l,p); }
        @Override protected boolean hasAnalogOutputSignal(BlockState s){ return Support.analog(bridge)||super.hasAnalogOutputSignal(s); }
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){ return Support.analog(bridge)?MagniaBlock.getPower(s):super.getAnalogOutputSignal(s,l,p,d); }
    }

    private static final class MagniaLayer extends BgeLayerBlock implements MagniaCarrier {
        private final ExternalMaterialStateBridge bridge;
        private MagniaLayer(ExternalMaterialStateBridge bridge,NibaruMaterialProfile profile,Properties p){ super(profile,p); this.bridge=bridge; reset(); }
        @Override public ExternalMaterialStateBridge bridge(){ return bridge; }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){ super.createBlockStateDefinition(b); Support.add(b,Support.constructingBridge()); }
        private void reset(){ registerDefaultState(Support.defaults(bridge,defaultBlockState())); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){ return Support.place(bridge,super.getStateForPlacement(c),c,this); }
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos n,BlockState ns,RandomSource r){ return Support.update(bridge,super.updateShape(s,l,t,p,d,n,ns,r),l,t,p,this); }
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){ super.tick(s,l,p,r); Support.tick(bridge,s,l,p,this); }
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean moved){ super.neighborChanged(s,l,p,n,o,moved); Support.neighborChanged(bridge,s,l,p,this); }
        @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean notify){ super.onPlace(s,l,p,old,notify); Support.onPlace(bridge,s,l,p); }
        @Override protected boolean hasAnalogOutputSignal(BlockState s){ return Support.analog(bridge)||super.hasAnalogOutputSignal(s); }
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){ return Support.analog(bridge)?MagniaBlock.getPower(s):super.getAnalogOutputSignal(s,l,p,d); }
    }

    private static final class MagniaCorner extends BgeCornerBlock implements MagniaCarrier {
        private final ExternalMaterialStateBridge bridge;
        private MagniaCorner(ExternalMaterialStateBridge bridge,NibaruMaterialProfile profile,Properties p){ super(profile,p); this.bridge=bridge; reset(); }
        @Override public ExternalMaterialStateBridge bridge(){ return bridge; }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){ super.createBlockStateDefinition(b); Support.add(b,Support.constructingBridge()); }
        private void reset(){ registerDefaultState(Support.defaults(bridge,defaultBlockState())); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){ return Support.place(bridge,super.getStateForPlacement(c),c,this); }
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos n,BlockState ns,RandomSource r){ return Support.update(bridge,super.updateShape(s,l,t,p,d,n,ns,r),l,t,p,this); }
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){ super.tick(s,l,p,r); Support.tick(bridge,s,l,p,this); }
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,net.minecraft.world.level.redstone.Orientation o,boolean moved){ super.neighborChanged(s,l,p,n,o,moved); Support.neighborChanged(bridge,s,l,p,this); }
        @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean notify){ super.onPlace(s,l,p,old,notify); Support.onPlace(bridge,s,l,p); }
        @Override protected boolean hasAnalogOutputSignal(BlockState s){ return Support.analog(bridge)||super.hasAnalogOutputSignal(s); }
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){ return Support.analog(bridge)?MagniaBlock.getPower(s):super.getAnalogOutputSignal(s,l,p,d); }
    }

    private static final class MagniaColumn extends BgeColumnBlock implements MagniaCarrier {
        private final ExternalMaterialStateBridge bridge;
        private MagniaColumn(ExternalMaterialStateBridge bridge,NibaruMaterialProfile profile,Properties p){ super(profile,p); this.bridge=bridge; reset(); }
        @Override public ExternalMaterialStateBridge bridge(){ return bridge; }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){ super.createBlockStateDefinition(b); Support.add(b,Support.constructingBridge()); }
        private void reset(){ registerDefaultState(Support.defaults(bridge,defaultBlockState())); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){ return Support.place(bridge,super.getStateForPlacement(c),c,this); }
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos n,BlockState ns,RandomSource r){ return Support.update(bridge,super.updateShape(s,l,t,p,d,n,ns,r),l,t,p,this); }
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){ super.tick(s,l,p,r); Support.tick(bridge,s,l,p,this); }
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean moved){ super.neighborChanged(s,l,p,n,o,moved); Support.neighborChanged(bridge,s,l,p,this); }
        @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean notify){ super.onPlace(s,l,p,old,notify); Support.onPlace(bridge,s,l,p); }
        @Override protected boolean hasAnalogOutputSignal(BlockState s){ return Support.analog(bridge)||super.hasAnalogOutputSignal(s); }
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){ return Support.analog(bridge)?MagniaBlock.getPower(s):super.getAnalogOutputSignal(s,l,p,d); }
    }

    /* Blinklamp does not implement a Magnia contract: only its material luminance is projected. */
    private static final class BlinklmpSlab extends SlabBlock { private final ExternalMaterialStateBridge bridge; BlinklmpSlab(ExternalMaterialStateBridge b,Properties p){super(p);bridge=b;reset();} @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);Support.add(b,bridge);} private void reset(){registerDefaultState(Support.defaults(bridge,defaultBlockState()));} @Override public BlockState getStateForPlacement(BlockPlaceContext c){return Support.place(bridge,super.getStateForPlacement(c),c,this);} @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean m){super.neighborChanged(s,l,p,n,o,m);Support.neighborChanged(bridge,s,l,p,this);} @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){super.tick(s,l,p,r);Support.tick(bridge,s,l,p,this);} }
    private static final class BlinklmpStairs extends StairBlock { private final ExternalMaterialStateBridge bridge; BlinklmpStairs(ExternalMaterialStateBridge b,BlockState s,Properties p){super(s,p);bridge=b;reset();} @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);Support.add(b,bridge);} private void reset(){registerDefaultState(Support.defaults(bridge,defaultBlockState()));} @Override public BlockState getStateForPlacement(BlockPlaceContext c){return Support.place(bridge,super.getStateForPlacement(c),c,this);} @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean m){super.neighborChanged(s,l,p,n,o,m);Support.neighborChanged(bridge,s,l,p,this);} @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){super.tick(s,l,p,r);Support.tick(bridge,s,l,p,this);} }
    private static final class BlinklmpWall extends WallBlock { private final ExternalMaterialStateBridge bridge; BlinklmpWall(ExternalMaterialStateBridge b,Properties p){super(p);bridge=b;reset();} @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);Support.add(b,bridge);} private void reset(){registerDefaultState(Support.defaults(bridge,defaultBlockState()));} @Override public BlockState getStateForPlacement(BlockPlaceContext c){return Support.place(bridge,super.getStateForPlacement(c),c,this);} @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean m){super.neighborChanged(s,l,p,n,o,m);Support.neighborChanged(bridge,s,l,p,this);} @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){super.tick(s,l,p,r);Support.tick(bridge,s,l,p,this);} }
    private static final class BlinklmpVertical extends VerticalSlabBlock { private final ExternalMaterialStateBridge bridge; BlinklmpVertical(ExternalMaterialStateBridge b,Properties p){super(p);bridge=b;reset();} @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);Support.add(b,bridge);} private void reset(){registerDefaultState(Support.defaults(bridge,defaultBlockState()));} @Override public BlockState getStateForPlacement(BlockPlaceContext c){return Support.place(bridge,super.getStateForPlacement(c),c,this);} @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean m){super.neighborChanged(s,l,p,n,o,m);Support.neighborChanged(bridge,s,l,p,this);} @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){super.tick(s,l,p,r);Support.tick(bridge,s,l,p,this);} }
    private static final class BlinklmpStep extends StepBlock { private final ExternalMaterialStateBridge bridge; BlinklmpStep(ExternalMaterialStateBridge b,Properties p){super(p);bridge=b;reset();} @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);Support.add(b,bridge);} private void reset(){registerDefaultState(Support.defaults(bridge,defaultBlockState()));} @Override public BlockState getStateForPlacement(BlockPlaceContext c){return Support.place(bridge,super.getStateForPlacement(c),c,this);} @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean m){super.neighborChanged(s,l,p,n,o,m);Support.neighborChanged(bridge,s,l,p,this);} @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){super.tick(s,l,p,r);Support.tick(bridge,s,l,p,this);} }
    private static final class BlinklmpLayer extends BgeLayerBlock { private final ExternalMaterialStateBridge bridge; BlinklmpLayer(ExternalMaterialStateBridge b,NibaruMaterialProfile f,Properties p){super(f,p);bridge=b;reset();} @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);Support.add(b,bridge);} private void reset(){registerDefaultState(Support.defaults(bridge,defaultBlockState()));} @Override public BlockState getStateForPlacement(BlockPlaceContext c){return Support.place(bridge,super.getStateForPlacement(c),c,this);} @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean m){super.neighborChanged(s,l,p,n,o,m);Support.neighborChanged(bridge,s,l,p,this);} @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){super.tick(s,l,p,r);Support.tick(bridge,s,l,p,this);} }
    private static final class BlinklmpCorner extends BgeCornerBlock { private final ExternalMaterialStateBridge bridge; BlinklmpCorner(ExternalMaterialStateBridge b,NibaruMaterialProfile f,Properties p){super(f,p);bridge=b;reset();} @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);Support.add(b,bridge);} private void reset(){registerDefaultState(Support.defaults(bridge,defaultBlockState()));} @Override public BlockState getStateForPlacement(BlockPlaceContext c){return Support.place(bridge,super.getStateForPlacement(c),c,this);} @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,net.minecraft.world.level.redstone.Orientation o,boolean m){super.neighborChanged(s,l,p,n,o,m);Support.neighborChanged(bridge,s,l,p,this);} @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){super.tick(s,l,p,r);Support.tick(bridge,s,l,p,this);} }
    private static final class BlinklmpColumn extends BgeColumnBlock { private final ExternalMaterialStateBridge bridge; BlinklmpColumn(ExternalMaterialStateBridge b,NibaruMaterialProfile f,Properties p){super(f,p);bridge=b;reset();} @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);Support.add(b,bridge);} private void reset(){registerDefaultState(Support.defaults(bridge,defaultBlockState()));} @Override public BlockState getStateForPlacement(BlockPlaceContext c){return Support.place(bridge,super.getStateForPlacement(c),c,this);} @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,Orientation o,boolean m){super.neighborChanged(s,l,p,n,o,m);Support.neighborChanged(bridge,s,l,p,this);} @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){super.tick(s,l,p,r);Support.tick(bridge,s,l,p,this);} }

    private static final class Support {
        private Support() {}

        static ExternalMaterialStateBridge constructingBridge() {
            ExternalMaterialStateBridge bridge = CONSTRUCTING_BRIDGE.get();
            if (bridge == null) throw new IllegalStateException("Bridge state definition created outside its factory");
            return bridge;
        }

        static void add(StateDefinition.Builder<Block, BlockState> builder, ExternalMaterialStateBridge bridge) {
            // Block constructs its state definition from the superclass constructor, before a
            // concrete geometry constructor can assign its bridge field.
            if (bridge == null) bridge = constructingBridge();
            switch (bridge) {
                case ENDERSCAPE_ALLURING_MAGNIA, ENDERSCAPE_REPULSIVE_MAGNIA -> builder.add(MagniaBlock.POWER);
                case ENDERSCAPE_BLISTERED_MAGNIA -> builder.add(BlisteredMagniaBlock.POLARITY);
                case ENDERSCAPE_BLINKLAMP -> builder.add(StateProperties.BLINKLAMP_LUMINANCE);
                case NONE, ENDERSCAPE_NEBULITE -> { }
            }
        }

        static BlockState defaults(ExternalMaterialStateBridge bridge, BlockState state) {
            return switch (bridge) {
                case ENDERSCAPE_ALLURING_MAGNIA, ENDERSCAPE_REPULSIVE_MAGNIA -> state.setValue(MagniaBlock.POWER, 0);
                case ENDERSCAPE_BLISTERED_MAGNIA -> state.setValue(BlisteredMagniaBlock.POLARITY,
                        OptionalMagniaPolarityProperty.NONE);
                case ENDERSCAPE_BLINKLAMP -> state.setValue(StateProperties.BLINKLAMP_LUMINANCE, 7);
                case NONE, ENDERSCAPE_NEBULITE -> state;
            };
        }

        static BlockState place(ExternalMaterialStateBridge bridge, BlockState placed,
                BlockPlaceContext context, Block self) {
            if (placed == null || placed.getBlock() != self) return placed;
            BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
            if (existing.getBlock() == self) return placed; // preserve state while composing geometry.
            return switch (bridge) {
                case ENDERSCAPE_ALLURING_MAGNIA, ENDERSCAPE_REPULSIVE_MAGNIA -> placed.setValue(MagniaBlock.POWER,
                        MagniaUtil.getStrongestPowerSignal(placed, context.getLevel(), context.getClickedPos()));
                case ENDERSCAPE_BLISTERED_MAGNIA -> placed.setValue(BlisteredMagniaBlock.POLARITY,
                        selectPolarity(context.getLevel(), context.getClickedPos()));
                case ENDERSCAPE_BLINKLAMP -> placed.setValue(StateProperties.BLINKLAMP_LUMINANCE,
                        luminance(context.getLevel(), context.getClickedPos()));
                case NONE, ENDERSCAPE_NEBULITE -> placed;
            };
        }

        static BlockState update(ExternalMaterialStateBridge bridge, BlockState state, LevelReader level,
                ScheduledTickAccess ticks, BlockPos pos, Block self) {
            if (bridge.isBlisteredMagnia() && state.getBlock() == self
                    && selectPolarity(level, pos) != state.getValue(BlisteredMagniaBlock.POLARITY)) {
                ticks.scheduleTick(pos, self, 5);
            }
            return state;
        }

        static void neighborChanged(ExternalMaterialStateBridge bridge, BlockState state, Level level,
                BlockPos pos, Block self) {
            if (level.isClientSide()) return;
            if (bridge.isFixedMagnia()) {
                int power = MagniaUtil.getStrongestPowerSignal(state, level, pos);
                if (power != MagniaBlock.getPower(state)) level.setBlockAndUpdate(pos,
                        state.setValue(MagniaBlock.POWER, power));
            } else if (bridge.isBlinklamp()) {
                int luminance = luminance(level, pos);
                if (luminance != state.getValue(StateProperties.BLINKLAMP_LUMINANCE)) level.scheduleTick(pos, self, 5);
            }
        }

        static void tick(ExternalMaterialStateBridge bridge, BlockState state, ServerLevel level,
                BlockPos pos, Block self) {
            if (bridge.isBlisteredMagnia()) {
                OptionalMagniaPolarityProperty selected = selectPolarity(level, pos);
                if (!selected.equals(state.getValue(BlisteredMagniaBlock.POLARITY))) {
                    level.playSound(null, pos, selected == OptionalMagniaPolarityProperty.NONE
                                    ? EnderscapeBlockSounds.BLISTERED_MAGNIA_POWER_OFF
                                    : EnderscapeBlockSounds.BLISTERED_MAGNIA_POWER_ON,
                            net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.setBlockAndUpdate(pos, state.setValue(BlisteredMagniaBlock.POLARITY, selected));
                }
                level.updateNeighborsAt(pos, self);
            } else if (bridge.isBlinklamp()) {
                int selected = luminance(level, pos);
                int current = state.getValue(StateProperties.BLINKLAMP_LUMINANCE);
                if (selected != current) {
                    level.setBlock(pos, state.setValue(StateProperties.BLINKLAMP_LUMINANCE, selected), 3);
                    level.playSound(null, pos, selected > current ? EnderscapeBlockSounds.BLINKLAMP_INCREASE
                                    : EnderscapeBlockSounds.BLINKLAMP_DECREASE,
                            net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }

        static void onPlace(ExternalMaterialStateBridge bridge, BlockState state, Level level, BlockPos pos) {
            if (bridge.isBlisteredMagnia()) level.playSound(null, pos,
                    state.getValue(BlisteredMagniaBlock.POLARITY) == OptionalMagniaPolarityProperty.NONE
                            ? EnderscapeBlockSounds.BLISTERED_MAGNIA_POWER_OFF
                            : EnderscapeBlockSounds.BLISTERED_MAGNIA_POWER_ON,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            if (bridge.isFixedMagnia() && !level.isClientSide()) level.updateNeighborsAt(pos, state.getBlock());
        }

        static boolean analog(ExternalMaterialStateBridge bridge) { return bridge.isFixedMagnia(); }

        static Optional<MagniaPolarity> polarity(ExternalMaterialStateBridge bridge, BlockState state) {
            return switch (bridge) {
                case ENDERSCAPE_ALLURING_MAGNIA -> Optional.of(MagniaPolarity.ALLURING);
                case ENDERSCAPE_REPULSIVE_MAGNIA -> Optional.of(MagniaPolarity.REPULSIVE);
                case ENDERSCAPE_BLISTERED_MAGNIA -> state.getValue(BlisteredMagniaBlock.POLARITY).optional();
                default -> Optional.empty();
            };
        }

        static int magniaSignal(ExternalMaterialStateBridge bridge, BlockState state, BlockState requester) {
            if (bridge == ExternalMaterialStateBridge.ENDERSCAPE_NEBULITE) return 15;
            if (bridge.isFixedMagnia()) return MagniaUtil.isMatchingPolarity(state, requester)
                    ? MagniaBlock.getPower(state) : 0;
            return bridge.isBlisteredMagnia() && MagniaUtil.isMatchingPolarity(state, requester) ? 15 : 0;
        }

        private static int luminance(Level level, BlockPos pos) {
            return (15 - level.getBestNeighborSignal(pos)) * 7 / 15;
        }

        /** Canonical selection plus BGE fixed-Magnia carriers, whose material is still Magnia. */
        private static OptionalMagniaPolarityProperty selectPolarity(LevelReader level, BlockPos pos) {
            EnumMap<MagniaPolarity, Integer> counts = new EnumMap<>(MagniaPolarity.class);
            for (Direction direction : Direction.values()) {
                BlockState neighbor = level.getBlockState(pos.relative(direction));
                boolean canonicalMagnia = neighbor.getBlock() instanceof MagniaBlock;
                boolean bridgedMagnia = neighbor.getBlock() instanceof MagniaCarrier carrier
                        && carrier.bridge().isFixedMagnia();
                if ((canonicalMagnia || bridgedMagnia) && HasMagniaPolarity.has(neighbor)) {
                    HasMagniaPolarity.optional(neighbor).ifPresent(polarity -> counts.merge(polarity, 1, Integer::sum));
                }
            }
            if (counts.isEmpty()) return OptionalMagniaPolarityProperty.NONE;
            return counts.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).orElseThrow()
                    .getKey().equals(MagniaPolarity.ALLURING)
                    ? OptionalMagniaPolarityProperty.ALLURING : OptionalMagniaPolarityProperty.REPULSIVE;
        }
    }
}
