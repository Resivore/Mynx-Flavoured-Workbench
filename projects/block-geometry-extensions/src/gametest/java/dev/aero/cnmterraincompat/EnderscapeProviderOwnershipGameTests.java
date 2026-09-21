package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Exact Enderscape provider-role ownership and visible-selector regressions. */
public final class EnderscapeProviderOwnershipGameTests implements CustomTestMethodInvoker {
    private static final Family END_STONE = providerFamily("minecraft:end_stone",
            "enderscape:end_stone_slab", "enderscape:end_stone_stairs", "enderscape:end_stone_wall");
    private static final Family PURPUR = providerFamily("minecraft:purpur_block",
            "minecraft:purpur_slab", "minecraft:purpur_stairs", "enderscape:purpur_wall");

    private static final List<Family> PLANKS = List.of(
            missingWallFamily("enderscape:veiled_planks", "enderscape:veiled_slab",
                    "enderscape:veiled_stairs"),
            missingWallFamily("enderscape:celestial_planks", "enderscape:celestial_slab",
                    "enderscape:celestial_stairs"),
            missingWallFamily("enderscape:murublight_planks", "enderscape:murublight_slab",
                    "enderscape:murublight_stairs"));

    private static final List<Family> REQUESTED = List.of(
            providerFamily("enderscape:shadoline_block", "shadoline_block"),
            providerFamily("enderscape:cut_shadoline", "cut_shadoline"),
            providerFamily("enderscape:overgrown_end_stone_bricks", "overgrown_end_stone_brick"),
            providerFamily("enderscape:veradite", "veradite"),
            providerFamily("enderscape:veradite_bricks", "veradite_brick"),
            providerFamily("enderscape:mirestone", "mirestone"),
            providerFamily("enderscape:polished_mirestone", "polished_mirestone"),
            providerFamily("enderscape:overgrown_mirestone_bricks", "overgrown_mirestone_brick"),
            providerFamily("enderscape:kurodite", "kurodite"),
            providerFamily("enderscape:polished_kurodite", "polished_kurodite"),
            providerFamily("enderscape:kurodite_bricks", "kurodite_brick"),
            providerFamily("enderscape:etched_alluring_magnia", "etched_alluring_magnia"),
            providerFamily("enderscape:etched_repulsive_magnia", "etched_repulsive_magnia"),
            providerFamily("enderscape:dusk_purpur_block", "dusk_purpur"),
            missingWallFamily("enderscape:purpur_tiles", "enderscape:purpur_tile_slab",
                    "enderscape:purpur_tile_stairs"),
            providerFamily("enderscape:celestial_bricks", "celestial_brick"),
            providerFamily("enderscape:murublight_bricks", "murublight_brick"));

