# Testing

Canary 1 is the current unaccepted failed candidate. Exact retained `artifacts/coal-consolidation-0.1.0-canary1.jar`, 7,488 bytes, SHA-256 `D7598F640184613F03F0EA2FB558CD4D94099EC54CABC189EF978DBB7F5D4F9D`, from release source `ff8ea31e3c5959fdb4a2886b1113071bfda5770f` recorded `RUNTIME_FAIL`: the user observed that smelting logs through the vanilla furnace `minecraft:charcoal` route still produced `minecraft:charcoal` instead of coal. That observation satisfies the runtime stopping condition. The separate Matcha `smoking:charcoal` route was not reported as tested, and no result is inferred for it or any other matrix row. These two cooking recipes remain the audited ordinary production routes; Matcha kindling/campfire is a regression control and does not provide ordinary charcoal acquisition in this baseline.

C1 has been removed from Slot B and occupies neither test slot, so the project is `ACTIVE / NOT_DEPLOYED` while the fixable implementation defect awaits separate work. No successor exists, and unchanged C1 must not be treated as ready for another acceptance run.

Legacy checkpoint `8efd7ff3c00c7bb82356e06f3aaa19d3079be3f8` records a Java 25, Gradle 9.5.1, Loom 1.17.20 clean build with 16/16 focused tests passing. Migration separately ran the same focused build and again passed 16/16 with no failures, errors, or skips. Its fresh JAR reproduced the retained artifact byte-for-byte without replacing it. Both results are static evidence only.

## Automated contracts

Run from the repository root with Java 25:

```powershell
gradle -p projects/coal-consolidation clean test build --no-daemon
```

The four suites contain 16 tests covering all ten packaged recipes through Minecraft 26.2's production `Recipe.CODEC`, representative furnace/smoker inputs and retained timing/experience, the seven explicit consumers, one-way legacy conversion, unchanged tag-backed behavior, Creative and recipe-viewer suppression boundaries, and continued charcoal registry identity.

## Successor runtime prerequisites

After a separately authorized fix produces a successor, deploy its exact retained artifact through the canonical manager rather than redeploying unchanged C1. Reconfirm the manager identity/readiness before launch. Test with the intended Matcha 1.12 stack and JEI 30.18.0.144 for viewer coverage; separately confirm the optional no-JEI boundary when applicable.

## Successor runtime matrix

1. Confirm the exact artifact and dependency identities, reach the title screen, enter a disposable test world, and check the log for Coal Consolidation resource or initializer errors.
2. In a furnace, process an oak log, spruce log, and oak wood. Confirm each produces exactly one coal, never charcoal, while the `minecraft:charcoal` recipe retains ordinary furnace timing and `0.15` experience.
3. Use Matcha's `smoking:charcoal` route with representative burnable log and wood inputs. Confirm each produces exactly one coal at 100 ticks with `0.15` experience.
4. Craft the four vanilla explicit consumers (`torch`, `soul_torch`, `copper_torch`, and `fire_charge`) and the three Matcha consumers (`crafting:torch`, `crafting:fire_charge`, and `crafting:black_dye`) with coal. Confirm their recipe identities, patterns, other ingredients, groups/categories, and output counts remain normal and charcoal is no longer an explicit alternative.
5. Use `/give @s minecraft:charcoal 2` only as a legacy fixture. Confirm the registry ID still works, one charcoal converts to exactly one coal, two convert to two, coal cannot convert back, and representative untouched `#minecraft:coals` consumers continue to use the vanilla tag contract.
6. Open Creative Ingredients and Creative Search. Confirm charcoal is absent, coal remains present/searchable, and neighboring entries remain normal. With JEI installed, confirm charcoal is absent from the ordinary ingredient/search list while coal lookup, the cleanup recipe's charcoal input, and representative `#minecraft:coals` recipe behavior remain available.
7. Reload data and regress Matcha's campfire self-drop, alternate coal-block recipe, unrelated cooking/crafting recipes, and client shutdown. Confirm the overrides persist, no ordinary charcoal acquisition appears, and no Coal Consolidation, JEI, or Matcha-companion error is logged.
8. When dedicated-server coverage is authorized, repeat the applicable cooking, crafting, legacy-conversion, reload, and no-JEI checks on the real server host's disposable playtest world and record that result separately.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if identity/readiness differs, either cooking route emits charcoal or changes timing/experience, any audited explicit consumer still offers charcoal or changes unrelated semantics, a tag-backed consumer is rewritten, charcoal registry/legacy conversion breaks, Creative or viewer suppression removes coal or unrelated items, reload fails, or another Matcha behavior regresses. Do not mark the project runtime-passed from only a subset.
