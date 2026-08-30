# Testing

Canary 2 is the current unaccepted successor. Exact retained `artifacts/coal-consolidation-0.1.1-canary2.jar`, 11,217 bytes, SHA-256 `713B4F0F0CBF68A712A1BA8C75ADDF120899B8963AFB01C88A14F2E6FCEECC29`, from release source `03f70b54c774f766ea9edc3487622153a253afe2` is `STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. It occupies neither test slot and must not be represented as runtime-validated.

Exact retained C1 remains the failed predecessor: `artifacts/coal-consolidation-0.1.0-canary1.jar`, 7,488 bytes, SHA-256 `D7598F640184613F03F0EA2FB558CD4D94099EC54CABC189EF978DBB7F5D4F9D`, from release source `ff8ea31e3c5959fdb4a2886b1113071bfda5770f` recorded `RUNTIME_FAIL` because the user observed the vanilla furnace `minecraft:charcoal` route still produce charcoal. The separate Matcha `smoking:charcoal` route was not reported as tested, and no other C1 matrix result is inferred. C1 has been removed from Slot B and must not be redeployed or promoted unchanged.

C1's retained JAR already packaged `data/minecraft/recipe/charcoal.json` with a coal result. The successor investigation found no Matcha 1.12 or other loaded-mod resource at that same recipe path, while Minecraft 26.2 resolves the singular `data/<namespace>/recipe/...` resource before `RecipeManager.apply`. C1 nevertheless had no post-resolution enforcement and its tests decoded only its own resource, so those tests could pass without proving the effective manager entry. C2 retains the data override and adds a required common-side mixin at the typed `RecipeManager.apply` boundary. It replaces only the resolved `minecraft:charcoal` holder's result with one coal while preserving the holder ID, input, category/group, notification flag, experience, and cooking time; missing, duplicate, or non-smelting targets fail closed.

Java 25, Gradle 9.5.1, and Loom 1.17.20 passed the focused competing-pack regression, all 17 tests in four suites with zero failures, errors, or skips, and the successor JAR build. The retained artifact is byte-identical to the built JAR. Compilation, tests, and JAR creation are static evidence only; Minecraft was not launched.

## Automated contracts

Run from the repository root with Java 25:

```powershell
gradle -p projects/coal-consolidation clean test build --no-daemon
```

The four suites contain 17 tests. Existing contracts still cover all ten packaged recipes through Minecraft 26.2's production `Recipe.CODEC`, representative furnace/smoker inputs and retained timing/experience, the seven explicit consumers, one-way legacy conversion, unchanged tag-backed behavior, Creative and recipe-viewer suppression boundaries, and continued charcoal registry identity. The added regression creates a later competing pack whose `minecraft:charcoal` JSON returns charcoal, proves that resource wins preparation, then exercises the transformed `RecipeManager.apply` and verifies that the effective manager recipe returns exactly one coal while retaining `#minecraft:logs_that_burn`, `0.15` experience, and 200 ticks. The C1 resource-only architecture fails that effective-reload assertion.

## Successor runtime prerequisites

Under separate authorization, allocate exact C2 through the canonical manager; never redeploy unchanged C1. Reconfirm the manager identity/readiness before launch. Test with the intended Matcha 1.12 stack and JEI 30.18.0.144 for viewer coverage; separately confirm the optional no-JEI boundary when applicable.

## Successor runtime matrix

1. Confirm the exact artifact and dependency identities, reach the title screen, enter a disposable test world, and confirm the log reports `Enforced minecraft:charcoal furnace output as one minecraft:coal after recipe reload` without Coal Consolidation resource or mixin errors.
2. In a furnace, process an oak log, spruce log, and oak wood. Confirm each produces exactly one coal, never charcoal, while the `minecraft:charcoal` recipe retains ordinary furnace timing and `0.15` experience.
3. Use Matcha's `smoking:charcoal` route with representative burnable log and wood inputs. Confirm each produces exactly one coal at 100 ticks with `0.15` experience.
4. Craft the four vanilla explicit consumers (`torch`, `soul_torch`, `copper_torch`, and `fire_charge`) and the three Matcha consumers (`crafting:torch`, `crafting:fire_charge`, and `crafting:black_dye`) with coal. Confirm their recipe identities, patterns, other ingredients, groups/categories, and output counts remain normal and charcoal is no longer an explicit alternative.
5. Use `/give @s minecraft:charcoal 2` only as a legacy fixture. Confirm the registry ID still works, one charcoal converts to exactly one coal, two convert to two, coal cannot convert back, and representative untouched `#minecraft:coals` consumers continue to use the vanilla tag contract.
6. Open Creative Ingredients and Creative Search. Confirm charcoal is absent, coal remains present/searchable, and neighboring entries remain normal. With JEI installed, confirm charcoal is absent from the ordinary ingredient/search list while coal lookup, the cleanup recipe's charcoal input, and representative `#minecraft:coals` recipe behavior remain available.
7. Reload data and regress Matcha's campfire self-drop, alternate coal-block recipe, unrelated cooking/crafting recipes, and client shutdown. Confirm the overrides persist, no ordinary charcoal acquisition appears, and no Coal Consolidation, JEI, or Matcha-companion error is logged.
8. When dedicated-server coverage is authorized, repeat the applicable cooking, crafting, legacy-conversion, reload, and no-JEI checks on the real server host's disposable playtest world and record that result separately.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if identity/readiness differs, either cooking route emits charcoal or changes timing/experience, any audited explicit consumer still offers charcoal or changes unrelated semantics, a tag-backed consumer is rewritten, charcoal registry/legacy conversion breaks, Creative or viewer suppression removes coal or unrelated items, reload fails, or another Matcha behavior regresses. Do not mark the project runtime-passed from only a subset.
