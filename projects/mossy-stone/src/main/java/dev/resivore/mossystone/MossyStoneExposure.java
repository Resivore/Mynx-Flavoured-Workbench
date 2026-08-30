package dev.resivore.mossystone;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

final class MossyStoneExposure {
    private MossyStoneExposure() {
    }

    static <T> List<T> missing(Collection<T> owned, Collection<T> existing) {
        LinkedHashSet<T> missing = new LinkedHashSet<>(owned);
        missing.removeAll(existing);
        return List.copyOf(missing);
    }
}
