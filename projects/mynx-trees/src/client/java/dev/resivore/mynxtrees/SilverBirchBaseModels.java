package dev.resivore.mynxtrees;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

final class SilverBirchBaseModels {
    static final TagKey<Block> SOILS = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath("mynx_trees", "silver_birch_base_soils"));
    private static final Identifier BASE = Identifier.fromNamespaceAndPath("mynx_trees", "block/silver_birch_log_base");

    static boolean uprightLog(BlockState state) {
        return state != null && state.is(MynxTrees.SILVER_LOG)
                && state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y;
    }

    static void register() {
        ModelLoadingPlugin.register(context -> context.modifyBlockModelOnLoad().register(
                ModelModifier.WRAP_PHASE, (original, modelContext) -> uprightLog(modelContext.state())
                        ? new Unbaked(original) : original));
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (client) {
                Minecraft minecraft = Minecraft.getInstance();
                // Only an actual tag sync invalidates terrain. No scans or per-tick work.
                minecraft.execute(() -> {
                    if (minecraft.level != null) minecraft.levelExtractor.allChanged();
                });
            }
        });
    }

    private record Unbaked(BlockStateModel.UnbakedRoot original) implements BlockStateModel.UnbakedRoot {
        private static final SimpleUnbakedExtraModel<BlockStateModel> BASE_MODEL = SimpleUnbakedExtraModel.blockStateModel(BASE);

        @Override public void resolveDependencies(ResolvableModel.Resolver resolver) {
            original.resolveDependencies(resolver);
            BASE_MODEL.resolveDependencies(resolver);
        }

        @Override public BlockStateModel bake(BlockState state, ModelBaker baker) {
            // Both delegates belong to this resource reload; never retain atlas/model globals.
            return new SilverBirchBaseModel(original.bake(state, baker), BASE_MODEL.bake(baker));
        }

        @Override public Object visualEqualityGroup(BlockState state) { return this; }
    }
}
