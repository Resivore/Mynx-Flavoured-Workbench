package com.starfish_studios.bbb.gametest;

import com.starfish_studios.bbb.block.FacingSlabBlock;
import com.starfish_studios.bbb.block.HammerableBlock;
import com.starfish_studios.bbb.block.LatticeBlock;
import com.starfish_studios.bbb.block.WoodenLanternBlock;
import com.starfish_studios.bbb.block.WoodenWallBlock;
import com.starfish_studios.bbb.compat.EnderscapeWoodIntegration;
import com.starfish_studios.bbb.registry.BBBContent;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootTable;
import net.penumbra.enderscape.Enderscape;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Runtime contracts for BBB's explicit, late Enderscape wood-family completion. */
public final class EnderscapeWoodFamilyGameTests implements CustomTestMethodInvoker {
    private static final List<String> BASE_MATERIALS = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak"
    );
    private static final List<String> PROVIDER_MATERIALS = List.of(
            "veiled", "celestial", "murublight"
    );
    private static final Map<String, SourceIds> SOURCES = Map.of(
            "veiled", new SourceIds("veiled_planks", "stripped_veiled_log"),
            "celestial", new SourceIds("celestial_planks", "stripped_celestial_stem"),
            "murublight", new SourceIds("murublight_planks", "stripped_murublight_stem")
    );

    @GameTest(maxTicks = 40)
    public void lateProviderAddsExactlyThreeCompleteFamilies(GameTestHelper helper) {
        helper.assertTrue(Enderscape.BASE_PHASE_OBSERVED,
                "The fixture did not observe BBB's standalone registry before provider registration");
        helper.assertTrue(Enderscape.LATE_REGISTRATION_COMPLETE
                        && EnderscapeWoodIntegration.familiesRegistered()
                        && BBBContent.enderscapeFamiliesRegistered(),
                "The explicit Enderscape completion hook did not finish");
        helper.assertTrue(Enderscape.SOURCES.keySet().equals(Set.of(
                        "veiled_planks", "stripped_veiled_log",
                        "celestial_planks", "stripped_celestial_stem",
                        "murublight_planks", "stripped_murublight_stem")),
                "The Enderscape fixture registered a source set other than the exact six blocks: "
                        + Enderscape.SOURCES.keySet());

        helper.assertTrue(BBBContent.BLOCKS.size() == 204
                        && BBBContent.ITEMS.size() == 205
                        && BBBContent.WOOD_FAMILIES.size() == 15
                        && BBBContent.BEAM_FAMILIES.size() == 15
                        && BBBContent.LATTICES.size() == 15
                        && BBBContent.STONE_FAMILIES.size() == 7,
                "Provider-present BBB registry counts changed: blocks=" + BBBContent.BLOCKS.size()
                        + ", items=" + BBBContent.ITEMS.size()
                        + ", woods=" + BBBContent.WOOD_FAMILIES.size()
                        + ", beams=" + BBBContent.BEAM_FAMILIES.size()
                        + ", lattices=" + BBBContent.LATTICES.size()
                        + ", stones=" + BBBContent.STONE_FAMILIES.size());

        List<String> materials = BBBContent.WOOD_FAMILIES.stream()
                .map(BBBContent.WoodFamily::material)
                .toList();
        List<String> expectedMaterials = java.util.stream.Stream
                .concat(BASE_MATERIALS.stream(), PROVIDER_MATERIALS.stream())
                .toList();
        helper.assertTrue(materials.equals(expectedMaterials),
                "BBB wood-family identity/order changed: expected=" + expectedMaterials
                        + ", actual=" + materials);

        Set<String> providerBlockPaths = new LinkedHashSet<>();
        for (String material : PROVIDER_MATERIALS) {
            BBBContent.WoodFamily family = woodFamily(material);
            helper.assertTrue(new LinkedHashSet<>(family.blocks().keySet())
                            .equals(new LinkedHashSet<>(BBBContent.WOOD_FORMS)),
                    material + " does not have exactly the eleven retained BBB forms: "
                            + family.blocks().keySet());
            for (String form : BBBContent.WOOD_FORMS) {
                String path = material + "_" + form;
                providerBlockPaths.add(path);
                Block block = family.form(form);
                Identifier expectedId = id("bbb", path);
                helper.assertTrue(BBBContent.BLOCKS.get(path) == block
                                && BuiltInRegistries.BLOCK.getKey(block).equals(expectedId)
                                && BBBContent.ITEMS.get(path) == block.asItem()
                                && BuiltInRegistries.ITEM.getKey(block.asItem()).equals(expectedId),
                        "Stable BBB block/item identity is wrong for " + expectedId);
            }
        }
        helper.assertTrue(providerBlockPaths.size() == 33,
                "Expected exactly 33 Enderscape-derived BBB IDs, found " + providerBlockPaths.size());
        helper.assertTrue(Enderscape.BASE_BLOCKS.size() == 171
                        && Enderscape.BASE_ITEMS.size() == 172
                        && Enderscape.BASE_BLOCKS.entrySet().stream()
                                .allMatch(entry -> BBBContent.BLOCKS.get(entry.getKey()) == entry.getValue())
                        && Enderscape.BASE_ITEMS.entrySet().stream()
                                .allMatch(entry -> BBBContent.ITEMS.get(entry.getKey()) == entry.getValue())
                        && BBBContent.STONE_FAMILIES.equals(Enderscape.BASE_STONE_FAMILIES),
                "Appending Enderscape families changed an existing vanilla/Pale Oak/stone identity");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void sourcePlanksAndBeamMaterialsBindByExactIdentity(GameTestHelper helper) {
        for (String material : PROVIDER_MATERIALS) {
            SourceIds ids = SOURCES.get(material);
            Block planks = requiredBlock("enderscape", ids.planks());
            Block beamMaterial = requiredBlock("enderscape", ids.beamMaterial());
            BBBContent.WoodFamily family = woodFamily(material);
            BBBContent.BeamFamily beamFamily = beamFamily(material);

            helper.assertTrue(planks == Enderscape.SOURCES.get(ids.planks())
                            && beamMaterial == Enderscape.SOURCES.get(ids.beamMaterial()),
                    "The live Enderscape fixture identity changed for " + material);
            helper.assertTrue(family.sourcePlanks() == planks
                            && family.beamMaterial() == beamMaterial
                            && beamFamily.sourcePlanks() == planks
                            && beamFamily.beamMaterial() == beamMaterial
                            && beamFamily.beam() == family.form("beam")
                            && beamFamily.beamSlab() == family.form("beam_slab")
                            && beamFamily.beamStairs() == family.form("beam_stairs")
                            && beamFamily.wall() == family.form("wall"),
                    "BBB metadata is not bound to the exact Enderscape sources for " + material);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void everyFormRetainsPaleOakClassStateWaterAndHammerContracts(GameTestHelper helper) {
        BBBContent.WoodFamily reference = woodFamily("pale_oak");
        for (String material : PROVIDER_MATERIALS) {
            BBBContent.WoodFamily family = woodFamily(material);
            for (String form : BBBContent.WOOD_FORMS) {
                Block expected = reference.form(form);
                Block actual = family.form(form);
                helper.assertTrue(actual.getClass() == expected.getClass(),
                        material + "_" + form + " uses " + actual.getClass().getName()
                                + " instead of " + expected.getClass().getName());
                helper.assertTrue(propertySignature(actual).equals(propertySignature(expected)),
                        material + "_" + form + " state properties differ from Pale Oak: expected="
                                + propertySignature(expected) + ", actual=" + propertySignature(actual));
                helper.assertTrue((actual instanceof SimpleWaterloggedBlock)
                                == (expected instanceof SimpleWaterloggedBlock)
                                && actual.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)
                                == expected.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED),
                        material + "_" + form + " changed its waterlogging contract");
                helper.assertTrue((actual instanceof HammerableBlock)
                                == (expected instanceof HammerableBlock),
                        material + "_" + form + " changed its Hammer interaction contract");
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void latticeWallBeamSlabAndLanternSpecializationsRemainExact(GameTestHelper helper) {
        Set<String> latticeProperties = Set.of(
                "berries", "facing", "left", "middle", "plant_type", "right", "waterlogged");
        Set<String> wallProperties = Set.of(
                "east", "north", "south", "up", "waterlogged", "west");

        for (String material : PROVIDER_MATERIALS) {
            BBBContent.WoodFamily family = woodFamily(material);
            BBBContent.BeamFamily beams = beamFamily(material);
            Block lattice = family.form("lattice");
            Block wall = family.form("wall");
            Block beam = family.form("beam");
            Block slab = family.form("beam_slab");
            Block lantern = family.form("lantern");

            helper.assertTrue(lattice instanceof LatticeBlock
                            && propertyNames(lattice).equals(latticeProperties),
                    material + " lattice lost its exact facing/connection/plant/water state contract: "
                            + propertyNames(lattice));
            helper.assertTrue(wall instanceof WoodenWallBlock
                            && propertyNames(wall).equals(wallProperties),
                    material + " wall lost its vanilla wall connectivity/water state contract: "
                            + propertyNames(wall));
            helper.assertTrue(beam instanceof RotatedPillarBlock,
                    material + " beam is not an axial pillar block");
            for (Direction.Axis axis : Direction.Axis.values()) {
                BlockState state = beam.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
                helper.assertTrue(beams.beamAxis(state) == axis,
                        material + " beam did not retain axis " + axis);
            }

            helper.assertTrue(slab instanceof FacingSlabBlock,
                    material + " beam slab is not BBB's facing slab class");
            for (Direction direction : Direction.values()) {
                BlockState state = slab.defaultBlockState().setValue(FacingSlabBlock.FACING, direction);
                helper.assertTrue(beams.slabMaterialAxis(state) == direction.getAxis(),
                        material + " beam slab did not retain material axis for " + direction);
            }

            helper.assertTrue(lantern instanceof WoodenLanternBlock
                            && lantern instanceof HammerableBlock
                            && lantern.defaultBlockState().getLightEmission() == 15,
                    material + " lantern lost its BBB class, Hammer, or luminance-15 contract");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void everyProviderFormReceivesTheEstablishedFuelValue(GameTestHelper helper) {
        for (String material : PROVIDER_MATERIALS) {
            for (String form : BBBContent.WOOD_FORMS) {
                Block block = woodFamily(material).form(form);
                int duration = helper.getLevel().fuelValues().burnDuration(new ItemStack(block));
                helper.assertTrue(duration == 100,
                        "Expected fuel duration 100 for bbb:" + material + "_" + form
                                + ", found " + duration);
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void recipesLootAndTagsResolveForEveryProviderForm(GameTestHelper helper) {
        Set<String> expectedRecipes = new LinkedHashSet<>();
        for (String material : PROVIDER_MATERIALS) {
            for (String form : BBBContent.WOOD_FORMS) expectedRecipes.add(material + "_" + form);
            expectedRecipes.add(material + "_planks_from_beam");
            expectedRecipes.add(material + "_planks_from_beam_slab");
        }
        Set<String> loadedRecipes = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .map(holder -> holder.id().identifier())
                .filter(id -> id.getNamespace().equals("bbb"))
                .map(Identifier::getPath)
                .filter(path -> PROVIDER_MATERIALS.stream().anyMatch(path::startsWith))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        helper.assertTrue(loadedRecipes.equals(expectedRecipes),
                "Expected all 39 optional recipes and no extras: expected=" + expectedRecipes
                        + ", actual=" + loadedRecipes);

        BBBContent.WoodFamily cherry = woodFamily("cherry");
        for (String material : PROVIDER_MATERIALS) {
            BBBContent.WoodFamily family = woodFamily(material);
            for (String form : BBBContent.WOOD_FORMS) {
                Block block = family.form(form);
                Block reference = cherry.form(form);
                Set<String> expectedBlockTags = reference.builtInRegistryHolder().tags()
                        .map(tag -> tag.location().toString()).collect(Collectors.toSet());
                Set<String> actualBlockTags = block.builtInRegistryHolder().tags()
                        .map(tag -> tag.location().toString()).collect(Collectors.toSet());
                Set<String> expectedItemTags = reference.asItem().builtInRegistryHolder().tags()
                        .map(tag -> tag.location().toString()).collect(Collectors.toSet());
                Set<String> actualItemTags = block.asItem().builtInRegistryHolder().tags()
                        .map(tag -> tag.location().toString()).collect(Collectors.toSet());
                helper.assertTrue(actualBlockTags.equals(expectedBlockTags)
                                && actualItemTags.equals(expectedItemTags),
                        material + "_" + form + " tag membership differs from Cherry: block="
                                + actualBlockTags + ", item=" + actualItemTags);

                LootTable loot = block.getLootTable()
                        .map(helper.getLevel().getServer().reloadableRegistries()::getLootTable)
                        .orElse(LootTable.EMPTY);
                helper.assertTrue(loot != LootTable.EMPTY,
                        "Missing loaded loot table for bbb:" + material + "_" + form);
            }
        }
        helper.succeed();
    }

    private static BBBContent.WoodFamily woodFamily(String material) {
        return BBBContent.WOOD_FAMILIES.stream()
                .filter(family -> family.material().equals(material))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing BBB wood family " + material));
    }

    private static BBBContent.BeamFamily beamFamily(String material) {
        return BBBContent.BEAM_FAMILIES.stream()
                .filter(family -> family.material().equals(material))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing BBB beam family " + material));
    }

    private static Block requiredBlock(String namespace, String path) {
        Identifier id = id(namespace, path);
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == null || !BuiltInRegistries.BLOCK.getKey(block).equals(id)) {
            throw new AssertionError("Missing exact live block " + id);
        }
        return block;
    }

    private static Map<String, Integer> propertySignature(Block block) {
        LinkedHashMap<String, Integer> signature = new LinkedHashMap<>();
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            signature.put(property.getName(), property.getPossibleValues().size());
        }
        return Map.copyOf(signature);
    }

    private static Set<String> propertyNames(Block block) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            names.add(property.getName());
        }
        return Set.copyOf(names);
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    private record SourceIds(String planks, String beamMaterial) {}

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
