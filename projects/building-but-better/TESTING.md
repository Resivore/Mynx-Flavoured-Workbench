# Testing

The current `2.0pre4+26.2-pale-oak-dev.2` candidate is `ACTIVE` with `STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. It adds the port-native eleven-form Pale Oak family. Compilation, focused tests, generated-resource verification, and a local JAR establish only source/build properties; they do not establish Minecraft runtime behavior.

No Minecraft client or server was launched for this successor, no artifact was deployed, no Test Slot was assigned, no Test Instance Manager transition was performed, and neither Minecraft profile was changed. The user reported that the prior port “looks good”, but current canonical BBB records identify no exact retained/deployed candidate and `runtime-state.json` has no BBB deployment record; that report therefore cannot safely be bound to a release result.

## Static verification

The Pale Oak verification used Temurin Java `25.0.4.1+1`, the imported Gradle `9.5.1` wrapper, and the retained Fabric Loom selector `1.17-SNAPSHOT` (the build reported Fabric Loom `1.17.20`). From this canonical project directory, the exact successful command was:

```powershell
$env:JAVA_HOME = 'C:\Users\resiv\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat clean check build --console=plain "-PbbbOriginalJar=C:\Users\resiv\OneDrive\Documents\Minecraft 26.2 Workbench\originals\mods\bbb-fabric-2.0pre4.jar"
```

Two clean runs of the command completed `BUILD SUCCESSFUL` with 9 executed tasks, Java compilation, and all 19 focused JUnit tests passing with no failures, errors, or skips. They established:

1. the exact pristine input guard for `bbb-fabric-2.0pre4.jar`, 1,701,505 bytes, SHA-256 `1E7AE114AAEC53475133E11C607FC65DCE493BBA5897EAF0044D53959B508FC0`, before opening the JAR for staging;
2. the 171-block/172-item curated registry formula: the historical 160-block/161-item curated baseline plus the explicit eleven-form Pale Oak extension, while retaining the exact disjoint historical 87-block/82-item removal set;
3. no removed BBB Layer/Ladder registration or Hammer implementation and no restored Chisel or related removed tool branch;
4. the explicit twelve-entry `BeamFamily` metadata contract with state-derived axis access and no registry-name parsing; Pale Oak binds `Blocks.PALE_OAK_PLANKS` and `Blocks.STRIPPED_PALE_OAK_LOG` explicitly;
5. the exact retained Hammer-capable class set and state-property, directional-slab, waterlogging, frame, brazier, and tooltip contracts;
6. 171 blockstates, 172 Minecraft 26.2 item definitions, 171 block loot tables, 224 recipes, and 140 migrated recipe-unlock advancements under the singular 26.2 paths;
7. the audited model, texture, tag, language, recipe, advancement, and loot-table closure with no direct removed or unknown `bbb:` ID;
8. 1,133 generated models and 246 texture/sidecar files, including 242 PNGs (exactly 112 with transparency), binary alpha only, resolved Minecraft/BBB model and texture references, and no unsupported retained `render_type` metadata;
9. twelve Pale Oak-specific authored sheets generated from the actual Minecraft 26.2 Pale Oak plank palette, with original dimensions/UV contracts, transparent pixels, and material-independent lantern detail preserved;
10. the narrow Dark Oak lattice generated-reference correction and retained beam-axis/slab-state contracts;
11. ARR resources staged only under ignored `build/generated/bbb-resources/`, with no tracked ARR asset/data copy.

The alpha and closure checks are static evidence; they do not replace the deferred in-game transparency/culling checks. Both clean builds produced the byte-identical local ARR-bearing candidate `bbb-fabric-26.2-2.0pre4+26.2-pale-oak-dev.2.jar`, 1,144,473 bytes, SHA-256 `8C591183CD820E557189AE0A0F3A9EFF8B2519CD6E29C4C167242DD2CF204DDE`. It remains ignored, local-only, and undistributed.

## Deferred in-game matrix

This matrix is for a later explicitly authorized runtime canary. It has **not** been run.

Before testing, obtain explicit Test Instance Manager ownership and perform one verified, serialized deployment transition that assigns UUID `5d42f47f-b006-4125-840d-dec0d2728afa` to Test Slot A or B while preserving the other slot. Only that verified slot assignment may change lifecycle from `ACTIVE` to `TESTING`. Rebuild or select the exact local candidate, record its actual size and SHA-256, keep its ARR-bearing JAR local, and use Minecraft 26.2 with Fabric Loader 0.19.3, Fabric API 0.156.0+26.2, and Java 25 in a disposable test world.

1. Launch Minecraft 26.2 Fabric with the exact BBB candidate and confirm registry/data loading completes without missing models, mixins, tags, recipes, advancements, or loot-table errors.
2. Confirm the BBB creative tab contains exactly 171 block items plus the Hammer, with no layer, ladder, small-stone-block, Chisel, `bbb`, Tall Oak Door, Copper Gateway, Roofing, or other excluded entry.
3. For each of the twelve retained wood families (Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Crimson, Warped, Mangrove, Bamboo, Cherry, and Pale Oak), place and break every retained form; verify item/block model, texture, drops, recipe, rotation, waterlogging, connectivity, light, collision, and sound where applicable.
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

## Pale Oak successor matrix

1. Launch Minecraft 26.2 Fabric and confirm registry/data loading succeeds.
2. Confirm the ten non-wall Pale Oak forms appear wherever equivalent existing BBB forms appear.
3. Confirm Pale Oak Wall follows exactly the current treatment of the other BBB wood walls and has not acquired a distinct gate or policy.
4. Confirm no Pale Oak Layer or Ladder exists.
5. Inspect Pale Oak Balustrade appearance and all Hammer states.
6. Inspect Pale Oak Lattice texture, transparency, connectivity, waterlogging, plant placement/removal, berries, and shears behavior.
7. Inspect Pale Oak Beam on X, Y, and Z axes.
8. Confirm Beam side grain and end grain remain correctly oriented on every axis.
9. Inspect Pale Oak Beam Stairs placement, stair shape, rotations, texture orientation, waterlogging if applicable, collision, drops, and crafting.
10. Inspect Pale Oak Beam Slab for all six facings, bottom/top/double states, texture orientation, waterlogging, collision, drops, and crafting.
11. Inspect Pale Oak Support placement and Hammer state.
12. Inspect Pale Oak Pallet placement and every pallet/Hammer state.
13. Inspect Pale Oak Frame center cycling, ordinary-attack reset, waterlogging, targeting, collision, and save/reload persistence.
14. Inspect Pale Oak Lantern standing/hanging appearance, lighting, Hammer state, held-item/use behavior, collision/push reaction, and save/reload persistence.
15. Inspect Pale Oak Trim connections and orientation in every relevant direction.
16. Confirm crafting recipes and recipe-book unlocks for broad Pale Oak representatives.
17. Confirm mined drops are correct.
18. Confirm Pale Oak members participate in the same semantic tags as their Cherry equivalents, including vanilla stair/slab/wall tags.
19. Save/reload a representative set of stateful Pale Oak blocks and verify state persistence.
20. Inspect for missing models, purple/black textures, wrong UVs, transparency/culling defects, incorrect wood color, incorrect end-grain texture, or relevant log errors.

Record a confirmed Pale Oak/BBB defect as `RUNTIME_FAIL`, and environment/dependency ambiguity as `INCONCLUSIVE`. Do not promote from a partial matrix.

## Known scope exclusions

- Pale Oak includes exactly balustrade, lattice, wall, beam, beam stairs, beam slab, support, pallet, frame, lantern, and trim. Pale Oak Layers and Ladders remain excluded, as do all other BBB layers, ladders, small stone blocks, Chisel, and their Hammer behavior.
- No rope enhancement beyond the stock BBB baseline.
- No Inventory Bridge Framework or Clutter No More source change.
- No dedicated-server claim.
