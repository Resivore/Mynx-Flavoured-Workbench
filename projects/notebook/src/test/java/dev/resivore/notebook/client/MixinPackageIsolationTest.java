package dev.resivore.notebook.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards against placing ordinary client classes in a Mixin-owned package. */
class MixinPackageIsolationTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final String MIXIN_PACKAGE = "dev.resivore.notebook.mixin.client";
    private static final String MIXIN_CLASS = "ContainerScreenAccess";

    @Test
    void sourceMixinPackageContainsOnlyTheDedicatedAccessor() throws IOException {
        String configuration = read("src/main/resources/notebook.client.mixins.json");
        String accessor = read("src/main/java/dev/resivore/notebook/mixin/client/ContainerScreenAccess.java");
        String screen = read("src/main/java/dev/resivore/notebook/client/NotebookScreen.java");
        String entrypoint = read("src/main/java/dev/resivore/notebook/NotebookClient.java");

        assertTrue(configuration.contains("\"package\": \"" + MIXIN_PACKAGE + "\""));
        assertTrue(configuration.contains("\"" + MIXIN_CLASS + "\""));
        assertTrue(accessor.startsWith("package " + MIXIN_PACKAGE + ";"));
        assertTrue(accessor.contains("@Mixin(AbstractContainerScreen.class)"));
        assertTrue(screen.startsWith("package dev.resivore.notebook.client;"));
        assertTrue(entrypoint.startsWith("package dev.resivore.notebook;"));
        assertFalse(screen.contains("package " + MIXIN_PACKAGE));
        assertFalse(entrypoint.contains("package " + MIXIN_PACKAGE + ";"));
    }

    @Test
    void packagedJarResolvesMixinClassesWithoutOwningNormalClientClasses() throws IOException {
        Path jar = findPackagedJar();
        String mixinClassEntry = MIXIN_PACKAGE.replace('.', '/') + "/" + MIXIN_CLASS + ".class";
        String screenEntry = "dev/resivore/notebook/client/NotebookScreen.class";
        String entrypointEntry = "dev/resivore/notebook/NotebookClient.class";
        String mixinPackagePath = MIXIN_PACKAGE.replace('.', '/') + "/";

        try (ZipFile archive = new ZipFile(jar.toFile())) {
            assertNotNull(archive.getEntry("fabric.mod.json"));
            assertNotNull(archive.getEntry("notebook.client.mixins.json"));
            assertNotNull(archive.getEntry(mixinClassEntry));
            assertNotNull(archive.getEntry(screenEntry));
            assertNotNull(archive.getEntry(entrypointEntry));
            assertNotNull(archive.getEntry("assets/notebook/textures/gui/notebook_book.png"));
            assertNotNull(archive.getEntry("assets/notebook/textures/gui/notebook_book_arrow_right.png"));

            List<String> mixinClasses = archive.stream()
                    .map(entry -> entry.getName())
                    .filter(name -> name.startsWith(mixinPackagePath) && name.endsWith(".class"))
                    .toList();
            assertEquals(List.of(mixinClassEntry), mixinClasses);
            assertFalse(screenEntry.startsWith(mixinPackagePath));
            assertFalse(entrypointEntry.startsWith(mixinPackagePath));

            BufferedImage bookArt = ImageIO.read(archive.getInputStream(
                    archive.getEntry("assets/notebook/textures/gui/notebook_book.png")));
            assertEquals(640, bookArt.getWidth());
            assertEquals(400, bookArt.getHeight());
        }
    }

    private static Path findPackagedJar() throws IOException {
        try (var files = Files.list(ROOT.resolve("build/libs"))) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> !path.getFileName().toString().contains("-sources"))
                    .filter(path -> !path.getFileName().toString().contains("-dev"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Notebook JAR was not built"));
        }
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
