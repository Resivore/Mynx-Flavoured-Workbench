package com.fizzware.dramaticdoors.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.fizzware.dramaticdoors.entity.ai.goal.OpenShortDoorsTask;
import com.fizzware.dramaticdoors.entity.ai.goal.OpenTallDoorsTask;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Set;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.schedule.Activity;

@Mixin(Piglin.class)
public class PiglinBrainMixin
{

	@Inject(method = "makeBrain(Lnet/minecraft/world/entity/ai/Brain$Packed;)Lnet/minecraft/world/entity/ai/Brain;", at = @At("RETURN"))
	private void injectCreate(Brain.Packed packed, CallbackInfoReturnable<Brain<Piglin>> cir) {
		initDramaticDoorActivities(cir.getReturnValue());
	}
	
    private static void initDramaticDoorActivities(Brain<Piglin> brain) {
        brain.addActivity(Activity.CORE, ImmutableList.of(
                Pair.of(1, OpenTallDoorsTask.<Piglin>create()),
                Pair.of(1, OpenShortDoorsTask.<Piglin>create())), Set.of(), Set.of());
    }
}
