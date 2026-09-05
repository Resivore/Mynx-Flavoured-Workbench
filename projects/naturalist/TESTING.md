# Testing

## Current gate

**PRIVATE CANARY 2 RETAINED — NOT DEPLOYED / RUNTIME UNTESTED**

- Version: `2.0.3+26.2-port-canary2`.
- Artifact: `artifacts/naturalist-2.0.3+26.2-port-canary2.jar`, 11,369,726 bytes.
- SHA-256: `8e4032ac0948bbe7c45ab48d047312b6a6bbc041102a137d4f3f21e79de1feff`.
- Source checkpoint (also embedded in the JAR): `da1c33d7eb1040ee8425d8b7a746dbcfc4352436`.
- Java 25 build, all 22 focused tests (14 existing and 8 startup regressions),
  all six preservation verifiers, and root/nested dependency-policy inspection pass.
  All existing JAR entries except MonsterMixin and the three intended metadata/
  configuration entries are byte-identical to Canary 1; only the gate class is added.

Both canonical Workbench slots were occupied at manager revision 96: A held
Container Slot Reservations Canary 9, and B held Quick Stack Nearby compatibility
Canary 9. No deployment or Minecraft launch was performed for this task. Keep
Naturalist ACTIVE until its UUID actually occupies a verified manager slot.
Do not displace another project or treat static checks as bootstrap evidence.

The September 5, 2026 user-reported **Matcha 26.2 Player Instance** launch used
Minecraft 26.2, Loader 0.19.3, Java 25, and Naturalist version
`2.0.3+26.2-port-canary1`. Its MonsterMixin callback lacked ServerLevel before
Player and failed bootstrap. Field Guide was absent; its missing-target warnings
were a separate optional-integration defect. The reported launch's exact JAR hash
is unknown and must not be inferred from its version or bound to the retained
Canary 1 hash. This was external historical evidence, not a dedicated Workbench test.

## Startup and sleep regression procedure

1. When a slot becomes available, use the serialized Test Instance Manager to
   deploy the exact retained Canary 2 into the dedicated **Matcha Flavoured 26.2
   Workbench**. Recheck current main, slot ownership, exact artifact identity,
   dependency resolution, and the normal readiness receipt before launching.
   Do not modify, deploy to, stop, or relaunch the Player Instance. Never access
   the protected 26.1.2 instance.
2. Prioritize **Field Guide absent**. Confirm the enabled mod inventory lacks
   `fieldguide`, launch the dedicated Workbench, and inspect that launch's log:
   no `MonsterMixin.onIsPreventingPlayerRest` descriptor/injection failure and
   no Naturalist-owned Field Guide missing targets (`EntryRenderHelper`,
   `FieldGuideEntryScreen`, `IconCacheManager`). Reach the title screen, then a
   disposable test world. A later failure is a separate blocker; record its exact
   log and stop without repairing unrelated mods.
3. In the test world, use a valid Overworld bed at night in Survival, with a
   nearby vanilla hostile monster whose normal rest predicate is true. Hold no
   plush bear and attempt sleep: the nearby-monster refusal must remain.
   Hold Naturalist's plush bear in the main hand and retry, then in the offhand:
   that monster must no longer prevent rest in either case. Keep other sleep
   conditions valid, reset nighttime between attempts, and remove the plush bear
   for a final vanilla-refusal control. Record each actual result independently.
4. Check server startup only in an authorized dedicated Workbench setup through
   the same manager/readiness boundaries. The gate must not load client or Field
   Guide classes on a server; static present/absent server gate tests already pass.
5. Check **Field Guide present** only when a compatible dependency and an
   authorized setup are available; do not install or bundle it merely to complete
   this matrix. Confirm all three existing client mixins remain enabled and check
   variants, entry-screen rendering, aquatic previews, and icon generation.
   This branch is currently runtime-untested despite passing gate fixtures.
6. Stop on startup failure, missing-target warnings, identity drift, occupied
   slots, or changed sleep behavior. Record only observations actually made.
   Successful bootstrap/sleep checks constitute focused partial evidence and do
   not imply full Naturalist acceptance; the broader matrix below remains required.

## Reproducible static checks

