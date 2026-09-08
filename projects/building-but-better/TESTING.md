# Testing

The current `2.0pre4+26.2-pale-oak-dev.5` candidate is `ACTIVE` with `STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. It retains the C3 Pale Oak corrections and the BBB-owned, state-aware rope mechanic: only `bbb:rope[axis=y]` is climbable; a non-sneaking main-hand rope use pays out exactly one vertical segment from the bottom of the contiguous vertical column; an empty main hand reels in exactly that bottom segment. C5 corrects C4's invalid `LivingEntity` method shadows by accessing the inherited `Entity` methods through the typed target instance. Compilation, focused tests, generated-resource verification, production-Knot Mixin transformation, and the local JAR establish only source/build properties, not a Minecraft runtime pass.

The user externally launched a BBB file which identified itself as C4 (`2.0pre4+26.2-pale-oak-dev.4`) and observed a confirmed startup `FAIL`: `LivingEntityRopeClimbMixin` could not shadow `getInBlockState()` on `LivingEntity`. The external log did not establish that the loaded file had the canonical retained C4 SHA-256; it was not deployed by the Test Instance Manager and occupied no slot. No Minecraft client or server was launched for C5, no C5 artifact was deployed, no Test Slot was assigned, no Test Instance Manager transition was performed, and neither Minecraft profile was changed.

## Static verification

From this canonical project directory, Temurin Java `25.0.4.1+1`, Gradle `9.5.1`, and Fabric Loom `1.17.20` completed:

```powershell
$env:JAVA_HOME = 'C:\\Users\\resiv\\.gradle\\jdks\\eclipse_adoptium-25-amd64-windows.2'
.\\gradlew.bat clean check build --console=plain "-PbbbOriginalJar=C:\\Users\\resiv\\OneDrive\\Documents\\Minecraft 26.2 Workbench\\originals\\mods\\bbb-fabric-2.0pre4.jar"
```

The C5 run was `BUILD SUCCESSFUL` with 11 executed tasks and all 25 focused JUnit tests passing without failures, errors, or skips. The exact pristine input guard verified `bbb-fabric-2.0pre4.jar` at 1,701,505 bytes and SHA-256 `1e7ae114aaec53475133e11c607fc65dce493bba5897eaf0044d53959b508fc0`. Resource validation reported 171 blocks, 172 items, 1,132 models, 248 texture/sidecar files (244 PNGs, 112 transparent, binary alpha), 224 recipes, 140 advancements, 171 loot tables, and 44 tags. The rope contracts cover unchanged registry identity/counts and ChainBlock state/shape inheritance; Y-only climbability; bounded contiguous-column bottom resolution; one-segment vanilla placement with actual replaceability, world-border, permission, collision, water, and non-water-fluid guards; main-hand/no-fallthrough behavior; Creative versus Survival handling; water restoration; non-cascading ordinary breaking; and sneaking bypass. The new regression uses the resolved Minecraft 26.2 class hierarchy to prove `getInBlockState()` and `blockPosition()` belong to `Entity`, while `lastClimbablePos` and `onClimbable()` belong to `LivingEntity`; it rejects inherited-method shadows. The production harness loaded the exact official-namespace C5 JAR through Fabric Knot and successfully transformed `LivingEntity` with `LivingEntityRopeClimbMixin`. `bbb.mixins.json`, `fabric.mod.json`, the mixin class, and the official namespace manifest were inspected in the packaged JAR. The C4 `No refMap loaded` text is incidental for this direct Loom/Fabric configuration: C5 transforms successfully without adding manual refmap metadata. Static/source/build transformation evidence is not Minecraft runtime validation.

The local ARR-bearing C5 candidate is `bbb-fabric-26.2-2.0pre4+26.2-pale-oak-dev.5.jar`, 1,149,433 bytes, SHA-256 `b86c90226cb5082aba5329116633705f0bcd5141754ff50f509a5582b4eff7d4`. It is ignored, retained locally only, and not redistributed.

## Deferred C5 Minecraft rope matrix

This matrix has not been run. Before testing, obtain explicit Test Instance Manager ownership and perform one verified serialized deployment transition that assigns UUID `5d42f47f-b006-4125-840d-dec0d2728afa` to Test Slot A or B while preserving the other slot. Only that verified assignment may change lifecycle from `ACTIVE` to `TESTING`. Use the exact retained C5 JAR in the dedicated Matcha Flavoured 26.2 Workbench; never access the protected Matcha Flavoured 26.1.2 gameplay profile.

1. Launch/load the exact C5 candidate and confirm no registry, mixin, resource, recipe, advancement, or loot error, specifically confirming `LivingEntity` transforms without BBB Mixin errors.
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
- C5 does not add rope entities, block entities, persistent column ownership, global ticking, cascade breaking, or a globally climbable rope tag.
