package dev.resivore.matchajei.client;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class MatchaComponentInventory {
    private static final String ITEM_RESOURCE = "/matcha_jei_integration/component-item-ids.txt";
    private static final String COMPONENT_RESOURCE = "/matcha_jei_integration/identity-component-ids.txt";

    private MatchaComponentInventory() {
    }

    static Set<Identifier> itemIds() {
        return readIdentifiers(ITEM_RESOURCE);
    }

    static DataComponentType<?>[] identityComponentTypes() {
        List<DataComponentType<?>> componentTypes = readIdentifiers(COMPONENT_RESOURCE).stream()
                .map(BuiltInRegistries.DATA_COMPONENT_TYPE::getValue)
                .toList();
        if (componentTypes.stream().anyMatch(type -> type == null)) {
            throw new IllegalStateException("Unknown component type in " + COMPONENT_RESOURCE);
        }
        return componentTypes.toArray(DataComponentType<?>[]::new);
    }

    private static Set<Identifier> readIdentifiers(String resourcePath) {
        InputStream stream = MatchaComponentInventory.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Missing audited Matcha component inventory: " + resourcePath);
        }

        LinkedHashSet<Identifier> identifiers = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(Identifier::parse)
                    .forEach(identifiers::add);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + resourcePath, exception);
        }
        return Set.copyOf(identifiers);
    }
}
