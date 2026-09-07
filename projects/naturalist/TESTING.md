# Testing

Exact current release `naturalist-2.0.3+26.2-port-canary8.jar` (version `2.0.3+26.2-port-canary8`, SHA-256 `65e9ff11e94f17011acbac25ee40f42723516fe6b04c78d93662800461317b3d`, source `637ba2f7f5c6916fa1451449f8d4e05b25b75eed`) is accepted in Workbench Stack v27 after the user's 2026-09-06 user-reported external aggregate PASS. No Test Slot deployment or slot history was created; Slot A remained empty and Slot B was preserved. No row-level observations beyond the reported PASS are inferred.

## Future regression procedure

The retained procedure below is for future regressions of this exact accepted identity or an explicitly identified successor. Any former pre-promotion candidate wording is historical and superseded by this accepted result.

# Testing

## Current gate

**PRIVATE CANARY 8 RETAINED — NOT DEPLOYED / RUNTIME UNTESTED**

- Version: `2.0.3+26.2-port-canary8`.
- Artifact: `artifacts/naturalist-2.0.3+26.2-port-canary8.jar`, 11,040,164 bytes.
- SHA-256: `65e9ff11e94f17011acbac25ee40f42723516fe6b04c78d93662800461317b3d`.
- Source checkpoint: `637ba2f7f5c6916fa1451449f8d4e05b25b75eed`.
- Build result: Java 25 `jar`, `resourceCodecTest` (73 tests), `lifecycleTest`
  (93 tests), and `Verify-ContentCull.ps1` pass.

Canary 8 is a static-only fauna-controls successor. It was not deployed through the
Test Instance Manager, no Minecraft client/server was launched, and neither the
Player Instance nor protected 26.1.2 were accessed. Keep Naturalist `ACTIVE`
until the serialized manager assigns this exact project UUID to a free dedicated
slot and verifies readiness.

## Focused static coverage

- `Verify-ContentCull.ps1` checks removed registry IDs and staged-resource absence,
  vanilla replacement data, retained client roots, Glow Goop, and fish entities.
- `Verify-PortBaseline.ps1`, `Verify-PersistenceMigration.ps1`,
  `Verify-ItemModelMigration.ps1`, `Verify-RenderMigration.ps1`,
  `Verify-EggBlockMigration.ps1`, and `Verify-BehaviorMigration.ps1` pass against
  the pinned original JAR and legacy client input where required.
- The lifecycle suite checks 49 entity registrations, retained creative entries,
  recipes, loot, client roots, and registry/data codecs. This is static evidence,
  not runtime gameplay evidence.

## Runtime procedure when a slot is free

1. Recheck authoritative `main` and both manager slots. Deploy only this exact
   retained artifact through a serialized Test Instance Manager transition into a
   free dedicated Workbench slot and require readiness verification. Do not
   displace another project or touch the Player Instance/protected 26.1.2.
2. In a disposable world, confirm Naturalist starts and `/reload` completes with
   no missing-resource or registry errors. Confirm Ant, ant hills, snail shells,
   Froglass, Shellstone, antivenom, Whistle, plush bear, and retired custom materials
   are unavailable.
3. Open the Naturalist creative tab and search inventory. Confirm the retained
   animals/spawn eggs, buckets, capture net, Knapsack, Glow Goop, music discs,
   eggs, chrysalis, and starfish entries render and are usable.
4. Spawn representatives of aquatic, passive, hostile, flying, tameable, and
   egg-laying retained entities. Check variants, buckets/capture/release, Knapsack,
   vanilla replacement drops, and save/reload.
5. For every retained tameable species, have its owner use empty-hand secondary
   interaction three times. Require FOLLOW → WANDER → STAY → FOLLOW, correct action
   bar text, and no mode change from a non-owner, an occupied hand, or ordinary use.
6. Test Glow Goop placement/selection/light/drop behavior and the glow-berry
   recipe. Stop at identity drift, a new Naturalist error, or changed retained
   behavior; record only observed runtime evidence.

## Reproducible static checks

Use Temurin 25.0.4.1, Gradle 9.6.1, Loom 1.17.20, Loader 0.19.3, and Fabric API
0.157.0+26.2. Supply the existing `naturalistOriginalJar` and
`naturalistLegacyMinecraftJar` properties:

`clean build persistenceTest itemModelTest`

Then run the seven verifier scripts listed above, including
`tools/Verify-ContentCull.ps1`. The pinned Naturalist input SHA-256 is
`3d16c975326e0df24486d44d8010d9e614fc9efdf891864de7b5a0efedfc12f9`.
