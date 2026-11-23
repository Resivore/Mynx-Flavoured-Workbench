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
 * Local-only C93 texture assembly. It reads the exact retained provider artifacts and produces only
 * ignored build output; no upstream image/model bytes are present in source resources.
 */
public final class PrivateBeamResourceAssembler {
    private static final String BBB_SHA256 = "0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a";
    private static final String ENDERSCAPE_SHA256 = "9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b";

    private PrivateBeamResourceAssembler() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 7) throw new IllegalArgumentException(
                "Expected <bbb-jar> <enderscape-jar> <output> <bbb-dev7> <ribbits> <vanilla-client>");
        Path bbb = Path.of(args[0]).toRealPath();
        Path enderscape = Path.of(args[1]).toRealPath();
        Path output = Path.of(args[2]).toAbsolutePath().normalize();
        Path currentBbb = Path.of(args[3]);
        Path ribbits = Path.of(args[4]);
        requireSha256(currentBbb, "57ddb5dfe62f2eb9f4a2ce22fbeeb5cce4386bbd93aab3f7df0dd8e6d19ddaf0");
        requireSha256(ribbits, "088c4b7e88c432d6275395e29273597cf42575f59350fe8b4f48c46e6df7f9fd");
        try (ZipFile oak = new ZipFile(currentBbb.toFile()); ZipFile rib = new ZipFile(ribbits.toFile());
             ZipFile vanilla = new ZipFile(args[5])) {
            BufferedImage planks = image(vanilla, "assets/minecraft/textures/block/oak_planks.png");
            BufferedImage moss = image(rib, "assets/ribbits/textures/block/mossy_oak_planks.png");
            for (String suffix : List.of("", "_top")) {
                BufferedImage beam = image(oak, "assets/bbb/textures/block/beam/oak" + suffix + ".png");
                write(output, "ribbits/mossy_oak_beam" + suffix + ".png", mossTransfer(beam, planks, moss));
            }
        }
        requireSha256(bbb, BBB_SHA256);
        requireSha256(enderscape, ENDERSCAPE_SHA256);
        try (ZipFile bbbZip = new ZipFile(bbb.toFile()); ZipFile enderscapeZip = new ZipFile(enderscape.toFile())) {
            // Validate the authored Beam topology as well as the exact source pixels used below.
            require(bbbZip, "assets/bbb/models/block/beam/oak_beam.json");
            BufferedImage beam = image(bbbZip, "assets/bbb/textures/block/beam/oak.png");
            BufferedImage beamTop = image(bbbZip, "assets/bbb/textures/block/beam/oak_top.png");
            try (ZipFile trees = new ZipFile(args[6])) {
                for (String family : List.of("silver_birch", "wisteria")) {
                    BufferedImage palette = image(trees, "assets/mynx_trees/textures/block/" + family + "_planks.png");
                    write(output, "mynx_trees/" + family + "_beam.png", recolor(beam, palette));
                    write(output, "mynx_trees/" + family + "_beam_top.png", recolor(beamTop, palette));
                }
            }
            for (String family : List.of("veiled", "celestial", "murublight")) {
                BufferedImage palette = image(enderscapeZip,
                        "assets/enderscape/textures/block/" + family + "_planks.png");
                write(output, "enderscape/" + family + "_beam.png", recolor(beam, palette));
                write(output, "enderscape/" + family + "_beam_top.png", recolor(beamTop, palette));
            }
        }
    }

    private static void write(Path root, String file, BufferedImage image) throws IOException {
        Path target = root.resolve("assets/cnm_terrain_slabs_compat/textures/block/private/")
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

    // AMC mossy_oak.moss_sheet for native 16px UVs. Only the olive footprint in
    // Ribbits' plank sprite changes, with the original Beam's per-texel luminance.
    private static BufferedImage mossTransfer(BufferedImage beam, BufferedImage oak, BufferedImage moss) {
        BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        if (beam.getWidth() != 16 || beam.getHeight() != 16) throw new IllegalArgumentException("Expected 16px Beam tile");
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int source = beam.getRGB(x, y), before = oak.getRGB(x, y), after = moss.getRGB(x, y);
            int pixel = source;
            if (source >>> 24 != 0 && before != after && ((after >> 16) & 255) - ((after >> 8) & 255) <= 13) {
                double gain = luminance(source) / (double) luminance(before);
                pixel = source & 0xff000000;
                for (int shift : List.of(16, 8, 0))
                    pixel |= Math.min(255, (int) Math.rint(((after >> shift) & 255) * gain)) << shift;
            }
            result.setRGB(x, y, pixel);
        }
        return result;
    }

    private static int luminance(int rgb) {
        return 299 * ((rgb >> 16) & 255) + 587 * ((rgb >> 8) & 255) + 114 * (rgb & 255);
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
