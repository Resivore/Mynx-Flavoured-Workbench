package com.yungnickyoung.minecraft.ribbits.network.payload;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/** Minimal owner request; the server trusts neither movement nor equipment from the client. */
public record ChutePressC2S(long sequence, Identifier dimensionId) implements CustomPacketPayload {
    public static final Type<ChutePressC2S> TYPE = new Type<>(RibbitsCommon.id("chute_press_c2s"));
    public static final StreamCodec<FriendlyByteBuf, ChutePressC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG,
            ChutePressC2S::sequence,
            Identifier.STREAM_CODEC,
            ChutePressC2S::dimensionId,
            ChutePressC2S::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
