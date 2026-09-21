package dev.aero.cnmterraincompat.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Local-only C92 texture assembly. It reads the exact retained provider artifacts and produces only
 * ignored build output; no upstream image/model bytes are present in source resources.
 */
public final class PrivateBeamResourceAssembler {
    private static final String BBB_SHA256 = "0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a";
    private static final String ENDERSCAPE_SHA256 = "9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b";

    private PrivateBeamResourceAssembler() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException(
                "Expected <bbb-jar> <enderscape-jar> <private-output-root>");
        Path bbb = Path.of(args[0]).toRealPath();
        Path enderscape = Path.of(args[1]).toRealPath();
        Path output = Path.of(args[2]).toAbsolutePath().normalize();
        requireSha256(bbb, BBB_SHA256);
        requireSha256(enderscape, ENDERSCAPE_SHA256);
        try (ZipFile bbbZip = new ZipFile(bbb.toFile()); ZipFile enderscapeZip = new ZipFile(enderscape.toFile())) {
            // Validate the authored Beam topology as well as the exact source pixels used below.
            require(bbbZip, "assets/bbb/models/block/beam/oak_beam.json");
            BufferedImage beam = image(bbbZip, "assets/bbb/textures/block/beam/oak.png");
            BufferedImage beamTop = image(bbbZip, "assets/bbb/textures/block/beam/oak_top.png");
            for (String family : List.of("veiled", "celestial", "murublight")) {
                BufferedImage palette = image(enderscapeZip,
                        "assets/enderscape/textures/block/" + family + "_planks.png");
                write(output, family + "_beam.png", recolor(beam, palette));
                write(output, family + "_beam_top.png", recolor(beamTop, palette));
            }
        }
    }

    private static void write(Path root, String file, BufferedImage image) throws IOException {
        Path target = root.resolve("assets/cnm_terrain_slabs_compat/textures/block/private/enderscape/")
                .resolve(file);
        Files.createDirectories(target.getParent());
        if (!ImageIO.write(image, "PNG", target.toFile())) {
            throw new IOException("No PNG writer available for " + target);
        }
    }

    /** Maps each source luminance rank to the exact target plank palette while retaining pixel layout and alpha. */
    private static BufferedImage recolor(BufferedImage source, BufferedImage paletteSource) {
        List<Integer> palette = palette(paletteSource);
        if (palette.isEmpty()) throw new IllegalStateException("Enderscape plank palette is empty");
        int min = 255, max = 0;
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
            int argb = source.getRGB(x, y);
            if ((argb >>> 24) == 0) continue;
            int brightness = brightness(argb);
            min = Math.min(min, brightness);
            max = Math.max(max, brightness);
        }
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int range = Math.max(1, max - min);
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
            int sourceArgb = source.getRGB(x, y);
            int alpha = sourceArgb >>> 24;
            int rank = Math.round((brightness(sourceArgb) - min) * (palette.size() - 1) / (float) range);
            int color = palette.get(Math.max(0, Math.min(palette.size() - 1, rank)));
            result.setRGB(x, y, (alpha << 24) | (color & 0x00ffffff));
        }
        return result;
    }

    private static List<Integer> palette(BufferedImage image) {
        List<Integer> values = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int argb = image.getRGB(x, y);
            if ((argb >>> 24) != 0 && !values.contains(argb & 0x00ffffff)) values.add(argb & 0x00ffffff);
        }
        values.sort(Comparator.comparingInt(PrivateBeamResourceAssembler::brightness));
        return List.copyOf(values);
    }

    private static BufferedImage image(ZipFile archive, String entry) throws IOException {
        ZipEntry zipEntry = require(archive, entry);
        try (InputStream input = archive.getInputStream(zipEntry)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) throw new IOException("Not a readable PNG: " + entry);
            return image;
        }
    }

    private static ZipEntry require(ZipFile archive, String entry) {
        ZipEntry value = archive.getEntry(entry);
        if (value == null) throw new IllegalStateException("Required private source entry is absent: " + entry);
        return value;
    }

    private static int brightness(int rgb) {
        return (299 * ((rgb >> 16) & 0xff) + 587 * ((rgb >> 8) & 0xff) + 114 * (rgb & 0xff)) / 1000;
    }

    private static void requireSha256(Path file, String expected) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) >= 0;) digest.update(buffer, 0, read);
        }
        String actual = HexFormat.of().formatHex(digest.digest());
        if (!actual.equals(expected)) throw new IllegalStateException(
                "Private provider input SHA-256 mismatch: expected " + expected + " but got " + actual);
    }
}
