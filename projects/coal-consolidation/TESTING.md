# Testing

## Matcha 1.12.5 Stage 1, Canary 4

`coal-consolidation-0.2.0-canary4.jar`

SHA-256 `35cd9e9271b1000801b45816ab3c57bae34f792d61666e6c63f76567d3c49ec8`

Source `e3a13df63e041949a8341a025ac4d937ebacdf83`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 19 tests. After building the ten coordinated projects, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_stage1.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v12.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. Cook one burnable log in a furnace and smoker. Expect one coal after 200 and 100 ticks respectively and the original experience. Check all seven explicit consumers, the unchanged coals tag consumers and one-to-one legacy charcoal conversion.
2. Check Creative Ingredients, Search and a compatible recipe viewer. Repeat the recipe checks after reload with the Matcha pack present, including upstream-filtered vanilla recipe IDs.

The [1.12 testing procedure](https://github.com/Resivore/Mynx-Flavoured-Workbench/blob/c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd/projects/coal-consolidation/TESTING.md) retains the full previous release checklist and historical evidence. Reuse its unaffected controls alongside the cases above.
