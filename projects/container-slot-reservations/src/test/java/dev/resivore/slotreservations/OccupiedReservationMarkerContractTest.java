package dev.resivore.slotreservations;

import com.mojang.blaze3d.pipeline.BlendFunction;
import net.minecraft.client.renderer.RenderPipelines;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OccupiedReservationMarkerContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final String RESOURCE =
            "assets/container_slot_reservations/textures/gui/sprites/occupied_reservation_marker.png";
    private static final Path SOURCE = ROOT.resolve("src/main/resources").resolve(RESOURCE);
    private static final String SHA256 =
            "D12D0BE850A742C69795259FDE9A5D9D52E822DF529196445948241B4B199F02";
    private static final int[][] PIXELS = {
            {0xFF6E7C48, 0xFF768450, 0xFF6E7C48},
            {0xFF586632, 0xFF6E7C48, 0xFF586632},
            {0x00000000, 0xFF586632, 0x00000000}
    };

    @Test
    void sourceAndProcessedResourceAreTheExactThreeByThreeRgbaPng() throws Exception {
        byte[] source = Files.readAllBytes(SOURCE);
        assertEquals(1_723, source.length);
        assertEquals(SHA256, sha256(source));
        assertArrayEquals(
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                java.util.Arrays.copyOf(source, 8)
        );
        assertEquals(8, Byte.toUnsignedInt(source[24]), "PNG must use eight-bit channels");
        assertEquals(6, Byte.toUnsignedInt(source[25]), "PNG must use RGBA color type 6");

        try (InputStream input = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(RESOURCE),
                "Missing processed marker resource"
        )) {
            assertArrayEquals(source, input.readAllBytes(),
                    "Resource processing must preserve the supplied PNG byte-for-byte");
        }

        BufferedImage image = ImageIO.read(SOURCE.toFile());
        assertNotNull(image);
        assertEquals(3, image.getWidth());
        assertEquals(3, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        for (int y = 0; y < PIXELS.length; y++) {
            for (int x = 0; x < PIXELS[y].length; x++) {
                assertEquals(PIXELS[y][x], image.getRGB(x, y), "Unexpected pixel at " + x + "," + y);
            }
        }
        assertEquals(0, image.getRGB(0, 2) >>> 24);
        assertEquals(0, image.getRGB(2, 2) >>> 24);
    }

    @Test
    void compiledRendererUsesOnlyThePixelExactGuiSpritePathForOccupiedReservations()
            throws IOException {
        MethodCode extract = methodCode(
                classpathEntry("dev/resivore/slotreservations/client/ReservationVisualRenderer.class"),
                "extract"
        );
        assertEquals(1, extract.calls.stream().filter(call ->
                call.owner().equals("net/minecraft/client/gui/GuiGraphicsExtractor")
                        && call.name().equals("blitSprite")
                        && call.descriptor().equals(
                            "(Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
                                    + "Lnet/minecraft/resources/Identifier;IIII)V")
        ).count());
        assertFalse(extract.calls.stream().anyMatch(call ->
                call.owner().equals("net/minecraft/client/gui/GuiGraphicsExtractor")
                        && call.name().equals("fill")),
                "The former opaque teal fill path must be absent");
    }

    @Test
    void minecraftGuiSpriteAtlasRetainsNearestSampling() throws IOException {
        assertEquals(
                BlendFunction.TRANSLUCENT,
                RenderPipelines.GUI_TEXTURED.getColorTargetState().blendFunction().orElseThrow(),
                "Transparent marker pixels must reveal the existing item sprite"
        );
        MethodCode upload = methodCode(
                classpathEntry("net/minecraft/client/renderer/texture/TextureAtlas.class"),
                "upload"
        );
        assertTrue(upload.fields.stream().anyMatch(field ->
                field.owner().equals("com/mojang/blaze3d/textures/FilterMode")
                        && field.name().equals("NEAREST")),
                "Minecraft 26.2 GUI sprites must retain nearest-neighbor sampling");
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes)
        );
    }

    private static byte[] classpathEntry(String name) throws IOException {
        try (InputStream input = OccupiedReservationMarkerContractTest.class
                .getClassLoader()
                .getResourceAsStream(name)) {
            assertNotNull(input, "Missing compiled classpath entry: " + name);
            return input.readAllBytes();
        }
    }

    private static MethodCode methodCode(byte[] classBytes, String methodName) {
        MethodCode result = new MethodCode();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!methodName.equals(name)) {
                    return null;
                }
                result.found = true;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name,
                                               String descriptor) {
                        result.fields.add(new FieldAccess(owner, name));
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                                                String descriptor, boolean isInterface) {
                        result.calls.add(new Invocation(owner, name, descriptor));
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertTrue(result.found, "Required compiled method is missing: " + methodName);
        return result;
    }

    private record Invocation(String owner, String name, String descriptor) {
    }

    private record FieldAccess(String owner, String name) {
    }

    private static final class MethodCode {
        private boolean found;
        private final List<Invocation> calls = new ArrayList<>();
        private final List<FieldAccess> fields = new ArrayList<>();
    }
}
