package dev.resivore.inventoryparticlesmatchacompat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ExclusiveParticleTargetTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.initialize();
    }

    @Test
    void exactComponentBackedMatchaStacksAreRecognizedButOrdinaryPotatoesPassThrough() {
        assertTarget(matchaStack(ExclusiveParticleTarget.GREEN_CURRY_MODEL), ExclusiveParticleTarget.GREEN_CURRY);
        assertTarget(matchaStack(ExclusiveParticleTarget.RAMEN_MODEL), ExclusiveParticleTarget.RAMEN);
        assertTarget(matchaStack(ExclusiveParticleTarget.CRYSTAL_HEART_MODEL), ExclusiveParticleTarget.CRYSTAL_HEART);

        assertTrue(ExclusiveParticleTarget.find(new ItemStack(Items.POISONOUS_POTATO)).isEmpty());
        assertTrue(ExclusiveParticleTarget.find(matchaStack(Identifier.withDefaultNamespace("apple_empanada"))).isEmpty());
    }

    @Test
    void exactRibbitsRegistryIdentitiesAreNarrowAndDoNotCatchOtherRibbitsItems() {
        assertEquals(Optional.of(ExclusiveParticleTarget.GLOWCAP), ExclusiveParticleTarget.find(
                new ExclusiveParticleTarget.StackIdentity(
                        ExclusiveParticleTarget.GLOWCAP_ITEM, null, false, false)));
        assertEquals(Optional.of(ExclusiveParticleTarget.TOADSTOOL_HEART), ExclusiveParticleTarget.find(
                new ExclusiveParticleTarget.StackIdentity(
                        ExclusiveParticleTarget.TOADSTOOL_HEART_ITEM, null, false, false)));
        assertTrue(ExclusiveParticleTarget.find(new ExclusiveParticleTarget.StackIdentity(
                Identifier.parse("ribbits:swamp_daisy"), null, false, false)).isEmpty());
    }

    @Test
    void explorerMapRequiresTheFullFilledMapAndMarkerShape() {
        assertTarget(explorerMap(true, true), ExclusiveParticleTarget.RIBBIT_VILLAGE_EXPLORER_MAP);
        assertTrue(ExclusiveParticleTarget.find(explorerMap(false, true)).isEmpty(),
                "A normal filled map must not be intercepted");
        assertTrue(ExclusiveParticleTarget.find(explorerMap(true, false)).isEmpty(),
                "The marker alone without map_id is not a real explorer map");

        ItemStack wrongItem = new ItemStack(Items.MAP);
        wrongItem.set(DataComponents.MAP_ID, new MapId(9));
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(ExclusiveParticleTarget.RIBBIT_VILLAGE_MARKER, true);
        wrongItem.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        assertTrue(ExclusiveParticleTarget.find(wrongItem).isEmpty());
    }

    @Test
    void eachTargetMapsToOneAndOnlyOneC4DataHolder() {
        assertEquals(ExclusiveParticleTarget.values().length,
                new HashSet<>(Arrays.stream(ExclusiveParticleTarget.values())
                        .map(ExclusiveParticleTarget::compatHolderName).toList()).size());
        for (ExclusiveParticleTarget target : ExclusiveParticleTarget.values()) {
            for (ExclusiveParticleTarget candidate : ExclusiveParticleTarget.values()) {
                assertEquals(target == candidate,
                        ExclusiveParticleDispatch.permits(target, candidate.compatHolderName()));
            }
        }
    }

    @Test
    void noTargetTakesTheExclusivePath() {
        assertFalse(ExclusiveParticleTarget.find(new ItemStack(Items.DIAMOND)).isPresent());
        assertFalse(ExclusiveParticleTarget.find(new ItemStack(Items.POISONOUS_POTATO)).isPresent());
    }

    private static ItemStack matchaStack(Identifier model) {
        ItemStack stack = new ItemStack(Items.POISONOUS_POTATO);
        stack.set(DataComponents.ITEM_MODEL, model);
        return stack;
    }

    private static ItemStack explorerMap(boolean markerPresent, boolean mapIdPresent) {
        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        if (mapIdPresent) {
            stack.set(DataComponents.MAP_ID, new MapId(41));
        }
        if (markerPresent) {
            CompoundTag marker = new CompoundTag();
            marker.putBoolean(ExclusiveParticleTarget.RIBBIT_VILLAGE_MARKER, true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        }
        return stack;
    }

    private static void assertTarget(ItemStack stack, ExclusiveParticleTarget expected) {
        assertEquals(Optional.of(expected), ExclusiveParticleTarget.find(stack));
    }
}
