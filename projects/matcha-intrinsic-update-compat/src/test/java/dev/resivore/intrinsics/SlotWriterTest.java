package dev.resivore.intrinsics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.permissions.PermissionSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SlotWriterTest {
    @Test
    void allSixWritersCompileAgainst26_2AndOnlyWriteTheCallingSlot() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var context = Commands.createValidationContext(VanillaRegistries.createLookup());
        var dispatcher = new Commands(Commands.CommandSelection.ALL, context).getDispatcher();
        var source = Commands.createCompilationContext(PermissionSet.ALL_PERMISSIONS);
        var held = new CompoundTag();
        held.putInt("minecraft:efficiency", 5);
        held.putInt("minecraft:mending", 1);
        var arguments = new CompoundTag();
        arguments.put("held", held);
        for (String slot : List.of("mainhand", "offhand", "feet", "chest", "head", "legs")) {
            String group = slot.endsWith("hand") ? "weapon." : "armor.";
            var lines = Files.readAllLines(Path.of("src/main/resources/data/matcha_item/function/enchants/" + slot + ".mcfunction"));
            assertTrue(lines.getFirst().startsWith("$item modify entity @s " + group + slot + " "));
            assertEquals("function matcha_intrinsic_update_compat:clear_enchantment_scratch", lines.getLast());
            var function = CommandFunction.fromLines(Identifier.parse("matcha_item:enchants/" + slot), dispatcher, source, lines);
            assertNotNull(function.instantiate(arguments, dispatcher));
        }
    }
}
