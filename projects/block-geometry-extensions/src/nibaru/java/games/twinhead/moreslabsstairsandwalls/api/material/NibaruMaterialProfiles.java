package games.twinhead.moreslabsstairsandwalls.api.material;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.concretepowder.ConcretePowderSlab;
import games.twinhead.moreslabsstairsandwalls.block.coral.CoralSlab;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathSlab;
import games.twinhead.moreslabsstairsandwalls.block.dirt.DirtSlab;
import games.twinhead.moreslabsstairsandwalls.block.falling.FallingSlab;
import games.twinhead.moreslabsstairsandwalls.block.honey.HoneySlab;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeavesSlab;
import games.twinhead.moreslabsstairsandwalls.block.magma.MagmaSlab;
import games.twinhead.moreslabsstairsandwalls.block.oxidizable.CustomOxidizable;
import games.twinhead.moreslabsstairsandwalls.block.oxidizable.CustomWaxedCopper;
import games.twinhead.moreslabsstairsandwalls.block.oxidizable.WaxedSlab;
import games.twinhead.moreslabsstairsandwalls.block.oxidizable.WaxedStairs;
import games.twinhead.moreslabsstairsandwalls.block.oxidizable.WaxedWall;
import games.twinhead.moreslabsstairsandwalls.block.redstone.RedstoneSlab;
import games.twinhead.moreslabsstairsandwalls.block.slime.SlimeSlab;
import games.twinhead.moreslabsstairsandwalls.block.soulsand.SoulSandSlab;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableSlab;
import games.twinhead.moreslabsstairsandwalls.block.strippable.StrippableGeometry;
import games.twinhead.moreslabsstairsandwalls.block.terracotta.GlazedTerracottaSlab;
import games.twinhead.moreslabsstairsandwalls.block.translucent.TranslucentSlab;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WallBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Immutable public view of Nibaru's authoritative ModBlocks material catalog. */
public final class NibaruMaterialProfiles {
    public static final String PROFILE_VERSION = "canary67-vanilla-family-coverage-v1";
    private static final Map<ModBlocks, Block> EXACT_VANILLA_SLAB_SOURCES = Map.of(
            ModBlocks.SMOOTH_STONE, Blocks.SMOOTH_STONE_SLAB,
            // Stone intentionally has no duplicate BGE slab registration. Its existing vanilla
            // slab is nevertheless the exact horizontal material form for the Stone profile.
            ModBlocks.STONE, Blocks.STONE_SLAB);
    private static volatile Inventory inventory;

    private NibaruMaterialProfiles() {}

    public static List<NibaruMaterialProfile> all() { return inventory().profiles; }
    public static Optional<NibaruMaterialProfile> fromFamily(ModBlocks family) {
        return Optional.ofNullable(inventory().byFamily.get(family));
    }
    public static Optional<NibaruMaterialProfile> fromBlock(Block block) {
        return Optional.ofNullable(inventory().byBlock.get(block));
    }
    public static Optional<NibaruMaterialProfile> fromId(Identifier id) {
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        return block == null ? Optional.empty() : fromBlock(block);
    }

    /**
     * The complete current-26.2 vanilla family set eligible for BGE's three
     * additional geometries.  Eligibility is intentionally structural: a
     * canonical Minecraft BlockFamily must own all of its slab, stair, and
     * wall forms.  This is independent of whether MSSW had to create those
     * forms, and keeps future vanilla additions from silently becoming gaps.
     */
    public static List<VanillaFamily> eligibleVanillaFamilies() {
        return BlockFamilies.getAllFamilies()
                .map(NibaruMaterialProfiles::vanillaFamily)
                .flatMap(Optional::stream)
                .sorted(java.util.Comparator.comparing(family -> family.id().toString()))
                .toList();
    }

    public static synchronized void refresh() { inventory = build(); }

