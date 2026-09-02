package com.yungnickyoung.minecraft.ribbits.module;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.data.RibbitData;
import com.yungnickyoung.minecraft.ribbits.data.RibbitProfession;
import com.yungnickyoung.minecraft.yungsapi.api.autoregister.AutoRegister;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@AutoRegister(RibbitsCommon.MOD_ID)
public class RibbitProfessionModule {
    private static final Identifier SHARED_TEXTURE = RibbitsCommon.id("textures/entity/ribbit.png");

    /* Registration of built-in RibbitProfessions. */
    public static final RibbitProfession NITWIT = sharedTextureProfession("nitwit", "nitwit_ribbit");
    public static final RibbitProfession GARDENER = sharedTextureProfession("gardener", "gardener_ribbit");
    public static final RibbitProfession SORCERER = sharedTextureProfession("sorcerer", "sorcerer_ribbit");
    public static final RibbitProfession FISHERMAN = sharedTextureProfession("fisherman", "fisherman_ribbit");
    public static final RibbitProfession MERCHANT = sharedTextureProfession("merchant", "merchant_ribbit");
    public static final RibbitProfession CHEF = privateTextureProfession("chef", "chef_ribbit");
    public static final RibbitProfession FARMER = privateTextureProfession("farmer", "farmer_ribbit");
    public static final RibbitProfession PROSPECTOR = privateTextureProfession("prospector", "prospector_ribbit");
    public static final RibbitProfession GUARD = privateTextureProfession("guard", "guard_ribbit");

    /** Complete, stable profession order. Save identity remains the profession {@link Identifier}. */
    public static final List<RibbitProfession> ALL_PROFESSIONS = List.of(
            NITWIT,
            GARDENER,
            SORCERER,
            FISHERMAN,
            MERCHANT,
            CHEF,
            FARMER,
            PROSPECTOR,
            GUARD
    );

    /**
     * Equal-weight natural Ribbit-village pool. Sorcerers intentionally remain registered but are
     * unavailable through village generation.
     */
    public static final List<RibbitProfession> VILLAGE_PROFESSIONS = List.of(
            NITWIT,
            GARDENER,
            FISHERMAN,
            MERCHANT,
            CHEF,
            FARMER,
            PROSPECTOR,
            GUARD
    );

    private static final Map<Identifier, RibbitProfession> PROFESSION_REGISTRY = createRegistry();

    private static RibbitProfession sharedTextureProfession(String name, String modelPath) {
        return new RibbitProfession(RibbitsCommon.id(name), RibbitsCommon.id(modelPath), SHARED_TEXTURE);
    }

    private static RibbitProfession privateTextureProfession(String name, String modelPath) {
        return new RibbitProfession(
                RibbitsCommon.id(name),
                RibbitsCommon.id(modelPath),
                RibbitsCommon.id("textures/entity/" + modelPath + ".png"));
    }

    private static Map<Identifier, RibbitProfession> createRegistry() {
        Map<Identifier, RibbitProfession> professions = new LinkedHashMap<>();
        for (RibbitProfession profession : ALL_PROFESSIONS) {
            RibbitProfession duplicate = professions.put(profession.id(), profession);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate Ribbit profession ID: " + profession.id());
            }
        }
        return Collections.unmodifiableMap(professions);
    }

    /**
     * Gets a RibbitProfession by its Identifier.
     *
     * @param id Identifier of the RibbitProfession to get.
     * @return RibbitProfession with the given Identifier, or the stable musician fallback if unknown.
     */
    public static RibbitProfession getProfession(Identifier id) {
        return PROFESSION_REGISTRY.getOrDefault(id, NITWIT);
    }

    /**
     * Gets an equal-weight profession from the explicit natural village pool using caller-owned
     * world/entity randomness.
     *
     * @param random world- or entity-provided random source
     * @return a village-eligible profession
     */
    public static RibbitProfession getRandomVillageProfession(RandomSource random) {
        Objects.requireNonNull(random, "random");
        return VILLAGE_PROFESSIONS.get(random.nextInt(VILLAGE_PROFESSIONS.size()));
    }

    public static boolean isMynxVisualProfession(RibbitProfession profession) {
        return profession == CHEF
                || profession == FARMER
                || profession == PROSPECTOR
                || profession == GUARD;
    }

    public static RibbitData createVillageRibbitData(RandomSource random) {
        RibbitProfession profession = getRandomVillageProfession(random);
        return new RibbitData(
                profession,
                RibbitUmbrellaTypeModule.getRandomUmbrellaType(random),
                profession == NITWIT ? RibbitInstrumentModule.BONGO : RibbitInstrumentModule.NONE);
    }

    public static RibbitData createTypedSpawnEggData(RibbitProfession profession, RandomSource random) {
        Objects.requireNonNull(profession, "profession");
        Objects.requireNonNull(random, "random");
        return new RibbitData(
                profession,
                RibbitUmbrellaTypeModule.getRandomUmbrellaType(random),
                RibbitInstrumentModule.NONE);
    }

    public static Map<Identifier, RibbitProfession> professionRegistry() {
        return PROFESSION_REGISTRY;
    }

    /**
     * The AutoRegister system will call this method after mod initialization is complete.
     * The method itself is a NO-OP, but calling it will trigger the static initialization above.
     */
    @AutoRegister("_ignored")
    public static void initRibbitsProfessions() {
        RibbitsCommon.LOGGER.info("Registering Ribbit professions...");
    }
}
