# Testing

## Current gate

**PRIVATE CANARY 5 RETAINED — NOT DEPLOYED / RUNTIME UNTESTED**

- Version: `2.0.3+26.2-port-canary5`.
- Artifact: `artifacts/naturalist-2.0.3+26.2-port-canary5.jar`, 11,374,087 bytes.
- SHA-256: `18a1e4aafaf4b2a11df3dba6d7f94a657fbd2a3ff29b3b20fb884f5f8ab36a04`.
- Source checkpoint (also embedded): `76ea6ea90976efdf0c4f65ceafe915bb8078bee2`.
- Java 25 clean build, 315 tests, six preservation verifiers, dependency-policy
  scan and narrow comparison against retained Canary 4 pass.

Manager revision 98 has occupied A (CSR Canary 10) and B (QSN Canary 9).
Recheck current main before deployment. Keep Naturalist ACTIVE until a verified
serialized manager transition assigns its UUID to a free slot. No Minecraft
runtime validation, dedicated deployment or Player Instance mutation occurred.

Canary 4 is **external user-reported runtime FAIL**, superseding its earlier
untested description: September 5, 2026 ~15:30 America/Chicago, Matcha 26.2 Player
Instance, Minecraft 26.2, Loader 0.19.3, Java 25. Title, world and player entry
worked; prior helper, mass recipe, dragonfly loot and stale sound failures were
absent. Butterfly crashed querying `tempt_range`; catch_bee failed before item
components existed; three husbandry advancements had a missing parent; Polymer
rejected Shellstone Brick Wall in the tab. The user deleted the installed JAR
and identified the retained Canary 4 as the forensic input. Its verified hash
is `fc7a214b59fa1ef9ebc4ffbb8a603867a12276742c56278d705123b0ea0cd922`, filename
`naturalist-2.0.3+26.2-port-canary4.jar`, source
`d577b5bb4c93484cea6be0871fa1297372662a0a`, 11,369,270 bytes. No post-deletion
installed-file hash was measured. This is external evidence, not Workbench acceptance.

## Focused executable coverage

`lifecycleTest` bootstraps actual vanilla/Fabric/Naturalist registries and applies
both production creative-identity and advancement-manager Mixins. It uses Fabric's
world-registry list to load all 48 variant registries, 99 authored definitions,
and eight damage/song/painting/worldgen entries. Actual reloadable loot registries
load and validate all 78 tables and resolved references. RecipeManager prepares
all 101 Naturalist recipes plus four Naturalist-owned vanilla overrides before
pending components are published, then finalizes recipes. catch_bee's declared
result materializes only afterward; its upstream declaration is preserved exactly.
The existing resource codec and 661-reference sound tests remain independent.

Final advancement publication checks all 40 nodes with vanilla parents and with
a fixture matching Matcha's filtered husbandry tree. The uncorrected tree rejects
exactly three nodes. The fallback changes only their parent to `main:tutorial/root`
when vanilla husbandry/root is absent and that Matcha root exists. Criteria,
requirements, rewards and display remain equal. The decoded tactical-fishing hook
retains the original catfish/bass bucket alternatives and is idempotent.

All 51 actual entity factories and 48 living suppliers are checked using mocked
server services, real constructors, installed goals and real attributes. Bytecode
checks cover installed vanilla goal/control hierarchies, direct Naturalist family
attribute reads and attack-damage call paths. Representative goals exercise canUse
and, after a valid start, canContinueToUse. The 23 repaired suppliers are Bear,
Black Bear, Boar, Butterfly, Capybara, Crab, Deer, Duck, Elephant, Giant Isopod,
Giraffe, Hedgehog, Hippo, Komodo Dragon, Lion, Lizard, Mammoth, Mole, Ostrich, Rat,
Tiger, Tortoise and Turkey. Zebra already inherits the canonical range. No other
missing required attribute was demonstrated. Mocked services are not a game world
or evidence of successful real AI ticks, combat, spawning or integration.

Creative checks verify every deferred holder, item/block ownership, all 141 item
identities, 198 exact-distinct stacks, all snail/shell colors and bucket variants,
search entries and repeated builds. The exact retained Canary 4 generator and
registries passed isolated vanilla and Polymer 0.17.3 output checks, but failed
with `naturalist:shellstone_brick_wall` under the verified CNM 2.0.7 shape-family
RETURN-hook behavior plus a forced hash-bucket collision. The new required Mixin
scopes strict item/component comparison to Naturalist generation; the same
collision fixture now passes, and inventory family equality remains unchanged.
Scope nesting and exception cleanup are checked. No output deduplication occurs.
This reconstructs the integration seam; it is not a rerun of the entire Player Instance.

