# Codex Log

## 2026-09-13T20:59:13Z — Implement Mynx Matcha Trade Tweaks Canary 1
- Revision: 1
- Source checkpoint: `2144aa1111b310bbfc5bd8dc87613c8dae494201`
- Changes: Added an exact-pack-priority Fabric overlay with 124 edited villager-trade resources, five minimal tag/trade-set pairs effecting four non-filler removals and one Leatherworker Master addition, and a deterministic 129-row comparison. Preserved accepted Matcha data, unchanged trades, all 18 tentative black-wool fillers, custom Cleaver/Clay Fetish identities, and non-target trade metadata. The Matcha resource pack's Tomato Seeds resolve to `minecraft:beetroot_seeds`.
- Build/static: Java 25 / Gradle 9.5.1 `clean build` passed. Focused static validation passed 124 changes, four effective removals, one addition, 18 retained fillers, pool cardinality, one-use-per-restock limits, baseline-relative metadata, and all overlay JSON. External item IDs were checked against local SimpleCopperPipes 2.1.7 and Copper Hopper 0.24.0 JARs by exact SHA-256 as validation baselines, not runtime pins. `git diff --cached --check` and `tools/workbench.py validate-repository --root .` passed.
- Runtime: NOT_DEPLOYED / RUNTIME_UNTESTED; no Test Slot or gameplay profile was accessed.
- Artifact: `mynx-matcha-trade-tweaks-0.1.0+26.2-canary1.jar`; 82,935 bytes; SHA-256 `f0cd947b19a2fd6b242cc1ef5133a39367384eddb27daaa0edfe0ce8435cca9b`.
- Result: ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED.
- Next state: Retain the exact ignored JAR in the primary checkout, integrate to current main, verify GitHub Sheet reconciliation, then test offer generation, removals, provider items, components, and animal restocking in the dedicated Matcha Flavoured 26.2 Workbench.

## 2026-11-24T16:34:20Z — Migrate to Matcha 1.12.5, Canary 2
- Revision: 2
- Source checkpoint: `1ce48a4c2b288c0a18d71e625531e56b618b7ae7`
- Changes: Resolves 68 Matcha pools and 280 eligible offers, preserves the approved economy, filters the 42 food writers, and cleans their persisted offers on entity load.
- Build/static: All 307 decisions reconciled across 68 effective sets. Both Java registry/cleanup tests passed and both external item IDs matched the hash-verified Copper Pipes provider. The coordinated packaged-stack fixture passed.
- Runtime: RUNTIME_UNTESTED. No Minecraft profile or server deployment was used.
- Artifact: C2 `mynx-matcha-trade-tweaks-0.2.0+26.2-canary2.jar`, 105,167 bytes, SHA-256 `46b4ec2d45e1ccfe8e3789bbb02a23d8f895e40c43e4a64d55f927f35421679f`, built_at `2026-11-24T16:14:29.563094Z`, artifact source `1ce48a4c2b288c0a18d71e625531e56b618b7ae7`.
- Result: ACTIVE, STATIC_PASS.
- Next state: Follow TESTING.md and record owner-directed runtime observations against this release.
