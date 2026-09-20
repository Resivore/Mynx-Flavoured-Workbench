package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Declares the provider state which remains material state after BGE supplies a different shape.
 *
 * <p>This is deliberately an allowlist.  Copying every property from an external parent would
 * confuse source geometry (for example AXIS) with the shape contract of the derived block.  A
 * bridge therefore names only the provider state that its companion block implementation knows
 * how to preserve, and validates both endpoints before the provider family is published.</p>
 */
public enum ExternalMaterialStateBridge {
    NONE(null, Set.of()),
    ENDERSCAPE_NEBULITE("enderscape:nebulite_block", Set.of()),
    ENDERSCAPE_ALLURING_MAGNIA("enderscape:alluring_magnia", Set.of("power")),
    ENDERSCAPE_REPULSIVE_MAGNIA("enderscape:repulsive_magnia", Set.of("power")),
    ENDERSCAPE_BLISTERED_MAGNIA("enderscape:blistered_magnia", Set.of("polarity")),
    ENDERSCAPE_BLINKLAMP("enderscape:blinklamp", Set.of("luminance"));

    private static final Map<Identifier, ExternalMaterialStateBridge> BY_SOURCE = Map.ofEntries(
            Map.entry(Identifier.parse("enderscape:nebulite_block"), ENDERSCAPE_NEBULITE),
            Map.entry(Identifier.parse("enderscape:alluring_magnia"), ENDERSCAPE_ALLURING_MAGNIA),
            Map.entry(Identifier.parse("enderscape:repulsive_magnia"), ENDERSCAPE_REPULSIVE_MAGNIA),
            Map.entry(Identifier.parse("enderscape:blistered_magnia"), ENDERSCAPE_BLISTERED_MAGNIA),
            Map.entry(Identifier.parse("enderscape:blinklamp"), ENDERSCAPE_BLINKLAMP));

    private final Identifier sourceId;
    private final Set<String> materialProperties;

    ExternalMaterialStateBridge(String sourceId, Set<String> materialProperties) {
        this.sourceId = sourceId == null ? null : Identifier.parse(sourceId);
        this.materialProperties = Set.copyOf(materialProperties);
    }

    public static ExternalMaterialStateBridge forSource(Identifier sourceId) {
        return BY_SOURCE.getOrDefault(sourceId, NONE);
    }

    public static ExternalMaterialStateBridge forProfile(NibaruMaterialProfile profile) {
        return forSource(profile.canonicalParentId());
    }

    public boolean requiresBridge() { return this != NONE; }

    public boolean isMagnia() {
        return this == ENDERSCAPE_NEBULITE || this == ENDERSCAPE_ALLURING_MAGNIA
                || this == ENDERSCAPE_REPULSIVE_MAGNIA || this == ENDERSCAPE_BLISTERED_MAGNIA;
    }

    public boolean isFixedMagnia() {
        return this == ENDERSCAPE_ALLURING_MAGNIA || this == ENDERSCAPE_REPULSIVE_MAGNIA;
    }

    public boolean isBlisteredMagnia() { return this == ENDERSCAPE_BLISTERED_MAGNIA; }

    public boolean isBlinklamp() { return this == ENDERSCAPE_BLINKLAMP; }

    public Set<String> materialProperties() { return materialProperties; }

    public void validateSource(Block source) {
        if (!requiresBridge()) return;
        Set<String> actual = source.getStateDefinition().getProperties().stream()
                .map(property -> property.getName()).collect(java.util.stream.Collectors.toSet());
        if (!actual.containsAll(materialProperties)) {
            throw new IllegalStateException("External material bridge " + name() + " requires "
                    + materialProperties + " on " + source + " but provider supplied " + actual);
        }
    }

    public void validateDerived(Identifier source, Map<String, Block> roles) {
        if (!requiresBridge()) return;
        for (Map.Entry<String, Block> entry : roles.entrySet()) {
            Set<String> actual = entry.getValue().getStateDefinition().getProperties().stream()
                    .map(property -> property.getName()).collect(java.util.stream.Collectors.toSet());
            if (!actual.containsAll(materialProperties)) {
                throw new IllegalStateException("External material bridge " + name() + " lost "
                        + materialProperties + " while deriving " + source + " role " + entry.getKey()
                        + "; state definition is " + actual);
            }
        }
    }
}
