# Testing

## Matcha 1.12.5, Canary 3

`matcha-frost-protection-0.2.0-canary3.jar`

SHA-256 `d13bb3122a35f8c7bb573057582b69904accdbaf367e49255cb7c831637b572b`

Source `e3a13df63e041949a8341a025ac4d937ebacdf83`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 26 tests. After building the eleven coordinated mods, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_migration.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v13.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. For head, chest, legs and feet separately, test Frost Protection I, II and III in powder snow. Compare traversal, sinking, freeze ticks, overlay and damage with leather boots and bare equipment. Test mixed slots and players standing together. Repeat with the saved main:freezing_protection registry entry before the upstream item updater runs.
2. In frozen-biome water compare protected and unprotected players side by side over multiple penalty ticks. Any single level III piece must suppress all cold-water penalties for its wearer. Check creative and spectator exclusion. Craft the Demeter book before and after reload and verify only Frost Protection III.

The [1.12 testing procedure](https://github.com/Resivore/Mynx-Flavoured-Workbench/blob/c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd/projects/matcha-frost-protection/TESTING.md) retains the full previous release checklist and historical evidence. Reuse its unaffected controls alongside the cases above.
