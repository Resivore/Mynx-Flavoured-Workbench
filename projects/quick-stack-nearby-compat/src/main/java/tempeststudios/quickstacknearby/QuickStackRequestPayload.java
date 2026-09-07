package tempeststudios.quickstacknearby;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record QuickStackRequestPayload(List<SlotRule> slotRules) {
    private static final int MAX_SLOT_RULES = 64;

    public static final QuickStackRequestPayload EMPTY = new QuickStackRequestPayload(List.of());

    public QuickStackRequestPayload {
        slotRules = List.copyOf(slotRules);
    }

    public void write(FriendlyByteBuf buf) {
        int count = Math.min(slotRules.size(), MAX_SLOT_RULES);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            SlotRule rule = slotRules.get(i);
            buf.writeVarInt(rule.slotIndex());
            buf.writeBoolean(rule.locked());
            buf.writeVarInt(Math.max(0, rule.keepCount()));
        }
    }

    public static QuickStackRequestPayload read(FriendlyByteBuf buf) {
        int count = Math.max(0, Math.min(MAX_SLOT_RULES, buf.readVarInt()));
        List<SlotRule> rules = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int slotIndex = buf.readVarInt();
            boolean locked = buf.readBoolean();
            int keepCount = Math.max(0, buf.readVarInt());
            rules.add(new SlotRule(slotIndex, locked, keepCount));
        }
        return new QuickStackRequestPayload(rules);
    }

    public record SlotRule(int slotIndex, boolean locked, int keepCount) {
    }
}
