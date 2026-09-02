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
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RibbitDataAndModelContractTest {
    @BeforeAll
    static void initializeInstrumentSounds() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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
    void allNineProfessionsRoundTripThroughPersistentAndStreamCodecs() {
        for (RibbitProfession profession : RibbitProfessionModule.ALL_PROFESSIONS) {
            RibbitData expected = new RibbitData(
                    profession, RibbitUmbrellaTypeModule.UMBRELLA_3, RibbitInstrumentModule.NONE);

            RibbitData persistent = RibbitData.CODEC.parse(
                    JsonOps.INSTANCE,
                    RibbitData.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow()).getOrThrow();
            assertSame(profession, persistent.getProfession(), profession.toString());
            assertSame(RibbitUmbrellaTypeModule.UMBRELLA_3, persistent.getUmbrellaType());
            assertSame(RibbitInstrumentModule.NONE, persistent.getInstrument());

            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                RibbitData.STREAM_CODEC.encode(buffer, expected);
                RibbitData streamed = RibbitData.STREAM_CODEC.decode(buffer);
                assertSame(profession, streamed.getProfession(), profession.toString());
                assertSame(RibbitUmbrellaTypeModule.UMBRELLA_3, streamed.getUmbrellaType());
                assertSame(RibbitInstrumentModule.NONE, streamed.getInstrument());
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    void unknownPersistentAndStreamIdsFallBackToSafeCanonicalValues() {
        JsonObject unknown = new JsonObject();
        unknown.addProperty("profession", "example:removed_profession");
        unknown.addProperty("umbrella", "example:removed_umbrella");
        unknown.addProperty("instrument", "example:removed_instrument");
        RibbitData persistent = RibbitData.CODEC.parse(JsonOps.INSTANCE, unknown).getOrThrow();

        assertSame(RibbitProfessionModule.NITWIT, persistent.getProfession());
        assertSame(RibbitUmbrellaTypeModule.UMBRELLA_1, persistent.getUmbrellaType());
        assertSame(RibbitInstrumentModule.NONE, persistent.getInstrument());

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            Identifier.STREAM_CODEC.encode(buffer, Identifier.parse("example:removed_profession"));
            Identifier.STREAM_CODEC.encode(buffer, Identifier.parse("example:removed_umbrella"));
            Identifier.STREAM_CODEC.encode(buffer, Identifier.parse("example:removed_instrument"));
            RibbitData streamed = RibbitData.STREAM_CODEC.decode(buffer);
            assertSame(RibbitProfessionModule.NITWIT, streamed.getProfession());
            assertSame(RibbitUmbrellaTypeModule.UMBRELLA_1, streamed.getUmbrellaType());
            assertSame(RibbitInstrumentModule.NONE, streamed.getInstrument());
        } finally {
            buffer.release();
        }
    }

    @Test
    void professionRegistryAndVillagePoolHaveSeparateExactImmutableOrders() {
        List<String> allIds = RibbitProfessionModule.ALL_PROFESSIONS.stream()
                .map(profession -> profession.id().toString())
                .toList();
        assertEquals(List.of(
                "ribbits:nitwit",
                "ribbits:gardener",
                "ribbits:sorcerer",
                "ribbits:fisherman",
                "ribbits:merchant",
                "ribbits:chef",
                "ribbits:farmer",
                "ribbits:prospector",
                "ribbits:guard"), allIds);
        assertEquals(RibbitProfessionModule.ALL_PROFESSIONS,
                new ArrayList<>(RibbitProfessionModule.professionRegistry().values()));
        assertEquals(9, new HashSet<>(allIds).size());
        for (RibbitProfession profession : RibbitProfessionModule.ALL_PROFESSIONS) {
            assertSame(profession, RibbitProfessionModule.getProfession(profession.id()));
        }

        assertEquals(List.of(
                        RibbitProfessionModule.NITWIT,
                        RibbitProfessionModule.GARDENER,
                        RibbitProfessionModule.FISHERMAN,
                        RibbitProfessionModule.MERCHANT,
                        RibbitProfessionModule.CHEF,
                        RibbitProfessionModule.FARMER,
                        RibbitProfessionModule.PROSPECTOR,
                        RibbitProfessionModule.GUARD),
                RibbitProfessionModule.VILLAGE_PROFESSIONS);
        assertFalse(RibbitProfessionModule.VILLAGE_PROFESSIONS.contains(RibbitProfessionModule.SORCERER));
        assertTrue(RibbitProfessionModule.ALL_PROFESSIONS.contains(RibbitProfessionModule.SORCERER));
        assertThrows(UnsupportedOperationException.class,
                () -> RibbitProfessionModule.ALL_PROFESSIONS.add(RibbitProfessionModule.NITWIT));
        assertThrows(UnsupportedOperationException.class,
                () -> RibbitProfessionModule.VILLAGE_PROFESSIONS.clear());
        assertThrows(UnsupportedOperationException.class,
                () -> RibbitProfessionModule.professionRegistry().clear());
    }

    @Test
    void allNormalModelAndTextureIdentitiesAreExactAndIdDefinesEquality() {
        Map<RibbitProfession, String> models = Map.of(
                RibbitProfessionModule.NITWIT, "ribbits:nitwit_ribbit",
                RibbitProfessionModule.GARDENER, "ribbits:gardener_ribbit",
                RibbitProfessionModule.SORCERER, "ribbits:sorcerer_ribbit",
                RibbitProfessionModule.FISHERMAN, "ribbits:fisherman_ribbit",
                RibbitProfessionModule.MERCHANT, "ribbits:merchant_ribbit",
                RibbitProfessionModule.CHEF, "ribbits:chef_ribbit",
                RibbitProfessionModule.FARMER, "ribbits:farmer_ribbit",
                RibbitProfessionModule.PROSPECTOR, "ribbits:prospector_ribbit",
                RibbitProfessionModule.GUARD, "ribbits:guard_ribbit");
        for (Map.Entry<RibbitProfession, String> entry : models.entrySet()) {
            assertEquals(entry.getValue(), entry.getKey().modelLocation().toString());
        }

        for (RibbitProfession profession : List.of(
                RibbitProfessionModule.NITWIT,
                RibbitProfessionModule.GARDENER,
                RibbitProfessionModule.SORCERER,
                RibbitProfessionModule.FISHERMAN,
                RibbitProfessionModule.MERCHANT)) {
            assertEquals("ribbits:textures/entity/ribbit.png", profession.textureLocation().toString());
        }
        for (RibbitProfession profession : List.of(
                RibbitProfessionModule.CHEF,
                RibbitProfessionModule.FARMER,
                RibbitProfessionModule.PROSPECTOR,
                RibbitProfessionModule.GUARD)) {
            assertEquals("ribbits:textures/entity/" + profession.id().getPath() + "_ribbit.png",
                    profession.textureLocation().toString());
        }

        RibbitProfession sameIdDifferentResources = new RibbitProfession(
                RibbitProfessionModule.GUARD.id(),
                RibbitsCommon.id("different_model"),
                RibbitsCommon.id("textures/entity/different.png"));
        assertEquals(RibbitProfessionModule.GUARD, sameIdDifferentResources);
        assertEquals(RibbitProfessionModule.GUARD.hashCode(), sameIdDifferentResources.hashCode());
    }

    @Test
    void callerRandomControlsEqualWeightVillageSelectionWithoutSorcerers() {
        Set<RibbitProfession> observed = new HashSet<>();
        RandomSource oracle = RandomSource.create(0x5EEDC0DEL);
        RandomSource caller = RandomSource.create(0x5EEDC0DEL);
        for (int sample = 0; sample < 512; sample++) {
            RibbitProfession expectedProfession = RibbitProfessionModule.VILLAGE_PROFESSIONS
                    .get(oracle.nextInt(RibbitProfessionModule.VILLAGE_PROFESSIONS.size()));
            var expectedUmbrella = List.of(
                            RibbitUmbrellaTypeModule.UMBRELLA_1,
                            RibbitUmbrellaTypeModule.UMBRELLA_2,
                            RibbitUmbrellaTypeModule.UMBRELLA_3)
                    .get(oracle.nextInt(3));

            RibbitData actual = RibbitProfessionModule.createVillageRibbitData(caller);
            assertSame(expectedProfession, actual.getProfession(), "sample " + sample);
            assertSame(expectedUmbrella, actual.getUmbrellaType(), "sample " + sample);
            assertSame(expectedProfession == RibbitProfessionModule.NITWIT
                            ? RibbitInstrumentModule.BONGO
                            : RibbitInstrumentModule.NONE,
                    actual.getInstrument(), "sample " + sample);
            assertNotEquals(RibbitProfessionModule.SORCERER, actual.getProfession());
            observed.add(actual.getProfession());
        }
        assertEquals(new HashSet<>(RibbitProfessionModule.VILLAGE_PROFESSIONS), observed);
    }

    @Test
    void typedNewProfessionEggDataAlwaysUsesNoInstrument() {
        for (RibbitProfession profession : List.of(
                RibbitProfessionModule.CHEF,
                RibbitProfessionModule.FARMER,
                RibbitProfessionModule.PROSPECTOR,
                RibbitProfessionModule.GUARD)) {
            RibbitData data = RibbitProfessionModule.createTypedSpawnEggData(
                    profession, RandomSource.create(42));
            assertSame(profession, data.getProfession());
            assertSame(RibbitInstrumentModule.NONE, data.getInstrument());
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
    void allFortyOneDirectBehaviorModelIdsResolveThroughProductionSelection() {
        RibbitModel model = new RibbitModel();
        List<RibbitProfession> professions = RibbitProfessionModule.ALL_PROFESSIONS;
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
            assertEquals(profession.textureLocation(),
                    model.getTextureResource(state(ordinary, false, false, false, false)));

            for (RibbitUmbrellaType umbrella : umbrellas) {
                RibbitData rainy = new RibbitData(profession, umbrella, RibbitInstrumentModule.NONE);
                assertEquals(RibbitsCommon.id("umbrella/" + profession.id().getPath() + "/" + umbrella.modelLocationSuffix()),
                        model.getModelResource(state(rainy, false, false, true, false)));
                assertEquals(profession.textureLocation(),
                        model.getTextureResource(state(rainy, false, false, true, false)));
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

        for (RibbitProfession profession : List.of(
                RibbitProfessionModule.CHEF,
                RibbitProfessionModule.FARMER,
                RibbitProfessionModule.PROSPECTOR,
                RibbitProfessionModule.GUARD)) {
            RibbitData impossibleInstrumentState = new RibbitData(
                    profession, RibbitUmbrellaTypeModule.UMBRELLA_1, RibbitInstrumentModule.BASS);
            assertEquals(profession.modelLocation(),
                    model.getModelResource(state(impossibleInstrumentState, true, false, false, false)));
        }
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
