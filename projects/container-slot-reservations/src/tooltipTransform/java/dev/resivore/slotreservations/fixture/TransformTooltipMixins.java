package dev.resivore.slotreservations.fixture;

import java.nio.file.*;
import java.util.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

public final class TransformTooltipMixins {
    public static void main(String[] args) throws Exception {
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        var ctor = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();
        ctor.setAccessible(true);
        var transformer = (IMixinTransformer) ctor.newInstance();
        com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
        Mixins.addConfiguration("container_slot_reservations.client.mixins.json");
        var root = Path.of(args[0]);
        var tweaker = net.fabricmc.classtweaker.api.ClassTweaker.newInstance();
        var reader = net.fabricmc.classtweaker.api.ClassTweakerReader.create(tweaker);
        try (var input = TransformTooltipMixins.class.getClassLoader().getResourceAsStream("iteminteractions.classtweaker")) { reader.read(input.readAllBytes()); }
        try (var zip = new java.util.zip.ZipFile(System.getProperty("puzzlesLibJar"))) { reader.read(zip.getInputStream(zip.getEntry("puzzleslib.classtweaker")).readAllBytes()); }
        var mixinTargets = List.of(
                "net.minecraft.client.gui.Gui", "net.minecraft.client.gui.GuiGraphicsExtractor",
                "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
                "fuzs.iteminteractions.common.api.v2.world.inventory.tooltip.ItemContentsTooltip",
                "fuzs.iteminteractions.common.api.v2.client.gui.screens.inventory.tooltip.ClientItemContentsTooltip",
                "fuzs.iteminteractions.common.api.v2.world.item.storage.ContainerStorage",
                "fuzs.iteminteractions.common.api.v2.world.item.storage.ItemStorageHolder");
        var targets = new LinkedHashSet<>(mixinTargets);
        tweaker.getTargets().forEach(name -> targets.add(name.replace('/','.')));
        for (String name : targets) {
            String entry = name.replace('.', '/') + ".class";
            byte[] before;
            try (var input = TransformTooltipMixins.class.getClassLoader().getResourceAsStream(entry)) { before = Objects.requireNonNull(input, entry).readAllBytes(); }
            byte[] after = transformer.transformClassBytes(name, name, before);
            if (mixinTargets.contains(name) && Arrays.equals(before, after) && !(args.length > 1 && name.equals("net.minecraft.client.gui.Gui")))
                throw new AssertionError("Mixin did not apply: " + name);
            var writer = new org.objectweb.asm.ClassWriter(0);
            new org.objectweb.asm.ClassReader(after).accept(tweaker.createClassVisitor(org.objectweb.asm.Opcodes.ASM9, writer, null),0);
            after = writer.toByteArray();
            Path output = root.resolve(entry); Files.createDirectories(output.getParent()); Files.write(output, after);
            if (mixinTargets.contains(name)) System.out.println("APPLIED " + name);
        }
    }
}
