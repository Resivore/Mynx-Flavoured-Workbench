package com.fizzware.dramaticdoors.blockentities;

import org.jetbrains.annotations.Nullable;

import com.fizzware.dramaticdoors.tags.DDItemTags;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TallNetheriteDoorBlockEntity extends BlockEntity
{
	
	public String password = null;
	
	public TallNetheriteDoorBlockEntity(BlockPos pos, BlockState state) {
		super(DDBlockEntities.TALL_NETHERITE_DOOR, pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput tag) {
		super.saveAdditional(tag);
        if (this.password != null) {
            tag.putString("Password", this.password);
        }
	}
	
	@Override
	protected void loadAdditional(ValueInput tag) {
		super.loadAdditional(tag);
		this.password = tag.getString("Password").orElse(null);
	}
	
    public void setPassword(String password) {
        this.password = password;
    }

    public void setPassword(ItemStack stack) {
        this.setPassword(stack.getHoverName().getString());
    }
    
    @Nullable
    public String getPassword() {
        return this.password;
    }

    public void clearOwner() {
        this.password = null;
    }

    public static boolean isCorrectKey(ItemStack key, String password) {
        return key.getHoverName().getString().equals(password);
    }

    public boolean isCorrectKey(ItemStack key) {
        return isCorrectKey(key, this.password);
    }

    public enum KeyStatus {
        CORRECT_KEY,
        INCORRECT_KEY,
        NO_KEY
    }

    public static KeyStatus hasKeyInInventory(Player player, String key) {
        if (key == null) return KeyStatus.CORRECT_KEY;
        KeyStatus found = KeyStatus.INCORRECT_KEY;
        Inventory inventory = player.getInventory();
        for (int idx = 0; idx < inventory.getContainerSize(); idx++) {
            ItemStack stack = inventory.getItem(idx);
            if (stack.is(DDItemTags.KEY)) {
                found = KeyStatus.INCORRECT_KEY;
                if (isCorrectKey(stack, key)) return KeyStatus.CORRECT_KEY;
            }
        }
        return found;
    }

    public static boolean doesPlayerHaveKeyToOpen(Player player, String lockPassword, boolean feedbackMessage, @Nullable String translName) {
        KeyStatus key = hasKeyInInventory(player, lockPassword);
        if (key == KeyStatus.INCORRECT_KEY) {
            if (feedbackMessage) {
                player.sendSystemMessage(Component.translatable("message.supplementaries.safe.incorrect_key"));
            }
            return false;
        } 
        else if (key == KeyStatus.CORRECT_KEY) {
        	return true;
        }
        if (feedbackMessage) {
            player.sendSystemMessage(Component.translatable("message.supplementaries." + translName + ".locked"));
        }
        return false;
    }


    //returns true if door has to open
    public boolean handleAction(Player player, InteractionHand handIn, String translName) {
        if (player.isSpectator()) return false;

        ItemStack stack = player.getItemInHand(handIn);

        boolean isKey = stack.is(DDItemTags.KEY);
        //clear ownership
        if (player.isCrouching() && isKey && (player.isCreative() || this.isCorrectKey(stack))) {
        	this.clearOwner();
            player.sendSystemMessage(Component.translatable("message.supplementaries.safe.cleared"));
            this.level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.5F);
            return false;
        }
        //set key
        else if (this.password == null) {
            if (isKey) {
            	this.setPassword(stack);
                player.sendSystemMessage(Component.translatable("message.supplementaries.safe.assigned_key", this.password));
                this.level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.5F);
                return false;
            }
            return true;
        }
        //open
        else return player.isCreative() || doesPlayerHaveKeyToOpen(player, this.password, true, translName);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
