package dev.resivore.mynxtrees;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.treedecorators.*;

/** A single bounded pass during successful tree placement; occupied targets are never replaced. */
public final class GroveFlowers extends TreeDecorator {
    public static final MapCodec<GroveFlowers> CODEC = Codec.BOOL.fieldOf("wisteria").xmap(GroveFlowers::new, f -> f.wisteria);
    private static TreeDecoratorType<GroveFlowers> TYPE;
    private final boolean wisteria;
    public GroveFlowers(boolean wisteria) { this.wisteria = wisteria; }
    public static void register() { TYPE = Registry.register(BuiltInRegistries.TREE_DECORATOR_TYPE, MynxTrees.id("grove_flowers"), new TreeDecoratorType<>(CODEC)); }
    @Override protected TreeDecoratorType<?> type() { return TYPE; }
    @Override public void place(Context context) {
        if (context.logs().isEmpty()) return;
        BlockPos base = context.logs().getFirst();
        Block flower = wisteria ? MynxTrees.SWEET_VIOLETS : Blocks.PINK_PETALS;
        var random = context.random();
        for (int attempt=0; attempt<48; ++attempt) {
            int dx=random.nextInt(11)-5, dz=random.nextInt(11)-5;
            if (dx*dx+dz*dz>25) continue;
            for (int dy=2; dy>=-2; --dy) {
                BlockPos pos=base.offset(dx,dy,dz);
                var state=flower.defaultBlockState().setValue(FlowerBedBlock.AMOUNT, 1+random.nextInt(4))
                    .setValue(FlowerBedBlock.FACING, Direction.from2DDataValue(random.nextInt(4)));
                if (context.level().isOutsideBuildHeight(pos) || !context.isAir(pos) || !context.level().getFluidState(pos).isEmpty()) continue;
                if (state.canSurvive(context.level(), pos)) { context.setBlock(pos,state); break; }
            }
        }
    }
}
