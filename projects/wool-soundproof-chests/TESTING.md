# Testing

C1, historically named Alpha 1, is the current, accepted, and retained rollback release. Its embedded version is `0.1.0-alpha1`; the exact accepted artifact is `wool-soundproof-chests-0.1.0-alpha1.jar` with SHA-256 `e8f0d10dff823e97a4f4ab39029bf7a71b13501f2294544c55e38f7e1ca9d020`.

The frozen release was first built and deployment-verified without runtime evidence. The same unchanged bytes subsequently received the focused user runtime pass below and were accepted for the intended vanilla Ancient City scope. This migration performs no Minecraft deployment or new runtime validation.

## Preserved focused runtime evidence

1. A chest with a full floor and three wool sides suppressed both open and close vibration: **PASS**.
2. A chest with a full floor, one ordinary full-block wall, and two wool sides suppressed both events: **PASS**.
3. Four sealed faces with only one wool block remained detectable: **PASS**.
4. Three sealed faces with three wool blocks remained detectable: **PASS**.
5. All five non-top faces sealed with no wool remained detectable: **PASS**.
6. A slab, stair, fence, pane, or carpet did not count as a full seal: **PASS**. A representative modded full cube was **NOT TESTED / WAIVED** for the accepted scope.
7. Qualified and unqualified ordinary chests behaved correctly for both open and close events: **PASS**.
8. Walking, block place/break, projectile, and non-chest-container vibrations stayed vanilla: **PASS**.
9. Ordinary chest sound, lid animation, and inventory opening stayed vanilla: **PASS**.
10. A full block above the chest is **NOT TESTABLE / NOT REQUIRED** because vanilla prevents opening it; the top face is deliberately never evaluated.

Hoppers, comparators, multiple viewers, double-chest permutations, trapped chests, barrels, ender chests, shulker boxes, and dedicated-server behavior remain untested, deferred, and non-blocking. Do not infer exhaustive validation from the focused pass.

## Representative future recheck

Use the exact retained JAR with an ordinary `minecraft:chest` and an observable sculk listener:

1. Confirm the two qualifying arrangements above suppress both `CONTAINER_OPEN` and `CONTAINER_CLOSE`.
2. Confirm each insufficient-seal or insufficient-wool arrangement above leaves both events detectable.
3. Replace a qualifying full seal with a representative partial collision shape and confirm it no longer counts.
4. Add or remove a top block without otherwise changing the enclosure and confirm the predicate result is unchanged; account for vanilla's blocked-lid behavior.
5. Confirm unrelated vibrations, non-chest containers, chest sound, lid animation, opener behavior, and inventory access remain vanilla.
6. If double chests enter the test scope, evaluate each emitting half independently; complete silence is expected only when both halves independently satisfy the same 4-of-5 and two-wool rule.

Stop and record a failure or inconclusive result if the exact artifact or embedded identity differs, the required mixin fails to apply to all three call sites, a qualifying chest leaks an open/close vibration, a nonqualifying chest is suppressed, top or partial-shape behavior drifts, any non-chest or unrelated vibration is suppressed, or ordinary chest behavior regresses. Do not broaden acceptance claims beyond the cases actually observed.
