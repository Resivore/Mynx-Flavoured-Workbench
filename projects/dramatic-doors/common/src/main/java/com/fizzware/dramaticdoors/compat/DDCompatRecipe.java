package com.fizzware.dramaticdoors.compat;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;

public class DDCompatRecipe
{
	// The list of recipes that will be filled out.
	public static List<JsonObject> SHORT_DOOR_RECIPES = new ArrayList<JsonObject>();
	public static List<JsonObject> TALL_DOOR_RECIPES = new ArrayList<JsonObject>();
	
	public static JsonObject createShortDoorRecipe(String recipeID, Identifier baseDoor) {
		JsonObject json = createStonecutterRecipeJson(Identifier.fromNamespaceAndPath(baseDoor.getNamespace(), baseDoor.getPath()), Identifier.fromNamespaceAndPath("dramaticdoors", recipeID));
		SHORT_DOOR_RECIPES.add(json);
		return json;
	}
	
	public static void createShortDoorRecipe(String recipeID, Identifier baseDoor, boolean isWood) {
		JsonObject json;
		if (isWood) {
			if (Compats.isModLoaded("woodworks", Compats.modChecker, true) || Compats.isModLoaded("aurorasdeco", Compats.modChecker, true)) {
				//Woodworks
				if (Compats.WOODWORKS_INSTALLED) {
					json = createSawmillRecipeJson(Identifier.fromNamespaceAndPath(baseDoor.getNamespace(), baseDoor.getPath()), Identifier.fromNamespaceAndPath("dramaticdoors", recipeID), "woodworks:sawmill");
					SHORT_DOOR_RECIPES.add(json);
				}
				//Aurora's Decorations
				if (Compats.isModLoaded("aurorasdeco", Compats.modChecker)) {
					json = createSawmillRecipeJson(Identifier.fromNamespaceAndPath(baseDoor.getNamespace(), baseDoor.getPath()), Identifier.fromNamespaceAndPath("dramaticdoors", recipeID), "aurorasdeco:woodcutting");
					SHORT_DOOR_RECIPES.add(json);
				}
			}
			else {
				json = createStonecutterRecipeJson(Identifier.fromNamespaceAndPath(baseDoor.getNamespace(), baseDoor.getPath()), Identifier.fromNamespaceAndPath("dramaticdoors", recipeID));
				SHORT_DOOR_RECIPES.add(json);
			}
		}
		else {
			json = createStonecutterRecipeJson(Identifier.fromNamespaceAndPath(baseDoor.getNamespace(), baseDoor.getPath()), Identifier.fromNamespaceAndPath("dramaticdoors", recipeID));
			SHORT_DOOR_RECIPES.add(json);
		}
	}
	
	public static void createTallDoorRecipe(String recipeID, Identifier baseDoor, @Nullable String group) {
		JsonObject json = createShapedRecipeJson(Lists.newArrayList('#'), // The keys we are using for the input items/tags.
				Lists.newArrayList(Identifier.fromNamespaceAndPath(baseDoor.getNamespace(), baseDoor.getPath())), // The items/tags we are using as input.
				Lists.newArrayList("item"), // Whether the input we provided is a tag or an item.
				Lists.newArrayList("#", "#", "#"), // The crafting pattern.
				Identifier.fromNamespaceAndPath("dramaticdoors", recipeID), group // The crafting output
		);
		TALL_DOOR_RECIPES.add(json);
	}

    public static JsonObject createShapedRecipeJson(ArrayList<Character> keys, ArrayList<Identifier> items, ArrayList<String> type, ArrayList<String> pattern, Identifier output, @Nullable String group) {
        //Creating a new json object, where we will store our recipe.
        JsonObject json = new JsonObject();
        //The "type" of the recipe we are creating. In this case, a shaped recipe.
        json.addProperty("type", "minecraft:crafting_shaped");
        //This creates:
        //"type": "minecraft:crafting_shaped"
         if (group != null) {
        	json.addProperty("group", group);
        }
        //We create a new Json Element, and add our crafting pattern to it.
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(pattern.get(0));
        jsonArray.add(pattern.get(1));
        jsonArray.add(pattern.get(2));
        //Then we add the pattern to our json object.
        json.add("pattern", jsonArray);
 
        //Next we need to define what the keys in the pattern are.
        JsonObject keyList = new JsonObject(); //The main key object, containing all the keys
 
        for (int i = 0; i < keys.size(); ++i) {
            String ingredient = ("tag".equals(type.get(i)) ? "#" : "") + items.get(i);
            keyList.addProperty(keys.get(i) + "", ingredient);
        }
 
        json.add("key", keyList);
        //Finally, we define our result object
        JsonObject result = new JsonObject();
        result.addProperty("id", output.toString());
        result.addProperty("count", 2);
        json.add("result", result);
 
        return json;
    }
    
    public static JsonObject createStonecutterRecipeJson(Identifier input, Identifier output) {
        //Creating a new json object, where we will store our recipe.
        JsonObject json = new JsonObject();
        //The "type" of the recipe we are creating. In this case, a stonecutting recipe.
        json.addProperty("type", "minecraft:stonecutting");
        //Add ingredients that will be the input.
        json.addProperty("ingredient", input.toString());
        //Add output.
        JsonObject result = new JsonObject();
        result.addProperty("id", output.toString());
        result.addProperty("count", 2);
        json.add("result", result);
        return json;
    }
    
    public static JsonObject createSawmillRecipeJson(Identifier input, Identifier output, String recipeType) {
        //Creating a new json object, where we will store our recipe.
        JsonObject json = new JsonObject();
        //The "type" of the recipe we are creating. In this case, a sawmill recipe.
        json.addProperty("type", recipeType);
        //Add ingredients that will be the input.
        json.addProperty("ingredient", input.toString());
        //Add output.
        JsonObject result = new JsonObject();
        result.addProperty("id", output.toString());
        result.addProperty("count", 2);
        json.add("result", result);
        return json;
    }
}
