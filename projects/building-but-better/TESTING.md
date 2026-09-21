# Testing

The current `2.0pre4+26.2-enderscape-dev.7` candidate is `ACTIVE` with `CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`. Its exact local-only artifact is `bbb-fabric-26.2-2.0pre4+26.2-enderscape-dev.7.jar`, 1,363,575 bytes, SHA-256 `57ddb5dfe62f2eb9f4a2ce22fbeeb5cce4386bbd93aab3f7df0dd8e6d19ddaf0`, finalized at `2026-09-21T20:21:08.3914151Z` from source checkpoint `1e64edc51af2582197af6eb18dea3314965fc1c7`.

The exact accepted release remains `2.0pre4+26.2-pale-oak-dev.6`, filename `bbb-fabric-26.2-2.0pre4+26.2-pale-oak-dev.6.jar`, SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`. It is both the accepted baseline and rollback; dev.7 is not accepted. Both artifacts contain ARR-bound resource closure and remain ignored, locally retained, and non-redistributable.

## Observed controlled validation

Temurin Java 25, Gradle 9.5.1, and Fabric Loom 1.17.21 completed the following from this project directory with the exact accepted baseline and exact Enderscape provider inputs:

```powershell
$env:JAVA_HOME = '<java-25-home>'
.\gradlew.bat clean check build --stacktrace `
  -PbbbBaselineJar='<workbench>/projects/building-but-better/artifacts/bbb-fabric-26.2-2.0pre4+26.2-pale-oak-dev.6.jar' `
  -PenderscapeJar='<workbench>/originals/mods/enderscape-fabric-3.0.2+mc26.2.jar'
```

`BUILD SUCCESSFUL` included 31/31 focused JUnit tests, the production `LivingEntityRopeClimbMixin` application harness, exact resource verification, production-JAR inspection, and six required headless server GameTests using a synthetic provider fixture with the exact six Enderscape block IDs. Observed closure was:

- unchanged standalone registry: 171 blocks, 172 items, and 12 complete wood families;
- provider-present BBB registry: 204 blocks, 205 items, and 15 complete wood families;
- optional delta: exactly three families, 33 blocks, 33 block items, and eleven forms per family;
- 1,132 accepted-base plus 177 optional models, 244 accepted-base plus 42 optional PNGs, and 224 accepted-base plus 39 optional recipes;
- exact source-plank and stripped axial identities, stable BBB IDs, form classes/state properties, Hammer and waterlogging parity, lattice/wall/beam/slab/lantern specializations, fuel value 100, loaded recipes, loot, and tag parity;
- a 2,768-entry production JAR with exactly 409 optional-pack entries, no Enderscape classes, no raw `assets/enderscape` or `data/enderscape` namespace, no nested JAR, and no foreign class.

The build verified the size and SHA-256 of `enderscape-fabric-3.0.2+mc26.2.jar` (104,162,822 bytes, SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`) and its exact embedded license before inspecting registrations, models, and textures. The provider's real plank sheets are opaque 16-by-16 textures. Generated BBB sheets use only inspected provider-palette colors while retaining audited BBB geometry, alpha, and non-wood details; the Veiled and Murublight seven-rank mappings do not claim to reproduce all ten source colors or the provider's plank grain. Every generated model and texture reference resolved statically without a missing-resource fallback.

The separate provider-absent path was run with:

```powershell
.\gradlew.bat runGameTest --stacktrace -PbbbAbsentGameTest=true `
  -PbbbBaselineJar='<workbench>/projects/building-but-better/artifacts/bbb-fabric-26.2-2.0pre4+26.2-pale-oak-dev.6.jar' `
  -PenderscapeJar='<workbench>/originals/mods/enderscape-fabric-3.0.2+mc26.2.jar'
```

Its one required headless server GameTest passed. With no `enderscape` provider identity, BBB retained exactly 171 blocks/172 items/12 wood families, registered no optional IDs, and loaded none of the 39 optional recipes.

These GameTests are controlled exact-ID fixture evidence. They did not load the real 104 MB Enderscape runtime or its dependency stack, launch a client, render the generated resources, exercise a real save/reload cycle, or constitute owner-supplied gameplay validation. No Minecraft testing profile was inspected or changed.

## Required owner runtime matrix

Test the exact dev.7 filename and SHA-256 in an owner-controlled Minecraft 26.2 environment; Codex must not inspect or manipulate a testing profile.

1. With actual Enderscape `3.0.2+mc26.2` present, launch both client and server paths and confirm all 33 expected `bbb:veiled_*`, `bbb:celestial_*`, and `bbb:murublight_*` IDs register without registry, built-in-pack, model, texture, recipe, advancement, loot, or tag errors.
2. Place and inspect all eleven forms for each family. Confirm the real material appearance, no missing-resource fallback, correct item models, drops, recipes, creative-tab entries, and fuel behavior.
3. Exercise form-specific behavior against the accepted family equivalents: placement and rotation, beam X/Y/Z orientation, directional beam slabs and stairs, wall connectivity, lattice facing/plant/connectivity, support/pallet/frame/trim Hammer interactions, lantern luminance 15, collision, and dry/waterlogged states.
4. Launch the same dev.7 artifact without Enderscape. Confirm clean startup, the exact existing vanilla/Pale Oak/stone registry, absence of all 33 optional IDs and recipes, and no invalid optional resource load.
5. Recheck the accepted BBB behavior surface, especially Pale Oak and vertical rope pay-out/reel-in, and confirm Layers, Ladders, Chisel, the small-stone system, and other pruned content remain absent.

Record only observations actually made against the exact candidate identity. A defect is `RUNTIME_FAIL`; environment ambiguity is `INCONCLUSIVE`. Acceptance remains an explicit owner decision.