Use Temurin 25.0.4.1, Gradle 9.6.1, Loom 1.17.20, Loader 0.19.3, Fabric API
0.157.0+26.2, and optional compile surfaces LambDynamicLights 4.12.2+26.2 and
Field Guide 1.7.10-26.2-fabric. These are validation baselines. Packaged runtime
requirements are Minecraft 26.2, Loader >=0.19.3, Fabric API >=0.157.0, Java >=25;
LambDynamicLights remains optional and Field Guide is neither required nor bundled.

Run `clean build persistenceTest itemModelTest` from this project with the
existing `naturalistOriginalJar` and `naturalistLegacyMinecraftJar` properties.
`build` includes `startupTest`; its classpath excludes Field Guide. To reproduce
the retained source stamp after a later administrative commit, also pass
`-PnaturalistSourceCommit=da1c33d7eb1040ee8425d8b7a746dbcfc4352436` while using this exact source tree.
Run all six `tools/Verify-*.ps1` preservation verifiers after resource generation,
passing the external input paths where supported.

Keep immutable private inputs external: Naturalist 2.0.3 SHA-256
`3d16c975326e0df24486d44d8010d9e614fc9efdf891864de7b5a0efedfc12f9`;
Minecraft 1.21.1 client SHA-256
`499f6897d1837516680f3114072d8106e11c9adcd933fe5cf051b551089b0c99`.
The Field Guide configuration is patched only in ignored staging; every other
upstream mixin configuration setting and compatibility initializer is preserved.

## Preservation guards

- Keep upstream resource directories All Rights Reserved and external; retain
  only the existing license, MIT/Workbench-authored source, authored 26.2
  metadata/tag, build wiring, tests, and verifiers in Git.
- Keep the enabled Minecraft 26.2 access widener limited to its audited single
  `Entity.wasTouchingWater` accessibility entry unless new evidence requires a
  separately reviewed change; never reactivate the staged 1.21.1 widener.
- Preserve all 51 entity types, 48 synced variant registries and 99 definitions,
  legacy variant decoding, persistence and inventory/reference semantics,
  authored geometry, render layers, AI priorities, eggs, and dynamic item
  models. Do not use feature deletion or inert stubs to obtain a build.
- Keep the upstream drop policy seam inactive and preserve both loot-table and
  code-owned acquisition paths without duplicate emissions.
- Add no Matcha-specific spawn rule during the baseline port. Later runtime
  judgment must cover Pale Garden forest inheritance, Sulfur Caves snails, and
  the preserved Frozen River edge against the actually deployed Matcha version.

## Broader preservation acceptance matrix

Run this only after the exact retained Canary is deployed and the normal
readiness verifier reports **READY TO TEST**:

1. Launch both client and dedicated server; confirm registration and spawning
   for representative reptiles, mammals, birds, aquatic mobs, insects, and the
   three supporting entity types without optional-integration classloading
   failures.
2. Save and reload representative entities; verify namespaced and legacy
   variants, tame/anger/entity references, Ant carried food, Elephant's 27-slot
   inventory, Rat's compact inventory, and caught/bucket/Knapsack data.
3. Inspect adult/baby and variant rendering, authored geometry, animation,
   scales, layers, riders/held items, emissive and translucent effects, all
   dynamic item-model families, and every spawn egg's identity and two-color
   appearance for unintended model, texture, or resource drift.
4. Exercise representative AI and interactions: movement/climbing/swimming/
   flight, targeting and combat, breeding and egg laying, hatching, taming,
   saddling/equipment, capture/release, inventories, and species-specific goals.
5. Exercise blocks and items including ant hills, egg blocks, starfish,
   froglass, capture nets, buckets, Knapsack, and Elephant inventory. Place,
   populate, break, save, and reload snail shells while checking their state-era
   renderer, plant-only quad filtering, transforms, and break overlay.
6. Check upstream spawning tags and blacklists across representative vanilla
   and Matcha biomes, including Pale Garden, swamp/mangrove, savanna, jungle,
   Frozen River, ocean, and Sulfur Caves.
7. Verify ordinary loot tables plus code- and interaction-owned drops, capture
   conversions, inventories, and equipment returns without missing or duplicate
   emissions.
8. Repeat relevant client checks with the Workbench rendering stack. Verify
   LambDynamicLights both present and absent; verify Field Guide both present
   and absent, including aquatic previews; then check Inventory Extended and
   Trinkets coexistence where applicable.
