# Testing

## Matcha 1.12.5 Stage 1, Canary 9

`matcha-jei-integration-0.4.0-canary9.jar`

SHA-256 `1c17faef836a53eb2dee8f24a1dea34799c74c25af79d533a5a0f0f7bc155a10`

Source `e3a13df63e041949a8341a025ac4d937ebacdf83`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 21 tests. After building the ten coordinated projects, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_stage1.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v12.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. Compare Creative and JEI results for Braised Mushroom, Glow Berries, Glow Berry Mash and same-carrier stacks with distinct models, food, custom data or enchantments. Check exact subtype UIDs, recipe-book awards and the seven vanilla categories. Check Prayer and Blessing of Demeter after Frost Protection replaces its result.
2. Before and after reload, check an overridden output, removed recipe, dynamic output and ambiguous loot or trade. Compare displayed results against the effective holder. Repeat across reconnect and Creative tab rebuild, with and without client JEI. Check a dedicated-server startup without JEI.

The [1.12 testing procedure](https://github.com/Resivore/Mynx-Flavoured-Workbench/blob/c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd/projects/matcha-jei-integration/TESTING.md) retains the full previous release checklist and historical evidence. Reuse its unaffected controls alongside the cases above.
