package com.starfish_studios.bbb.porting;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImplementationContractTest {
    private static final Path PROJECT = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    private static final Path JAVA = PROJECT.resolve("src/main/java/com/starfish_studios/bbb");
    private static final Path BLOCKS = JAVA.resolve("block");

    @Test
    void beamCompatibilityUsesExplicitMetadataAndBlockState() throws IOException {
        String registry = read("registry/BBBContent.java");

        assertTrue(registry.contains("private static final List<WoodSpec> VANILLA_WOOD_SPECS = List.of("));
        assertEquals(15, occurrences(registry, "new WoodSpec("));
        assertTrue(registry.contains("new WoodSpec(\"pale_oak\", Blocks.PALE_OAK_PLANKS, Blocks.STRIPPED_PALE_OAK_LOG)"));
        assertTrue(registry.contains("new WoodSpec(\"veiled\", veiledPlanks, strippedVeiledLog)"));
        assertTrue(registry.contains("new WoodSpec(\"celestial\", celestialPlanks, strippedCelestialStem)"));
        assertTrue(registry.contains("new WoodSpec(\"murublight\", murublightPlanks, strippedMurublightStem)"));
        assertEquals(3, occurrences(registry, "registerWoodFamily(new WoodSpec(\""),
                "Only the three explicit provider families may extend the standalone wood catalog");
        assertTrue(registry.contains("properties(spec.beamMaterial(), material + \"_beam\")"));
        assertTrue(registry.contains("public record WoodFamily("));
        assertTrue(registry.contains("public record BeamFamily("));
        assertTrue(registry.contains("Block sourcePlanks"));
        assertTrue(registry.contains("Block beamMaterial"));
        assertTrue(registry.contains("Block beam"));
        assertTrue(registry.contains("Block beamSlab"));
        assertTrue(registry.contains("Block beamStairs"));
        assertTrue(registry.contains("Block wall"));
        assertTrue(registry.contains("new WoodFamily(material, source, spec.beamMaterial(),"));
        assertTrue(registry.contains("new BeamFamily(material, source, spec.beamMaterial(), beam,"));
        assertTrue(registry.contains("state.getValue(RotatedPillarBlock.AXIS)"));
        assertTrue(registry.contains("state.getValue(FacingSlabBlock.FACING).getAxis()"));
        assertTrue(registry.contains("MUTABLE_BLOCKS.size() != 171 || MUTABLE_ITEMS.size() != 172"));
        assertTrue(registry.contains("MUTABLE_BLOCKS.size() != 204 || MUTABLE_ITEMS.size() != 205"));
        assertTrue(registry.contains("MUTABLE_WOOD_FAMILIES.size() != 15 || MUTABLE_BEAM_FAMILIES.size() != 15"));

        assertFalse(Pattern.compile("getPath\\s*\\(").matcher(registry).find());
        assertFalse(Pattern.compile("\\.split\\s*\\(").matcher(registry).find());
        assertFalse(Pattern.compile("\\.substring\\s*\\(").matcher(registry).find());
        assertFalse(Pattern.compile("(?:startsWith|endsWith|contains)\\s*\\(\\s*\\\"[^\\\"]*beam")
                .matcher(registry).find());
    }

    @Test
    void optionalEnderscapeIntegrationUsesOnlySixExplicitProviderBlocks() throws IOException {
        String bootstrap = read("BuildingButBetter.java");
        String registry = read("registry/BBBContent.java");
        String integration = read("compat/EnderscapeWoodIntegration.java");

        assertTrue(bootstrap.indexOf("BBBContent.initialize()")
                        < bootstrap.indexOf("EnderscapeWoodIntegration.initialize()"),
                "The standalone registry must initialize before the optional provider bridge");
        assertTrue(integration.contains("if (!FabricLoader.getInstance().isModLoaded(PROVIDER_ID)) return;"));
        assertTrue(integration.contains("ResourceLoader.registerBuiltinPack("));
        assertTrue(integration.contains("PackActivationType.ALWAYS_ENABLED"));
        assertTrue(integration.contains("RegistryEntryAddedCallback.event(BuiltInRegistries.BLOCK)"));
        assertTrue(integration.contains("if (registered || registering || !allProviderBlocksPresent()) return;"));
        assertTrue(integration.contains("ServerLifecycleEvents.SERVER_STARTING.register(server -> requireProviderReady())"));
        assertEquals(6, occurrences(integration, "= id(\""),
                "The provider bridge must bind exactly the six audited Enderscape source blocks");

        for (String binding : Set.of(
                "VEILED_PLANKS = id(\"veiled_planks\")",
                "STRIPPED_VEILED_LOG = id(\"stripped_veiled_log\")",
                "CELESTIAL_PLANKS = id(\"celestial_planks\")",
                "STRIPPED_CELESTIAL_STEM = id(\"stripped_celestial_stem\")",
                "MURUBLIGHT_PLANKS = id(\"murublight_planks\")",
                "STRIPPED_MURUBLIGHT_STEM = id(\"stripped_murublight_stem\")")) {
            assertTrue(integration.contains(binding), () -> "Missing explicit provider binding " + binding);
        }
        assertTrue(integration.contains("requireBlock(VEILED_PLANKS), requireBlock(STRIPPED_VEILED_LOG)"));
        assertTrue(integration.contains("requireBlock(CELESTIAL_PLANKS), requireBlock(STRIPPED_CELESTIAL_STEM)"));
        assertTrue(integration.contains("requireBlock(MURUBLIGHT_PLANKS), requireBlock(STRIPPED_MURUBLIGHT_STEM)"));
        assertEquals(3, occurrences(registry, "registerWoodFamily(new WoodSpec(\""));

        String explicitProviderCode = registry + System.lineSeparator() + integration;
        assertFalse(Pattern.compile("getPath\\s*\\(").matcher(explicitProviderCode).find());
        assertFalse(Pattern.compile("\\.split\\s*\\(").matcher(explicitProviderCode).find());
        assertFalse(Pattern.compile("\\.substring\\s*\\(").matcher(explicitProviderCode).find());
        assertFalse(Pattern.compile("BuiltInRegistries\\.BLOCK\\s*\\.\\s*(?:stream|iterator|spliterator|entrySet|keySet|forEach)\\s*\\(")
                .matcher(explicitProviderCode).find(), "Provider integration must not scan the block registry");

        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("net.penumbra.enderscape"),
                        () -> file + " directly links production BBB to Enderscape classes");
            }
        }
    }

    @Test
    void onlyAuditedRetainedBlocksImplementTheHammerContract() throws IOException {
        Set<String> expected = Set.of(
                "BalustradeBlock.java", "ColumnBlock.java", "FrameBlock.java", "PalletBlock.java",
                "SupportBlock.java", "WoodenLanternBlock.java", "MouldingBlock.java", "StoneFenceBlock.java"
        );
        Set<String> actual = new LinkedHashSet<>();
        Pattern implementation = Pattern.compile("class\\s+\\w+[^\\{]*\\bHammerableBlock\\b", Pattern.DOTALL);
        try (Stream<Path> files = Files.list(BLOCKS)) {
            files.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("HammerableBlock.java"))
                    .filter(path -> {
                        try {
                            return implementation.matcher(Files.readString(path)).find();
                        } catch (IOException error) {
                            throw new RuntimeException(error);
                        }
                    })
                    .map(path -> path.getFileName().toString())
                    .forEach(actual::add);
        }
        assertEquals(expected, actual);

        String registry = read("registry/BBBContent.java");
        assertTrue(registry.contains("instanceof HammerableBlock hammerable"));
        assertTrue(registry.contains("if (player.isSpectator()) return InteractionResult.PASS"));
        assertTrue(registry.contains("if (player.isShiftKeyDown()"));
        assertTrue(registry.contains("instanceof BalustradeBlock"));
        assertTrue(registry.contains("instanceof MouldingBlock"));
        assertTrue(registry.contains("instanceof StoneFenceBlock"));
        assertTrue(registry.contains("frame.resetCenter(state, level, pos)"));
        assertTrue(registry.contains("return InteractionResult.SUCCESS"));
        assertTrue(read("block/FrameBlock.java").contains("protected void attack("),
                "Frame center reset must remain an ordinary Block attack, not a Hammer-only branch");
        assertTrue(read("block/FrameBlock.java").contains("public void resetCenter("));
        assertFalse(registry.contains("onHammerAttack"));
        assertFalse(registry.contains("_ladder"));
        assertFalse(registry.contains("_layer"));
        assertFalse(Files.exists(BLOCKS.resolve("BBBLadderBlock.java")));
        assertFalse(Files.exists(BLOCKS.resolve("LayerBlock.java")));
        assertFalse(read("item/HammerItem.java").contains("hurtAndBreak"));

        String balustrade = read("block/BalustradeBlock.java");
        assertTrue(balustrade.contains("if (y > 0.5D)"));
        assertTrue(balustrade.contains("else if (y < 0.5D)"));
        assertFalse(balustrade.contains("y >= 0.5D"));
    }

    @Test
    void retainedInteractiveStatePropertiesStayWired() throws IOException {
        Map<String, Set<String>> requiredTokens = Map.of(
                "block/BalustradeBlock.java", Set.of("TOP", "BOTTOM", "TILTED"),
                "block/ColumnBlock.java", Set.of("AXIS", "LAYER_1_AABB", "LAYER_2_AABB", "LAYER_3_AABB", "LAYER_4_AABB"),
                "block/FrameBlock.java", Set.of("FRAME_CENTER"),
                "block/PalletBlock.java", Set.of("LAYER_1", "LAYER_2"),
                "block/SupportBlock.java", Set.of("SUPPORT"),
                "block/WoodenLanternBlock.java", Set.of("HANGING"),
                "block/MouldingBlock.java", Set.of("DENTIL"),
                "block/StoneFenceBlock.java", Set.of("SIDE_FILL", "PILLAR")
        );
        requiredTokens.forEach((file, tokens) -> {
            try {
                String source = read(file);
                tokens.forEach(token -> assertTrue(source.contains(token), () -> file + " lost " + token));
            } catch (IOException error) {
                throw new RuntimeException(error);
            }
        });

        for (String noHammerTarget : Set.of("BrazierBlock.java", "IronFenceBlock.java", "LatticeBlock.java",
                "UrnBlock.java", "FacingSlabBlock.java", "WoodenWallBlock.java")) {
            assertFalse(read("block/" + noHammerTarget).contains("HammerableBlock"),
                    () -> noHammerTarget + " gained an unaudited Hammer interaction");
        }
    }

    @Test
    void directionalBeamSlabsAndRepresentativeWaterloggingKeepTheirStateLifecycle() throws IOException {
        String beamSlab = read("block/FacingSlabBlock.java");
        assertTrue(beamSlab.contains("builder.add(TYPE, FACING, WATERLOGGED)"));
        assertTrue(beamSlab.contains("setValue(FACING, context.getClickedFace())"));
        assertTrue(beamSlab.contains("setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false)"));
        assertTrue(beamSlab.contains("scheduleTick(pos, Fluids.WATER"));

        for (String waterloggedFamily : Set.of("LatticeBlock.java", "FrameBlock.java", "UrnBlock.java")) {
            String source = read("block/" + waterloggedFamily);
            assertTrue(source.contains("BlockStateProperties.WATERLOGGED"),
                    () -> waterloggedFamily + " lost its serialized waterlogged property");
            assertTrue(source.contains("getStateForPlacement"),
                    () -> waterloggedFamily + " lost water-aware placement");
            assertTrue(source.contains("Fluids.WATER"),
                    () -> waterloggedFamily + " lost its water fluid state");
            assertTrue(source.contains("WATERLOGGED"),
                    () -> waterloggedFamily + " no longer registers waterlogged state");
        }
    }

    @Test
    void retainedDefaultFrameTargetingBrazierImmunityAndTooltipsRemainExplicit() throws IOException {
        String frame = read("block/FrameBlock.java");
        assertTrue(frame.contains("player.isShiftKeyDown() || player.isHolding"));
        assertTrue(frame.contains("stack.is(BBBContent.HAMMERS)"));
        assertTrue(frame.contains("stack.is(BBBContent.FRAME_ITEMS)"));
        assertTrue(frame.contains("state.is(BBBContent.WOODEN_FRAMES)"));
        assertTrue(frame.contains("Blocks.SCAFFOLDING.defaultBlockState()"));
        assertTrue(frame.contains("state.is(BBBContent.STONE_FRAMES)"));
        assertTrue(frame.contains("Blocks.STONE.defaultBlockState()"));

        String brazier = read("block/BrazierBlock.java");
        assertTrue(brazier.contains("Enchantments.FROST_WALKER"));
        assertTrue(brazier.contains("EnchantmentHelper.getEnchantmentLevel"));
        assertFalse(brazier.contains("isSteppingCarefully"));

        String registry = read("registry/BBBContent.java");
        assertTrue(registry.contains("TagKey<Item> HAMMERS"));
        assertTrue(registry.contains("TagKey<Item> FRAME_ITEMS"));
        assertTrue(registry.contains("TagKey<Block> WOODEN_FRAMES"));
        assertTrue(registry.contains("TagKey<Block> STONE_FRAMES"));
        assertTrue(read("block/FrameBlock.java").contains("state.is(BBBContent.FRAMES)"));
        assertTrue(read("block/IronFenceBlock.java").contains("state.is(BBBContent.METAL_FENCES)"));
        assertTrue(read("block/StoneFenceBlock.java").contains("state.is(BBBContent.STONE_FENCES)"));
        assertEquals(2, occurrences(registry, "getItemInHand(hand).is(HAMMERS)"));
        assertTrue(registry.contains("new DescriptionBlockItem(block, itemProperties)"));
        String descriptions = read("item/DescriptionBlockItem.java");
        assertFalse(descriptions.contains("net.minecraft.client"));
        assertFalse(descriptions.contains("com.mojang.blaze3d"));
        assertTrue(read("client/BuildingButBetterClient.java").contains("installControlKeyCheck"));
        for (String family : Set.of("BALUSTRADE", "URN", "LATTICE", "LANTERN", "STONE_FENCE",
                "MOULDING", "SUPPORT", "PALLET", "COLUMN", "FRAME")) {
            assertTrue(descriptions.contains(family), () -> "Missing retained tooltip family " + family);
        }
        assertFalse(descriptions.contains("LADDER"));
        assertFalse(descriptions.contains("LAYER"));

        assertEquals(4, occurrences(brazier, "Block.UPDATE_ALL_IMMEDIATE"));
        assertEquals(1, occurrences(brazier, "Block.UPDATE_ALL)"));
        assertTrue(read("block/PalletBlock.java").contains("state.cycle(OPEN)"));
        assertEquals(2, occurrences(read("block/PalletBlock.java"), "Block.UPDATE_CLIENTS"));
        String lantern = read("block/WoodenLanternBlock.java");
        assertTrue(lantern.contains("protected InteractionResult useItemOn("));
        assertTrue(lantern.contains("protected InteractionResult useWithoutItem("));
        assertEquals(2, occurrences(lantern, "return InteractionResult.FAIL"));
        assertFalse(read("block/FacingConnectingBlock.java").contains("setValue(AXIS, Direction.Axis.Y)"));
        String lattice = read("block/LatticeBlock.java");
        assertTrue(lattice.contains("if (!level.isClientSide() && stack.is(Items.SHEARS)"));
        assertEquals(2, occurrences(lattice, "CaveVines.use(null, state, level, pos)"));
        assertEquals(2, occurrences(lattice,
                "state.setValue(PLANT_TYPE, LatticePlantType.NONE), Block.UPDATE_ALL"));
        assertFalse(lattice.contains("setValue(PLANT_TYPE, LatticePlantType.NONE).setValue(BERRIES, false)"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(JAVA.resolve(relative));
    }

    private static int occurrences(String text, String token) {
        int result = 0;
        int offset = 0;
        while ((offset = text.indexOf(token, offset)) >= 0) {
            result++;
            offset += token.length();
        }
        return result;
    }
}