    @GameTest(maxTicks = 40)
    public void vanillaEndStoneAndPurpurAdoptOneCanonicalProviderCompletedFamily(GameTestHelper helper) {
        assertProviderSelection(helper, END_STONE);
        assertExactNineRoleSelector(helper, END_STONE);
        assertProviderSelection(helper, PURPUR);
        assertExactNineRoleSelector(helper, PURPUR);

        for (String path : List.of("end_stone_slab", "end_stone_stairs", "end_stone_wall",
                "purpur_wall")) {
            assertUnregistered(helper, Identifier.fromNamespaceAndPath("more_slabs_stairs_and_walls", path),
                    "suppressed legacy Nibaru standard role");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void enderscapePlanksUseActualProviderRolesWithoutGuessedAliases(GameTestHelper helper) {
        for (Family family : PLANKS) {
            ExternalMaterialFamilies.Binding binding = assertProviderSelection(helper, family);
            List<Identifier> actual = selectorIds(binding);
            List<Identifier> expectedVariation = expectedNineRoleIds(binding, family);
            helper.assertTrue(actual.size() >= expectedVariation.size()
                            && actual.subList(0, expectedVariation.size()).equals(expectedVariation),
                    "Enderscape Planks did not begin with its exact nine-role variation: "
                            + family.root() + " " + actual);

            String rootPath = family.root().getPath();
            assertUnregistered(helper, Identifier.fromNamespaceAndPath("enderscape", rootPath + "_slab"),
                    "guessed Enderscape Planks slab");
            assertUnregistered(helper, Identifier.fromNamespaceAndPath("enderscape", rootPath + "_stairs"),
                    "guessed Enderscape Planks stairs");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void allSeventeenRequestedRootsUseExactProviderRolesAndNineVisibleItems(GameTestHelper helper) {
        helper.assertTrue(REQUESTED.size() == 17,
                "Requested Enderscape provider-family inventory is not exactly 17");
        for (Family family : REQUESTED) {
            assertProviderSelection(helper, family);
            assertExactNineRoleSelector(helper, family);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void purpurTilesGeneratesOnlyItsMissingWallAndNoDeferredIdentities(GameTestHelper helper) {
        Family family = REQUESTED.stream().filter(candidate -> candidate.root().equals(
                Identifier.parse("enderscape:purpur_tiles"))).findFirst().orElseThrow();
        ExternalMaterialFamilies.Binding binding = assertProviderSelection(helper, family);
        Identifier wall = ExternalMaterialFamilies.id(binding.spec(), "wall");
        helper.assertTrue(binding.isGeneratedRole("wall")
                        && BuiltInRegistries.BLOCK.getKey(binding.wall()).equals(wall)
                        && wall.equals(Identifier.parse(
                                "cnm_terrain_slabs_compat:enderscape/purpur_tiles_wall")),
                "Purpur Tiles did not retain its one BGE-owned standard role: "
                        + BuiltInRegistries.BLOCK.getKey(binding.wall()));

        boolean deferred = Stream.concat(BuiltInRegistries.BLOCK.keySet().stream(),
                        BuiltInRegistries.ITEM.keySet().stream())
                .anyMatch(id -> id.getPath().startsWith("deferred/")
                        || id.getPath().endsWith("_slab_wall")
                        || id.getPath().endsWith("_wall_slab"));
        helper.assertTrue(!deferred,
                "Explicit Enderscape completion created a deferred or role-combination identity");
        helper.succeed();
    }

    private static ExternalMaterialFamilies.Binding assertProviderSelection(GameTestHelper helper,
            Family family) {
        ExternalMaterialFamilies.Binding binding = ExternalMaterialFamilies.fromSource(family.root())
                .orElseThrow(() -> new IllegalStateException(
                        "Enderscape family was not registered: " + family.root()));
        Map<String, Identifier> expectedRoles = family.providerRoles();
        helper.assertTrue(binding.spec().provider().equals("enderscape")
                        && binding.spec().providerRoles().equals(expectedRoles),
                "Exact provider-role declaration changed for " + family.root() + ": "
                        + binding.spec().providerRoles());

        for (Map.Entry<String, Identifier> role : expectedRoles.entrySet()) {
            Block selected = binding.roles().get(role.getKey());
            helper.assertTrue(selected != null
                            && BuiltInRegistries.BLOCK.getKey(selected).equals(role.getValue())
                            && !binding.isGeneratedRole(role.getKey()),
                    "Provider role was not adopted exactly for " + family.root() + " " + role
                            + ": " + (selected == null ? null : BuiltInRegistries.BLOCK.getKey(selected)));
            assertUnregistered(helper, ExternalMaterialFamilies.id(binding.spec(), role.getKey()),
                    "duplicate BGE provider role for " + family.root());
        }

        if (!family.providerWall()) {
            Identifier wall = ExternalMaterialFamilies.id(binding.spec(), "wall");
            helper.assertTrue(binding.isGeneratedRole("wall")
                            && BuiltInRegistries.BLOCK.getKey(binding.wall()).equals(wall),
                    "Family without a provider wall did not generate exactly one BGE wall: "
                            + family.root());
        }
        return binding;
    }

    private static void assertExactNineRoleSelector(GameTestHelper helper, Family family) {
        ExternalMaterialFamilies.Binding binding = ExternalMaterialFamilies.fromSource(family.root())
                .orElseThrow();
        List<Identifier> expected = expectedNineRoleIds(binding, family);
        List<Identifier> bound = binding.roles().values().stream()
                .map(BuiltInRegistries.BLOCK::getKey).toList();
        List<Identifier> actual = selectorIds(binding);
        helper.assertTrue(bound.equals(expected),
                "Nine-role binding order/identity changed for " + family.root() + ": " + bound);
        helper.assertTrue(actual.equals(expected)
                        && new LinkedHashSet<>(actual).size() == 9
                        && expected.stream().allMatch(id -> Collections.frequency(actual, id) == 1),
                "Final visible selector is not one exact nine-role sequence for "
                        + family.root() + ": " + actual);

        Item root = binding.source().asItem();
        long components = ShapeMap.shapesView().values().stream()
                .filter(component -> component.contains(root)).count();
        helper.assertTrue(components == 1,
                "Canonical root appears in more than one ShapeMap selector: " + family.root());
        for (Identifier id : expected) {
            Item item = BuiltInRegistries.ITEM.getValue(id);
            helper.assertTrue(id.equals(BuiltInRegistries.ITEM.getKey(item))
                            && ShapeMap.getParent(item) == root,
                    "Nine-role selector member has the wrong parent for " + family.root() + ": " + id);
        }
    }

    private static List<Identifier> selectorIds(ExternalMaterialFamilies.Binding binding) {
        return ShapeMap.getShapes(binding.source().asItem()).stream()
                .map(BuiltInRegistries.ITEM::getKey).toList();
    }

    private static List<Identifier> expectedNineRoleIds(ExternalMaterialFamilies.Binding binding,
            Family family) {
        List<Identifier> result = new ArrayList<>(9);
        result.add(family.root());
        result.add(family.slab());
        result.add(family.stairs());
        result.add(family.providerWall() ? family.wall() : ExternalMaterialFamilies.id(binding.spec(), "wall"));
        // Vanilla-backed End Stone/Purpur retain their native CNM identities; late provider-only
        // families use provider-qualified CNM paths. The production binding is the authority for
        // these already-registered Vertical Slab and Step roles in both cases.
        result.add(BuiltInRegistries.BLOCK.getKey(binding.verticalSlab()));
        result.add(BuiltInRegistries.BLOCK.getKey(binding.step()));
        result.add(tail(family.root(), "corner"));
        result.add(tail(family.root(), "quarter_column"));
        result.add(tail(family.root(), "layer"));
        return List.copyOf(result);
    }

    private static Identifier tail(Identifier root, String role) {
        return Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID,
                root.getNamespace() + "/" + root.getPath() + "_" + role);
    }

    private static void assertUnregistered(GameTestHelper helper, Identifier id, String label) {
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        Item item = BuiltInRegistries.ITEM.getValue(id);
        helper.assertTrue(!id.equals(BuiltInRegistries.BLOCK.getKey(block))
                        && !id.equals(BuiltInRegistries.ITEM.getKey(item)),
                label + " was registered: " + id);
    }

    private static Family providerFamily(String root, String rolePrefix) {
        return providerFamily(root, "enderscape:" + rolePrefix + "_slab",
                "enderscape:" + rolePrefix + "_stairs", "enderscape:" + rolePrefix + "_wall");
    }

    private static Family providerFamily(String root, String slab, String stairs, String wall) {
        return new Family(Identifier.parse(root), Identifier.parse(slab), Identifier.parse(stairs),
                Identifier.parse(wall), true);
    }

    private static Family missingWallFamily(String root, String slab, String stairs) {
        return new Family(Identifier.parse(root), Identifier.parse(slab), Identifier.parse(stairs), null, false);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }

    private record Family(Identifier root, Identifier slab, Identifier stairs, Identifier wall,
            boolean providerWall) {
        private Map<String, Identifier> providerRoles() {
            LinkedHashMap<String, Identifier> result = new LinkedHashMap<>();
            result.put("slab", slab);
            result.put("stairs", stairs);
            if (providerWall) result.put("wall", wall);
            return Map.copyOf(result);
        }
    }
}
