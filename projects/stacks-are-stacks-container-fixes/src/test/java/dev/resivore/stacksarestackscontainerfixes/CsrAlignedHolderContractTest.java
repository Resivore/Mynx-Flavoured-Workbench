package dev.resivore.stacksarestackscontainerfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import dev.resivore.slotreservations.api.Reservation;
import dev.resivore.slotreservations.api.ReservationSlotClass;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.zip.ZipFile;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CsrAlignedHolderContractTest {
    private static final Path CSR_ARTIFACT = Path.of(System.getProperty("csrJar"));

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void exactUnchangedCsrCanary5IsTheSharedContractReference() throws Exception {
        assertEquals(112_541L, Files.size(CSR_ARTIFACT));
        assertEquals(
                "D228EF4ECE4893A6538A845B34C1684A72FE8659790DC63AA12AB608AA886B8E",
                sha256(CSR_ARTIFACT));

        try (ZipFile zip = new ZipFile(CSR_ARTIFACT.toFile())) {
            String metadata = new String(
                    zip.getInputStream(Objects.requireNonNull(zip.getEntry("fabric.mod.json"))).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertTrue(metadata.contains("\"id\": \"container_slot_reservations\""));
            assertTrue(metadata.contains("\"version\": \"0.1.0-canary5\""));
        }
    }

    @Test
    void alignedNormallyNonStackableReservationIsCountIndependentAndComponentExact() {
        Holder<Item> alignedSaddle = alignedHolder(Items.SADDLE, 64);
        ItemStack threeSaddles = new ItemStack(alignedSaddle, 3);
        Reservation reservation = new Reservation(threeSaddles);

        assertEquals(64, threeSaddles.getMaxStackSize());
        assertTrue(threeSaddles.getComponentsPatch().isEmpty());
        ItemStack rematerializedTemplate = reservation.template();
        assertEquals(1, rematerializedTemplate.getCount());
        assertEquals(64, rematerializedTemplate.getMaxStackSize());
        assertTrue(rematerializedTemplate.getComponentsPatch().isEmpty(),
                "The aligned holder default must not be copied into an explicit MAX_STACK_SIZE patch");
        assertTrue(reservation.matches(new ItemStack(alignedSaddle, 17)));

        ItemStack componentDistinct = new ItemStack(alignedSaddle, 1);
        componentDistinct.set(DataComponents.CUSTOM_NAME, Component.literal("component-distinct"));
        assertFalse(reservation.matches(componentDistinct));
    }

    @Test
    void unchangedCsrClassificationUsesTheAlignedEffectiveMaximum() throws Exception {
        Holder<Item> alignedSaddle = alignedHolder(Items.SADDLE, 64);
        ItemStack incoming = new ItemStack(alignedSaddle, 3);
        ReservationData reservation = ReservationData.EMPTY.with(4, incoming);

        assertEquals(ReservationSlotClass.RESERVED_MATCH,
                classify(reservation, ItemStack.EMPTY, incoming, 64));
        assertEquals(ReservationSlotClass.OCCUPIED_COMPATIBLE,
                classify(reservation, new ItemStack(alignedSaddle, 3), incoming, 64));
        assertEquals(ReservationSlotClass.NON_WRITABLE,
                classify(reservation, new ItemStack(alignedSaddle, 64), incoming, 64));

        ItemStack distinct = new ItemStack(alignedSaddle, 1);
        distinct.set(DataComponents.CUSTOM_NAME, Component.literal("different"));
        assertEquals(ReservationSlotClass.RESERVED_OTHER,
                classify(reservation, ItemStack.EMPTY, distinct, 64));
    }

    private static ReservationSlotClass classify(
            ReservationData reservation,
            ItemStack physical,
            ItemStack incoming,
            int maximum
    ) throws Exception {
        Method classify = ContainerSlotReservationsApi.class.getDeclaredMethod(
                "classify",
                ReservationData.class,
                int.class,
                ItemStack.class,
                ItemStack.class,
                boolean.class,
                int.class);
        classify.setAccessible(true);
        return (ReservationSlotClass) classify.invoke(
                null, reservation, 4, physical, incoming, true, maximum);
    }

    private static Holder<Item> alignedHolder(Item item, int maximum) {
        return Holder.direct(item, DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, maximum)
                .build());
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = stream.read(buffer)) >= 0; ) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
