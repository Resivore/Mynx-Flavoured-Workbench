package dev.resivore.pickupaudiodiag;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticArchitectureTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void onlyTheVanillaPickupIdentifierQualifiesForPerSoundLogging() {
        assertTrue(PickupAudioDiag.isPickupIdentifier(Identifier.fromNamespaceAndPath("minecraft", "entity.item.pickup")));
        assertFalse(PickupAudioDiag.isPickupIdentifier(Identifier.fromNamespaceAndPath("minecraft", "entity.item.throw")));
        assertFalse(PickupAudioDiag.isPickupIdentifier(Identifier.fromNamespaceAndPath("minecraft", "block.chest.open")));
    }

    @Test
    void mixinsAreObservationOnlyAndNeverConsumeCcarState() throws IOException {
        String sources = readMainSources();

        assertFalse(sources.contains("@ModifyArgs"));
        assertFalse(sources.contains("@ModifyArg"));
        assertFalse(sources.contains("@ModifyVariable"));
        assertFalse(sources.contains("@Redirect"));
        assertFalse(sources.contains("@Overwrite"));
        assertFalse(sources.contains("cancellable = true"));
        assertFalse(sources.contains("setReturnValue"));
        assertFalse(sources.contains("Args"));
        assertFalse(sources.contains("RoutedPickupSoundState"));
        assertFalse(sources.contains("carriedrouting"));
        assertFalse(sources.contains("ClientPlayNetworking"));
        assertFalse(sources.contains("ServerPlayNetworking"));
        assertFalse(sources.contains("getSoundManager().play"));
        assertTrue(sources.contains("ENGINE_SURVIVED_HEAD"));
        assertTrue(sources.contains("SoundInstance;resolve"));
    }

    @Test
    void metadataIsClientOnlyAndHasNoOptionalDiagnosticTargetDependency() throws IOException {
        String metadata = Files.readString(ROOT.resolve("src/main/resources/fabric.mod.json"));

        assertTrue(metadata.contains("\"environment\": \"client\""));
        assertFalse(metadata.contains("carried_container_auto_routing\""));
        assertFalse(metadata.contains("sound_physics"));
        assertFalse(metadata.contains("inventory_extended"));
        assertTrue(metadata.contains("CAPABILITY_OR_PROVIDER"));
    }

    private static String readMainSources() throws IOException {
        try (var files = Files.walk(ROOT.resolve("src/main/java"))) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .map(DiagnosticArchitectureTest::readUnchecked)
                    .reduce("", String::concat);
        }
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
