package tempeststudios.quickstacknearby;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Packet family intentionally contains only bounded live results and a server-validated target. */
final class NearbySearchPayload {
    static final CustomPacketPayload.Type<Snapshot> SNAPSHOT_TYPE = new CustomPacketPayload.Type<>(ResourceIdCompat.quickStackId("nearby_search_snapshot"));
    static final CustomPacketPayload.Type<Request> REQUEST_TYPE = new CustomPacketPayload.Type<>(ResourceIdCompat.quickStackId("nearby_search_request"));
    static final CustomPacketPayload.Type<Target> TARGET_TYPE = new CustomPacketPayload.Type<>(ResourceIdCompat.quickStackId("nearby_search_target"));
    static final CustomPacketPayload.Type<TargetResult> TARGET_RESULT_TYPE = new CustomPacketPayload.Type<>(ResourceIdCompat.quickStackId("nearby_search_target_result"));

    static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> SNAPSHOT_CODEC = CustomPacketPayload.codec(Snapshot::write, Snapshot::read);
    static final StreamCodec<RegistryFriendlyByteBuf, Request> REQUEST_CODEC = CustomPacketPayload.codec(Request::write, Request::read);
    static final StreamCodec<RegistryFriendlyByteBuf, Target> TARGET_CODEC = CustomPacketPayload.codec(Target::write, Target::read);
    static final StreamCodec<RegistryFriendlyByteBuf, TargetResult> TARGET_RESULT_CODEC = CustomPacketPayload.codec(TargetResult::write, TargetResult::read);

    private NearbySearchPayload() {}

    record Entry(ItemStack stack, int count, BlockPos position, String containerName, String nestedName, double distance) {
        void write(RegistryFriendlyByteBuf buf) {
            ItemStack.STREAM_CODEC.encode(buf, stack.copyWithCount(1));
            buf.writeVarInt(Math.max(0, Math.min(count, 1_000_000)));
            buf.writeBlockPos(position);
            buf.writeUtf(containerName, 128);
            buf.writeUtf(nestedName, 128);
            buf.writeDouble(Math.max(0D, Math.min(distance, 4096D)));
        }
        static Entry read(RegistryFriendlyByteBuf buf) {
            return new Entry(ItemStack.STREAM_CODEC.decode(buf), Math.max(0, Math.min(buf.readVarInt(), 1_000_000)),
                    buf.readBlockPos(), buf.readUtf(128), buf.readUtf(128), Math.max(0D, Math.min(buf.readDouble(), 4096D)));
        }
    }

    record Snapshot(List<Entry> entries) implements CustomPacketPayload {
        @Override public Type<? extends CustomPacketPayload> type() { return SNAPSHOT_TYPE; }
        void write(RegistryFriendlyByteBuf buf) { int n = Math.min(entries.size(), NearbySearchService.MAX_RECORDS); buf.writeVarInt(n); for (int i = 0; i < n; i++) entries.get(i).write(buf); }
        static Snapshot read(RegistryFriendlyByteBuf buf) { int n = Math.max(0, Math.min(buf.readVarInt(), NearbySearchService.MAX_RECORDS)); List<Entry> entries = new ArrayList<>(n); for (int i = 0; i < n; i++) entries.add(Entry.read(buf)); return new Snapshot(List.copyOf(entries)); }
    }
    record Request() implements CustomPacketPayload {
        @Override public Type<? extends CustomPacketPayload> type() { return REQUEST_TYPE; }
        void write(RegistryFriendlyByteBuf buf) {}
        static Request read(RegistryFriendlyByteBuf buf) { return new Request(); }
    }
    /** The client echoes the selected live result; the server re-finds it before allowing aim. */
    record Target(BlockPos position, ItemStack stack, String nestedName) implements CustomPacketPayload {
        @Override public Type<? extends CustomPacketPayload> type() { return TARGET_TYPE; }
        void write(RegistryFriendlyByteBuf buf) { buf.writeBlockPos(position); ItemStack.STREAM_CODEC.encode(buf, stack.copyWithCount(1)); buf.writeUtf(nestedName, 128); }
        static Target read(RegistryFriendlyByteBuf buf) { return new Target(buf.readBlockPos(), ItemStack.STREAM_CODEC.decode(buf), buf.readUtf(128)); }
    }
    record TargetResult(BlockPos position, boolean accepted) implements CustomPacketPayload {
        @Override public Type<? extends CustomPacketPayload> type() { return TARGET_RESULT_TYPE; }
        void write(RegistryFriendlyByteBuf buf) { buf.writeBlockPos(position); buf.writeBoolean(accepted); }
        static TargetResult read(RegistryFriendlyByteBuf buf) { return new TargetResult(buf.readBlockPos(), buf.readBoolean()); }
    }
}
