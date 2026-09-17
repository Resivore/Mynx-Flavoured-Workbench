package dev.resivore.slabdecorations.gametest;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Registers test-only geometry needed to exercise provider-supplied canonical parents. */
public final class SystemicFixtureInitializer implements ModInitializer {
    public static final Identifier FARMLAND_SLAB_ID = Identifier.fromNamespaceAndPath(
            "slab-decorations-fixtures", "farmland_slab");
    public static final Block FARMLAND_SLAB = new SlabBlock(BlockBehaviour.Properties
            .ofFullCopy(Blocks.FARMLAND)
            .setId(ResourceKey.create(Registries.BLOCK, FARMLAND_SLAB_ID)));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.BLOCK,
                ResourceKey.create(Registries.BLOCK, FARMLAND_SLAB_ID), FARMLAND_SLAB);
    }

    public static synchronized void ensureFarmlandProfile() {
        if (NibaruMaterialProfiles.fromBlock(FARMLAND_SLAB).isPresent()) return;
        // BGE C70 deliberately has no built-in Farmland family. This fixture proves that Slab
        // Decorations honors an external provider's canonical-parent profile without pretending
        // a Dirt slab is Farmland or adding any production registry exception. Registration is
        // deliberately deferred until the GameTest runs so this orphan fixture does not enter
        // CNM's provider ShapeMap during the preceding datapack reload.
        NibaruMaterialProfiles.registerExternal(new NibaruMaterialProfile(
                NibaruMaterialProfiles.PROFILE_VERSION,
                null,
                Blocks.FARMLAND,
                BuiltInRegistries.BLOCK.getKey(Blocks.FARMLAND),
                Optional.of(FARMLAND_SLAB),
                Optional.empty(),
                Optional.empty(),
                Optional.of(FARMLAND_SLAB),
                Optional.empty(),
                Optional.of(FARMLAND_SLAB_ID),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Set.of(),
                VisualProfile.UNIFORM,
                NibaruMaterialProfile.VisualSupport.GENERIC_SUPPORTED,
                TintProfile.NONE,
                NibaruMaterialProfile.RenderLayer.SOLID,
                NibaruMaterialProfile.OrientationPolicy.UNIFORM,
                NibaruMaterialProfile.SurfaceSamplingPolicy.BLOCK_ABSOLUTE,
                NibaruMaterialProfile.DoubleFormPolicy.COMPOSE_SEMANTIC_SURFACES,
                new NibaruMaterialProfile.TextureRoles(
                        "farmland", "farmland", "dirt", "", "farmland"),
                Optional.empty(),
                Optional.empty(),
                false,
                List.of()));
    }
}
