# Testing

## Matcha 1.12.5 Stage 1, Canary 3

`matcha-flavoured-data-0.2.0-canary3.jar`

SHA-256 `034f1f281078a81ae2c8dbc84481f96a0527c4c67bd8739399f1c578da3b6807`

Source `e3a13df63e041949a8341a025ac4d937ebacdf83`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 2 tests. After building the ten coordinated projects, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_stage1.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v12.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. Start a new disposable world, then open a copy of a 1.12 world with the old provider removed. Inspect /datapack list enabled and the reload log for exactly one Matcha generation. Repeat /reload twice and check startup, tick and advancement functions. In a separate copy deliberately enable a renamed old world datapack and require an explicit duplicate-generation failure.
2. Use client pack 1.12.3. With Village Restoration enabled, inspect the five village types and placement using spacing 80, separation 50 and salt 10387312. Record the selected resource-pack order and check Foundation and Continuity against representative models.

The [1.12 testing procedure](https://github.com/Resivore/Mynx-Flavoured-Workbench/blob/c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd/projects/matcha-flavoured-data/TESTING.md) retains the full previous release checklist and historical evidence. Reuse its unaffected controls alongside the cases above.
