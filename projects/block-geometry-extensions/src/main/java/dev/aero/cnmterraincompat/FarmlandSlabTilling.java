package dev.aero.cnmterraincompat;

import com.mojang.datafixers.util.Pair;
import dev.aero.cnmterraincompat.mixin.HoeItemAccessor;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathSemantics;
import net.fabricmc.fabric.api.registry.TillableBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Bridges exact vanilla HoeItem rules onto the corresponding BGE horizontal slabs. */
public final class FarmlandSlabTilling {
    private static boolean registered;

    private FarmlandSlabTilling() {}

    public static synchronized void register() {
        if (registered) return;

        Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> vanilla =
                HoeItemAccessor.bge$getTillables();
        Block farmland = CnmTerrainCompat.FARMLAND_SLAB;
        Block dirt = ModBlocks.DIRT.getBlock(ModBlocks.BlockType.SLAB);
        List<Rule> rules = List.of(
                new Rule(Blocks.GRASS_BLOCK, ModBlocks.GRASS_BLOCK.getBlock(ModBlocks.BlockType.SLAB), farmland, false),
                new Rule(Blocks.DIRT_PATH, ModBlocks.DIRT_PATH.getBlock(ModBlocks.BlockType.SLAB), farmland, false),
                new Rule(Blocks.DIRT, ModBlocks.DIRT.getBlock(ModBlocks.BlockType.SLAB), farmland, false),
                new Rule(Blocks.COARSE_DIRT, ModBlocks.COARSE_DIRT.getBlock(ModBlocks.BlockType.SLAB), dirt, false),
                new Rule(Blocks.ROOTED_DIRT, ModBlocks.ROOTED_DIRT.getBlock(ModBlocks.BlockType.SLAB), dirt, true));

        Set<Block> expected = new LinkedHashSet<>();
        rules.forEach(rule -> expected.add(rule.vanillaSource()));
        Set<Block> actualProfileSources = new LinkedHashSet<>();
        for (Block source : Set.copyOf(vanilla.keySet())) {
            if (NibaruMaterialProfiles.fromBlock(source)
                    .flatMap(profile -> profile.nativeSlab()).isPresent()) {
                actualProfileSources.add(source);
            }
        }
        if (!actualProfileSources.equals(expected)) {
            throw new IllegalStateException("Minecraft 26.2 HoeItem/BGE slab coverage drift: expected="
                    + expected + ", actual=" + actualProfileSources);
        }

        for (Rule rule : rules) {
            register(rule, vanilla);
        }

        // These hidden compatibility identities are real BGE slabs too, but never become
        // the canonical Dirt return/drop target.
        registerAlias(Blocks.DIRT, CnmTerrainCompat.DIRT_SLAB, farmland, vanilla);
        registerAlias(Blocks.GRASS_BLOCK, CnmTerrainCompat.GRASS_SLAB, farmland, vanilla);

        registered = true;
    }

    private static void register(Rule rule,
            Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> vanilla) {
        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> vanillaRule = vanilla.get(rule.vanillaSource());
        if (vanillaRule == null) {
            throw new IllegalStateException("Missing vanilla tilling rule for " + rule.vanillaSource());
        }
        TillableBlockRegistry.register(rule.slabSource(),
                dry(vanillaRule.getFirst()), changeInto(rule.slabTarget(), rule.dropHangingRoots()));
    }

    private static void registerAlias(Block vanillaSource, Block alias, Block target,
            Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> vanilla) {
        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> vanillaRule = vanilla.get(vanillaSource);
        if (vanillaRule == null) {
            throw new IllegalStateException("Missing vanilla tilling rule for compatibility alias "
                    + vanillaSource);
        }
        TillableBlockRegistry.register(alias, dry(vanillaRule.getFirst()), changeInto(target, false));
    }

    private static Predicate<UseOnContext> dry(Predicate<UseOnContext> vanillaPredicate) {
        return context -> {
            BlockState state = context.getLevel().getBlockState(context.getClickedPos());
            return state.hasProperty(SlabBlock.WATERLOGGED)
                    && !state.getValue(SlabBlock.WATERLOGGED)
                    && vanillaPredicate.test(context);
        };
    }

    private static Consumer<UseOnContext> changeInto(Block targetBlock, boolean dropHangingRoots) {
        return context -> {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockState source = level.getBlockState(pos);
            BlockState target = PathSemantics.copySharedProperties(source, targetBlock.defaultBlockState());
            level.setBlock(pos, target, Block.UPDATE_ALL_IMMEDIATE);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(context.getPlayer(), target));
            if (dropHangingRoots) {
                Block.popResourceFromFace(level, pos, context.getClickedFace(),
                        new ItemStack(Items.HANGING_ROOTS));
            }
        };
    }

    private record Rule(Block vanillaSource, Block slabSource, Block slabTarget,
            boolean dropHangingRoots) {}
}
