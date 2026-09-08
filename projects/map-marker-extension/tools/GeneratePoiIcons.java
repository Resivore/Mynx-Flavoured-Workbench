import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Reframes the authoritative 16-by-16 POI artwork into Minecraft's native
 * 8-by-8 map-decoration sprite canvas. The visible artwork, not its original
 * transparent canvas, is nearest-neighbour resampled to the largest centred
 * footprint that fits the red-X reference canvas.
 */
public final class GeneratePoiIcons {
    private static final int LOGICAL_SPRITE_SIZE = 8;

    private GeneratePoiIcons() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                "usage: GeneratePoiIcons <authoritative-input-dir> <jar-output-dir> <pack-output-dir>"
            );
        }

        Path input = Path.of(args[0]);
        Path jarOutput = Path.of(args[1]);
        Path packOutput = Path.of(args[2]);
        Files.createDirectories(jarOutput);
        Files.createDirectories(packOutput);

        try (var files = Files.list(input)) {
            for (Path source : files.filter(path -> path.getFileName().toString().endsWith(".png")).sorted().toList()) {
                BufferedImage reframed = reframe(ImageIO.read(source.toFile()));
                String name = source.getFileName().toString();
                write(reframed, jarOutput.resolve(name));
                write(reframed, packOutput.resolve(name));
            }
        }
    }

    static BufferedImage reframe(BufferedImage source) {
        Bounds artwork = bounds(source);
        double scale = Math.min(
            (double) LOGICAL_SPRITE_SIZE / artwork.width(),
            (double) LOGICAL_SPRITE_SIZE / artwork.height()
        );
        int targetWidth = Math.min(LOGICAL_SPRITE_SIZE, Math.max(1, (int) Math.round(artwork.width() * scale)));
        int targetHeight = Math.min(LOGICAL_SPRITE_SIZE, Math.max(1, (int) Math.round(artwork.height() * scale)));
        int offsetX = (LOGICAL_SPRITE_SIZE - targetWidth) / 2;
        int offsetY = (LOGICAL_SPRITE_SIZE - targetHeight) / 2;
        BufferedImage output = new BufferedImage(
            LOGICAL_SPRITE_SIZE,
            LOGICAL_SPRITE_SIZE,
            BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < targetHeight; y++) {
            int sourceY = artwork.minY() + Math.min(
                artwork.height() - 1,
                (int) Math.floor((y + 0.5D) * artwork.height() / targetHeight)
            );
            for (int x = 0; x < targetWidth; x++) {
                int sourceX = artwork.minX() + Math.min(
                    artwork.width() - 1,
                    (int) Math.floor((x + 0.5D) * artwork.width() / targetWidth)
                );
                output.setRGB(offsetX + x, offsetY + y, source.getRGB(sourceX, sourceY));
            }
        }
        return output;
    }

    private static Bounds bounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            throw new IllegalArgumentException("POI artwork must not be fully transparent");
        }
        return new Bounds(minX, minY, maxX, maxY);
    }

    private static void write(BufferedImage image, Path output) throws IOException {
        if (!ImageIO.write(image, "png", output.toFile())) {
            throw new IOException("No PNG writer available for " + output);
        }
    }

    private record Bounds(int minX, int minY, int maxX, int maxY) {
        int width() {
            return maxX - minX + 1;
        }

        int height() {
            return maxY - minY + 1;
        }
    }
}
