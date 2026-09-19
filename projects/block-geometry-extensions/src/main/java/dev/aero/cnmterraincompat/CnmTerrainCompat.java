package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.MoreSlabsStairsAndWalls;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableSemantics;
import games.twinhead.moreslabsstairsandwalls.registry.fabric.ModRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class CnmTerrainCompat implements ModInitializer {
    public static final String MOD_ID = "cnm_terrain_slabs_compat";
    public static final Identifier DIRT_VERTICAL_SLAB_ID = id("dirt_vertical_slab");
    public static final Identifier GRASS_VERTICAL_SLAB_ID = id("grass_vertical_slab");
    public static final Identifier DIRT_SLAB_ID = id("dirt_slab");
    public static final Identifier GRASS_SLAB_ID = id("grass_slab");
    /** Stable public identity consumed by crop-compatibility integrations. */
    public static final Identifier FARMLAND_SLAB_ID = id("farmland_slab");

    public static final VerticalSlabBlock DIRT_VERTICAL_SLAB = new DirtVerticalSlab(
            properties(DIRT_VERTICAL_SLAB_ID, Blocks.DIRT));
    public static final VerticalSlabBlock GRASS_VERTICAL_SLAB = new GrassVerticalSlab(
            properties(GRASS_VERTICAL_SLAB_ID, Blocks.GRASS_BLOCK));
    public static final DirtHorizontalSlab DIRT_SLAB = new DirtHorizontalSlab(
            properties(DIRT_SLAB_ID, Blocks.DIRT));
    public static final GrassHorizontalSlab GRASS_SLAB = new GrassHorizontalSlab(
            properties(GRASS_SLAB_ID, Blocks.GRASS_BLOCK));
    /** The one canonical horizontal BGE representation of vanilla Farmland. */
    public static final FarmlandSlabBlock FARMLAND_SLAB = new FarmlandSlabBlock(
            properties(FARMLAND_SLAB_ID, Blocks.FARMLAND));
    private static boolean nativeCatalogRegistered;
    private static boolean bgeBaseRegistered;
    private static boolean bgeGeometryRegistered;

    @Override
    public void onInitialize() {
        initializeNativeCatalog();
        registerBgeBase();
    }

    /**
     * Phase 1/2 of the unified bootstrap. CNM can begin its own entrypoint
     * before Fabric invokes BGE's entrypoint, so the CNM scan mixin calls this
     * same guarded method at scan HEAD. There is one registration path and it
     * is safe for the later BGE entrypoint to revisit it.
     */
    public static synchronized void initializeNativeCatalog() {
        if (nativeCatalogRegistered) return;
        MoreSlabsStairsAndWalls.init();
        ModRegistry.registerBlocks();
        NibaruMaterialProfiles.refresh();
        SpreadableSemantics.registerNativePairs();
        nativeCatalogRegistered = true;
    }

    private static synchronized void registerBgeBase() {
        if (bgeBaseRegistered) return;
        register(DIRT_VERTICAL_SLAB_ID, DIRT_VERTICAL_SLAB);
        register(GRASS_VERTICAL_SLAB_ID, GRASS_VERTICAL_SLAB);
        register(DIRT_SLAB_ID, DIRT_SLAB);
        register(GRASS_SLAB_ID, GRASS_SLAB);
        registerBlockOnly(FARMLAND_SLAB_ID, FARMLAND_SLAB);
        FarmlandSlabTilling.register();
        CanonicalGeometryRegistry.register(
                DIRT_VERTICAL_SLAB,
                GRASS_VERTICAL_SLAB,
                DIRT_SLAB,
                GRASS_SLAB);
        NibaruMaterialProfile dirt = NibaruMaterialProfiles.fromBlock(Blocks.DIRT).orElseThrow();
        NibaruMaterialProfile grass = NibaruMaterialProfiles.fromBlock(Blocks.GRASS_BLOCK).orElseThrow();
        BgeMaterialBindings.bindRetainedAlias(DIRT_SLAB, dirt,
                BgeMaterialBindings.Role.HORIZONTAL_SLAB);
        BgeMaterialBindings.bindRetainedAlias(GRASS_SLAB, grass,
                BgeMaterialBindings.Role.HORIZONTAL_SLAB);
        BgeMaterialBindings.bindRetainedAlias(DIRT_VERTICAL_SLAB, dirt,
                BgeMaterialBindings.Role.VERTICAL_SLAB);
        // ExistingDerivedGeometryBindings deliberately makes this retained identity the primary
        // Vertical Slab owner for Grass rather than a second alias.
        BgeMaterialBindings.bindFarmlandSpecial(FARMLAND_SLAB);
        GrassFamilyBehavior.registerDefaults();
        bgeBaseRegistered = true;
        validateBindingsWhenComplete();
    }

    /** Called at CNM's scan boundary after optional providers have registered their sources. */
    public static synchronized void registerCnmBridgeFamilies() {
        initializeNativeCatalog();
        registerBgeBase();
    }

    /** Called from CNM's registry-bootstrap tail before the built-in registries freeze. */
    public static synchronized void registerLayers() {
        initializeNativeCatalog();
        if (bgeGeometryRegistered) return;
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            Identifier layerId = layerId(profile);
            register(layerId, NibaruProviderAdapter.createLayer(
                    profile, geometryProperties(layerId, profile)));

            Identifier cornerId = cornerId(profile);
            register(cornerId, NibaruProviderAdapter.createCorner(
                    profile, geometryProperties(cornerId, profile)));

            Identifier columnId = quarterColumnId(profile);
            register(columnId, NibaruProviderAdapter.createQuarterColumn(
                    profile, geometryProperties(columnId, profile)));
        }
        ExternalMaterialFamilies.finalizeGeneratedBindings();
        BgeMaterialBindings.bindNormalCatalog();
        LayerGeneratedData.generate();
        QuarterGeometryGeneratedData.generate();
        ExternalMaterialGeneratedData.generate();
        bgeGeometryRegistered = true;
        // ShapeMap chooses CNM's actual parent after this registry-tail callback.  Keep the
        // material registry mutable until that decision has bound any deferred candidates.
        CnmShapeMapCandidateBridge.finishRegistryAdmission();
    }

    /**
     * Completes every exact family only after its optional provider entrypoint
     * returns. At that point registries remain writable and provider semantics
     * (including fire and stripping registrations) are observable.
     */
    public static synchronized void registerExternalFamilies(String provider,
            java.util.List<ExternalMaterialCatalog.Spec> specs) {
        initializeNativeCatalog();
        registerBgeBase();
        for (ExternalMaterialCatalog.Spec spec : specs) {
            if (!spec.provider().equals(provider)) {
                throw new IllegalArgumentException("Cross-provider external material batch: " + spec.id());
            }
            ExternalMaterialFamilies.register(spec);
        }
    }

    /** Collision-safe registry identity derived from the typed canonical profile. */
    public static Identifier layerId(NibaruMaterialProfile profile) {
        return geometryId(profile, "layer");
    }

    public static Identifier cornerId(NibaruMaterialProfile profile) {
        return geometryId(profile, "corner");
    }

    public static Identifier quarterColumnId(NibaruMaterialProfile profile) {
        return geometryId(profile, "quarter_column");
    }

    private static Identifier geometryId(NibaruMaterialProfile profile, String suffix) {
        Identifier parent = profile.canonicalParentId();
        return Identifier.fromNamespaceAndPath(MOD_ID,
                parent.getNamespace() + "/" + parent.getPath() + "_" + suffix);
    }

    private static BlockBehaviour.Properties geometryProperties(Identifier ownId,
            NibaruMaterialProfile profile) {
        return BlockBehaviour.Properties.ofFullCopy(profile.canonicalParent())
                .setId(ResourceKey.create(Registries.BLOCK, ownId));
    }

    private static BlockBehaviour.Properties properties(Identifier ownId, Block source) {
        return BlockBehaviour.Properties.ofFullCopy(source)
                .setId(ResourceKey.create(Registries.BLOCK, ownId))
                .randomTicks();
    }

    static void register(Identifier id, Block block) {
        registerBlockOnly(id, block);
        Item.Properties properties = new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .useBlockDescriptionPrefix();
        BlockItem item = block instanceof BlockspaceFundedGeometry
                ? new BgeBlockItem(block, properties)
                : new BlockItem(block, properties);
        Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, id), item);
    }

    /**
     * Registers a Phase-A CNM tail block whose material parent is intentionally not known yet.
     * The identity is derived from CNM's admitted horizontal source, never from a guessed family
     * path.  {@link CnmShapeMapCandidateBridge} gives it its canonical binding only after CNM
     * resolves the ShapeMap component.
     */
    static void registerDeferredCandidate(Identifier id, Block block) {
        register(id, block);
    }

    /** Called at ShapeMap resolution tail, after every deferred candidate is either bound or dormant. */
    static synchronized void finalizeResolvedCnmFamilies() {
        validateBindingsWhenComplete();
    }

    /** Registers state-only blocks such as Farmland Slab without an obtainable BlockItem. */
    private static void registerBlockOnly(Identifier id, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK, id), block);
        BgeMaterialBindings.noteOwnedRegistration(block);
    }

    private static void validateBindingsWhenComplete() {
        if (bgeBaseRegistered && bgeGeometryRegistered) {
            BgeMaterialBindings.validateAndFreeze();
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
