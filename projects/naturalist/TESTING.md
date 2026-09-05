# Testing

## Current gate

**PRIVATE CANARY 3 RETAINED — NOT DEPLOYED / RUNTIME UNTESTED**

- Version: `2.0.3+26.2-port-canary3`.
- Artifact: `artifacts/naturalist-2.0.3+26.2-port-canary3.jar`, 11,369,742 bytes.
- SHA-256: `5aabc54ae858c77d90870baa3bcbfbee6448f67cecf489a448085b04c15d8d75`.
- Source checkpoint (also embedded in the JAR): `038de70b6579cd66e43426539468cd684bb46d4a`.
- Java 25 clean build, 28 focused tests, all six preservation verifiers, and
  root/nested dependency-policy checks pass. Only GlowGoopBlock.class and the
  two release-metadata entries differ from Canary 2; no entries were added or
  removed. Protected resources, MonsterMixin, Field Guide gating, and the
  production registry/registration classes remain byte-identical.

Both canonical Workbench slots remain occupied at manager revision 97: A holds
Container Slot Reservations Canary 9, and B holds Quick Stack Nearby compatibility
Canary 9. No deployment or Minecraft launch was performed for Canary 3. Keep
Naturalist ACTIVE until its UUID actually occupies a verified manager slot.

The user-reported September 5, 2026 **12:28 Matcha 26.2 Player Instance** launch
used Minecraft 26.2, Loader 0.19.3, Java 25, and Naturalist
`2.0.3+26.2-port-canary2`. It failed with GLOW_GOOP null in
`GlowGoopBlock.getShape`, reached through `BlockStateBase.initCache`,
`propagatesSkylightDown`, and Fabric registration. The previous MonsterMixin
failure and Naturalist-owned Field Guide missing-target warnings did not appear
in the supplied report; Field Guide was absent. This is external historical
evidence, not a dedicated Workbench test. Its exact binary hash/source were not
established and must not be inferred from the version or retained Canary 2.

## Focused executable regression

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

1. When a slot becomes available, use the serialized Test Instance Manager to
   deploy this exact Canary 3 into the dedicated **Matcha Flavoured 26.2
   Workbench**. Recheck current main, slot ownership, artifact identity,
   dependencies, and the normal readiness receipt before launching.
   Do not displace another project. Do not modify, deploy to, stop, or relaunch
   the Player Instance; never access the protected 26.1.2 instance.
2. Prioritize **Field Guide absent**. Confirm the enabled inventory lacks
   `fieldguide`; launch and inspect the new log for the Glow Goop null-holder
   error, the prior MonsterMixin descriptor error, and Naturalist-owned missing
   Field Guide targets. Reach the title screen and a disposable world. Record any
   later independent startup blocker and stop without repairing unrelated mods.
3. Place Glow Goop in dry and waterlogged positions. Cycle GOOP through 1, 2, and
   3, verify further stacking respects the maximum, and switch held items:
   Glow Goop must give full-block selection; another item and no item must give
   empty selection. Repeat switches on the same placed state and after reload.
   Check no collision, invisible rendering, expected water fluid state, and light
   emission 5/10/15. Verify replacement/drop counts and pick-block return the
   existing Glow Goop item with no lost or duplicate drops. Record each result.
4. Preserve the plush-bear sleep checks: in Survival at a valid Overworld bed
   at night, an appropriately nearby hostile monster prevents rest with no plush
   bear. Holding the plush bear in either hand removes that monster's refusal.
   Reset nighttime between attempts and remove it for a final vanilla control.
5. Check server startup only in an authorized dedicated Workbench setup through
   the same manager/readiness boundaries. Optional integration must not eagerly
   load Field Guide or client-only classes on a server.
6. Check Field Guide-present variants, entry rendering, aquatic previews, and
   icon generation only when a compatible dependency and an authorized setup
   are available. Do not install or bundle it merely to complete this matrix.
   Both present and absent Minecraft runtime paths remain untested for Canary 3.
7. Stop on identity drift, occupied slots, startup failure, or changed behavior.
   A build, fixture pass, successful bootstrap, or focused world check does not
   imply full Naturalist acceptance; the broader matrix below remains required.

## Reproducible static checks

Use Temurin 25.0.4.1, Gradle 9.6.1, Loom 1.17.20, Loader 0.19.3, Fabric API
0.157.0+26.2, and optional compile surfaces LambDynamicLights 4.12.2+26.2 and
Field Guide 1.7.10-26.2-fabric. These are validation baselines. Packaged runtime
requirements are Minecraft 26.2, Loader >=0.19.3, Fabric API >=0.157.0, Java >=25;
LambDynamicLights remains optional and Field Guide is neither required nor bundled.

Run `clean build persistenceTest itemModelTest` from this project with the
existing `naturalistOriginalJar` and `naturalistLegacyMinecraftJar` properties.
`build` includes `startupTest` and `glowGoopTest`; both exclude Field Guide. To reproduce
the retained source stamp after a later administrative commit, also pass
`-PnaturalistSourceCommit=038de70b6579cd66e43426539468cd684bb46d4a` while using this exact source tree.
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
