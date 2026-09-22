package dev.resivore.villagerwork;

import java.util.Objects;
import net.minecraft.core.BlockPos;

/** Requires stable physical arrival followed by a later tick that visibly faces the receiver. */
public final class BarrelArrivalGate {
    private BlockPos receiver;
    private int firstArrivedTick = -1;

    public boolean ready(BlockPos selectedReceiver, int currentTick,
                         boolean physicallyArrived, boolean facingReceiver) {
        if (!Objects.equals(receiver, selectedReceiver)) {
            receiver = selectedReceiver.immutable();
            firstArrivedTick = -1;
        }
        if (!physicallyArrived) {
            firstArrivedTick = -1;
            return false;
        }
        if (firstArrivedTick < 0) {
            firstArrivedTick = currentTick;
            return false;
        }
        return currentTick > firstArrivedTick && facingReceiver;
    }

    public void clear() {
        receiver = null;
        firstArrivedTick = -1;
    }
}
