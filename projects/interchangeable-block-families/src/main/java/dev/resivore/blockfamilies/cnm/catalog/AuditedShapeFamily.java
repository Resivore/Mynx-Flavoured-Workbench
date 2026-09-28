package dev.resivore.blockfamilies.cnm.catalog;

import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One exact, audited CNM ShapeMap equivalence family.
 *
 * <p>The canonical parent is deliberately the first member so that consumers
 * can preserve the audited presentation and recipe-parent order without
 * reconstructing it from registry names.</p>
 */
public record AuditedShapeFamily(
        Identifier key,
        Category category,
        Identifier canonicalParent,
        List<Identifier> members
) {
    public AuditedShapeFamily {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(canonicalParent, "canonicalParent");
        members = List.copyOf(Objects.requireNonNull(members, "members"));

        if (members.size() < 2) {
            throw new IllegalArgumentException("An audited ShapeMap family requires at least two members: " + key);
        }
        if (!canonicalParent.equals(members.getFirst())) {
            throw new IllegalArgumentException("Canonical parent must be the first member: " + key);
        }

        Set<Identifier> uniqueMembers = new HashSet<>();
        for (Identifier member : members) {
            Objects.requireNonNull(member, "member");
            if (!uniqueMembers.add(member)) {
                throw new IllegalArgumentException("Duplicate member in audited ShapeMap family " + key + ": " + member);
            }
        }
    }

    public enum Category {
        TWO_HIGH_DOOR("two_high_door"),
        THREE_HIGH_DOOR("three_high_door"),
        TRAPDOOR("trapdoor"),
        WINDOW("window"),
        DISPLAY_FIXTURE("display_fixture"),
        FENCE_GATE("fence_gate"),
        BAR_CHAIN("bar_chain"),
        BBB_DETAIL("bbb_detail"),
        BUILDING_ACCESSORY("building_accessory");

        private final String keySegment;

        Category(String keySegment) {
            this.keySegment = keySegment;
        }

        public String keySegment() {
            return keySegment;
        }
    }
}
