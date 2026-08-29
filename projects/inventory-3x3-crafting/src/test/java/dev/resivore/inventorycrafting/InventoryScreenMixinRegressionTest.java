package dev.resivore.inventorycrafting;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryScreenMixinRegressionTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final String RECIPE_BOOK_SCREEN =
            "net/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen";

    @Test
    void inheritedScreenCoordinatesUseTheExactTargetSuperclassWithoutShadows() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/resivore/inventorycrafting/mixin/client/InventoryScreenMixin.java"
        ));

        assertTrue(source.contains("extends AbstractRecipeBookScreen<InventoryMenu>"));
        assertTrue(source.contains("super(menu, recipeBookComponent, inventory, title);"));
        assertFalse(source.contains("@Shadow"),
                "InventoryScreen does not declare the inherited leftPos/topPos fields");
        assertTrue(source.contains("this.leftPos + InventoryCraftingLayout.RECIPE_BOOK_X"));
        assertTrue(source.contains("this.topPos + InventoryCraftingLayout.RECIPE_BOOK_Y"));

        ClassNode target = readClass("net/minecraft/client/gui/screens/inventory/InventoryScreen.class");
        ClassNode mixin = readClass(
                "dev/resivore/inventorycrafting/mixin/client/InventoryScreenMixin.class"
        );
        assertEquals(RECIPE_BOOK_SCREEN, target.superName);
        assertEquals(target.superName, mixin.superName,
                "the mixin must inherit InventoryScreen's exact direct superclass");
        assertFalse(mixin.fields.stream().anyMatch(field ->
                        field.name.equals("leftPos") || field.name.equals("topPos")),
                "the mixin must not redeclare inherited screen-coordinate fields");
    }

    private static ClassNode readClass(String resource) throws IOException {
        try (InputStream stream = Objects.requireNonNull(
                InventoryScreenMixinRegressionTest.class.getClassLoader().getResourceAsStream(resource),
                resource
        )) {
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }
}
