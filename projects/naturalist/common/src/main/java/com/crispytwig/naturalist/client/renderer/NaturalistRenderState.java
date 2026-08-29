package com.crispytwig.naturalist.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * Shared 26.2 render-state bridge for Naturalist's entity-backed animation
 * models. Values used by the common renderer are extracted before submission;
 * the entity reference keeps the upstream model animation implementations
 * intact while the wider port is still source-only.
 */
@Environment(EnvType.CLIENT)
public class NaturalistRenderState<E extends Entity> extends LivingEntityRenderState {
    public E entity;
    public float partialTick;
    public Identifier texture;
}
