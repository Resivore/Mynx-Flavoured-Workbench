package com.crispytwig.naturalist.persistence;

import com.mojang.serialization.Codec;
import com.crispytwig.naturalist.server.entity.persistence.NaturalistEntityPersistence;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalistEntityPersistenceTest {
    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createLookup();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(pending -> pending.apply());
    }

    @Test
    void ordinaryAndNonDefaultReptileVariantsRoundTrip() {
        Identifier ordinary = Identifier.fromNamespaceAndPath("naturalist", "brown");
        Identifier reptile = Identifier.fromNamespaceAndPath("naturalist", "coral_snake");

        assertAll(
                () -> assertEquals(Optional.of(ordinary), roundTripVariant(ordinary)),
                () -> assertEquals(Optional.of(reptile), roundTripVariant(reptile))
        );
    }

    @Test
    void legacyNumericVariantsUseStableNamesAndFloorMod() {
        String[] legacyNames = {"brown", "gray", "coral_snake"};
        CompoundTag positiveTag = new CompoundTag();
        positiveTag.putInt(NaturalistEntityPersistence.VARIANT_TAG, 2);
        CompoundTag negativeTag = new CompoundTag();
        negativeTag.putInt(NaturalistEntityPersistence.VARIANT_TAG, -1);

        Identifier expected = Identifier.fromNamespaceAndPath("naturalist", "coral_snake");
        assertAll(
                () -> assertEquals(Optional.of(expected), NaturalistEntityPersistence.readVariant(
                        input(positiveTag), legacyNames)),
                () -> assertEquals(Optional.of(expected), NaturalistEntityPersistence.readVariant(
                        input(negativeTag), legacyNames))
        );
    }

    @Test
    void missingAndMalformedVariantValuesRemainTolerable() {
        CompoundTag invalidIdentifier = new CompoundTag();
        invalidIdentifier.putString(NaturalistEntityPersistence.VARIANT_TAG, "not a valid identifier");
        CompoundTag wrongType = new CompoundTag();
        wrongType.put(NaturalistEntityPersistence.VARIANT_TAG, new CompoundTag());

        assertAll(
                () -> assertTrue(NaturalistEntityPersistence.readVariant(input(new CompoundTag()),
                        new String[]{"brown"}).isEmpty()),
                () -> assertTrue(NaturalistEntityPersistence.readVariant(input(invalidIdentifier),
                        new String[]{"brown"}).isEmpty()),
                () -> assertTrue(NaturalistEntityPersistence.readVariant(input(wrongType),
                        new String[]{"brown"}).isEmpty()),
                () -> assertTrue(NaturalistEntityPersistence.readVariant(input(wrongType),
                        new String[0]).isEmpty())
        );
    }

    @Test
    void elephantStyleFixedInventoryPreservesSlotsAndContents() {
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(0, new ItemStack(Items.APPLE, 3));
        source.setItem(12, new ItemStack(Items.CARROT, 5));
        source.setItem(25, new ItemStack(Items.SADDLE));
        source.setItem(26, new ItemStack(Items.BANNER.white()));

        TagValueOutput output = output();
        NaturalistEntityPersistence.saveFixedInventory(output, source);
        SimpleContainer restored = new SimpleContainer(27);
        NaturalistEntityPersistence.loadFixedInventory(input(output.buildResult()), restored);

        assertAll(
                () -> assertStack(restored, 0, Items.APPLE, 3),
                () -> assertStack(restored, 12, Items.CARROT, 5),
                () -> assertStack(restored, 25, Items.SADDLE, 1),
                () -> assertStack(restored, 26, Items.BANNER.white(), 1),
                () -> assertTrue(restored.getItem(1).isEmpty())
        );
    }

    @Test
    void ratStylePackedInventoryRetainsCompactInsertionOrder() {
        SimpleContainer source = new SimpleContainer(27);
        source.setItem(5, new ItemStack(Items.CARROT, 4));
        source.setItem(26, new ItemStack(Items.BREAD, 2));

        TagValueOutput output = output();
        NaturalistEntityPersistence.savePackedInventory(output, "CarriedItems", source);
        SimpleContainer restored = new SimpleContainer(27);
        restored.setItem(0, new ItemStack(Items.STONE));
        NaturalistEntityPersistence.loadPackedInventory(input(output.buildResult()), "CarriedItems", restored);

        assertAll(
                () -> assertStack(restored, 0, Items.CARROT, 4),
                () -> assertStack(restored, 1, Items.BREAD, 2),
                () -> assertTrue(restored.getItem(2).isEmpty()),
                () -> assertTrue(restored.getItem(26).isEmpty())
        );
    }

    @Test
    void missingAndMalformedPackedInventoriesLeaveExistingContentsAlone() {
        SimpleContainer missing = new SimpleContainer(27);
        missing.setItem(3, new ItemStack(Items.APPLE, 2));
        NaturalistEntityPersistence.loadPackedInventory(input(new CompoundTag()), "CarriedItems", missing);

        SimpleContainer malformed = new SimpleContainer(27);
        malformed.setItem(4, new ItemStack(Items.BREAD, 3));
        CompoundTag malformedTag = new CompoundTag();
        malformedTag.putString("CarriedItems", "not-an-item-list");
        NaturalistEntityPersistence.loadPackedInventory(input(malformedTag), "CarriedItems", malformed);

        assertAll(
                () -> assertStack(missing, 3, Items.APPLE, 2),
                () -> assertTrue(missing.getItem(0).isEmpty()),
                () -> assertStack(malformed, 4, Items.BREAD, 3),
                () -> assertTrue(malformed.getItem(0).isEmpty())
        );
    }

    @Test
    void currentAndLegacyAngerReferencesRetainUuid() {
        UUID currentUuid = UUID.fromString("8a97a4da-8aa7-4578-a090-2f499a4b04af");
        EntityReference<LivingEntity> current = EntityReference.of(currentUuid);
        TagValueOutput output = output();
        NaturalistEntityPersistence.saveReference(output, "angry_at", current);

        UUID legacyUuid = UUID.fromString("75f3b989-0d8c-4010-b9d1-e6be0b50326f");
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putIntArray("AngryAt", UUIDUtil.uuidToIntArray(legacyUuid));

        EntityReference<LivingEntity> restoredCurrent = NaturalistEntityPersistence.readReference(
                input(output.buildResult()), "angry_at");
        EntityReference<LivingEntity> restoredLegacy = NaturalistEntityPersistence.readReference(
                input(legacyTag), "AngryAt");

        assertAll(
                () -> assertEquals(currentUuid, restoredCurrent.getUUID()),
                () -> assertEquals(legacyUuid, restoredLegacy.getUUID())
        );
    }

    @Test
    void missingAndMalformedAngerReferencesRemainTolerable() {
        CompoundTag malformed = new CompoundTag();
        malformed.putString("angry_at", "definitely-not-a-uuid");

        assertAll(
                () -> assertNull(NaturalistEntityPersistence.<LivingEntity>readReference(
                        input(new CompoundTag()), "angry_at")),
                () -> assertNull(NaturalistEntityPersistence.<LivingEntity>readReference(
                        input(malformed), "angry_at"))
        );
    }

    @Test
    void ostrichLegacyLongArrayDecodesAndCurrentListRoundTrips() {
        long[] packedEggs = {17L, -42L, 8_293_764_391L};
        CompoundTag legacy = new CompoundTag();
        legacy.putLongArray("OwnedEggs", packedEggs);

        List<Long> decoded = input(legacy).read("OwnedEggs", Codec.LONG.listOf()).orElseThrow();
        TagValueOutput output = output();
        output.store("OwnedEggs", Codec.LONG.listOf(), decoded);

        assertAll(
                () -> assertEquals(List.of(17L, -42L, 8_293_764_391L), decoded),
                () -> assertEquals(decoded, input(output.buildResult())
                        .read("OwnedEggs", Codec.LONG.listOf()).orElseThrow())
        );
    }

    private static Optional<Identifier> roundTripVariant(Identifier variant) {
        TagValueOutput output = output();
        NaturalistEntityPersistence.saveVariant(output, variant);
        return NaturalistEntityPersistence.readVariant(input(output.buildResult()), new String[]{"unused"});
    }

    private static TagValueOutput output() {
        return TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
    }

    private static ValueInput input(CompoundTag tag) {
        return TagValueInput.create(ProblemReporter.DISCARDING, registries, tag);
    }

    private static void assertStack(SimpleContainer inventory, int slot,
                                    net.minecraft.world.item.Item expectedItem, int expectedCount) {
        ItemStack stack = inventory.getItem(slot);
        assertFalse(stack.isEmpty());
        assertTrue(stack.is(expectedItem));
        assertEquals(expectedCount, stack.getCount());
    }
}