    /**
     * Appends one optional-provider family after that provider has completed its
     * own registry initialization. Native ordering is never rebuilt or changed.
     */
    public static synchronized void registerExternal(NibaruMaterialProfile profile) {
        Inventory current = inventory();
        NibaruMaterialProfile existing = current.byBlock.get(profile.canonicalParent());
        if (existing == profile || (existing != null
                && existing.canonicalParentId().equals(profile.canonicalParentId()))) return;
        if (existing != null) throw new IllegalStateException("External material collides with profile: "
                + profile.canonicalParentId());

        List<NibaruMaterialProfile> profiles = new ArrayList<>(current.profiles);
        profiles.add(profile);
        Map<Block, NibaruMaterialProfile> byBlock = new IdentityHashMap<>(current.byBlock);
        putGeometry(byBlock, profile.canonicalParent(), profile);
        profile.nativeSlab().ifPresent(block -> putGeometry(byBlock, block, profile));
        profile.nativeStair().ifPresent(block -> putGeometry(byBlock, block, profile));
        profile.nativeWall().ifPresent(block -> putGeometry(byBlock, block, profile));
        profile.effectiveSlabSource().filter(block -> block != profile.nativeSlab().orElse(null))
                .ifPresent(block -> putGeometry(byBlock, block, profile));
        profile.effectiveStairSource().filter(block -> block != profile.nativeStair().orElse(null))
                .ifPresent(block -> putGeometry(byBlock, block, profile));
        inventory = new Inventory(List.copyOf(profiles), current.byFamily,
                Collections.unmodifiableMap(byBlock));
    }

    private static Inventory inventory() {
        Inventory result = inventory;
        if (result == null) {
            synchronized (NibaruMaterialProfiles.class) {
                if (inventory == null) inventory = build();
                result = inventory;
            }
        }
        return result;
    }

    private static Inventory build() {
        List<NibaruMaterialProfile> profiles = new ArrayList<>(ModBlocks.values().length);
        Map<ModBlocks, NibaruMaterialProfile> byFamily = new LinkedHashMap<>();
        Map<Block, NibaruMaterialProfile> byBlock = new IdentityHashMap<>();
        for (ModBlocks family : ModBlocks.values()) {
            Block slab = geometry(family, ModBlocks.BlockType.SLAB);
            Block stair = geometry(family, ModBlocks.BlockType.STAIRS);
            Block wall = geometry(family, ModBlocks.BlockType.WALL);
            Block effectiveSlab = effectiveSource(family, slab, BlockFamily.Variant.SLAB);
            Block effectiveStair = effectiveSource(family, stair, BlockFamily.Variant.STAIRS);
            Set<BehaviorCapability> capabilities = capabilities(family, slab, stair, wall);
            VisualProfile visual = visual(family);
            NibaruMaterialProfile profile = new NibaruMaterialProfile(
                    PROFILE_VERSION, family, family.parentBlock, BuiltInRegistries.BLOCK.getKey(family.parentBlock),
                    Optional.ofNullable(slab), Optional.ofNullable(stair), Optional.ofNullable(wall),
                    Optional.ofNullable(effectiveSlab), Optional.ofNullable(effectiveStair),
                    id(family, ModBlocks.BlockType.SLAB), id(family, ModBlocks.BlockType.STAIRS),
                    id(family, ModBlocks.BlockType.WALL), Set.copyOf(family.blockTags), capabilities, visual,
                    visualSupport(visual), tint(family), renderLayer(visual), orientation(visual), sampling(family),
                    doubleForm(visual), textureRoles(family, visual), insetVisualContract(visual),
                    oxidationStage(family), isWaxedCopper(family),
                    transitions(family, capabilities));
            if (byFamily.put(family, profile) != null || byBlock.put(family.parentBlock, profile) != null) {
                throw new IllegalStateException("Duplicate Nibaru material profile for " + family);
            }
            if (slab != null) putGeometry(byBlock, slab, profile);
            if (stair != null) putGeometry(byBlock, stair, profile);
            if (wall != null) putGeometry(byBlock, wall, profile);
            if (effectiveSlab != null && effectiveSlab != slab) putGeometry(byBlock, effectiveSlab, profile);
            if (effectiveStair != null && effectiveStair != stair) putGeometry(byBlock, effectiveStair, profile);
            profiles.add(profile);
        }
        for (VanillaFamily family : eligibleVanillaFamilies()) {
            if (byBlock.containsKey(family.parent())) continue;
            NibaruMaterialProfile profile = vanillaProfile(family);
            putGeometry(byBlock, family.parent(), profile);
            putGeometry(byBlock, family.slab(), profile);
            putGeometry(byBlock, family.stairs(), profile);
            putGeometry(byBlock, family.wall(), profile);
            profiles.add(profile);
        }
        return new Inventory(List.copyOf(profiles), Collections.unmodifiableMap(byFamily),
                Collections.unmodifiableMap(byBlock));
    }

