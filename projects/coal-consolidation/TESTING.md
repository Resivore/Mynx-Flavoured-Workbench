# Testing

Exact Canary 3 is current and accepted in the physically verified Stack v6 baseline. Retained `artifacts/coal-consolidation-0.1.2-canary3.jar`, 11,711 bytes, SHA-256 `F1D36DFAD276927D527CC911D92F47EE979EF7600479F950C76ED25DD18EB871`, from release source `d2ad841f6a8edfbc765ec681d803b193e5d1f7ad` is `STATIC_PASS / READY_TO_TEST_VERIFIED / RUNTIME_PASS`. The user reported an aggregate pass for this exact candidate; no individual matrix row is inferred. No separate rollback is designated.

Java 25, Gradle 9.5.1, and Loom 1.17.20 passed both focused competing-pack reload regressions, all 19 tests in four suites with zero failures, errors, or skips, and the C3 JAR build. The retained artifact is byte-identical to the built JAR. C1 remains a failed historical candidate and C2 remains an unaccepted predecessor; neither is a rollback or eligible for unchanged redeployment.

## Automated contracts

Run from the repository root with Java 25:

```powershell
gradle -p projects/coal-consolidation clean test build --no-daemon
```

The four suites contain 19 tests. Existing contracts still cover all ten packaged recipes through Minecraft 26.2's production `Recipe.CODEC`, representative furnace/smoker inputs and retained timing/experience, the seven explicit consumers, one-way legacy conversion, unchanged tag-backed behavior, Creative and recipe-viewer suppression boundaries, and continued charcoal registry identity. The retained furnace regression proves a later charcoal-producing `minecraft:charcoal` resource wins preparation before the effective manager recipe is enforced to one coal. The new parallel smoker regression gives a later `smoking:charcoal` resource explicit category/group/notification metadata and a charcoal result, proves it wins normal preparation, then verifies the effective holder keeps the same ID, exact SmokingRecipe type, input object, metadata, `0.15` experience, and 100 ticks while returning exactly one coal. Negative cases verify that either missing or wrong-type owned target aborts enforcement.

## Future regression prerequisites

Reconfirm exact accepted C3 and dependency identities before launch. Test with the intended Matcha 1.12 stack and JEI 30.18.0.144 for viewer coverage; separately confirm the optional no-JEI boundary when applicable. Any successor requires its own artifact identity and independent static and runtime evidence.

## Future regression matrix

1. Confirm the exact artifact and dependency identities, reach the title screen, enter a disposable test world, and confirm the log reports `Enforced minecraft:charcoal furnace and smoking:charcoal smoker outputs as one minecraft:coal after recipe reload` without Coal Consolidation resource or mixin errors.
2. In a furnace, process an oak log, spruce log, and oak wood. Confirm each produces exactly one coal, never charcoal, while the `minecraft:charcoal` holder remains the normal furnace recipe with its burnable-log input, recipe-book category/group/notification behavior, 200-tick timing, and `0.15` experience.
3. Use Matcha's `smoking:charcoal` route with an oak log, spruce log, and oak wood. Confirm each produces exactly one coal, never charcoal, while the holder remains the normal smoker recipe with its burnable-log input, recipe-book category/group/notification behavior, 100-tick timing, and `0.15` experience.
4. Craft the four vanilla explicit consumers (`torch`, `soul_torch`, `copper_torch`, and `fire_charge`) and the three Matcha consumers (`crafting:torch`, `crafting:fire_charge`, and `crafting:black_dye`) with coal. Confirm their recipe identities, patterns, other ingredients, groups/categories, and output counts remain normal and charcoal is no longer an explicit alternative.
5. Use `/give @s minecraft:charcoal 2` only as a legacy fixture. Confirm the registry ID still works, one charcoal converts to exactly one coal, two convert to two, coal cannot convert back, and representative untouched `#minecraft:coals` consumers continue to use the vanilla tag contract.
6. Open Creative Ingredients and Creative Search. Confirm charcoal is absent, coal remains present/searchable, and neighboring entries remain normal. With JEI installed, confirm charcoal is absent from the ordinary ingredient/search list while coal lookup, the cleanup recipe's charcoal input, and representative `#minecraft:coals` recipe behavior remain available.
7. Reload data and regress Matcha's campfire self-drop, alternate coal-block recipe, unrelated cooking/crafting recipes, and client shutdown. Confirm the overrides persist, no ordinary charcoal acquisition appears, and no Coal Consolidation, JEI, or Matcha-companion error is logged.
8. When dedicated-server coverage is authorized, repeat the applicable cooking, crafting, legacy-conversion, reload, and no-JEI checks on the real server host's disposable playtest world and record that result separately.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if identity/readiness differs, either cooking route emits charcoal or changes timing/experience, any audited explicit consumer still offers charcoal or changes unrelated semantics, a tag-backed consumer is rewritten, charcoal registry/legacy conversion breaks, Creative or viewer suppression removes coal or unrelated items, reload fails, or another Matcha behavior regresses. Do not replace the accepted identity or infer a new aggregate pass from only a subset.
