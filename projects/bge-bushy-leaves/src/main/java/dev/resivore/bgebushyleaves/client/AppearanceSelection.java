package dev.resivore.bgebushyleaves.client;

import java.util.List;

/** Provider-neutral choice: retain every non-cull decorative sample, otherwise use normal leaf art. */
final class AppearanceSelection {
    private AppearanceSelection() {}

    record Candidate<T>(T value, boolean decorative) {}

    static <T> List<T> preferDecorative(List<Candidate<T>> candidates) {
        List<T> decorative = candidates.stream().filter(Candidate::decorative)
                .map(Candidate::value).toList();
        return decorative.isEmpty() ? candidates.stream().map(Candidate::value).toList() : decorative;
    }
}
