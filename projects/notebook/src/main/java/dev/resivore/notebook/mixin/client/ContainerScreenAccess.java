package dev.resivore.notebook.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only bounds needed to align the inventory utility button. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
    @Accessor("leftPos")
    int notebook$getLeftPos();

    @Accessor("topPos")
    int notebook$getTopPos();

    @Accessor("imageWidth")
    int notebook$getImageWidth();
}
