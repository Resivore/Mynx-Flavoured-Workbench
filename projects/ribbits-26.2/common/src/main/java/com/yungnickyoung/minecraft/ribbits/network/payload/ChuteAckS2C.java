package com.yungnickyoung.minecraft.ribbits.network.payload;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.chute.ChuteAckState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/** Owner acknowledgement keyed to the exact physical press sequence. */
public record ChuteAckS2C(long sequence, ChuteAckState state) implements CustomPacketPayload {
    private static final StreamCodec<ByteBuf, ChuteAckState> STATE_CODEC =
            ByteBufCodecs.BYTE.map(ChuteAckState::fromNetworkId, ChuteAckState::networkId);

    public static final Type<ChuteAckS2C> TYPE = new Type<>(RibbitsCommon.id("chute_ack_s2c"));
    public static final StreamCodec<FriendlyByteBuf, ChuteAckS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG,
            ChuteAckS2C::sequence,
            STATE_CODEC,
            ChuteAckS2C::state,
            ChuteAckS2C::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
