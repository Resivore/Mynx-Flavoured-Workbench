package dev.resivore.xaeroemfcompat;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Map;
import java.util.Set;

/**
 * C10's complete eligibility boundary.  This class deliberately contains no
 * prefixes, namespace wildcards, or model-name inference: an unlisted entity
 * cannot enter the successor policy and therefore retains the C9 path.
 */
public final class IconTargetPolicy {
    public enum Composition {
        NONE, FOX, GOAT, FROG, BOGGED, WITCH, VILLAGER
    }

    public record Selection(Composition composition, String profession, String expectedHat) {
        static final Selection NONE = new Selection(Composition.NONE, null, null);
        boolean isCompositionTarget() { return composition != Composition.NONE; }
    }

    private static final Map<String, String> VILLAGER_HATS = Map.of(
            "mynx_flora_trades:florist", "ribbits_gardener_hat",
            "minecraft:farmer", "ribbits_farmer_hat",
            "minecraft:butcher", "ribbits_chef_hat",
            "minecraft:cleric", "ribbits_sorcerer_hat",
            "minecraft:mason", "ribbits_prospector_hat");
    private static final Map<String, Composition> ENTITY_COMPOSITIONS = Map.of(
            "minecraft:fox", Composition.FOX,
            "minecraft:goat", Composition.GOAT,
            "minecraft:frog", Composition.FROG,
            "minecraft:bogged", Composition.BOGGED,
            "minecraft:witch", Composition.WITCH);
    private static final Map<String, Float> PRESENTATION_SCALES = Map.of(
            "minecraft:ghast", 1.50F,
            "minecraft:happy_ghast", 1.50F,
            "minecraft:bee", 0.75F,
            "minecraft:rabbit", 0.75F);

    private IconTargetPolicy() { }

    public static Selection select(String entityId, String professionId) {
        if ("minecraft:villager".equals(entityId)) {
            String hat = VILLAGER_HATS.get(professionId);
            return hat == null ? Selection.NONE : new Selection(Composition.VILLAGER, professionId, hat);
        }
        Composition composition = ENTITY_COMPOSITIONS.get(entityId);
        return composition == null ? Selection.NONE : new Selection(composition, null, null);
    }

    public static Selection select(Entity entity) {
        if (entity == null) return Selection.NONE;
        String entityId = EntityType.getKey(entity.getType()).toString();
        if (!(entity instanceof Villager villager)) return select(entityId, null);
        String profession = villager.getVillagerData().profession().unwrapKey()
                .map(key -> key.identifier().toString()).orElse(null);
        return select(entityId, profession);
    }

    public static float presentationScale(Entity entity) {
        if (entity == null) return 1.0F;
        return presentationScale(EntityType.getKey(entity.getType()).toString());
    }

    public static float presentationScale(String entityId) {
        return PRESENTATION_SCALES.getOrDefault(entityId, 1.0F);
    }

    static Set<String> targetedCompositionIds() { return ENTITY_COMPOSITIONS.keySet(); }
    static Set<String> targetedVillagerProfessions() { return VILLAGER_HATS.keySet(); }
}
