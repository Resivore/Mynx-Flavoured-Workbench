package com.fizzware.dramaticdoors.mixin;

import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fizzware.dramaticdoors.entity.ai.goal.DDVillagerTasks;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;

@Mixin(Villager.class)
public class VillagerMixin
{
	@Inject(method = "registerBrainGoals(Lnet/minecraft/world/entity/ai/Brain;)V", at = @At("HEAD"))
	private void injectInitBrain(Brain<Villager> brain, CallbackInfo ci) {
		VillagerProfession villagerProfession = ((Villager)(Object)this).getVillagerData().profession().value();
		brain.addActivity(Activity.CORE, ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>builder()
				.addAll(DDVillagerTasks.createTallDoorTasks(villagerProfession, 0.5f))
				.addAll(DDVillagerTasks.createShortDoorTasks(villagerProfession, 0.5f))
				.build(), Set.of(), Set.of());
	}
}
