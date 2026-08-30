package com.crispytwig.naturalist.server.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.crispytwig.naturalist.registry.NaturalistRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("unused")
public record BugNetInteractionRecipe(EntityType<?> entityType, ItemStack dropStack) implements Recipe<RecipeInput> {
    @Override
    public boolean matches(@NotNull RecipeInput input, @NotNull Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input) {
        return dropStack;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public @NotNull String group() {
        return "";
    }

    @Override
    public @NotNull RecipeSerializer<BugNetInteractionRecipe> getSerializer() {
        return NaturalistRecipes.BUG_NET_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<BugNetInteractionRecipe> getType() {
        return NaturalistRecipes.BUG_NET.get();
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    public static final MapCodec<BugNetInteractionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity_type").forGetter(BugNetInteractionRecipe::entityType),
                    ItemStack.CODEC.fieldOf("result").forGetter(BugNetInteractionRecipe::dropStack)
            ).apply(instance, BugNetInteractionRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BugNetInteractionRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                buf.writeIdentifier(BuiltInRegistries.ENTITY_TYPE.getKey(recipe.entityType));
                ItemStack.STREAM_CODEC.encode(buf, recipe.dropStack);
            },
            buf -> new BugNetInteractionRecipe(
                    Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getValue(buf.readIdentifier()), "Unknown bug-net entity type"),
                    ItemStack.STREAM_CODEC.decode(buf)
            )
    );

    public static final RecipeSerializer<BugNetInteractionRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
