# Testing

Canary 1 is the current candidate and remains `NOT_DEPLOYED` and `RUNTIME_UNTESTED`. The migration verification clean-built the unchanged source with Java 25 and Gradle 9.5.1, passed all 7 focused tests in 3 suites, and reproduced the retained 3,299-byte JAR at SHA-256 `3642f55e0cef611f2dd56478b8aaf2491fd81d370023a04ca71e6852b37307ee`. Those results are build/static evidence, not Minecraft runtime evidence.

## Runtime handoff

Run this matrix only after a Test Slot owner prepares a mutually exclusive deployment of the exact retained `rooted-dirt-qol-0.1.0-canary1.jar` and confirms `craft-rooted-dirt-107.1.jar` / `mr_craft_rooteddirt` is absent.

1. Confirm Rooted Dirt QoL Canary 1 loads and the old Craftable Rooted Dirt advancement tab is absent.
2. Shapelessly craft one Dirt plus one Hanging Roots; confirm exactly one Rooted Dirt is produced.
3. Place Hanging Roots and bonemeal it; confirm one bonemeal is consumed, the placed block remains, and exactly one Hanging Roots item drops.
4. Repeat with dispenser-applied bonemeal; confirm the same duplication and consumption behavior.
5. Bonemeal Rooted Dirt with free space below; confirm vanilla still places Hanging Roots below it.
6. Check logs for errors tied to the recipe, mixin, bonemeal, or dispenser paths.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if both predecessor and candidate are present, the game fails to launch, the recipe count differs, the placed Hanging Roots changes, the dropped count differs, bonemeal is not consumed, dispenser behavior diverges, or vanilla Rooted Dirt growth regresses. Do not promote Canary 1 without a controlled pass of the applicable matrix.