Prior render-helper package guards, state lookup, Glow Goop's six registration/
selection cases, required MonsterMixin descriptor, optional Field Guide gating,
persistence and item-model suites remain enabled. Protected originals are immutable.
Only four additional private recipe ingredient migrations and one required Mixin
registration change staged resources relative to Canary 4. All protected assets,
prior corrected data, 67 item roots and 47 spawn-egg roots remain byte-identical.

## Runtime procedure when a slot is free

1. Recheck authoritative main and both slots. Deploy this exact artifact through
   the serialized Test Instance Manager only into a free dedicated Workbench slot;
   require readiness verification. Never displace another project, modify/relaunch
   the Player Instance or access protected 26.1.2.
2. With Field Guide absent, reach title and enter a disposable world. Inspect logs
   for Naturalist errors during load and resource reload, including catch_bee,
   advancement parents, helper loading, Glow Goop, MonsterMixin and sounds.
3. Open Naturalist's tab under the full Workbench stack. Confirm Shellstone Brick
   Wall and the other ordinary shapes, snail colors and bucket variants appear;
   inspect search entries and confirm no false duplicate error.
4. Spawn and tick Butterfly, tempt with its intended flower, then tempt another
   repaired species. Exercise representatives of flying, passive, hostile, aquatic,
   inventory-bearing and tameable/equipment-bearing families. Check goal behavior,
   health, movement, targeting and inventory interactions against upstream semantics.
5. Exercise bee capture with the capture net and verify the original declared
   acquisition result without double emission. Confirm all three repaired
   advancements load and award only for their preserved honeycomb, melon and
   giraffe/map conditions. Verify shaped/shapeless/cooking/stonecutting recipes,
   including Naturalist's cake, leather, pumpkin-pie and spectral-arrow overrides.
6. Save/reload the world. Render representative mobs, an IK rider and a shoulder
   parrot under the Workbench rendering stack. Check variants, inventories and
   remembered partial ticks. Verify dragonfly/ordinary loot and flop/eating sounds.
7. Repeat Glow Goop GOOP 1/2/3, dry/waterlogged, selection with actual/other/no item,
   stacking, light 5/10/15, save/reload, drops and pick-block checks. Verify hostile
   bed blocking without a plush bear and its original main/offhand exception.
8. Dedicated-server and optional-provider-present checks require an authorized
   setup. Do not install Field Guide merely to complete the matrix.
9. Stop at identity drift, an occupied slot, changed behavior or any new
   Naturalist-specific error. Record actual observations and the exact artifact;
   do not repair unrelated mod noise during this procedure.

## Reproducible static checks

Use Temurin 25.0.4.1, Gradle 9.6.1, Loom 1.17.20, Loader 0.19.3, Fabric API
0.157.0+26.2; optional compile baselines are Field Guide 1.7.10-26.2-fabric and
LambDynamicLights 4.12.2+26.2. Mockito 5.20.0 is test-only. No optional provider or
test library is required or packaged at runtime.

Run `clean build persistenceTest itemModelTest` with the existing
`naturalistOriginalJar` and `naturalistLegacyMinecraftJar` input properties.
`build` includes startupTest, glowGoopTest, resourceCodecTest and lifecycleTest.
After administrative commits reproduce the stamp with
`-PnaturalistSourceCommit=76ea6ea90976efdf0c4f65ceafe915bb8078bee2` on this exact source tree.
Run all six `tools/Verify-*.ps1` verifiers after resource generation.

Sound verification uses cached `26.2-32.json`, index SHA-1
`773791767c043b4f9493b50c54257619cecb08a4` and sounds object
`9ac006d5537ed0fa4a7bcd1eccfc505155847686`. Override its path using
`-PnaturalistMinecraftAssetIndex=<absolute path>` if needed. Private input hashes:
Naturalist 2.0.3 `3d16c975326e0df24486d44d8010d9e614fc9efdf891864de7b5a0efedfc12f9`;
Minecraft 1.21.1 client `499f6897d1837516680f3114072d8106e11c9adcd933fe5cf051b551089b0c99`.

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
