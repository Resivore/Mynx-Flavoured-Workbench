package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Inverse coverage contract for vanilla full slab/stair/wall material families. */
public final class VanillaFamilyCoverageGameTests implements CustomTestMethodInvoker {
    private static final Set<Identifier> REQUIRED_AUDIT_SET = Set.of(
            id("cobblestone"), id("mossy_cobblestone"), id("stone_bricks"),
            id("mossy_stone_bricks"), id("andesite"), id("diorite"), id("granite"),
            id("cobbled_deepslate"), id("polished_deepslate"), id("deepslate_bricks"),
            id("deepslate_tiles"), id("tuff"), id("polished_tuff"), id("tuff_bricks"),
            id("bricks"), id("mud_bricks"), id("resin_bricks"), id("nether_bricks"),
            id("red_nether_bricks"), id("blackstone"), id("polished_blackstone"),
            id("polished_blackstone_bricks"), id("end_stone_bricks"), id("sandstone"),
            id("red_sandstone"), id("prismarine"), id("cinnabar"), id("polished_cinnabar"),
            id("cinnabar_bricks"), id("sulfur"), id("polished_sulfur"), id("sulfur_bricks"));

    @GameTest(maxTicks = 40)
    public void everyEligibleVanillaFamilyHasOneCompleteBgeProfile(GameTestHelper helper) {
        List<NibaruMaterialProfiles.VanillaFamily> eligible = NibaruMaterialProfiles.eligibleVanillaFamilies();
        Set<Identifier> eligibleIds = eligible.stream().map(NibaruMaterialProfiles.VanillaFamily::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        helper.assertTrue(eligibleIds.size() == eligible.size(),
                "Vanilla full-family discovery is not unique: " + eligibleIds);
        helper.assertTrue(eligibleIds.containsAll(REQUIRED_AUDIT_SET),
                "Required vanilla audit families are absent from the structural eligible set: missing="
                        + difference(REQUIRED_AUDIT_SET, eligibleIds));

        Set<Identifier> catalogIds = NibaruMaterialProfiles.all().stream()
                .map(NibaruMaterialProfile::canonicalParentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        helper.assertTrue(catalogIds.size() == NibaruMaterialProfiles.all().size(),
                "Canonical BGE material catalog contains duplicate parents");

        for (NibaruMaterialProfiles.VanillaFamily family : eligible) {
            NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(family.parent()).orElseThrow();
            helper.assertTrue(profile.canonicalParent() == family.parent()
                            && profile.canonicalParentId().equals(family.id())
                            && profile.nativeSlab().orElseThrow() == family.slab()
                            && profile.nativeStair().orElseThrow() == family.stairs()
                            && profile.nativeWall().orElseThrow() == family.wall(),
                    "Vanilla family did not retain its canonical parent/slab/stairs/wall: " + family.id());
            assertBgeTrio(helper, profile);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void endStoneBricksUsesVanillaStandardFormsAndOneBgeTrio(GameTestHelper helper) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(Blocks.END_STONE_BRICKS).orElseThrow();
        helper.assertTrue(profile.canonicalParent() == Blocks.END_STONE_BRICKS
                        && profile.nativeSlab().orElseThrow() == Blocks.END_STONE_BRICK_SLAB
                        && profile.nativeStair().orElseThrow() == Blocks.END_STONE_BRICK_STAIRS
                        && profile.nativeWall().orElseThrow() == Blocks.END_STONE_BRICK_WALL,
                "End Stone Bricks did not resolve to its exact vanilla standard forms");
        assertBgeTrio(helper, profile);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void stoneUsesItsExactVanillaSlabAsTheProfileHorizontalSource(GameTestHelper helper) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(Blocks.STONE_SLAB).orElseThrow();
        helper.assertTrue(profile.canonicalParent() == Blocks.STONE
                        && profile.nativeSlab().isEmpty()
                        && profile.effectiveSlabSource().orElseThrow() == Blocks.STONE_SLAB,
                "Stone must retain its registered vanilla slab as its exact profile horizontal source");
        helper.succeed();
    }

    private static void assertBgeTrio(GameTestHelper helper, NibaruMaterialProfile profile) {
        Map<BgeGeometryRole, Block> expected = Map.of(
                BgeGeometryRole.LAYER, NibaruProviderAdapter.derived(profile, BgeGeometryRole.LAYER).orElseThrow(),
                BgeGeometryRole.CORNER, NibaruProviderAdapter.derived(profile, BgeGeometryRole.CORNER).orElseThrow(),
                BgeGeometryRole.QUARTER_COLUMN,
                NibaruProviderAdapter.derived(profile, BgeGeometryRole.QUARTER_COLUMN).orElseThrow());
        Set<Identifier> ids = new LinkedHashSet<>();
        expected.forEach((role, block) -> {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            helper.assertTrue(id != null && id.equals(switch (role) {
                        case LAYER -> CnmTerrainCompat.layerId(profile);
                        case CORNER -> CnmTerrainCompat.cornerId(profile);
                        case QUARTER_COLUMN -> CnmTerrainCompat.quarterColumnId(profile);
                        default -> throw new IllegalStateException("Unexpected BGE role " + role);
                    }) && BuiltInRegistries.ITEM.getKey(block.asItem()).equals(id),
                    "BGE " + role + " has the wrong registered identity for " + profile.canonicalParentId());
            helper.assertTrue(ids.add(id), "BGE trio reuses a registry ID for " + profile.canonicalParentId());
        });
    }

    private static Set<Identifier> difference(Set<Identifier> expected, Set<Identifier> actual) {
        Set<Identifier> result = new LinkedHashSet<>(expected);
        result.removeAll(actual);
        return result;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
