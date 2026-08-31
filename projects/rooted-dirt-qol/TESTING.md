# Testing

Exact Canary 1 is current and accepted in the physically verified Stack v6 baseline. Retained `artifacts/rooted-dirt-qol-0.1.0-canary1.jar`, 3,299 bytes, SHA-256 `3642f55e0cef611f2dd56478b8aaf2491fd81d370023a04ca71e6852b37307ee`, from release source `46e66c9c9bb5701153edc017a969b24e1176dffc` is `STATIC_PASS / READY_TO_TEST_VERIFIED / RUNTIME_PASS`. The user reported an aggregate pass for this exact candidate; no individual matrix row is inferred.

Exact predecessor `craft-rooted-dirt-107.1.jar`, 72,444 bytes, SHA-256 `a5fc656fc345b416fc01281146d568de664c587e2c168e6bfd69f3ded7e42845`, source `b71932fb4c917edfa2c162600b01a4c1f2983dfe`, is retained manager-owned as `craft-rooted-dirt-107.1.jar.disabled` and is the distinct rollback. It must never be active together with Rooted Dirt QoL.

The migration verification clean-built the unchanged source with Java 25 and Gradle 9.5.1, passed all 7 focused tests in 3 suites, and reproduced the retained JAR exactly.

## Future regression procedure

Before rerunning this matrix, confirm the exact accepted C1 identity is active and `craft-rooted-dirt-107.1.jar` / `mr_craft_rooteddirt` remains disabled. Any successor requires its own artifact identity and independent static and runtime evidence.

1. Confirm Rooted Dirt QoL Canary 1 loads and the old Craftable Rooted Dirt advancement tab is absent.
2. Shapelessly craft one Dirt plus one Hanging Roots; confirm exactly one Rooted Dirt is produced.
3. Place Hanging Roots and bonemeal it; confirm one bonemeal is consumed, the placed block remains, and exactly one Hanging Roots item drops.
4. Repeat with dispenser-applied bonemeal; confirm the same duplication and consumption behavior.
5. Bonemeal Rooted Dirt with free space below; confirm vanilla still places Hanging Roots below it.
6. Check logs for errors tied to the recipe, mixin, bonemeal, or dispenser paths.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if both predecessor and candidate are present, the game fails to launch, the recipe count differs, the placed Hanging Roots changes, the dropped count differs, bonemeal is not consumed, dispenser behavior diverges, or vanilla Rooted Dirt growth regresses. Do not replace the accepted identity or infer a new aggregate pass from only a subset.
