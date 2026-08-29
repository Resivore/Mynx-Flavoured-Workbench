package com.crispytwig.naturalist.compat.fieldguide;

import com.crispytwig.naturalist.server.entity.mob.*;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.world.entity.Mob;

import java.util.List;

public class FieldGuidePlugin {

    private static final List<Class<? extends Mob>> VARIANT_MOBS = List.of(
            Alligator.class, Anglerfish.class, Ant.class, Bass.class, Bear.class,
            Bird.class, BlackBear.class, Blobfish.class, Boar.class, Butterfly.class,
            Capybara.class, Caterpillar.class, Catfish.class, Clam.class, Crab.class, Deer.class,
            DesertScorpion.class, Dragonfly.class, Duck.class, Elephant.class, Firefly.class,
            GiantIsopod.class, Giraffe.class, GreatWhiteShark.class, Hedgehog.class, Hippo.class, Jellyfish.class,
            JungleScorpion.class, KomodoDragon.class, Lion.class, Lizard.class, Mammoth.class,
            Mole.class, Ostrich.class, Piranha.class, Rat.class, Ray.class,
            Rhino.class, Snail.class, Snake.class, Starfish.class, Tiger.class,
            Tortoise.class, Turkey.class, Vulture.class, Whale.class, Zebra.class
    );

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register() {
        NaturalistVariantProvider provider = new NaturalistVariantProvider();
        for (Class<? extends Mob> entityClass : VARIANT_MOBS) {
            FieldGuideVariantManager.registerProvider((Class) entityClass, provider);
        }
    }
}
