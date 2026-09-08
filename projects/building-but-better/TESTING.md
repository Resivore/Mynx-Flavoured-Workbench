# Testing

The current `2.0pre4+26.2-pale-oak-dev.6` candidate is `ACTIVE` with `STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. It has not been deployed by the Test Instance Manager, assigned a Test Slot, launched in Minecraft, or accepted. Its combined JAR stages audited upstream ARR resources and is therefore locally retained, ignored, and not redistributed.

C5 (`2.0pre4+26.2-pale-oak-dev.5`, canonical SHA-256 `b86c90226cb5082aba5329116633705f0bcd5141754ff50f509a5582b4eff7d4`) has user-reported/external manual evidence only: vertical-rope extension passed for the observed interaction; empty-main-hand retraction failed because it did nothing and returned no rope. That manual test neither establishes the external file's exact SHA-256 nor represents a managed deployment or slot result. It must not be read as a complete C5 pass.

C6 keeps the Y-only rope mechanic and normal ChainBlock behavior. For an unhandled `useItemOn`, it delegates to the resolved Minecraft 26.2 superclass, whose default result is `TRY_WITH_EMPTY_HAND`; that allows a non-sneaking empty-main-hand click to reach `useWithoutItem`. Retraction resolves the common contiguous-column bottom, removes exactly one segment, restores water when appropriate, and returns one rope only in Survival. A successful custom extension still delegates placement to `BlockItem.place`; after that success it emits `Blocks.HAY_BLOCK.defaultBlockState().getSoundType().getPlaceSound()` with vanilla BlockItem placement volume `(volume + 1) / 2` and pitch `pitch * 0.8`. Failed extension emits no added Hay Bale sound. Ordinary rope placement remains wool-sounded.

## Static verification

From this project directory, Temurin Java `25.0.4.1+1`, Gradle `9.5.1`, and Fabric Loom `1.17.20` completed:

```powershell
$env:JAVA_HOME = 'C:\Users\resiv\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2'
.\gradlew.bat clean check build verifyProductionMixinContract productionLivingEntityMixinTest --console=plain "-PbbbOriginalJar=C:\Users\resiv\OneDrive\Documents\Minecraft 26.2 Workbench\originals\mods\bbb-fabric-2.0pre4.jar"
```

The C6 run was `BUILD SUCCESSFUL` with 27 focused JUnit tests passing. It validated the exact pristine input (`bbb-fabric-2.0pre4.jar`, 1,701,505 bytes, SHA-256 `1e7ae114aaec53475133e11c607fc65dce493bba5897eaf0044d53959b508fc0`), 171 blocks, 172 items, 1,132 models, 248 texture/sidecar files (244 PNGs; 112 transparent), 224 recipes, 140 advancements, 171 loot tables, and 44 tags. The new rope regressions verify the resolved default `TRY_WITH_EMPTY_HAND` contract, BBB's superclass fallback, bounded bottom retraction/Survival/Creative/water/offhand/sneaking/horizontal contracts, successful Hay Bale sound scaling, and failure-before-sound ordering. The packaged official-namespace JAR was inspected and the Fabric Knot production harness successfully transformed `LivingEntity` with `LivingEntityRopeClimbMixin`. This is static/build validation, not runtime validation.

The locally retained C6 artifact is `bbb-fabric-26.2-2.0pre4+26.2-pale-oak-dev.6.jar`, SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`.

## Required Minecraft runtime matrix

Obtain explicit Test Instance Manager ownership before touching the dedicated 26.2 Workbench. Never access the protected 26.1.2 gameplay profile. Test the exact retained C6 artifact in one serialized slot transition and record each result independently.

1. Launch C6 and verify no BBB Mixin, resource, registry, recipe, advancement, or loot failure.
2. Verify dry and waterlogged Y rope is climbable; X/Z rope is not.
3. From top, middle, and bottom, use a rope in the non-sneaking main hand; each successful use adds exactly one vertical bottom segment, consumes one Survival rope and no Creative rope, and emits the Hay Bale placement sound once. Verify blocked/full/lava/unsupported-fluid targets add no segment and emit no added Hay Bale placement sound.
4. From top, middle, and bottom, right-click with an empty non-sneaking main hand; each removes only the common bottom. Verify one Survival return or one safe inventory-full drop, no Creative return, and water restoration for a waterlogged bottom.
5. Verify offhand cannot produce a second extension or retraction; sneaking retains ordinary axis-aware ChainBlock behavior. Break a middle rope normally and test the split columns independently.
6. Confirm no regression to Pale Oak, Hammer, ordinary rope placement/breaking/walking, or excluded BBB content.

Stop at a confirmed defect with `RUNTIME_FAIL`, or record `INCONCLUSIVE` for environment ambiguity. Do not accept or promote C6 without runtime evidence.
