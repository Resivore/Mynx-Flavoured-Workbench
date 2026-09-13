package dev.resivore.villagerwork.mixin;

import dev.resivore.villagerwork.OwnedOutput;
import dev.resivore.villagerwork.WorkCoordinator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
abstract class VillagerMixin implements OwnedOutput {
    @Unique private final SimpleContainer villagerWork$output = new SimpleContainer(18);

    @Override public SimpleContainer villagerWork$ownedOutput() { return villagerWork$output; }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void villagerWork$tick(ServerLevel level, CallbackInfo ci) {
        WorkCoordinator.tick((Villager)(Object)this, level);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void villagerWork$clearPropBeforeSave(ValueOutput output, CallbackInfo ci) {
        WorkCoordinator.clearProp((Villager)(Object)this);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void villagerWork$saveOutput(ValueOutput output, CallbackInfo ci) {
        villagerWork$output.storeAsItemList(output.list("VillagerWorkOwnedOutput", ItemStack.CODEC));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void villagerWork$loadOutput(ValueInput input, CallbackInfo ci) {
        input.list("VillagerWorkOwnedOutput", ItemStack.CODEC).ifPresent(villagerWork$output::fromItemList);
        WorkCoordinator.clearProp((Villager)(Object)this);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void villagerWork$dropRealOutput(DamageSource source, CallbackInfo ci) {
        villagerWork$releaseRealOutput();
    }

    @Inject(method = "thunderHit", at = @At("HEAD"))
    private void villagerWork$lightningConversion(ServerLevel level, LightningBolt bolt, CallbackInfo ci) {
        if (level.getDifficulty() != Difficulty.PEACEFUL) villagerWork$releaseRealOutput();
    }

    @Unique private void villagerWork$releaseRealOutput() {
        Villager villager = (Villager)(Object)this;
        WorkCoordinator.clearProp(villager);
        if (villager.level() instanceof ServerLevel) {
            for (int i = 0; i < villagerWork$output.getContainerSize(); i++) {
                ItemStack stack = villagerWork$output.removeItemNoUpdate(i);
                if (!stack.isEmpty()) villager.drop(stack, false, true);
            }
        }
    }

    @Inject(method = "setVillagerData", at = @At("HEAD"))
    private void villagerWork$professionChanged(VillagerData data, CallbackInfo ci) {
        WorkCoordinator.clearProp((Villager)(Object)this);
    }
}
