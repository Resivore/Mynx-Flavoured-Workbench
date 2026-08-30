# Testing

## Current gate

**PRIVATE CANARY RETAINED — NOT DEPLOYED / RUNTIME UNTESTED**

Naturalist now compiles cleanly on Java 25. Exact private Canary 1 is retained
from implementation commit `2d239bcbbec10b4a15716924549bf3147c440465`, but
it has not been deployed to the dedicated Workbench or exercised in Minecraft.
It therefore remains generated/static-pass evidence rather than a runtime pass.

The next task is to deploy this exact retained binary through the Test Instance
Manager, run the normal focused readiness verifier, and execute the focused
runtime matrix below. Do not rebuild or substitute the artifact during that
handoff.

## Current static evidence

- Temurin 25.0.4.1, Gradle 9.6.1, and Loom 1.17.20 reduced the frozen baseline
  from exactly 190 raw javac diagnostics to a clean production compile.
- `clean build persistenceTest itemModelTest` passed with 16 actionable tasks
  (15 executed, one up-to-date); all 14 focused tests passed with no failures,
  errors, or skips, and all six static preservation verifiers passed.
- The external pristine Naturalist 2.0.3 JAR matched SHA-256
  `3d16c975326e0df24486d44d8010d9e614fc9efdf891864de7b5a0efedfc12f9`;
  its 2,097 protected files (9,946,791 bytes) staged only into ignored output.
- The external Minecraft 1.21.1 client JAR was 26,836,906 bytes with SHA-256
  `499f6897d1837516680f3114072d8106e11c9adcd933fe5cf051b551089b0c99`.
  Exactly the legacy spawn-egg model template and two texture layers were
  entry-hash verified and staged privately; no protected resource was tracked.
- The 26.2 client-item generator produced 67 roots, including all 47 spawn eggs
  with their two upstream color tuples in primary/secondary order. The enabled
  26.2 access widener contains exactly one audited field-access entry.
- Retained artifact: `naturalist-2.0.3+26.2-port-canary1.jar`, 11,368,203 bytes,
  SHA-256 `b5f136f042f28617b9fb130d8d298e70f8538824c5c03dcdbf53fec266d5c7ac`.
  JAR inspection confirmed version metadata, both mixin configs, the one-entry
  access widener, all three legacy template assets, 67 roots, and 47 egg roots.
- No Minecraft profile, runtime slot, deployment state, or gameplay result was
  touched or inferred from this static validation.

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

## Focused runtime matrix

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
