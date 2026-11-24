# Testing

## Matcha 1.12.5 Stage 1, Canary 6

`enderscape-integration-0.2.0-canary6.jar`

SHA-256 `72934f737240c491757ea1a6af08aef506efeef980682b5c3a8bbe27df14824b`

Source `e3a13df63e041949a8341a025ac4d937ebacdf83`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 30 tests. After building the ten coordinated projects, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_stage1.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v12.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. Check selected equipment and Rubble Chitin against loot, crafting, advancements, Creative and JEI. Compare each affected food effect, count, loot roll and entry weight to the existing procedure. Check the enchanted golden apple carrier and components. Open an existing container containing older items.
2. Craft Void Campfire with current and saved Kindling plus Void Shale, reject an ordinary chicken egg, and compare its JEI display. Test Veiled Sapling crafting, passive drops, named terrain blocks and Veiled Leaves with and without optional shaders before and after reload.

The [1.12 testing procedure](https://github.com/Resivore/Mynx-Flavoured-Workbench/blob/c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd/projects/enderscape-integration/TESTING.md) retains the full previous release checklist and historical evidence. Reuse its unaffected controls alongside the cases above.
