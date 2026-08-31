# Testing

The current `2.0pre4+26.2-port-dev` candidate is `ACTIVE` with `STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. Compilation, focused tests, generated-resource verification, and a local JAR establish only source/build properties; they do not establish Minecraft runtime behavior.

No Minecraft client or server was launched, no artifact was deployed, no Test Slot was assigned, no Test Instance Manager transition was performed, and neither Minecraft profile was changed for revision 1.

## Static verification

The migration verification used Temurin Java `25.0.4.1+1`, the imported Gradle `9.5.1` wrapper, and the retained Fabric Loom selector `1.17-SNAPSHOT` (the build reported Fabric Loom `1.17.20`). From this canonical project directory, the exact successful command was:

```powershell
$env:JAVA_HOME = 'C:\Users\resiv\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat clean check build --console=plain "-PbbbOriginalJar=C:\Users\resiv\OneDrive\Documents\Minecraft 26.2 Workbench\originals\mods\bbb-fabric-2.0pre4.jar"
```

The command completed `BUILD SUCCESSFUL` in 31 seconds with 9 actionable tasks (8 executed, 1 up-to-date), Java compilation, and all 17 focused JUnit tests passing with no failures, errors, or skips. It established:

1. the exact pristine input guard for `bbb-fabric-2.0pre4.jar`, 1,701,505 bytes, SHA-256 `1E7AE114AAEC53475133E11C607FC65DCE493BBA5897EAF0044D53959B508FC0`, before opening the JAR for staging;
2. the 160-block/161-item curated registry formula and its exact disjoint 87-block/82-item removal set;
3. no removed BBB Layer/Ladder registration or Hammer implementation and no restored Chisel or related removed tool branch;
4. the explicit eleven-entry `BeamFamily` metadata contract with state-derived axis access and no registry-name parsing;
5. the exact retained Hammer-capable class set and state-property, directional-slab, waterlogging, frame, brazier, and tooltip contracts;
6. 160 blockstates, 161 Minecraft 26.2 item definitions, 160 block loot tables, 211 recipes, and 131 migrated recipe-unlock advancements under the singular 26.2 paths;
7. the audited model, texture, tag, language, recipe, advancement, and loot-table closure with no direct removed or unknown `bbb:` ID;
8. 230 staged PNGs, exactly 106 containing transparency, binary alpha only, and no unsupported retained `render_type` metadata;
9. the narrow Dark Oak lattice generated-reference correction and retained beam-axis/slab-state contracts;
10. ARR resources staged only under ignored `build/generated/bbb-resources/`, with no tracked ARR asset/data copy.

The alpha and closure checks are static evidence; they do not replace the deferred in-game transparency/culling checks. The frozen legacy build records `bbb-fabric-26.2-2.0pre4+26.2-port-dev.jar`, 1,093,037 bytes, SHA-256 `98B9BFF4B9F905841511E87D61DDA820E86E500ECFAE3FA7831F816521B3996D`, with 17/17 tests passing. The current run produced the same filename at 1,093,051 bytes and SHA-256 `96B67E7F2910BF4083993C5A35711E97421B03E990FF41EC294FD953E723473A`; it did not reproduce the frozen ZIP identity. Both JAR identities remain local, ignored, unretained build output because the combined JAR stages upstream ARR resources.

## Deferred in-game matrix

This matrix is for a later explicitly authorized runtime canary. It has **not** been run.

Before testing, obtain explicit Test Instance Manager ownership and perform one verified, serialized deployment transition that assigns UUID `5d42f47f-b006-4125-840d-dec0d2728afa` to Test Slot A or B while preserving the other slot. Only that verified slot assignment may change lifecycle from `ACTIVE` to `TESTING`. Rebuild or select the exact local candidate, record its actual size and SHA-256, keep its ARR-bearing JAR local, and use Minecraft 26.2 with Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, and Java 25 in a disposable test world.

1. Launch Minecraft 26.2 Fabric with the exact BBB candidate and confirm registry/data loading completes without missing models, mixins, tags, recipes, advancements, or loot-table errors.
2. Confirm the BBB creative tab contains exactly 160 block items plus the Hammer, with no layer, ladder, small-stone-block, Chisel, `bbb`, Tall Oak Door, Copper Gateway, Roofing, or other excluded entry.
3. For each of the eleven retained wood families (Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Crimson, Warped, Mangrove, Bamboo, and Cherry), place and break every retained form; verify item/block model, texture, drops, recipe, rotation, waterlogging, connectivity, light, collision, and sound where applicable.
4. For each of the seven retained stone families (Stone, Blackstone, Deepslate, Nether Brick, Sandstone, Red Sandstone, and Quartz), place and break every retained form; verify models, textures, drops, recipes, rotation, waterlogging, connection geometry, and collision.
5. Inspect representative transparent/cutout faces and adjoining blocks, including lattices and other alpha-bearing models, for correct transparency, culling, section-layer choice, and absence of missing or opaque textures.
6. Verify brazier and soul brazier lighting/extinguishing behavior, rope's unchanged stock baseline, and iron-fence connections and collision.
7. Verify Hammer use on balustrades: upper/lower hit regions toggle their corresponding section, while sneak-use toggles tilt.
8. Verify Hammer use on wood and stone frames cycles the center stick, while an ordinary attack/punch with any held item resets it without mining the block.
9. Verify Hammer hit selection for all four column quarter-shell bands on both vertical and horizontal axes.
10. Verify both pallet board layers, the support brace, lantern hanging state, moulding dentil state, and stone-fence side-fill/pillar rules, including rotation, connectivity, collision, waterlogging, drops, and reload persistence where applicable.
11. Confirm every retained Hammer action consumes zero durability and no removed Layer/Ladder/Chisel or other excluded branch is reachable.
12. Confirm rope, braziers, iron fence, lattices, trims, beams, beam slabs, beam stairs, wooden walls, and urns gain no unintended Hammer action.
13. Verify lattice plant placement/removal and urn dirt, plant, drop, and shovel behavior independently of the Hammer; save/reload representative stateful blocks and recheck them.
14. Inspect recipe unlocking, recipe-book entries, crafting output, and mined drops for broad representatives plus edge families (Bamboo, Cherry, Crimson, Warped, Quartz, and Red Sandstone) to catch incomplete recipe, advancement, or resource closure.
15. With Inventory Bridge Framework and Clutter No More unchanged, perform observation-only compatibility checks if those mods are present. Missing downstream beam handling is outside BBB scope and must not trigger an unrequested compatibility implementation.

Stop and record `RUNTIME_FAIL` for a confirmed BBB defect, or `INCONCLUSIVE` for an environment/dependency ambiguity, on any registry/data-load failure, missing or excluded creative entry, wrong state transition, model/texture/culling defect, wrong rotation/waterlogging/connectivity/collision/drop/recipe behavior, Hammer misrouting or durability loss, state loss after reload, crash, or relevant error. Do not promote from build/static evidence or from a partially observed matrix.

## Known scope exclusions

- No Pale Oak family in this baseline.
- No BBB layers, ladders, small stone blocks, Chisel, or their Hammer behavior.
- No rope enhancement beyond the stock BBB baseline.
- No Inventory Bridge Framework or Clutter No More source change.
- No dedicated-server claim.
