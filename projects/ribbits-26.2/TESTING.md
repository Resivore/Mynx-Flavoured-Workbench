# Testing

## Matcha 1.12.5 Stage 1, Canary 28

`ribbits-private-reconstruction-4.1.6+26.2-matcha125-canary28.jar`

SHA-256 `0490d604ef8e14afe0cb89a80199fa937c0e98e9c4e6808065ffae6ca6edd740`

Source `e3a13df63e041949a8341a025ac4d937ebacdf83`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 177 tests. After building the ten coordinated projects, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_stage1.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v12.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. Open saved merchants and generate fresh merchants at every existing tier. Compare exact inputs, outputs, prices and restock behavior, including Estus Flask potion, foods, Opal, four compass buybacks and Crystal Heart. Check a renamed, generic or wrong-component substitute is rejected wherever strict matching applies.
2. Craft the Toadstool Heart with both the saved and migrated canonical Crystal Heart and five foliage items. Reject Reinforced Hearts and ordinary potatoes. Verify recipe-book/JEI output, existing saved offers and current offers across reconnect, reload and server restart. Recheck the established private reconstruction boundaries.

The [1.12 testing procedure](https://github.com/Resivore/Mynx-Flavoured-Workbench/blob/c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd/projects/ribbits-26.2/TESTING.md) retains the full previous release checklist and historical evidence. Reuse its unaffected controls alongside the cases above.
