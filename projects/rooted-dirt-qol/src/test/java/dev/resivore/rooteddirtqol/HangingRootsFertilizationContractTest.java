package dev.resivore.rooteddirtqol;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.RootedDirtBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HangingRootsFertilizationContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Path MIXIN_SOURCE = PROJECT_ROOT.resolve(
        "src/main/java/dev/resivore/rooteddirtqol/mixin/HangingRootsBlockMixin.java"
    );

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        RegistryAccess builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        HolderLookup.Provider vanillaData = VanillaRegistries.createLookup();
        HolderLookup.Provider registries = HolderLookup.Provider.create(
            java.util.stream.Stream.concat(
                builtIns.listRegistries(),
                vanillaData.listRegistries().filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())
            )
        );
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
            .forEach(pending -> pending.apply());
    }

    @Test
    void mixinAddsOnlyTheVanillaBonemealableContractToHangingRoots() throws Exception {
        String source = Files.readString(MIXIN_SOURCE);

        assertAll(
            () -> assertTrue(source.contains("@Mixin(HangingRootsBlock.class)")),
            () -> assertTrue(source.contains("implements BonemealableBlock")),
            () -> assertEquals(2, occurrences(source, "return true;")),
            () -> assertEquals(1, occurrences(source, "Block.popResource(")),
            () -> assertEquals(1, occurrences(source, "new ItemStack(Blocks.HANGING_ROOTS)")),
            () -> assertFalse(source.contains("RootedDirtBlock")),
            () -> assertFalse(source.contains("BoneMealItem")),
            () -> assertFalse(source.contains("Dispenser")),
            () -> assertFalse(source.contains("setBlock")),
            () -> assertFalse(source.contains("destroyBlock")),
            () -> assertFalse(source.contains("removeBlock"))
        );

        ItemStack vanillaHangingRootsStack = new ItemStack(
            net.minecraft.world.level.block.Blocks.HANGING_ROOTS
        );
        assertEquals(1, vanillaHangingRootsStack.getCount());
        assertTrue(vanillaHangingRootsStack.is(
            net.minecraft.world.level.block.Blocks.HANGING_ROOTS.asItem()
        ));
    }

    @Test
    void minecraft26_2VanillaContractsRemainAvailable() throws Exception {
        assertFalse(BonemealableBlock.class.isAssignableFrom(HangingRootsBlock.class),
            "Vanilla HangingRootsBlock should remain the sole interface addition target");
        assertTrue(BonemealableBlock.class.isAssignableFrom(TallFlowerBlock.class));
        assertTrue(BonemealableBlock.class.isAssignableFrom(RootedDirtBlock.class));

        assertBonemealableMethods(RootedDirtBlock.class);
        assertBonemealableMethods(TallFlowerBlock.class);

        Method growCrop = BoneMealItem.class.getDeclaredMethod(
            "growCrop", ItemStack.class, Level.class, BlockPos.class
        );
        assertEquals(boolean.class, growCrop.getReturnType());

        String boneMealContract = classContract(BoneMealItem.class);
        assertAll(
            () -> assertTrue(boneMealContract.contains("BonemealableBlock")),
            () -> assertTrue(boneMealContract.contains("isValidBonemealTarget")),
            () -> assertTrue(boneMealContract.contains("isBonemealSuccess")),
            () -> assertTrue(boneMealContract.contains("performBonemeal")),
            () -> assertTrue(boneMealContract.contains("shrink"))
        );
    }

    @Test
    void vanillaDispenserBonemealUsesTheSameGrowCropPath() throws Exception {
        String dispenserContract = classContract(
            Class.forName("net.minecraft.core.dispenser.DispenseItemBehavior$5")
        );
        assertAll(
            () -> assertTrue(dispenserContract.contains("BoneMealItem")),
            () -> assertTrue(dispenserContract.contains("growCrop"))
        );
    }

    private static void assertBonemealableMethods(Class<?> blockClass) throws Exception {
        assertEquals(boolean.class, blockClass.getDeclaredMethod(
            "isValidBonemealTarget", LevelReader.class, BlockPos.class, BlockState.class
        ).getReturnType());
        assertEquals(boolean.class, blockClass.getDeclaredMethod(
            "isBonemealSuccess", Level.class, RandomSource.class, BlockPos.class, BlockState.class
        ).getReturnType());
        assertEquals(void.class, blockClass.getDeclaredMethod(
            "performBonemeal", ServerLevel.class, RandomSource.class, BlockPos.class, BlockState.class
        ).getReturnType());
    }

    private static String classContract(Class<?> type) throws Exception {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resource)) {
            assertNotNull(input, "Missing class bytes for " + type.getName());
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
