package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Exact provider-ownership and state regressions added with the Mossy Stone catalog entry. */
public final class MossyStoneCatalogGameTests implements CustomTestMethodInvoker {
    private static final Identifier MOSSY_STONE = Identifier.parse("mossy_stone:mossy_stone");

    @GameTest(maxTicks = 40)
    public void mossyStoneSelectorUsesExactProviderAndGeneratedRoleOrder(GameTestHelper helper) {
        ExternalMaterialFamilies.Binding binding = ExternalMaterialFamilies.fromSource(MOSSY_STONE)
                .orElseThrow(() -> new IllegalStateException("Mossy Stone provider family was not registered"));
        Map<String, Identifier> providerRoles = Map.of(
                "slab", Identifier.parse("mossy_stone:mossy_stone_slab"),
                "stairs", Identifier.parse("mossy_stone:mossy_stone_stairs"),
                "wall", Identifier.parse("mossy_stone:mossy_stone_wall"));
        helper.assertTrue(binding.spec().provider().equals("mossy_stone")
                        && binding.spec().providerRoles().equals(providerRoles),
                "Mossy Stone did not retain its exact provider role declaration: " + binding.spec());
        for (Map.Entry<String, Identifier> role : providerRoles.entrySet()) {
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(binding.roles().get(role.getKey()))
                            .equals(role.getValue()),
                    "Mossy Stone did not adopt provider-owned " + role.getKey() + ": "
                            + BuiltInRegistries.BLOCK.getKey(binding.roles().get(role.getKey())));
            Identifier duplicate = ExternalMaterialFamilies.id(binding.spec(), role.getKey());
            Block registered = BuiltInRegistries.BLOCK.getValue(duplicate);
            helper.assertTrue(!duplicate.equals(BuiltInRegistries.BLOCK.getKey(registered)),
                    "Mossy Stone also registered a duplicate BGE " + role.getKey() + ": " + duplicate);
        }

        List<Identifier> expected = List.of(
                Identifier.parse("mossy_stone:mossy_stone"),
                Identifier.parse("mossy_stone:mossy_stone_slab"),
                Identifier.parse("mossy_stone:mossy_stone_stairs"),
                Identifier.parse("mossy_stone:mossy_stone_wall"),
                Identifier.parse("clutternomore:mossy_stone/vertical_mossy_stone_slab"),
                Identifier.parse("clutternomore:mossy_stone/mossy_stone_step"),
                Identifier.parse("cnm_terrain_slabs_compat:mossy_stone/mossy_stone_corner"),
                Identifier.parse("cnm_terrain_slabs_compat:mossy_stone/mossy_stone_quarter_column"),
                Identifier.parse("cnm_terrain_slabs_compat:mossy_stone/mossy_stone_layer"));
        List<Identifier> actual = ShapeMap.getShapes(binding.source().asItem()).stream()
                .map(BuiltInRegistries.ITEM::getKey).toList();
        helper.assertTrue(actual.equals(expected) && new LinkedHashSet<>(actual).size() == expected.size(),
                "Mossy Stone final selector order/extras changed: " + actual);
        for (Identifier id : expected) {
            Item item = BuiltInRegistries.ITEM.getValue(id);
            helper.assertTrue(id.equals(BuiltInRegistries.ITEM.getKey(item))
                            && ShapeMap.getParent(item) == binding.source().asItem(),
                    "Mossy Stone selector member has the wrong canonical parent: " + id);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void corruptOvergrowthRetainsFacingWithoutInventingMaterialAxis(GameTestHelper helper) {
        ExternalMaterialFamilies.Binding binding = ExternalMaterialFamilies.fromSource(
                        Identifier.parse("enderscape:corrupt_overgrowth"))
                .orElseThrow(() -> new IllegalStateException("Corrupt Overgrowth family was not registered"));
        helper.assertTrue(binding.profile().orientationPolicy()
                        == NibaruMaterialProfile.OrientationPolicy.UNIFORM
                        && binding.source().defaultBlockState().hasProperty(BlockStateProperties.FACING),
                "Corrupt Overgrowth lost its provider FACING/non-pillar classification");
        for (Map.Entry<String, Block> role : binding.roles().entrySet()) {
            helper.assertTrue(!role.getValue().defaultBlockState().hasProperty(BlockStateProperties.AXIS),
                    "Corrupt Overgrowth acquired an inappropriate AXIS on " + role.getKey() + ": "
                            + BuiltInRegistries.BLOCK.getKey(role.getValue()));
        }

        BlockState fullLayer = binding.layer().defaultBlockState()
                .setValue(BgeLayerBlock.LAYERS, 4)
                .setValue(BgeLayerBlock.FACING, Direction.WEST);
        BlockState projected = BgeMaterialBindings.projectToCanonical(fullLayer).orElseThrow();
        helper.assertTrue(projected.is(binding.source())
                        && projected.hasProperty(BlockStateProperties.FACING)
                        && projected.getValue(BlockStateProperties.FACING) == Direction.WEST
                        && !projected.hasProperty(BlockStateProperties.AXIS),
                "Corrupt Overgrowth full-Layer projection changed provider FACING state: " + projected);
        helper.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
