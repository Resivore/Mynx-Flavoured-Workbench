package dev.resivore.ribbitsxaeroicons;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Literal universal selector: exactly top-level main/direct body, then every direct body cube. */
public final class RibbitHeadSelector {
    private RibbitHeadSelector() {
    }

    public static <C> Optional<Selection<C>> select(
            List<? extends BoneView<C>> topLevelBones) {
        if (topLevelBones == null) {
            return Optional.empty();
        }

        List<BoneView<C>> mains = matching(topLevelBones, "main");
        if (mains.size() != 1 || mains.getFirst().parent() != null) {
            return Optional.empty();
        }

        BoneView<C> main = mains.getFirst();
        List<? extends BoneView<C>> mainChildren = main.children();
        if (mainChildren == null) {
            return Optional.empty();
        }
        List<BoneView<C>> bodies = matching(mainChildren, "body");
        if (bodies.size() != 1) {
            return Optional.empty();
        }

        BoneView<C> body = bodies.getFirst();
        if (!Objects.equals(body.parent(), main)) {
            return Optional.empty();
        }
        List<C> cubes = body.directCubes();
        if (cubes == null || cubes.isEmpty() || cubes.stream().anyMatch(Objects::isNull)) {
            return Optional.empty();
        }

        return Optional.of(new Selection<>(main, body, List.copyOf(cubes)));
    }

    private static <C> List<BoneView<C>> matching(
            List<? extends BoneView<C>> bones, String exactName) {
        List<BoneView<C>> matches = new ArrayList<>();
        for (BoneView<C> bone : bones) {
            if (bone == null) {
                return List.of();
            }
            if (exactName.equals(bone.name())) {
                matches.add(bone);
            }
        }
        return matches;
    }

    public interface BoneView<C> {
        String name();

        BoneView<C> parent();

        List<? extends BoneView<C>> children();

        List<C> directCubes();
    }

    public record Selection<C>(BoneView<C> main, BoneView<C> body, List<C> cubes) {
        public Selection {
            Objects.requireNonNull(main, "main");
            Objects.requireNonNull(body, "body");
            cubes = List.copyOf(cubes);
        }
    }
}
