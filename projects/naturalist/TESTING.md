# Testing

## Current gate

**NOT READY TO TEST**

Naturalist is still compile-blocked. The migrated Java 25 `compileJava` task
reproduces the frozen checkpoint's 190 API errors, so no port JAR or Canary
exists and no Minecraft deployment or gameplay validation is authorized.

**Stop condition:** do not perform gameplay runtime testing until production
Java compiles successfully and a real Canary has been generated, retained, and
hash-recorded. Compilation or JAR generation alone would remain generated and
runtime-untested evidence.

## Current static evidence

- The 451 retained source/build/test/license files, blob identities, and modes
  match legacy checkpoint `63a8b46ac939d6bd48e21d4840c480361ad852ef`.
- Temurin 25.0.4.1 with Gradle 9.6.1 and Loom 1.17.20 reproduced exactly 190
  raw javac diagnostics before Gradle's repeated exception rendering.
- The external pristine Naturalist 2.0.3 JAR matched SHA-256
  `3d16c975326e0df24486d44d8010d9e614fc9efdf891864de7b5a0efedfc12f9`;
  its 2,097 protected files (9,946,791 bytes) staged only into ignored output.
- The nine persistence and five dynamic item-model tests passed, all six
  focused static verifiers passed, and 20 client-item roots were generated.
- No JAR, Canary, deployment, or runtime result was produced by these checks.

## Preservation guards

- Keep upstream resource directories All Rights Reserved and external; retain
  only the existing license, MIT/Workbench-authored source, authored 26.2
  metadata/tag, build wiring, tests, and verifiers in Git.
- Do not activate the staged 1.21.1 access widener. Audit each required access
  against Minecraft 26.2 before authoring and enabling a replacement.
- Preserve all 51 entity types, 48 synced variant registries and 99 definitions,
  legacy variant decoding, persistence and inventory/reference semantics,
  authored geometry, render layers, AI priorities, eggs, and dynamic item
  models. Do not use feature deletion or inert stubs to obtain a build.
- Keep the upstream drop policy seam inactive and preserve both loot-table and
  code-owned acquisition paths without duplicate emissions.
- Add no Matcha-specific spawn rule during the baseline port. Later runtime
  judgment must cover Pale Garden forest inheritance, Sulfur Caves snails, and
  the preserved Frozen River edge against the actually deployed Matcha version.

## Eventual runtime matrix

Run this only after the stop condition is cleared:

1. Launch both client and dedicated server; confirm registration and spawning
   for representative reptiles, mammals, birds, aquatic mobs, insects, and the
   three supporting entity types without optional-integration classloading
   failures.
2. Save and reload representative entities; verify namespaced and legacy
   variants, tame/anger/entity references, Ant carried food, Elephant's 27-slot
   inventory, Rat's compact inventory, and caught/bucket/Knapsack data.
3. Inspect adult/baby and variant rendering, authored geometry, animation,
   scales, layers, riders/held items, emissive and translucent effects, and all
   dynamic item-model families for unintended model, texture, or resource drift.
4. Exercise representative AI and interactions: movement/climbing/swimming/
   flight, targeting and combat, breeding and egg laying, hatching, taming,
   saddling/equipment, capture/release, inventories, and species-specific goals.
5. Exercise blocks and items including ant hills, egg blocks, snail shells,
   starfish, froglass, capture nets, buckets, Knapsack, and Elephant inventory.
6. Check upstream spawning tags and blacklists across representative vanilla
   and Matcha biomes, including Pale Garden, swamp/mangrove, savanna, jungle,
   Frozen River, ocean, and Sulfur Caves.
7. Verify ordinary loot tables plus code- and interaction-owned drops, capture
   conversions, inventories, and equipment returns without missing or duplicate
   emissions.
8. Repeat relevant client checks with the Workbench rendering stack and verify
   LambDynamicLights and Field Guide behavior both present and absent, plus
   Inventory Extended and Trinkets coexistence where applicable.
