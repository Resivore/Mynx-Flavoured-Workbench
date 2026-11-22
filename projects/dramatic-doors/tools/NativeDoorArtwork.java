import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/** Compose local artwork at one source texel per destination texel. Coordinates start at the top left. */
class NativeDoorArtwork {
    static BufferedImage canvas(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    static void patch(BufferedImage src, int x, int y, int w, int h, BufferedImage dst, int dx, int dy) {
        dst.setRGB(dx, dy, w, h, src.getRGB(x, y, w, h, null, 0, w), 0, w);
    }

    static void rows(BufferedImage src, int y, int h, BufferedImage dst, int dy) {
        patch(src, 0, y, 16, h, dst, 0, dy);
    }

    static BufferedImage read(ZipFile jar, String path, String hash) throws Exception {
        var entry = jar.getEntry(path + ".png");
        if (entry == null) throw new IOException("Missing " + path);
        byte[] bytes;
        try (var stream = jar.getInputStream(entry)) {
            bytes = stream.readAllBytes();
        }
        var actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!actual.equals(hash)) throw new IOException("Unexpected artwork at " + path + ": " + actual);
        var image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image.getWidth() != 16 || image.getHeight() != 16) throw new IOException("Expected 16x16 at " + path);
        return image;
    }

    static BufferedImage shortVeiled(BufferedImage door) {
        var face = canvas(16, 16);
        rows(door, 0, 1, face, 0);
        rows(door, 2, 13, face, 1);       // Set the pierced lattice under a single cap.
        rows(door, 28, 1, face, 14);      // Carry the paired carving into a shallow apron.
        rows(door, 30, 1, face, 15);      // Bevel the foot and carry the hinge stile into the sill.
        patch(door, 0, 26, 2, 2, face, 0, 12);
        return face;
    }

    static BufferedImage shortMossy(BufferedImage door) {
        var face = canvas(16, 16);
        rows(door, 0, 2, face, 0);
        rows(door, 6, 12, face, 2);      // Keep all four lights, circular rim and adjacent moss.
        rows(door, 29, 2, face, 14);
        patch(door, 12, 21, 3, 3, face, 12, 11);
        return face;
    }

    static BufferedImage tallVeiled(BufferedImage door) {
        var face = canvas(16, 48);
        rows(door, 0, 2, face, 0);
        // Four solid rows under the cap. Facet the crown toward its center.
        for (int y = 2; y < 6; y++) {
            rows(door, 28 + (y - 2) % 2, 1, face, y);
            for (int x = 3; x <= 12; x++) {
                int inset = Math.min(x - 3, 12 - x);
                face.setRGB(x, y, door.getRGB(inset == y - 2 ? 6 : 5, inset == y - 2 ? 0 : 15));
            }
        }
        rows(door, 2, 14, face, 6);
        // A separate pair of recessed, chamfered leaves between the lattice and the carved foot.
        for (int y = 0; y < 12; y++) {
            rows(door, 17 + y, 1, face, 20 + y);
            for (int x = 3; x <= 12; x++) {
                int local = (x - 3) % 5;
                boolean corner = (y == 1 || y == 10) && (local == 0 || local == 4);
                int shade = door.getRGB(4, 14);
                if (y > 0 && y < 11 && !corner) {
                    if (local == 0 || y == 1) shade = door.getRGB(2, 2);
                    else if (local == 4 || y == 10) shade = door.getRGB(5, 0);
                    else shade = door.getRGB(3 + (local == 2 ? 1 : 0), 15);
                }
                face.setRGB(x, 20 + y, shade);
            }
        }
        rows(door, 16, 16, face, 32);
        return face;
    }

    static BufferedImage tallMossy(BufferedImage door) {
        var face = canvas(16, 48);
        rows(door, 0, 2, face, 0);
        // Continue the grain and lichen of the upper stiles below the cap.
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 4; y++) {
                int sampleY = 3 + (x + y) % 3;
                if (x == 0) sampleY = 3;
                face.setRGB(x, 2 + y, door.getRGB(x, sampleY));
            }
        }
        rows(door, 2, 22, face, 6);
        // Long lower battens have continuous grooves, a shaded top and rising patches of moss.
        int[] toe = {6, 0, 8, 5, 1, 0, 2, 4, 1, 0, 4, 6, 0, 1, 0, 0};
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 16; x++) {
                int sampleY = (y == 0 || y >= 12 - toe[x]) ? 24 + (y + x) % 5 : 25;
                int color = door.getRGB(x, sampleY);
                if (y > 0 && y < 12 - toe[x] && (x == 2 || x == 3 || x == 7)) {
                    color = door.getRGB(x == 2 ? 4 : 6, y < 3 + x % 3 ? 25 : 26);
                }
                face.setRGB(x, 28 + y, color);
            }
        }
        rows(door, 24, 8, face, 40);
        return face;
    }

    static BufferedImage shortItem(BufferedImage item, boolean veiled) {
        var icon = canvas(16, 16);
        patch(item, 3, 2, 10, 8, icon, 3, 3);
        patch(item, 3, veiled ? 12 : 11, 10, 1, icon, 3, 11);
        patch(item, 3, 14, 10, 2, icon, 3, 12);
        if (!veiled) patch(item, 10, 11, 2, 2, icon, 10, 10);
        return icon;
    }

    static BufferedImage tallItem(BufferedImage item, boolean veiled) {
        var icon = canvas(16, 16);
        // Retain both stiles and the complete cutouts. Remove only two solid flanking columns.
        int[] upperColumns = {3, 5, 6, 7, 8, 9, 10, 12};
        int[] lowerColumns = {3, 5, 6, 7, 8, 9, 11, 12};
        for (int x = 0; x < upperColumns.length; x++) {
            patch(item, upperColumns[x], 2, 1, 8, icon, x + 4, 0);
            int sx = lowerColumns[x];
            patch(item, sx, 10, 1, 3, icon, x + 4, 8);
            // Two narrow inset boards run down to the beveled foot, clear of the handle.
            for (int y = 11; y <= 12; y++) {
                int shade = item.getRGB(sx, veiled ? 11 : 13);
                if (x == 2 || x == 5) shade = item.getRGB(veiled ? 7 : 8, veiled ? 13 : 8);
                if (!veiled && x == 1 && y == 12) shade = item.getRGB(4, 11);
                icon.setRGB(x + 4, y, shade);
            }
            patch(item, sx, 13, 1, 3, icon, x + 4, 13);
        }
        if (!veiled) patch(item, 10, 11, 2, 2, icon, 9, 9);
        return icon;
    }

    static void save(Path dir, String path, BufferedImage image) throws IOException {
        Path output = dir.resolve(path + ".png");
        Files.createDirectories(output.getParent());
        if (!ImageIO.write(image, "png", output.toFile())) throw new IOException("No PNG writer");
    }

    static void compose(Path jarPath, String ns, String material, String[] hashes, Path out) throws Exception {
        var door = canvas(16, 32);
        BufferedImage item;
        try (var jar = new ZipFile(jarPath.toFile())) {
            String textures = "assets/" + ns + "/textures/";
            rows(read(jar, textures + "block/" + material + "_door_top", hashes[0]), 0, 16, door, 0);
            rows(read(jar, textures + "block/" + material + "_door_bottom", hashes[1]), 0, 16, door, 16);
            item = read(jar, textures + "item/" + material + "_door", hashes[2]);
        }
        var dir = out.resolve("resourcepacks/" + material + "_door/assets/dramaticdoors/textures");
        boolean veiled = material.equals("veiled");
        save(dir, "block/short_" + material + "_door", veiled ? shortVeiled(door) : shortMossy(door));
        var tall = veiled ? tallVeiled(door) : tallMossy(door);
        int offset = 0;
        for (String third : new String[]{"top", "middle", "bottom"}) {
            save(dir, "block/tall_" + material + "_door_" + third, tall.getSubimage(0, offset, 16, 16));
            offset += 16;
        }
        save(dir, "item/short_" + material + "_door", shortItem(item, veiled));
        save(dir, "item/tall_" + material + "_door", tallItem(item, veiled));
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[2]);
        compose(Path.of(args[0]), "enderscape", "veiled", new String[]{
                "9fb15510771a0bc5f0f64d5a772c3f622458478bd749907f812134698c527fcc",
                "6f09c5180f96604a17e4466a80d595ee2db5700ef29e01175b385accec9c0b65",
                "1863abebbce770c16a606e3a9282b3d36bf6e24ca69fa49956c604e2273e3eff"}, out);
        compose(Path.of(args[1]), "ribbits", "mossy_oak", new String[]{
                "94c788575b0f7e5b7cb949862f482a5ae4b0c11801ea6c69773b1e8b1cca8b01",
                "a339550fc925fe71d96cfc4c6e8e6eaa39b66e76c22bd6a6b9a81a6322f9e4bf",
                "ff7a3519d61ad8046e4177cf6dc53ae1a6814fd8d6fa5b224b10b67cfc91a8cc"}, out);
    }
}
