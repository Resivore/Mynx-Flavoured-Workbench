# Codex Log

## 2026-09-19T00:34:34Z — Audit exact Enderscape 3.0.2 pruning dependencies
- Revision: 1
- Source checkpoint: `f6614d253d5ec8429f398a218be36f9d192caec0`
- Changes: Created the canonical Enderscape Pruning project record and audited the exact supplied Enderscape 3.0.2 binary across registries, recipes, loot, advancements, tags, built-in packs, structures, world generation, decompiled gameplay code, Matcha Flavoured food identity, candidate equipment coupling, enchantments, potions/effects, and Magnia mechanics. The report separates safe recommendations from owner decisions and does not implement pruning.
- Build/static: The exact input `enderscape-fabric-3.0.2+mc26.2.jar` matched SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`. Vineflower 1.12.0 under Java 25 was used only as a static inspection aid. The audit covered 240 Enderscape loot tables, 368 recipes, 382 advancements, 197 structure templates, 38 configured features, and 49 placed features; repository validation passed. These results are static evidence, not Minecraft runtime validation.
- Runtime: No Minecraft launch, deployment, Test Slot operation, protected-profile access, user runtime report, or acceptance decision occurred. The project is `RUNTIME_UNTESTED`.
- Artifact: None. No Canary was built or retained, and the supplied upstream Enderscape jar was not copied, modified, or redistributed.
- Result: ACTIVE — the exact static audit is complete, implementation remains owner-gated, no blocker is recorded, and no lifecycle transition or acceptance is inferred.
- Next state: Resolve the owner decisions in `AUDIT.md`; then implement the narrowest data/compatibility layer while retaining registries, validate a separately identified artifact, and run the release-bound matrix in `TESTING.md` under explicit runtime ownership.

## 2026-09-19T07:29:57.6831896Z — Implement Enderscape Pruning Canary 1
- Revision: 2
- Source checkpoint: `a3bcd02bda69cf6d2413f1ec59c0a9acf62d8558`
- Changes: Added the separate Fabric 26.2 companion mod and its always-enabled generated data overlay. It retains upstream registries while suppressing ordinary recipes, candidate advancement branches, Creative/JEI discovery, exact Mirror/Dagger loot results, and normal Bundling/Stun Burst/Transdimensional acquisition. The overlay preserves audited pool rolls, bonus rolls, weights, conditions, count functions, and unrelated entries; it applies current source-fingerprinted Matcha component identities without nesting count-bearing helper tables. `explore_end` remains and now displays a Magnia Attractor.
  Native food: Drift Jelly Bottle, Puruberry, and Murublight Bracket bypass vanilla hunger and grant one current-Matcha heart while retaining their upstream special behavior. The exact 3.0.2 Chorus Cake Roll bridge bypasses direct vanilla hunger, grants two current-Matcha hearts per bite, and preserves the upstream seven-bite, comparator, sound/particle, teleport, and removal flow.
- Build/static: Java 25 Gradle `stageCanaryArtifact` passed with six focused JUnit static-contract tests. The generator and tests pin the exact Enderscape SHA-256 and current Matcha identity component sources; they verify all selected recipe/advancement identities, all nine Mirror and one Dagger direct loot paths, equal-weight empty replacements, retained advancement parents, exact food-component patches, narrow overlay contents, and absence of packaged upstream material.
- Artifact: `enderscape-pruning-0.1.0-canary1.jar`, version `0.1.0-canary1`, SHA-256 `93e8b9ae4f4bc010f6ca18b74a0a8ac1744b298e96f3ddfce2c3f876589a2fd5`, built `2026-09-19T07:29:22.9774388Z`. Required local retention passed (`already_retained`) with the same 46,409-byte artifact.
- Runtime: No Minecraft launch, gameplay profile access, deployment, GameTest execution, or user runtime observation occurred. C1 remains `RUNTIME_UNTESTED`.
- Result: ACTIVE — canonical C1 implementation and static evidence are recorded; acceptance is not inferred. Potions/Void and Magnia remain upstream except for Bundling availability.
- Next state: Under explicit runtime ownership, run the C1 matrix in `TESTING.md` against the exact retained artifact and record only supplied observations; otherwise retain this ACTIVE, unaccepted release unchanged.
