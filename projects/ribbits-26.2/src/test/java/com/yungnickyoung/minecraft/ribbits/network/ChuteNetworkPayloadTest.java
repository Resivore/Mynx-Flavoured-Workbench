package com.yungnickyoung.minecraft.ribbits.network;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.chute.ChuteAckState;
import com.yungnickyoung.minecraft.ribbits.network.payload.ChuteAckS2C;
import com.yungnickyoung.minecraft.ribbits.network.payload.ChutePressC2S;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChuteNetworkPayloadTest {
    @Test
    void payloadIdsAndMinimalFieldsRemainExact() {
        assertEquals(RibbitsCommon.id("chute_press_c2s"), ChutePressC2S.TYPE.id());
        assertEquals(RibbitsCommon.id("chute_ack_s2c"), ChuteAckS2C.TYPE.id());

        ChutePressC2S press = new ChutePressC2S(9223372036854775000L,
                Identifier.parse("minecraft:the_nether"));
        assertEquals(press, roundTrip(ChutePressC2S.STREAM_CODEC, press));
    }

    @Test
    void everyAcknowledgementStateRoundTripsWithItsSequence() {
        for (ChuteAckState state : ChuteAckState.values()) {
            ChuteAckS2C acknowledgement = new ChuteAckS2C(73L, state);
            assertEquals(acknowledgement, roundTrip(ChuteAckS2C.STREAM_CODEC, acknowledgement));
        }
    }

    private static <T> T roundTrip(StreamCodec<FriendlyByteBuf, T> codec, T value) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(buffer, value);
            return codec.decode(buffer);
        } finally {
            buffer.release();
        }
    }
}
