package workbench.yungsapi.fixture;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import com.yungnickyoung.minecraft.yungsapi.api.YungAutoRegister;
import com.yungnickyoung.minecraft.yungsapi.api.YungJigsawManager;
import com.yungnickyoung.minecraft.yungsapi.api.world.randomize.BlockStateRandomizer;
import com.yungnickyoung.minecraft.yungsapi.io.JSON;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations.CustomAdaptation;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations.BlockingTerrainAdaptation;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations.EnhancedTerrainAdaptation;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.beardifier.EnhancedBeardifierData;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.beardifier.EnhancedBeardifierHelper;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.beardifier.EnhancedBeardifierRigid;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.beardifier.EnhancedJigsawJunction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class YungsApiConsumerFixture implements ModInitializer {
    public static final String MOD_ID = "yungsapi_consumer_fixture";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Identifier PROCESSOR_ID = Identifier.fromNamespaceAndPath(MOD_ID, "replace_block");

    @Override
    public void onInitialize() {
        run("AUTO_REGISTRATION", YungsApiConsumerFixture::testAutoRegistration);
        run("JSON_RANDOMIZATION", YungsApiConsumerFixture::testJsonRandomization);
        run("STRUCTURE_PROCESSOR_CODEC", YungsApiConsumerFixture::testStructureProcessorCodec);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            run("JIGSAW_GENERATION", () -> testJigsaw(server.overworld()));
            run("TERRAIN_ADAPTATION", () -> testTerrainAdaptation(server.overworld()));
            server.halt(false);
        });
    }

    private static void testAutoRegistration() {
        check(BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.fromNamespaceAndPath(MOD_ID, "fixture_sound")).isEmpty(),
                "fixture sound registered before consumer initialization");
        YungAutoRegister.scanPackageForAnnotations("workbench.yungsapi.fixture");
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.fromNamespaceAndPath(MOD_ID, "fixture_sound"))
                .orElseThrow(() -> new AssertionError("annotated sound was not registered"));
        check(sound != null, "registered sound resolved to null");
        check(BuiltInRegistries.STRUCTURE_PROCESSOR.getOptional(PROCESSOR_ID).orElseThrow() == ReplaceBlockProcessor.CODEC,
                "AutoRegisterStructureProcessor did not register the supplied MapCodec");

        boolean duplicateRejected = false;
        try {
            net.minecraft.core.Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, PROCESSOR_ID, ReplaceBlockProcessor.CODEC);
        } catch (RuntimeException expected) {
            duplicateRejected = true;
        }
        check(duplicateRejected, "duplicate processor registration was silently accepted");
        check(BuiltInRegistries.STRUCTURE_PROCESSOR.getOptional(PROCESSOR_ID).orElseThrow() == ReplaceBlockProcessor.CODEC,
                "duplicate attempt changed the original registry entry");
    }

    private static void testJsonRandomization() {
        String json = """
                {"entries":{"minecraft:stone":0.25,"minecraft:oak_log[axis=x]":0.5},"defaultBlock":"minecraft:dirt"}
                """;
        BlockStateRandomizer randomizer = JSON.gson.fromJson(json, BlockStateRandomizer.class);
        check(randomizer.getEntriesAsMap().size() == 2, "expected two randomizer entries");
        check(randomizer.getEntriesAsMap().containsKey(Blocks.STONE.defaultBlockState()), "stone entry missing");
        check(randomizer.getEntriesAsMap().containsKey(Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.X)),
                "oak log axis state missing");
        check(randomizer.getDefaultBlockState().equals(Blocks.DIRT.defaultBlockState()), "default dirt state missing");

        RandomSource first = RandomSource.create(8675309L);
        RandomSource second = RandomSource.create(8675309L);
        for (int i = 0; i < 64; i++) {
            check(randomizer.get(first).equals(randomizer.get(second)), "seeded randomization is not reproducible at draw " + i);
        }

        String encoded = JSON.toJsonString(randomizer);
        BlockStateRandomizer roundTrip = JSON.gson.fromJson(encoded, BlockStateRandomizer.class);
        check(roundTrip.getEntriesAsMap().equals(randomizer.getEntriesAsMap()), "JSON round trip changed entries");
        check(roundTrip.getDefaultBlockState().equals(randomizer.getDefaultBlockState()), "JSON round trip changed default state");

        BlockStateRandomizer malformed = JSON.gson.fromJson(
                "{\"entries\":{\"minecraft:not_a_block\":1.0}}",
                BlockStateRandomizer.class
        );
        check(malformed.getEntriesAsMap().containsKey(Blocks.AIR.defaultBlockState()),
                "invalid block did not use YUNG's API's logged air fallback");
    }

    private static void testStructureProcessorCodec() {
        var json = com.google.gson.JsonParser.parseString("""
                {"processor_type":"yungsapi_consumer_fixture:replace_block","replacement":{"Name":"minecraft:gold_block"}}
                """);
        StructureProcessor decoded = StructureProcessorType.SINGLE_CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(error -> new AssertionError("processor decode failed: " + error));
        check(decoded instanceof ReplaceBlockProcessor, "registry dispatch produced the wrong processor class");
        check(((ReplaceBlockProcessor) decoded).replacement().equals(Blocks.GOLD_BLOCK.defaultBlockState()), "decoded replacement state is wrong");

        var encoded = StructureProcessorType.SINGLE_CODEC.encodeStart(JsonOps.INSTANCE, decoded)
                .getOrThrow(error -> new AssertionError("processor encode failed: " + error));
        StructureProcessor roundTrip = StructureProcessorType.SINGLE_CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(error -> new AssertionError("processor round-trip decode failed: " + error));
        check(roundTrip instanceof ReplaceBlockProcessor replacement && replacement.replacement().equals(Blocks.GOLD_BLOCK.defaultBlockState()),
                "processor codec round trip changed the processor");

        StructureTemplate.StructureBlockInfo input = new StructureTemplate.StructureBlockInfo(BlockPos.ZERO, Blocks.STONE.defaultBlockState(), null);
        StructureTemplate.StructureBlockInfo output = decoded.processBlock(null, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, input, new StructurePlaceSettings());
        check(output != null && output.state().equals(Blocks.GOLD_BLOCK.defaultBlockState()), "processor execution did not replace the state");
    }

    private static void testTerrainAdaptation(ServerLevel level) {
        var json = com.google.gson.JsonParser.parseString("""
                {"kernel_size":7,"kernel_distance":4,"top":"carve","bottom":"bury","bottom_offset":1.0,
                 "padding":{"x":1,"top":2,"bottom":1,"z":3}}
                """);
        CustomAdaptation adaptation = CustomAdaptation.CODEC.codec().parse(JsonOps.INSTANCE, json)
                .getOrThrow(error -> new AssertionError("terrain adaptation decode failed: " + error));
        check(adaptation.getKernelSize() == 7 && adaptation.getKernel().length == 343, "terrain kernel dimensions are wrong");
        check(adaptation.getPadding().equals(new EnhancedTerrainAdaptation.Padding(1, 2, 1, 3)), "terrain padding decoded incorrectly");
        double above = adaptation.computeDensityFactor(0, 0, 0, 1);
        double below = adaptation.computeDensityFactor(0, 0, 0, -1);
        double outside = adaptation.computeDensityFactor(20, 20, 20, 20);
        check(Double.isFinite(above) && above < 0, "carve density was not finite/negative");
        check(Double.isFinite(below) && below > 0, "bury density was not finite/positive");
        check(outside == 0.0, "out-of-kernel density should be zero");

        ChunkPos chunkPos = new ChunkPos(1200, 1200);
        Beardifier beardifier = Beardifier.forStructuresInChunk(level.structureManager(), chunkPos);
        check(beardifier instanceof EnhancedBeardifierData, "Beardifier mixin did not expose enhanced terrain data");
        EnhancedBeardifierData data = (EnhancedBeardifierData) beardifier;
        int centerX = chunkPos.getMiddleBlockX();
        int baseY = level.getMinY() + 32;
        int centerZ = chunkPos.getMiddleBlockZ();
        BoundingBox generatedPieceBounds = new BoundingBox(centerX, baseY, centerZ, centerX, baseY, centerZ);
        DensityFunction.SinglePointContext aboveContext = new DensityFunction.SinglePointContext(centerX, baseY + 1, centerZ);
        DensityFunction.SinglePointContext belowContext = new DensityFunction.SinglePointContext(centerX, baseY - 1, centerZ);

        data.yungsapi_setEnhancedPieces(new ObjectArrayList<>());
        data.yungsapi_setEnhancedJunctions(new ObjectArrayList<>());
        double neutralBaseDensity = 0.375;
        for (int evaluation = 0; evaluation < 64; evaluation++) {
            double neutralDensity = EnhancedBeardifierHelper.computeDensity(aboveContext, neutralBaseDensity, data);
            check(sameDouble(neutralDensity, neutralBaseDensity),
                    "empty enhanced beardifier data changed density on evaluation " + evaluation);
        }

        EnhancedBeardifierRigid rigid = new EnhancedBeardifierRigid(generatedPieceBounds, adaptation, 0, Rotation.NONE);
        EnhancedJigsawJunction junction = new EnhancedJigsawJunction(
                new JigsawJunction(centerX, baseY, centerZ, 0, StructureTemplatePool.Projection.RIGID),
                adaptation
        );
        data.yungsapi_setEnhancedPieces(new ObjectArrayList<>(List.of(rigid)));
        data.yungsapi_setEnhancedJunctions(new ObjectArrayList<>(List.of(junction)));

        var firstPieceIterator = data.yungsapi_getEnhancedPieceIterator();
        var secondPieceIterator = data.yungsapi_getEnhancedPieceIterator();
        check(firstPieceIterator != secondPieceIterator, "piece getter reused a mutable iterator instance");
        check(firstPieceIterator.next() == rigid && secondPieceIterator.hasNext() && secondPieceIterator.next() == rigid,
                "advancing one piece iterator changed another iterator's cursor");

        var firstJunctionIterator = data.yungsapi_getEnhancedJunctionIterator();
        var secondJunctionIterator = data.yungsapi_getEnhancedJunctionIterator();
        check(firstJunctionIterator != secondJunctionIterator, "junction getter reused a mutable iterator instance");
        check(firstJunctionIterator.next() == junction && secondJunctionIterator.hasNext() && secondJunctionIterator.next() == junction,
                "advancing one junction iterator changed another iterator's cursor");

        double adaptedAbove = EnhancedBeardifierHelper.computeDensity(
                aboveContext, 0.0, data);
        double adaptedBelow = EnhancedBeardifierHelper.computeDensity(
                belowContext, 0.0, data);
        check(adaptedAbove < 0.0, "generated-piece bounds did not carve terrain above the piece base");
        check(adaptedBelow > 0.0, "generated-piece bounds did not bury terrain below the piece base");

        for (int evaluation = 0; evaluation < 256; evaluation++) {
            double repeatedAbove = EnhancedBeardifierHelper.computeDensity(aboveContext, 0.0, data);
            double repeatedBelow = EnhancedBeardifierHelper.computeDensity(belowContext, 0.0, data);
            check(sameDouble(repeatedAbove, adaptedAbove),
                    "repeated above-piece density changed on evaluation " + evaluation);
            check(sameDouble(repeatedBelow, adaptedBelow),
                    "repeated below-piece density changed on evaluation " + evaluation);
        }

        testParallelDensity(data, generatedPieceBounds, aboveContext);
    }

    private static void testParallelDensity(
            EnhancedBeardifierData data,
            BoundingBox generatedPieceBounds,
            DensityFunction.SinglePointContext context
    ) {
        BlockingTerrainAdaptation blockingAdaptation = new BlockingTerrainAdaptation();
        data.yungsapi_setEnhancedPieces(new ObjectArrayList<>(List.of(
                new EnhancedBeardifierRigid(generatedPieceBounds, blockingAdaptation, 0, Rotation.NONE)
        )));
        data.yungsapi_setEnhancedJunctions(new ObjectArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Double> first = executor.submit(() -> EnhancedBeardifierHelper.computeDensity(context, 0.0, data));
        Future<Double> second = executor.submit(() -> EnhancedBeardifierHelper.computeDensity(context, 0.0, data));
        try {
            double firstDensity = first.get(15, TimeUnit.SECONDS);
            double secondDensity = second.get(15, TimeUnit.SECONDS);
            check(firstDensity < 0.0, "parallel enhanced density did not preserve carve behavior");
            check(sameDouble(firstDensity, secondDensity), "parallel enhanced density evaluations disagreed");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("parallel enhanced density fixture was interrupted", exception);
        } catch (ExecutionException exception) {
            throw new AssertionError("parallel enhanced density evaluation failed", exception.getCause());
        } catch (TimeoutException exception) {
            throw new AssertionError("parallel enhanced density fixture timed out", exception);
        } finally {
            first.cancel(true);
            second.cancel(true);
            executor.shutdownNow();
        }
    }

    private static void testJigsaw(ServerLevel level) {
        Identifier templateId = Identifier.fromNamespaceAndPath(MOD_ID, "runtime_start");
        BlockPos capturePos = new BlockPos(0, level.getMinY() + 16, 0);
        level.setBlock(capturePos, Blocks.JIGSAW.defaultBlockState(), 3);
        JigsawBlockEntity jigsaw = (JigsawBlockEntity) level.getBlockEntity(capturePos);
        check(jigsaw != null, "failed to create synthetic jigsaw block entity");
        jigsaw.setName(Identifier.fromNamespaceAndPath(MOD_ID, "start"));
        jigsaw.setTarget(Identifier.fromNamespaceAndPath(MOD_ID, "unused"));
        jigsaw.setPool(Pools.EMPTY);

        StructureTemplate template = level.getStructureManager().getOrCreate(templateId);
        template.fillFromWorld(level, capturePos, new Vec3i(1, 1, 1), false, List.of());
        level.removeBlock(capturePos, false);

        Holder<StructureTemplatePool> empty = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL).getOrThrow(Pools.EMPTY);
        StructureTemplatePool pool = new StructureTemplatePool(
                empty,
                List.of(Pair.of(StructurePoolElement.single(templateId.toString()), 1)),
                StructureTemplatePool.Projection.RIGID
        );

        ChunkPos chunkPos = new ChunkPos(1200, 1200);
        Structure.GenerationContext context = new Structure.GenerationContext(
                level.registryAccess(),
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGenerator().getBiomeSource(),
                level.getChunkSource().randomState(),
                level.getStructureManager(),
                level.getSeed(),
                chunkPos,
                level,
                biome -> true
        );
        Optional<Structure.GenerationStub> generated = YungJigsawManager.assembleJigsawStructure(
                context,
                Holder.direct(pool),
                Optional.of(Identifier.fromNamespaceAndPath(MOD_ID, "start")),
                1,
                new BlockPos(chunkPos.getMiddleBlockX(), level.getMinY() + 32, chunkPos.getMiddleBlockZ()),
                false,
                Optional.empty(),
                32,
                Optional.empty(),
                Optional.empty(),
                DimensionPadding.ZERO,
                LiquidSettings.IGNORE_WATERLOGGING
        );
        check(generated.isPresent(), "YUNG jigsaw manager returned no generation stub");
        check(!generated.get().getPiecesBuilder().isEmpty(), "YUNG jigsaw manager produced no pieces");
        level.getStructureManager().remove(templateId);
    }

    private static void run(String name, Runnable fixture) {
        try {
            fixture.run();
            LOGGER.info("[YUNGSAPI-FIXTURE] {} PASS", name);
        } catch (Throwable failure) {
            LOGGER.error("[YUNGSAPI-FIXTURE] {} FAIL", name, failure);
            throw failure instanceof AssertionError assertion ? assertion : new AssertionError(name + " failed", failure);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static boolean sameDouble(double first, double second) {
        return Double.doubleToLongBits(first) == Double.doubleToLongBits(second);
    }
}
