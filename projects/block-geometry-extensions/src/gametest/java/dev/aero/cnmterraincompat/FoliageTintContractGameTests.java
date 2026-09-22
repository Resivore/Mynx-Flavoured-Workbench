package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;
import java.util.Set;

/** Controlled registration-contract coverage for fixed and biome-derived leaf tint. */
public final class FoliageTintContractGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void spruceFamilyUsesTheExactVanillaFixedTintAcrossEveryRole(GameTestHelper helper) {
        NibaruMaterialProfile spruce = profile(Blocks.SPRUCE_LEAVES);
        helper.assertTrue(spruce.tintProfile() == TintProfile.FOLIAGE_SPRUCE
                        && FoliageTintContract.isFixed(spruce.tintProfile())
                        && FoliageTintContract.fixedArgb(spruce.tintProfile())
                                == FoliageTintContract.SPRUCE_FIXED_ARGB
                        && FoliageTintContract.SPRUCE_FIXED_ARGB == 0xFF619961,
                "Spruce profile no longer resolves to Minecraft 26.2's exact fixed ARGB foliage tint");

        Set<BgeMaterialBindings.Role> nativeRoles = EnumSet.of(
                BgeMaterialBindings.Role.HORIZONTAL_SLAB,
                BgeMaterialBindings.Role.STAIR,
                BgeMaterialBindings.Role.WALL);
        Set<BgeMaterialBindings.Role> derivedRoles = EnumSet.of(
                BgeMaterialBindings.Role.VERTICAL_SLAB,
                BgeMaterialBindings.Role.STEP,
                BgeMaterialBindings.Role.CORNER,
                BgeMaterialBindings.Role.QUARTER_COLUMN,
                BgeMaterialBindings.Role.LAYER);
        Set<Block> nativeTargets = Set.copyOf(FoliageTintContract.nativeTargets(ModBlocks.SPRUCE_LEAVES));
        Set<Block> derivedTargets = NibaruProviderAdapter.tintTargets(spruce);
        Set<BgeMaterialBindings.Role> covered = EnumSet.noneOf(BgeMaterialBindings.Role.class);

        for (BgeMaterialBindings.Binding binding : BgeMaterialBindings.all()) {
            if (binding.materialProfile().orElse(null) != spruce
                    || binding.ownership() != BgeMaterialBindings.Ownership.PRIMARY) continue;
            BgeMaterialBindings.Role role = binding.role();
            if (nativeRoles.contains(role)) {
                helper.assertTrue(nativeTargets.contains(binding.physicalBlock()),
                        "Spruce " + role + " bypassed native fixed-tint registration");
            } else if (derivedRoles.contains(role)) {
                helper.assertTrue(derivedTargets.contains(binding.physicalBlock())
                                && FoliageTintContract.fixedArgb(spruce.tintProfile())
                                        == FoliageTintContract.SPRUCE_FIXED_ARGB,
                        "Spruce " + role + " bypassed BGE's fixed profile-tint registration");
            } else {
                helper.assertTrue(role == BgeMaterialBindings.Role.CANONICAL_BLOCK,
                        "Unexpected spruce catalog role " + role);
            }
            covered.add(role);
        }
        helper.assertTrue(covered.equals(EnumSet.allOf(BgeMaterialBindings.Role.class)),
                "Spruce did not retain complete nine-role BGE coverage: " + covered);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void birchStaysFixedWhileOrdinaryLeavesRemainBiomeTinted(GameTestHelper helper) {
        NibaruMaterialProfile birch = profile(Blocks.BIRCH_LEAVES);
        NibaruMaterialProfile oak = profile(Blocks.OAK_LEAVES);
        helper.assertTrue(birch.tintProfile() == TintProfile.FOLIAGE_BIRCH
                        && FoliageTintContract.isFixed(birch.tintProfile())
                        && FoliageTintContract.fixedArgb(birch.tintProfile()) == 0x80A755,
                "Birch changed from its existing fixed foliage contract");
        helper.assertTrue(oak.tintProfile() == TintProfile.FOLIAGE_BIOME
                        && !FoliageTintContract.isFixed(oak.tintProfile()),
                "Ordinary oak leaves no longer use biome foliage tint");
        try {
            FoliageTintContract.fixedArgb(oak.tintProfile());
            throw new AssertionError("Biome foliage was accepted as a fixed color");
        } catch (IllegalArgumentException expected) {
            // FOLIAGE_BIOME must remain an explicit biome-source branch in the client translator.
        }
        helper.succeed();
    }

    private static NibaruMaterialProfile profile(Block block) {
        return NibaruMaterialProfiles.fromBlock(block).orElseThrow();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, java.lang.reflect.Method method)
            throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
