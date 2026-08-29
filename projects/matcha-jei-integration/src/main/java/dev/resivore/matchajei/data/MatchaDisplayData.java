package dev.resivore.matchajei.data;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class MatchaDisplayData {
    private MatchaDisplayData() {
    }

    public record Trade(
            Identifier sourceId,
            String displayKey,
            String profession,
            int level,
            ItemStack firstInput,
            ItemStack secondInput,
            ItemStack output,
            boolean conditional
    ) {
        public Trade {
            Objects.requireNonNull(sourceId, "sourceId");
            Objects.requireNonNull(displayKey, "displayKey");
            Objects.requireNonNull(profession, "profession");
            firstInput = firstInput.copy();
            secondInput = secondInput.copy();
            output = output.copy();
            if (firstInput.isEmpty() || output.isEmpty()) {
                throw new IllegalArgumentException("Trade displays require a first input and output");
            }
        }
    }

    public record Acquisition(
            Identifier sourceId,
            String displayKey,
            String description,
            ItemStack output
    ) {
        public Acquisition {
            Objects.requireNonNull(sourceId, "sourceId");
            Objects.requireNonNull(displayKey, "displayKey");
            Objects.requireNonNull(description, "description");
            output = output.copy();
            if (output.isEmpty()) {
                throw new IllegalArgumentException("Acquisition displays require an output");
            }
        }
    }
}
