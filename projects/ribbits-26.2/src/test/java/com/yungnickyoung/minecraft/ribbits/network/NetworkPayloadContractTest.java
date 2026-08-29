package com.yungnickyoung.minecraft.ribbits.network;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.network.payload.RequestSupporterHatStatePayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.RibbitStartMusicAllPayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.RibbitStartMusicSinglePayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.RibbitStopMusicSinglePayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.StartHearingMaracaPayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.StopHearingMaracaPayload;
import com.yungnickyoung.minecraft.ribbits.network.payload.ToggleSupporterHatPayloadC2S;
import com.yungnickyoung.minecraft.ribbits.network.payload.ToggleSupporterHatPayloadS2C;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkPayloadContractTest {
    private static final UUID FIRST = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
    private static final UUID SECOND = UUID.fromString("fedcba98-7654-3210-fedc-ba9876543210");

    @Test
    void payloadTypeIdsRemainExact() {
        assertEquals(RibbitsCommon.id("ribbit_start_music_single"), RibbitStartMusicSinglePayload.TYPE.id());
        assertEquals(RibbitsCommon.id("ribbit_start_music_all"), RibbitStartMusicAllPayload.TYPE.id());
        assertEquals(RibbitsCommon.id("ribbit_stop_music_single"), RibbitStopMusicSinglePayload.TYPE.id());
        assertEquals(RibbitsCommon.id("start_hearing_maraca"), StartHearingMaracaPayload.TYPE.id());
        assertEquals(RibbitsCommon.id("stop_hearing_maraca"), StopHearingMaracaPayload.TYPE.id());
        assertEquals(RibbitsCommon.id("request_supporter_hat_state"), RequestSupporterHatStatePayload.TYPE.id());
        assertEquals(RibbitsCommon.id("toggle_supporter_hat_c2s"), ToggleSupporterHatPayloadC2S.TYPE.id());
        assertEquals(RibbitsCommon.id("toggle_supporter_hat_s2c"), ToggleSupporterHatPayloadS2C.TYPE.id());
    }

    @Test
    void singleAndBandMusicPayloadsRoundTrip() {
        RibbitStartMusicSinglePayload single = new RibbitStartMusicSinglePayload(
                FIRST, RibbitsCommon.id("bass"), 37);
        RibbitStartMusicAllPayload band = new RibbitStartMusicAllPayload(
                List.of(FIRST, SECOND),
                List.of(RibbitsCommon.id("bass"), RibbitsCommon.id("flute")),
                91);

        assertEquals(single, roundTrip(RibbitStartMusicSinglePayload.STREAM_CODEC, single));
        assertEquals(band, roundTrip(RibbitStartMusicAllPayload.STREAM_CODEC, band));
        assertEquals(new RibbitStopMusicSinglePayload(FIRST), roundTrip(
                RibbitStopMusicSinglePayload.STREAM_CODEC,
                new RibbitStopMusicSinglePayload(FIRST)));
    }

    @Test
    void maracaAndSupporterPayloadsRoundTrip() {
        assertEquals(new StartHearingMaracaPayload(FIRST), roundTrip(
                StartHearingMaracaPayload.STREAM_CODEC,
                new StartHearingMaracaPayload(FIRST)));
        assertEquals(new StopHearingMaracaPayload(FIRST), roundTrip(
                StopHearingMaracaPayload.STREAM_CODEC,
                new StopHearingMaracaPayload(FIRST)));
        assertEquals(new RequestSupporterHatStatePayload(List.of(FIRST, SECOND)), roundTrip(
                RequestSupporterHatStatePayload.STREAM_CODEC,
                new RequestSupporterHatStatePayload(List.of(FIRST, SECOND))));
        assertEquals(new ToggleSupporterHatPayloadC2S(FIRST, true), roundTrip(
                ToggleSupporterHatPayloadC2S.STREAM_CODEC,
                new ToggleSupporterHatPayloadC2S(FIRST, true)));
        assertEquals(new ToggleSupporterHatPayloadS2C(SECOND, false), roundTrip(
                ToggleSupporterHatPayloadS2C.STREAM_CODEC,
                new ToggleSupporterHatPayloadS2C(SECOND, false)));
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
