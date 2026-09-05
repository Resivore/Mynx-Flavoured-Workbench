# Testing

## Current gate

**PRIVATE CANARY 4 RETAINED — NOT DEPLOYED / RUNTIME UNTESTED**

- Version: `2.0.3+26.2-port-canary4`.
- Artifact: `artifacts/naturalist-2.0.3+26.2-port-canary4.jar`, 11,369,270 bytes.
- SHA-256: `fc7a214b59fa1ef9ebc4ffbb8a603867a12276742c56278d705123b0ea0cd922`.
- Source checkpoint (also embedded in the JAR): `d577b5bb4c93484cea6be0871fa1297372662a0a`.
- Java 25 clean build, 255 tests, all six preservation verifiers, full private
  resource comparison, sound resolution and root/nested dependency policy pass.
- Compared with Canary 3, only five render mixin classes, the two relocated
  helpers and nested Source record, 100 recipe JSON files, three advancements,
  dragonfly loot, sounds.json and two release-metadata entries change.
  Glow Goop, MonsterMixin, optional Field Guide gating, other protected resources,
  67 client-item roots and all 47 spawn-egg roots are unchanged.

Both canonical Workbench slots remain occupied at manager revision 97: A holds
Container Slot Reservations Canary 9 and B holds Quick Stack Nearby compatibility
Canary 9. No deployment, profile access or Minecraft launch was performed for
Canary 4. Keep Naturalist ACTIVE until its UUID occupies a verified manager slot.

The new user-reported September 5, 2026 **Matcha 26.2 Player Instance** observation
is an **external runtime FAIL for Canary 3**, superseding its earlier untested
description. The launch definitely loaded Minecraft 26.2, Loader 0.19.3 and
Naturalist `2.0.3+26.2-port-canary3`. It crashed through EntityRenderDispatcher
with IllegalClassLoadError because `mixin.NaturalistRenderEntityLookup` was an
ordinary runtime class in a declared Mixin package. The report also identified
Naturalist ingredient and entity-subpredicate parsing failures and missing vanilla
sound paths. The supplied version alone does not bind the launched filename,
SHA-256 or source checkpoint; no exact-hash runtime claim is inferred.

The preserved local Canary 3 remains `naturalist-2.0.3+26.2-port-canary3.jar`,
11,369,742 bytes, SHA-256
`5aabc54ae858c77d90870baa3bcbfbee6448f67cecf489a448085b04c15d8d75`, source
`038de70b6579cd66e43426539468cd684bb46d4a`. It is provenance, not the current
runtime candidate. Canary 1 and Canary 2 also remain locally preserved.

## Focused executable regressions

`startupTest` scans both declared Mixin packages for ordinary classes, verifies
public helper call targets and the actual dispatcher extraction descriptor/RETURN
hook, and checks that baked-rider selection still uses the remembered entity.
`resourceCodecTest` exercises state reuse, entity/partial-tick isolation and the
queued shoulder-parrot marker. Opaque Entity identity fixtures use test-only
Objenesis 3.3 without constructing a world or exercising Entity behavior.

The same suite bootstraps real vanilla and production Naturalist registries,
loads private jukebox entries and real item tags, initializes item components,
and parses every staged recipe (101), advancement (40) and loot table (78) with
Minecraft 26.2 codecs. Its negative control reproduces exactly 100 recipe failures,
three advancement failures and dragonfly loot failure from the pristine private
data used by Canary 3. No fixture replaces a recipe, entity predicate or loot codec.

The preservation comparison permits only ingredient object-to-string/tag changes,
five `type` to `minecraft:entity_type` predicate keys, and 15 vanilla sound-path
replacements. It checks entire recipe types, ingredients, patterns, result IDs,
counts/components, criteria, flags, equipment, vehicle and NBT conditions. All 16
snail-shell color recipes are shapeless; every existing shaped pattern parses
after the ingredient correction, so no pattern or acquisition redesign is needed.
The custom net recipe and remaining data are preserved. All 661 sound references
resolve against private assets or the 26.2 vanilla index/events, retaining volume
and other sound settings. Protected upstream bytes stay external or ignored.

`glowGoopTest` uses production GlowGoopBlock, GlowGoopItem, DeferredHolder, and
FabricRegistrationProvider with a test-only item-holder fixture. Actual Fabric
registration must invoke the shape cache for all six GOOP 1–3 / dry-waterlogged
states before the holder exists. It also explicitly repeats initCache and direct
empty-context queries before registering the real Glow Goop item.

