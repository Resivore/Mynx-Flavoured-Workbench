# Testing

## Matcha 1.12.5, Canary 12

`matcha-heart-death-compat-0.2.0-canary12.jar`

SHA-256 `3ea8384b403a4048ce72c6de711ad2afc3ffa361534e7fd7f176da3ce66f862e`

Source `e3a13df63e041949a8341a025ac4d937ebacdf83`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 47 tests. After building the eleven coordinated mods, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_migration.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v13.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. Open a backup copy with existing 5-, 9-, 10- and 30-heart players. Record base max health and attachment before login, after two reloads, death, respawn and reconnect. Death must cost one heart down to five. Reinforced Hearts may repair only below ten, and Crystal Hearts may increase only from ten to thirty. Check stack counts on allowed, rejected, offhand and inventory-only use.
2. Exercise structure, loot, updated and saved hearts in both hands at lethal damage, with an ordinary totem as a control. Trigger the Wither and seven progression milestones. Check regular rewards and effects, absence of independent maximum-health changes, approved crafting, Silk Touch drops, fishing and trial/Ancient City reward pools.

The [1.12 testing procedure](https://github.com/Resivore/Mynx-Flavoured-Workbench/blob/c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd/projects/matcha-heart-death-compat/TESTING.md) retains the full previous release checklist and historical evidence. Reuse its unaffected controls alongside the cases above.
