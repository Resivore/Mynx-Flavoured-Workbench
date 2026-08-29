package com.fizzware.dramaticdoors.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;

public class DDCreativeTabs 
{
	public static final ResourceKey<CreativeModeTab> MAIN_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("dramaticdoors", "main_tab"));
	public static final ResourceKey<CreativeModeTab> CHIPPED_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("dramaticdoors", "chipped_tab"));
	public static final ResourceKey<CreativeModeTab> MACAW_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("dramaticdoors", "macaw_tab"));
	public static final ResourceKey<CreativeModeTab> MANYIDEAS_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("dramaticdoors", "manyideas_tab"));
}
