# Testing

The current `2.0pre4+26.2-pale-oak-dev.4` candidate is `ACTIVE` with `STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. It retains the C3 Pale Oak corrections and adds the BBB-owned, state-aware rope mechanic: only `bbb:rope[axis=y]` is climbable; a non-sneaking main-hand rope use pays out exactly one vertical segment from the bottom of the contiguous vertical column; an empty main hand reels in exactly that bottom segment. Compilation, focused tests, generated-resource verification, and the local JAR establish only source/build properties, not Minecraft runtime behavior.

No Minecraft client or server was launched for C4, no artifact was deployed, no Test Slot was assigned, no Test Instance Manager transition was performed, and neither Minecraft profile was changed. C4 remains runtime-untested and is not accepted.

## Static verification

From this canonical project directory, Temurin Java `25.0.4.1+1`, Gradle `9.5.1`, and Fabric Loom `1.17.20` completed:

```powershell
$env:JAVA_HOME = 'C:\\Users\\resiv\\.gradle\\jdks\\eclipse_adoptium-25-amd64-windows.2'
.\\gradlew.bat clean check build --console=plain "-PbbbOriginalJar=C:\\Users\\resiv\\OneDrive\\Documents\\Minecraft 26.2 Workbench\\originals\\mods\\bbb-fabric-2.0pre4.jar"
```

The C4 run was `BUILD SUCCESSFUL` with 9 executed tasks and all 24 focused JUnit tests passing without failures, errors, or skips. The exact pristine input guard verified `bbb-fabric-2.0pre4.jar` at 1,701,505 bytes and SHA-256 `1e7ae114aaec53475133e11c607fc65dce493bba5897eaf0044d53959b508fc0`. Resource validation reported 171 blocks, 172 items, 1,132 models, 248 texture/sidecar files (244 PNGs, 112 transparent, binary alpha), 224 recipes, 140 advancements, 171 loot tables, and 44 tags. The rope contracts cover unchanged registry identity/counts and ChainBlock state/shape inheritance; Y-only climbability; bounded contiguous-column bottom resolution; one-segment vanilla placement with actual replaceability, world-border, permission, collision, water, and non-water-fluid guards; main-hand/no-fallthrough behavior; Creative versus Survival handling; water restoration; non-cascading ordinary breaking; and sneaking bypass. Static source/build evidence is not runtime validation.

The local ARR-bearing C4 candidate is `bbb-fabric-26.2-2.0pre4+26.2-pale-oak-dev.4.jar`, 1,149,410 bytes, SHA-256 `b67eeb0f4da800e6af46813989efea5170ee48c5b912b4ef7d74d66e052c0786`. It is ignored, retained locally only, and not redistributed.

## Deferred C4 Minecraft rope matrix

This matrix has not been run. Before testing, obtain explicit Test Instance Manager ownership and perform one verified serialized deployment transition that assigns UUID `5d42f47f-b006-4125-840d-dec0d2728afa` to Test Slot A or B while preserving the other slot. Only that verified assignment may change lifecycle from `ACTIVE` to `TESTING`. Use the exact retained C4 JAR in the dedicated Matcha Flavoured 26.2 Workbench; never access the protected Matcha Flavoured 26.1.2 gameplay profile.

1. Launch/load the exact C4 candidate and confirm no registry, mixin, resource, recipe, advancement, or loot error.
2. Climb dry vertical rope and waterlogged vertical rope; verify X- and Z-axis rope are not climbable.
3. From the top, middle, and bottom of a multi-block vertical column, pay out once and confirm exactly one segment is added at the common bottom on each use.
4. Verify Survival consumes exactly one rope and Creative consumes none; verify air and representative replaceable foliage succeed using normal placement semantics.
5. Extend into water and repeatedly through water, verifying every new segment is vertical and waterlogged without draining the column.
6. Verify a full block, a representative non-replaceable non-full block, lava, and another unsupported fluid block extension; confirm no item loss or accidental side placement when blocked.
7. With an empty main hand, reel in from top, middle, and bottom; verify only the bottom segment is removed, Survival returns one rope (dropping it safely if inventory is full), and Creative creates no item.
8. Reel in a waterlogged bottom segment and verify water is restored at its position.
9. Break a middle rope normally; confirm only that block breaks, its normal drop behavior remains, and the two split contiguous sections operate independently afterward.
10. Sneak-use rope against rope to confirm normal axis-aware ChainBlock placement remains available; verify offhand processing cannot double-trigger pay-out or reel-in.
11. Save/reload representative vertical, horizontal, and waterlogged rope states; confirm no unrelated Hammer or BBB behavior changed, including the C3 Pale Oak fixes.

Stop and record `RUNTIME_FAIL` for a confirmed BBB defect, or `INCONCLUSIVE` for environment/dependency ambiguity. Do not promote from static/build evidence or partially observed runtime checks.

## Current exclusions

- BBB Layers, Ladders (including Pale Oak), small-stone blocks, Chisel, and other removed upstream content remain excluded.
- No Inventory Bridge Framework or Clutter No More source change is included.
- C4 does not add rope entities, block entities, persistent column ownership, global ticking, cascade breaking, or a globally climbable rope tag.
