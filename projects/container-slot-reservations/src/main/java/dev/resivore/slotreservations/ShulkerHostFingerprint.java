package dev.resivore.slotreservations;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ShulkerHostFingerprint {
    private ShulkerHostFingerprint() {}
    public static String of(ItemStack stack, RegistryAccess registries) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            ItemStack.STREAM_CODEC.encode(buffer, stack);
            byte[] bytes = new byte[buffer.readableBytes()]; buffer.readBytes(bytes);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        finally { buffer.release(); }
    }
}