    private static Optional<VanillaFamily> vanillaFamily(BlockFamily family) {
        Block parent = family.getBaseBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(parent);
        if (id == null || !id.getNamespace().equals("minecraft")) return Optional.empty();
        Block slab = family.get(BlockFamily.Variant.SLAB);
        Block stairs = family.get(BlockFamily.Variant.STAIRS);
        Block wall = family.get(BlockFamily.Variant.WALL);
        if (!(slab instanceof SlabBlock) || !(stairs instanceof StairBlock) || !(wall instanceof WallBlock)) {
            return Optional.empty();
        }
        return Optional.of(new VanillaFamily(parent, id, slab, stairs, wall));
    }

    /** Builds a generic profile from actual vanilla registrations, never guessed paths or names. */
    private static NibaruMaterialProfile vanillaProfile(VanillaFamily family) {
        String texture = family.id().getPath();
        return new NibaruMaterialProfile(PROFILE_VERSION, null, family.parent(), family.id(),
                Optional.of(family.slab()), Optional.of(family.stairs()), Optional.of(family.wall()),
                Optional.of(family.slab()), Optional.of(family.stairs()),
                Optional.of(registeredId(family.slab())), Optional.of(registeredId(family.stairs())),
                Optional.of(registeredId(family.wall())), Set.of(), Set.of(), VisualProfile.UNIFORM,
                NibaruMaterialProfile.VisualSupport.GENERIC_SUPPORTED, TintProfile.NONE,
                NibaruMaterialProfile.RenderLayer.SOLID, NibaruMaterialProfile.OrientationPolicy.UNIFORM,
                NibaruMaterialProfile.SurfaceSamplingPolicy.BLOCK_ABSOLUTE,
                NibaruMaterialProfile.DoubleFormPolicy.COMPOSE_SEMANTIC_SURFACES,
                new NibaruMaterialProfile.TextureRoles(texture, texture, texture, "", texture),
                Optional.empty(), Optional.empty(), false, List.of());
    }