The same states then switch repeatedly between Glow Goop, a different item, and
no item through both direct and BlockState shape queries. Full selection is
expected only for the actual Glow Goop item. The tests repeat cache initialization
after registration and check noncollision, invisibility, replaceability,
waterlogging, GOOP × 5 emission, and dry/waterlogged skylight propagation.
Substituting the original unguarded Canary 2 implementation was confirmed to fail
through FabricRegistrationProvider → Fabric shape-cache callback → initCache →
propagatesSkylightDown → getShape with the reported null-holder NPE.

Minecraft 26.2's selection getShape delegates to the block each time; initCache
does not freeze that selection result. Keep the holder lookup live, and never use
Block.asItem() for the early lookup because it can memoize AIR before registration.
No dynamic-shape or global-registration change is needed.

## Startup and world regression procedure

1. Once a slot is available, use the serialized Test Instance Manager to deploy
   this exact Canary 4 into the dedicated **Matcha Flavoured 26.2 Workbench**.
   Recheck current main, slot ownership, artifact identity, dependencies and the
   normal readiness receipt. Do not displace another project. Do not modify,
   deploy to, stop or relaunch the Player Instance; never access protected 26.1.2.
2. Prioritize Field Guide absent. Reach the title screen and a disposable world;
   inspect startup and data reload logs for Naturalist helper classload failures,
   rejected recipes/advancements/loot, stale pufferfish/horse sound paths, the
   prior Glow Goop null-holder/MonsterMixin errors and absent Field Guide targets.
3. Render representative Naturalist mobs, a rider on an IK mount, a shoulder
   parrot transitioning between perched and flying, and a digging wolf. Check
   model animation, rider visibility, deferred state isolation and partial ticks.
4. Craft representative shaped, shapeless, cooking and stonecutting recipes,
   including all snail-shell colors and shellstone forms. Verify original
   ingredients, shapes and result counts/components. Exercise the bear honeycomb,
   hippo melon and both-hand giraffe-map conditions; confirm frog/variant-dependent
   dragonfly loot, ordinary loot and the capture-net path without duplicate drops.
   Hear blobfish/piranha/catfish flops and zebra eating at the intended volume.
5. Place Glow Goop dry and waterlogged; test GOOP 1/2/3, stacking maximum, full
   selection while holding Glow Goop, empty selection with another/no item,
   repeated held-item switches on the same state, and save/reload. Check no
   collision, invisible rendering, fluid state, light 5/10/15, replacement,
   drops and pick-block identity without loss or duplication.
6. At a valid Overworld bed at night in Survival, verify a nearby hostile monster
   prevents sleep without a plush bear, then permits it while holding one in
   either hand. Reset nighttime and repeat the no-plush vanilla control.
7. Test dedicated-server startup and Field Guide-present rendering, entries,
   aquatic previews and icons only in an authorized setup with a compatible
   provider. Field Guide must stay optional and absent-safe; do not install or
   bundle it merely to complete this matrix. Both paths remain runtime-untested
   for Canary 4.
8. Stop on identity drift, occupied slots, startup/resource failure or changed
   behavior; record exact observations and any independent blocker. Static
   fixtures and codec passes do not establish Minecraft runtime acceptance.

## Reproducible static checks

Use Temurin 25.0.4.1, Gradle 9.6.1, Loom 1.17.20, Loader 0.19.3, Fabric API
0.157.0+26.2, and optional compile surfaces LambDynamicLights 4.12.2+26.2 and
Field Guide 1.7.10-26.2-fabric. These are validation baselines. Packaged runtime
requirements are Minecraft 26.2, Loader >=0.19.3, Fabric API >=0.157.0, Java >=25;
LambDynamicLights remains optional and Field Guide is neither required nor bundled.

Run `clean build persistenceTest itemModelTest` from this project with the
existing `naturalistOriginalJar` and `naturalistLegacyMinecraftJar` properties.
`build` includes `startupTest`, `glowGoopTest` and `resourceCodecTest`; optional
providers are excluded from the test runtimes. To reproduce
the retained source stamp after a later administrative commit, also pass
`-PnaturalistSourceCommit=d577b5bb4c93484cea6be0871fa1297372662a0a` while using this exact source tree.
Run all six `tools/Verify-*.ps1` preservation verifiers after resource generation,
passing the external input paths where supported.

Sound verification uses the cached `26.2-32.json` asset index and its hash-checked
`minecraft/sounds.json` object. Override the index location with
`-PnaturalistMinecraftAssetIndex=<absolute path>` when needed. The validation
baseline is Loom index SHA-1 `773791767c043b4f9493b50c54257619cecb08a4` and
vanilla sounds object `9ac006d5537ed0fa4a7bcd1eccfc505155847686`.

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
