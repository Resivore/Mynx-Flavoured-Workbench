package dev.resivore.slotreservations;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

public final class ShulkerHostFingerprint {
    private ShulkerHostFingerprint() {}

    public static String of(ItemStack stack, RegistryAccess registries) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                Tag persistentStack = ItemStack.CODEC
                        .encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), stack)
                        .getOrThrow();
                writeCanonical(output, persistentStack);
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (NoSuchAlgorithmException | IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * Canonical local encoding of the complete persistent ItemStack tag. This deliberately
     * avoids packet codecs: modded packet layers may attach packet-only component encoders there.
     */
    private static void writeCanonical(DataOutputStream output, Tag tag) throws IOException {
        output.writeByte(tag.getId());
        switch (tag) {
            case EndTag ignored -> { }
            case ByteTag value -> output.writeByte(value.value());
            case ShortTag value -> output.writeShort(value.value());
            case IntTag value -> output.writeInt(value.value());
            case LongTag value -> output.writeLong(value.value());
            case FloatTag value -> output.writeInt(Float.floatToRawIntBits(value.value()));
            case DoubleTag value -> output.writeLong(Double.doubleToRawLongBits(value.value()));
            case ByteArrayTag value -> writeBytes(output, value.getAsByteArray());
            case StringTag value -> writeString(output, value.value());
            case ListTag value -> {
                output.writeInt(value.size());
                for (Tag element : value) writeCanonical(output, element);
            }
            case CompoundTag value -> {
                List<String> keys = new ArrayList<>(value.keySet());
                keys.sort(Comparator.naturalOrder());
                output.writeInt(keys.size());
                for (String key : keys) {
                    writeString(output, key);
                    writeCanonical(output, value.get(key));
                }
            }
            case IntArrayTag value -> {
                int[] array = value.getAsIntArray();
                output.writeInt(array.length);
                for (int element : array) output.writeInt(element);
            }
            case LongArrayTag value -> {
                long[] array = value.getAsLongArray();
                output.writeInt(array.length);
                for (long element : array) output.writeLong(element);
            }
            default -> throw new IllegalArgumentException("Unsupported ItemStack tag: " + tag.getClass());
        }
    }

    private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
        output.writeInt(value.length);
        output.write(value);
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        writeBytes(output, value.getBytes(StandardCharsets.UTF_8));
    }
}
