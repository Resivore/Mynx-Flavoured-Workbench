package dev.resivore.slotreservations.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

/** Exact server-resolvable host coordinates; never an equality search for an item. */
public record ShulkerHostLocator(Kind kind, int visibleMenuSlot, int physicalPlayerSlot) {
    public enum Kind { MENU_SLOT, PLAYER_INVENTORY }

    public static ShulkerHostLocator menuSlot(int menuSlot) {
        return new ShulkerHostLocator(Kind.MENU_SLOT, menuSlot, -1);
    }

    public static ShulkerHostLocator playerInventory(int visibleMenuSlot, int physicalPlayerSlot) {
        return new ShulkerHostLocator(Kind.PLAYER_INVENTORY, visibleMenuSlot, physicalPlayerSlot);
    }

    public static ShulkerHostLocator read(FriendlyByteBuf buffer) {
        int id = buffer.readUnsignedByte();
        if (id < 0 || id >= Kind.values().length) {
            throw new DecoderException("Unknown shulker host locator kind " + id);
        }
        return new ShulkerHostLocator(Kind.values()[id], buffer.readVarInt(), buffer.readVarInt());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(kind.ordinal());
        buffer.writeVarInt(visibleMenuSlot);
        buffer.writeVarInt(physicalPlayerSlot);
    }
}
