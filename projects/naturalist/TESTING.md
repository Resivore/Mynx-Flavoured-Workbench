# Testing

## Current gate

**PRIVATE CANARY 7 RETAINED — NOT DEPLOYED / RUNTIME UNTESTED**

- Version: `2.0.3+26.2-port-canary7`.
- Artifact: `artifacts/naturalist-2.0.3+26.2-port-canary7.jar`, 11,056,134 bytes.
- SHA-256: `4dd2bd7baa5500740b3906f0e2b7d61608b3592bb336bc93f12dd7f9ef3400ba`.
- Source checkpoint: `83004e268a0407eb33a6053ec425843917bf1ebb`.
- Build result: Java 25 clean build and `persistenceTest`, `itemModelTest`,
  `startupTest`, `glowGoopTest`, `resourceCodecTest`, and `lifecycleTest` pass.

Canary 7 is a static-only content-cull successor. It was not deployed through the
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
   Froglass, Shellstone, antivenom, and retired custom materials are unavailable.
3. Open the Naturalist creative tab and search inventory. Confirm the retained
   animals/spawn eggs, buckets, capture net, Knapsack, Glow Goop, music discs,
   eggs, chrysalis, and starfish entries render and are usable.
4. Spawn representatives of aquatic, passive, hostile, flying, tameable, and
   egg-laying retained entities. Check variants, buckets/capture/release, Knapsack,
   vanilla replacement drops, and save/reload.
5. Test Glow Goop placement/selection/light/drop behavior and the glow-berry
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