    private static Identifier registeredId(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || id.equals(BuiltInRegistries.BLOCK.getDefaultKey())) {
            throw new IllegalStateException("Unregistered vanilla family geometry: " + block);
        }
        return id;
    }

    private static void putGeometry(Map<Block, NibaruMaterialProfile> map, Block block, NibaruMaterialProfile profile) {
        if (map.put(block, profile) != null) throw new IllegalStateException("Nibaru geometry belongs to two profiles: " + block);
    }

    private static Block geometry(ModBlocks family, ModBlocks.BlockType type) {
        if (!family.hasBlock(type)) return null;
        Block block = family.getBlock(type);
        if (block == Blocks.AIR) throw new IllegalStateException("Missing registered Nibaru geometry: "
                + family.getId(type));
        return block;
    }

    private static Block effectiveSource(ModBlocks family, Block nativeGeometry, BlockFamily.Variant variant) {
        if (nativeGeometry != null) return nativeGeometry;
        BlockFamily vanillaFamily = BlockFamilies.getFamily(family.parentBlock);
        Block familyVariant = vanillaFamily == null ? null : vanillaFamily.get(variant);
        if (familyVariant != null) return familyVariant;
        return variant == BlockFamily.Variant.SLAB ? EXACT_VANILLA_SLAB_SOURCES.get(family) : null;
    }

    private static Optional<Identifier> id(ModBlocks family, ModBlocks.BlockType type) {
        return family.hasBlock(type) ? Optional.of(family.getId(type)) : Optional.empty();
    }

    private static Set<BehaviorCapability> capabilities(ModBlocks family, Block slab, Block stair, Block wall) {
        EnumSet<BehaviorCapability> result = EnumSet.noneOf(BehaviorCapability.class);
        Block sample = slab != null ? slab : stair != null ? stair : wall;
        if (family.parentBlock instanceof LeavesBlock) result.add(BehaviorCapability.LEAF_LIFECYCLE);
        if (sample instanceof SpreadableSlab) result.add(BehaviorCapability.SPREADABLE);
        if (family == ModBlocks.DIRT) result.add(BehaviorCapability.SPREADABLE);
        if (sample instanceof StrippableGeometry) result.add(BehaviorCapability.STRIPPABLE);
        if (sample instanceof CustomOxidizable) {
            result.add(BehaviorCapability.OXIDIZABLE);
            result.add(BehaviorCapability.WAXABLE);
            result.add(BehaviorCapability.SCRAPEABLE);
        }
        if (sample instanceof CustomWaxedCopper) {
            result.add(BehaviorCapability.WAXABLE);
            result.add(BehaviorCapability.SCRAPEABLE);
        }
        if (sample instanceof CoralSlab) result.add(BehaviorCapability.CORAL_DEATH);
        if (sample instanceof ConcretePowderSlab) {
            result.add(BehaviorCapability.FALLING);
            result.add(BehaviorCapability.CONCRETE_HARDENING);
        } else if (sample instanceof FallingSlab) result.add(BehaviorCapability.FALLING);
        if (sample instanceof TranslucentSlab) {
            result.add(BehaviorCapability.TRANSLUCENT_ADJACENCY);
            if (family == ModBlocks.ICE) result.add(BehaviorCapability.ICE_MELTING);
        }
        if (sample instanceof RedstoneSlab) result.add(BehaviorCapability.REDSTONE_POWER);
        if (sample instanceof HoneySlab) result.add(BehaviorCapability.HONEY_INTERACTION);
        if (sample instanceof SlimeSlab) result.add(BehaviorCapability.SLIME_INTERACTION);
        if (sample instanceof MagmaSlab) result.add(BehaviorCapability.MAGMA_DAMAGE);
        if (sample instanceof SoulSandSlab) result.add(BehaviorCapability.SOUL_SAND_INTERACTION);
        if (sample instanceof GlazedTerracottaSlab) result.add(BehaviorCapability.GLAZED_ORIENTATION);
        if (sample instanceof DirtSlab) result.add(BehaviorCapability.FLATTENABLE_TO_PATH);
        if (sample instanceof PathSlab) result.add(BehaviorCapability.PATH_CONVERSION);
        return Set.copyOf(result);
    }

    private static VisualProfile visual(ModBlocks family) {
        if (family == ModBlocks.HONEY_BLOCK) return VisualProfile.HONEY_INSET;
        if (family == ModBlocks.SLIME_BLOCK) return VisualProfile.SLIME_INSET;
        if (family == ModBlocks.MYCELIUM || family == ModBlocks.PODZOL
                || family == ModBlocks.CRIMSON_NYLIUM || family == ModBlocks.WARPED_NYLIUM)
            return VisualProfile.TOP_SIDE_BOTTOM;
        if (family.modelType == ModBlocks.ModelType.LEAVES && !(family.parentBlock instanceof LeavesBlock))
            return VisualProfile.UNIFORM;
        return switch (family.modelType) {
            case CUBE_ALL, CUSTOM -> VisualProfile.UNIFORM;
            case CUBE_COLUMN, CUBE_BOTTOM_TOP, CUSTOM_SIDE_BOTTOM_TOP -> VisualProfile.TOP_SIDE_BOTTOM;
            case LOG -> VisualProfile.PILLAR;
            case GRASS -> VisualProfile.GRASS_OVERLAY;
            case LEAVES -> VisualProfile.LEAVES_CUTOUT_TINTED;
            case CUTOUT -> VisualProfile.CUTOUT_UNIFORM;
            case GLASS -> VisualProfile.GLASS_EDGE;
            case TRANSLUCENT -> VisualProfile.TRANSLUCENT_UNIFORM;
            case PATH -> VisualProfile.PATH;
            case GLAZED_TERRACOTTA, ROTATABLE -> VisualProfile.GLAZED_ORIENTED;
            case ROOTS -> VisualProfile.ROOTS;
            case HONEY -> VisualProfile.HONEY_INSET;
            case SLIME -> VisualProfile.SLIME_INSET;
        };
    }

    private static TintProfile tint(ModBlocks family) {
        if (family == ModBlocks.GRASS_BLOCK) return TintProfile.GRASS_BIOME;
        if (!(family.parentBlock instanceof LeavesBlock)) return TintProfile.NONE;
        if (family == ModBlocks.SPRUCE_LEAVES) return TintProfile.FOLIAGE_SPRUCE;
        if (family == ModBlocks.BIRCH_LEAVES) return TintProfile.FOLIAGE_BIRCH;
        return switch (family) {
            case CHERRY_LEAVES, PALE_OAK_LEAVES, AZALEA_LEAVES, FLOWERING_AZALEA_LEAVES -> TintProfile.NONE;
            default -> TintProfile.FOLIAGE_BIOME;
        };
    }

    private static NibaruMaterialProfile.RenderLayer renderLayer(VisualProfile visual) {
        return switch (visual) {
            case LEAVES_CUTOUT_TINTED -> NibaruMaterialProfile.RenderLayer.CUTOUT_MIPPED;
            case GRASS_OVERLAY, CUTOUT_UNIFORM, ROOTS -> NibaruMaterialProfile.RenderLayer.CUTOUT;
            case GLASS_EDGE, TRANSLUCENT_UNIFORM, HONEY_INSET, SLIME_INSET -> NibaruMaterialProfile.RenderLayer.TRANSLUCENT;
            default -> NibaruMaterialProfile.RenderLayer.SOLID;
        };
    }

    private static NibaruMaterialProfile.OrientationPolicy orientation(VisualProfile visual) {
        return switch (visual) {
            case PILLAR -> NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED;
            case GLAZED_ORIENTED -> NibaruMaterialProfile.OrientationPolicy.HORIZONTAL_FACING;
            default -> NibaruMaterialProfile.OrientationPolicy.UNIFORM;
        };
    }

    private static NibaruMaterialProfile.DoubleFormPolicy doubleForm(VisualProfile visual) {
        return switch (visual) {
            case GLAZED_ORIENTED, HONEY_INSET, SLIME_INSET, GLASS_EDGE -> NibaruMaterialProfile.DoubleFormPolicy.CUSTOM_REQUIRED;
            default -> NibaruMaterialProfile.DoubleFormPolicy.COMPOSE_SEMANTIC_SURFACES;
        };
    }

    private static NibaruMaterialProfile.SurfaceSamplingPolicy sampling(ModBlocks family) {
        if (family.modelType == ModBlocks.ModelType.PATH)
            return NibaruMaterialProfile.SurfaceSamplingPolicy.PATH_LOWERED_SURFACE;
        return family.modelType == ModBlocks.ModelType.GRASS
                ? NibaruMaterialProfile.SurfaceSamplingPolicy.NATIVE_STAIR_SURFACE_BAND
                : NibaruMaterialProfile.SurfaceSamplingPolicy.BLOCK_ABSOLUTE;
    }

    private static NibaruMaterialProfile.TextureRoles textureRoles(ModBlocks family, VisualProfile visual) {
        String base = BuiltInRegistries.BLOCK.getKey(family.parentBlock).getPath();
        String side = visual == VisualProfile.CUTOUT_UNIFORM
                ? MaterialTextureResolver.uniformTextureId(family)
                : family.textureId.isEmpty() ? base : family.textureId;
        String top = family.topId.isEmpty() ? side : family.topId;
        String bottom = family.bottomId.isEmpty() ? top : family.bottomId;
        String overlay = "";
        if (family == ModBlocks.CRIMSON_NYLIUM || family == ModBlocks.WARPED_NYLIUM) {
            side = base + "_side";
            top = base;
            bottom = "netherrack";
            overlay = side;
        } else if (family.modelType == ModBlocks.ModelType.GRASS && visual == VisualProfile.TOP_SIDE_BOTTOM) {
            side = base + "_side";
            top = base + "_top";
            bottom = "dirt";
            overlay = side;
        } else if (family.modelType == ModBlocks.ModelType.PATH) {
            side = "dirt_path_side";
            top = "dirt_path_top";
            bottom = "dirt";
        } else if (visual == VisualProfile.ROOTS) {
            side = base + "_side";
            top = base + "_top";
            bottom = top;
        } else if (family.modelType == ModBlocks.ModelType.LOG) {
            side = base;
            top = base + "_top";
            bottom = top;
        } else if (family.modelType == ModBlocks.ModelType.CUBE_BOTTOM_TOP) {
            side = base + "_side";
            top = base + "_top";
            bottom = base + "_bottom";
        }
        if (visual == VisualProfile.GRASS_OVERLAY) {
            side = base + "_side";
            top = base + "_top";
            bottom = "dirt";
            overlay = base + "_side_overlay";
        }
        if (visual == VisualProfile.HONEY_INSET) {
            side = "honey_block_side";
            top = "honey_block_top";
            bottom = "honey_block_bottom";
        }
        String particle = visual == VisualProfile.ROOTS ? side : bottom;
        return new NibaruMaterialProfile.TextureRoles(side, top, bottom, overlay, particle);
    }

    private static Optional<NibaruMaterialProfile.InsetVisualContract> insetVisualContract(VisualProfile visual) {
        return switch (visual) {
            case HONEY_INSET -> Optional.of(new NibaruMaterialProfile.InsetVisualContract(
                    1, 1, NibaruMaterialProfile.InsetVisualContract.ShellTexture.BOTTOM, true));
            case SLIME_INSET -> Optional.of(new NibaruMaterialProfile.InsetVisualContract(
                    3, 2, NibaruMaterialProfile.InsetVisualContract.ShellTexture.MATERIAL_FACES, false));
            default -> Optional.empty();
        };
    }

    private static NibaruMaterialProfile.VisualSupport visualSupport(VisualProfile visual) {
        return switch (visual) {
            case UNIFORM, TOP_SIDE_BOTTOM, PILLAR, GRASS_OVERLAY, LEAVES_CUTOUT_TINTED ->
                    NibaruMaterialProfile.VisualSupport.GENERIC_SUPPORTED;
            default -> NibaruMaterialProfile.VisualSupport.CLASSIFIED_NOT_ADAPTED;
        };
    }

    private static List<MaterialTransition> transitions(ModBlocks family, Set<BehaviorCapability> capabilities) {
        List<MaterialTransition> result = new ArrayList<>();
        if (java.util.Set.of(ModBlocks.GRASS_BLOCK, ModBlocks.PODZOL, ModBlocks.DIRT,
                ModBlocks.COARSE_DIRT, ModBlocks.MYCELIUM, ModBlocks.ROOTED_DIRT).contains(family))
            result.add(new MaterialTransition(MaterialTransition.Type.PODZOL_GROWTH, ModBlocks.PODZOL));
        if (family != ModBlocks.DIRT && capabilities.contains(BehaviorCapability.SPREADABLE))
            result.add(new MaterialTransition(MaterialTransition.Type.SPREADABLE_BASE, ModBlocks.DIRT));
        if (capabilities.contains(BehaviorCapability.PATH_CONVERSION))
            result.add(new MaterialTransition(MaterialTransition.Type.PATH_REVERSION, ModBlocks.DIRT));
        if (capabilities.contains(BehaviorCapability.FLATTENABLE_TO_PATH))
            result.add(new MaterialTransition(MaterialTransition.Type.PATH_TARGET, ModBlocks.DIRT_PATH));
        if (java.util.Set.of(ModBlocks.GRASS_BLOCK, ModBlocks.MYCELIUM,
                ModBlocks.PODZOL, ModBlocks.DIRT_PATH).contains(family))
            result.add(new MaterialTransition(MaterialTransition.Type.DROP_BASE, ModBlocks.DIRT));
        if (family.associatedBlock != null) {
            MaterialTransition.Type type;
            if (capabilities.contains(BehaviorCapability.STRIPPABLE)) type = MaterialTransition.Type.STRIPPED;
            else if (capabilities.contains(BehaviorCapability.OXIDIZABLE)) type = MaterialTransition.Type.NEXT_OXIDATION;
            else if (capabilities.contains(BehaviorCapability.CORAL_DEATH)) type = MaterialTransition.Type.CORAL_DEATH;
            else if (capabilities.contains(BehaviorCapability.CONCRETE_HARDENING)) type = MaterialTransition.Type.CONCRETE_HARDENING;
            else type = MaterialTransition.Type.UNWAXED;
            result.add(new MaterialTransition(type, family.associatedBlock));
        }
        if (capabilities.contains(BehaviorCapability.OXIDIZABLE)) {
            findAssociatedSource(family, BehaviorCapability.OXIDIZABLE)
                    .ifPresent(previous -> result.add(new MaterialTransition(
                            MaterialTransition.Type.PREVIOUS_OXIDATION, previous)));
            findWaxedCounterpart(family).ifPresent(waxed -> result.add(new MaterialTransition(
                    MaterialTransition.Type.WAXED, waxed)));
        }
        return List.copyOf(result);
    }

    private static Optional<WeatheringCopper.WeatherState> oxidationStage(ModBlocks family) {
        if (family.oxidationLevel != null) return Optional.of(family.oxidationLevel);
        if (isWaxedCopper(family) && family.associatedBlock != null)
            return Optional.ofNullable(family.associatedBlock.oxidationLevel);
        return Optional.empty();
    }

    private static boolean isWaxedCopper(ModBlocks family) {
        if (family.associatedBlock == null || family.oxidationLevel != null) return false;
        Block sample = family.hasBlock(ModBlocks.BlockType.SLAB) ? family.getBlock(ModBlocks.BlockType.SLAB)
                : family.hasBlock(ModBlocks.BlockType.STAIRS) ? family.getBlock(ModBlocks.BlockType.STAIRS)
                : family.hasBlock(ModBlocks.BlockType.WALL) ? family.getBlock(ModBlocks.BlockType.WALL) : null;
        return sample instanceof WaxedSlab || sample instanceof WaxedStairs || sample instanceof WaxedWall;
    }

    private static Optional<ModBlocks> findAssociatedSource(ModBlocks target, BehaviorCapability capability) {
        for (ModBlocks candidate : ModBlocks.values()) {
            if (candidate.associatedBlock == target && candidate.oxidationLevel != null
                    && capability == BehaviorCapability.OXIDIZABLE) return Optional.of(candidate);
        }
        return Optional.empty();
    }

    private static Optional<ModBlocks> findWaxedCounterpart(ModBlocks unwaxed) {
        for (ModBlocks candidate : ModBlocks.values()) {
            if (candidate.associatedBlock == unwaxed && isWaxedCopper(candidate)) return Optional.of(candidate);
        }
        return Optional.empty();
    }

    private record Inventory(List<NibaruMaterialProfile> profiles,
            Map<ModBlocks, NibaruMaterialProfile> byFamily,
            Map<Block, NibaruMaterialProfile> byBlock) {}

    /** Exact block identities supplied by a current vanilla full geometry family. */
    public record VanillaFamily(Block parent, Identifier id, Block slab, Block stairs, Block wall) {}
}
