# Testing

## Matcha 1.12.5 Stage 1, Canary 5

`inventory-particles-matcha-compat-0.2.0-canary5.jar`

SHA-256 `4cff7167fe263d698f02d9e3261a88d016657907d7c04a04745861f3a70ce321`

Source `e3a13df63e041949a8341a025ac4d937ebacdf83`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 8 tests. After building the ten coordinated projects, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_stage1.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v12.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. Compare saved and current Green Curry, Ramen and Crystal Heart items alongside an ordinary poisonous potato. Check the exclusive animation, colour and particle timing for each item. Confirm selecting another item restores its normal Inventory Particles behavior.
2. Exercise the remaining approved holders, vanilla controls, inventory reopen and resource reload. Inspect a multiplayer client with the companion installed and the dedicated server without the client-only mod.

The [1.12 testing procedure](https://github.com/Resivore/Mynx-Flavoured-Workbench/blob/c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd/projects/inventory-particles-matcha-compat/TESTING.md) retains the full previous release checklist and historical evidence. Reuse its unaffected controls alongside the cases above.
