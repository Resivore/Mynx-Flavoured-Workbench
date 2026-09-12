package dev.resivore.macawsmoreculling;

import ca.fxco.moreculling.api.block.MoreBlockCulling;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class MacawsTrapdoorsMoreCullingCompat implements ClientModInitializer {
    private static final String MACAWS_VERSION = "1.1.5";
    private static final String MORECULLING_VERSION = "1.8.1";

    private static final List<String> AFFECTED_BLOCK_IDS = List.of(
            "mcwtrpdoors:acacia_bark_trapdoor",
            "mcwtrpdoors:acacia_ranch_trapdoor",
            "mcwtrpdoors:bamboo_bark_trapdoor",
            "mcwtrpdoors:birch_bark_trapdoor",
            "mcwtrpdoors:birch_ranch_trapdoor",
            "mcwtrpdoors:cherry_bark_trapdoor",
            "mcwtrpdoors:cherry_ranch_trapdoor",
            "mcwtrpdoors:crimson_bark_trapdoor",
            "mcwtrpdoors:crimson_ranch_trapdoor",
            "mcwtrpdoors:dark_oak_bark_trapdoor",
            "mcwtrpdoors:dark_oak_ranch_trapdoor",
            "mcwtrpdoors:jungle_bark_trapdoor",
            "mcwtrpdoors:jungle_ranch_trapdoor",
            "mcwtrpdoors:mangrove_bark_trapdoor",
            "mcwtrpdoors:mangrove_ranch_trapdoor",
            "mcwtrpdoors:oak_bark_trapdoor",
            "mcwtrpdoors:oak_ranch_trapdoor",
            "mcwtrpdoors:pale_oak_bark_trapdoor",
            "mcwtrpdoors:pale_oak_ranch_trapdoor",
            "mcwtrpdoors:spruce_bark_trapdoor",
            "mcwtrpdoors:spruce_ranch_trapdoor",
            "mcwtrpdoors:warped_bark_trapdoor",
            "mcwtrpdoors:warped_ranch_trapdoor"
    );

    private boolean applied;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (this.applied) {
                return;
            }
            disableAffectedNeighborOcclusion();
            this.applied = true;
        });
    }

    private static void disableAffectedNeighborOcclusion() {
        requireExactProvider("mcwtrpdoors", MACAWS_VERSION);
        requireExactProvider("moreculling", MORECULLING_VERSION);

        List<Block> affectedBlocks = AFFECTED_BLOCK_IDS.stream()
                .map(Identifier::parse)
                .map(MacawsTrapdoorsMoreCullingCompat::requireBlock)
                .toList();

        for (Block block : affectedBlocks) {
            MoreBlockCulling culling = (MoreBlockCulling) block;
            culling.moreculling$setCanCull(false);
            if (culling.moreculling$canCull()) {
                throw new IllegalStateException("MoreCulling refused the per-block non-culling contract for "
                        + BuiltInRegistries.BLOCK.getKey(block));
            }
        }
    }

    private static Block requireBlock(Identifier id) {
        Holder.Reference<Block> holder = BuiltInRegistries.BLOCK.get(id)
                .orElseThrow(() -> new IllegalStateException("Audited Macaw block is missing: " + id));
        Block block = holder.value();
        if (!id.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            throw new IllegalStateException("Audited Macaw registry identity drifted: " + id);
        }
        return block;
    }

    private static void requireExactProvider(String modId, String expectedVersion) {
        String actualVersion = FabricLoader.getInstance().getModContainer(modId)
                .orElseThrow(() -> new IllegalStateException("Required provider is missing: " + modId))
                .getMetadata().getVersion().getFriendlyString();
        if (!expectedVersion.equals(actualVersion)) {
            throw new IllegalStateException("Unsupported " + modId + " version " + actualVersion
                    + "; expected exact audited version " + expectedVersion);
        }
    }
}

