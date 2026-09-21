package dev.resivore.enderscapepruning.client;

import dev.resivore.enderscapepruning.recipe.MatchaVoidCampfireRecipe;
import dev.resivore.enderscapepruning.recipe.MinecraftRecipeTestBootstrap;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.common.util.StackHelper;
import mezz.jei.library.ingredients.subtypes.SubtypeManager;
import mezz.jei.library.load.registration.SubtypeRegistration;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Ensures Enderscape's fallback subtype never conflicts with Matcha x JEI. */
final class EnderscapePruningJeiPluginTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        MinecraftRecipeTestBootstrap.initialize();
    }

    @Test
    void standaloneIntegrationKeysChickenEggFocusByTheKindlingItemModel() {
        RecordingRegistration registration = new RecordingRegistration();

        EnderscapePruningJeiPlugin.registerItemSubtypes(registration, false);

        assertEquals(1, registration.items.size());
        assertSame(Items.CHICKEN_SPAWN_EGG, registration.items.getFirst());
        assertEquals(1, registration.components.getFirst().length);
        assertSame(DataComponents.ITEM_MODEL, registration.components.getFirst()[0]);
    }

    @Test
    void matchaJeiRetainsSoleOwnershipOfItsRicherComponentSubtype() {
        RecordingRegistration registration = new RecordingRegistration();

        EnderscapePruningJeiPlugin.registerItemSubtypes(registration, true);

        assertEquals(List.of(), registration.items);
        assertEquals(List.of(), registration.components);
    }

    @Test
    void actualJeiRecipeUidsDoNotEquateKindlingWithAnOrdinaryEggOrStick() {
        SubtypeRegistration registration = new SubtypeRegistration();
        EnderscapePruningJeiPlugin.registerItemSubtypes(registration, false);
        StackHelper stackHelper = new StackHelper(new SubtypeManager(registration.getInterpreters()));
        ItemStack kindling = MatchaVoidCampfireRecipe.matchaKindlingDisplayStack();
        ItemStack ordinaryEgg = new ItemStack(Items.CHICKEN_SPAWN_EGG);
        ItemStack stick = new ItemStack(Items.STICK);

        assertFalse(stackHelper.isEquivalent(kindling, ordinaryEgg, UidContext.Recipe));
        assertFalse(stackHelper.isEquivalent(kindling, stick, UidContext.Recipe));
        assertNotEquals(stackHelper.getUidForStack(kindling, UidContext.Recipe),
                stackHelper.getUidForStack(ordinaryEgg, UidContext.Recipe));
        assertNotEquals(stackHelper.getUidForStack(kindling, UidContext.Recipe),
                stackHelper.getUidForStack(stick, UidContext.Recipe));
    }

    private static final class RecordingRegistration implements ISubtypeRegistration {
        private final List<Item> items = new ArrayList<>();
        private final List<DataComponentType<?>[]> components = new ArrayList<>();

        @Override
        public <B, I> void registerSubtypeInterpreter(
                IIngredientTypeWithSubtypes<B, I> ingredientType,
                B base,
                ISubtypeInterpreter<I> interpreter
        ) {
            throw new AssertionError("the fallback must use component subtype registration");
        }

        @Override
        public void registerSubtypeInterpreter(
                Item item,
                ISubtypeInterpreter<ItemStack> interpreter
        ) {
            throw new AssertionError("the fallback must use component subtype registration");
        }

        @Override
        public void registerFromDataComponentTypes(Item item, DataComponentType<?>... componentTypes) {
            items.add(item);
            components.add(componentTypes.clone());
        }
    }
}
