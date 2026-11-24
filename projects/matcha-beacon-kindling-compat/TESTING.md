# Testing

## Matcha 1.12.5, Canary 2

`matcha-beacon-kindling-compat-0.2.0-canary2.jar`

SHA-256 `1c6115c3227e891de21ee5a8bbd59626810e4225ccc3babee258c8cbdb7d9edd`

Source `e3a13df63e041949a8341a025ac4d937ebacdf83`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 26 tests. After building the eleven coordinated mods, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_migration.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v13.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. With two players and an unrelated wandering trader present, place Kindling on separate beacons. Check messages, marker ownership and one constructor per summon. Unload one location, disconnect its owner, wait and resume. Check saved countdowns after a restart.
2. Allow one summon to expire and remove the other beacon. Check marker and trader cleanup only for the affected record. Exercise the no-beacons reset, repeated ticks and reload. Verify the unrelated trader survives.

The [1.12 testing procedure](https://github.com/Resivore/Mynx-Flavoured-Workbench/blob/c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd/projects/matcha-beacon-kindling-compat/TESTING.md) retains the full previous release checklist and historical evidence. Reuse its unaffected controls alongside the cases above.
