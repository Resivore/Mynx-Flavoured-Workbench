package com.crispytwig.naturalist.lifecycle;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Test-only preceding reload hook, shaped like Matcha heart's immutable HEAD replacement. */
public final class AdvancementReloadFixture {
    public static UnaryOperator<Map<Identifier, Advancement>> replacement = UnaryOperator.identity();
    private AdvancementReloadFixture() { }
}
