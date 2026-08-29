package com.yungnickyoung.minecraft.ribbits.data;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.client.model.RibbitModel;
import com.yungnickyoung.minecraft.ribbits.module.DataTicketModule;
import com.yungnickyoung.minecraft.ribbits.module.RibbitInstrumentModule;
import com.yungnickyoung.minecraft.ribbits.module.RibbitProfessionModule;
import com.yungnickyoung.minecraft.ribbits.module.RibbitUmbrellaTypeModule;
import com.yungnickyoung.minecraft.ribbits.module.SoundModule;
import com.geckolib.renderer.base.GeoRenderState;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RibbitDataAndModelContractTest {
    @BeforeAll
    static void initializeInstrumentSounds() {
        SoundModule.MUSIC_RIBBIT_BASS.setSupplier(() -> sound("music.ribbit.bass"));
        SoundModule.MUSIC_RIBBIT_BONGO.setSupplier(() -> sound("music.ribbit.bongo"));
        SoundModule.MUSIC_RIBBIT_FLUTE.setSupplier(() -> sound("music.ribbit.flute"));
        SoundModule.MUSIC_RIBBIT_GUITAR.setSupplier(() -> sound("music.ribbit.guitar"));
    }

    @Test
    void persistentCodecKeepsExactFieldNamesAndIdentityOrder() {
        RibbitData data = fisherman(RibbitInstrumentModule.BASS);

        JsonObject encoded = RibbitData.CODEC.encodeStart(JsonOps.INSTANCE, data)
                .getOrThrow()
                .getAsJsonObject();

        assertEquals("ribbits:fisherman", encoded.get("profession").getAsString());
        assertEquals("ribbits:umbrella_2", encoded.get("umbrella").getAsString());
        assertEquals("ribbits:bass", encoded.get("instrument").getAsString());

        RibbitData decoded = RibbitData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(RibbitProfessionModule.FISHERMAN, decoded.getProfession());
        assertEquals(RibbitUmbrellaTypeModule.UMBRELLA_2, decoded.getUmbrellaType());
        assertEquals(RibbitInstrumentModule.BASS, decoded.getInstrument());
    }

    @Test
    void syncedStreamCodecKeepsProfessionUmbrellaInstrumentOrder() {
        RibbitData expected = fisherman(RibbitInstrumentModule.FLUTE);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            RibbitData.STREAM_CODEC.encode(buffer, expected);
            RibbitData actual = RibbitData.STREAM_CODEC.decode(buffer);

            assertEquals(expected.getProfession(), actual.getProfession());
            assertEquals(expected.getUmbrellaType(), actual.getUmbrellaType());
            assertEquals(expected.getInstrument(), actual.getInstrument());
        } finally {
            buffer.release();
        }
    }

    @Test
    void instrumentModelHasHighestSelectionPriority() {
        GeoRenderState state = state(fisherman(RibbitInstrumentModule.BASS), true, true, true, true);

        assertEquals(RibbitsCommon.id("bass_ribbit"), new RibbitModel().getModelResource(state));
    }

    @Test
    void umbrellaModelPrecedesPrideWhenFallingOrInRain() {
        GeoRenderState falling = state(fisherman(RibbitInstrumentModule.NONE), false, true, false, true);
        GeoRenderState raining = state(fisherman(RibbitInstrumentModule.NONE), false, false, true, true);

        assertEquals(RibbitsCommon.id("umbrella/fisherman/umbrella_2"),
                new RibbitModel().getModelResource(falling));
        assertEquals(RibbitsCommon.id("umbrella/fisherman/umbrella_2"),
                new RibbitModel().getModelResource(raining));
    }

    @Test
    void pridePrecedesProfessionAndNoneNeverSelectsBlankInstrumentModel() {
        GeoRenderState pride = state(fisherman(RibbitInstrumentModule.NONE), true, false, false, true);
        GeoRenderState ordinary = state(fisherman(RibbitInstrumentModule.NONE), true, false, false, false);

        assertEquals(RibbitsCommon.id("pride_ribbit"), new RibbitModel().getModelResource(pride));
        assertEquals(RibbitsCommon.id("fisherman_ribbit"), new RibbitModel().getModelResource(ordinary));
    }

    @Test
    void allTwentyFiveDirectBehaviorModelIdsResolveThroughProductionSelection() {
        RibbitModel model = new RibbitModel();
        List<RibbitProfession> professions = List.of(
                RibbitProfessionModule.NITWIT,
                RibbitProfessionModule.GARDENER,
                RibbitProfessionModule.SORCERER,
                RibbitProfessionModule.FISHERMAN,
                RibbitProfessionModule.MERCHANT);
        List<RibbitUmbrellaType> umbrellas = List.of(
                RibbitUmbrellaTypeModule.UMBRELLA_1,
                RibbitUmbrellaTypeModule.UMBRELLA_2,
                RibbitUmbrellaTypeModule.UMBRELLA_3);
        List<RibbitInstrument> instruments = List.of(
                RibbitInstrumentModule.BASS,
                RibbitInstrumentModule.BONGO,
                RibbitInstrumentModule.FLUTE,
                RibbitInstrumentModule.GUITAR);

        for (RibbitProfession profession : professions) {
            RibbitData ordinary = new RibbitData(
                    profession, RibbitUmbrellaTypeModule.UMBRELLA_1, RibbitInstrumentModule.NONE);
            assertEquals(profession.modelLocation(),
                    model.getModelResource(state(ordinary, false, false, false, false)));

            for (RibbitUmbrellaType umbrella : umbrellas) {
                RibbitData rainy = new RibbitData(profession, umbrella, RibbitInstrumentModule.NONE);
                assertEquals(RibbitsCommon.id("umbrella/" + profession.id().getPath() + "/" + umbrella.modelLocationSuffix()),
                        model.getModelResource(state(rainy, false, false, true, false)));
            }
        }

        for (RibbitInstrument instrument : instruments) {
            RibbitData musician = new RibbitData(
                    RibbitProfessionModule.NITWIT, RibbitUmbrellaTypeModule.UMBRELLA_1, instrument);
            assertEquals(instrument.modelId(),
                    model.getModelResource(state(musician, true, false, false, false)));
        }

        assertEquals(RibbitsCommon.id("pride_ribbit"), model.getModelResource(state(
                new RibbitData(RibbitProfessionModule.NITWIT,
                        RibbitUmbrellaTypeModule.UMBRELLA_1, RibbitInstrumentModule.NONE),
                false, false, false, true)));
    }

    private static SoundEvent sound(String path) {
        return SoundEvent.createVariableRangeEvent(RibbitsCommon.id(path));
    }

    private static RibbitData fisherman(RibbitInstrument instrument) {
        return new RibbitData(
                RibbitProfessionModule.FISHERMAN,
                RibbitUmbrellaTypeModule.UMBRELLA_2,
                instrument);
    }

    private static GeoRenderState state(RibbitData data, boolean playingInstrument,
                                        boolean umbrellaFalling, boolean inRain, boolean pride) {
        Map<com.geckolib.constant.dataticket.DataTicket<?>, Object> values = new HashMap<>();
        GeoRenderState state = () -> values;
        state.addGeckolibData(DataTicketModule.DT_RIBBIT_DATA, data);
        state.addGeckolibData(DataTicketModule.DT_PLAYING_INSTRUMENT, playingInstrument);
        state.addGeckolibData(DataTicketModule.DT_UMBRELLA_FALLING, umbrellaFalling);
        state.addGeckolibData(DataTicketModule.DT_IN_RAIN, inRain);
        state.addGeckolibData(DataTicketModule.DT_IS_PRIDE_RIBBIT, pride);
        return state;
    }
}
