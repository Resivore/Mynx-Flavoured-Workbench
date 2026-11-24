# Testing

## Matcha 1.12.5, Canary 28

`ribbits-private-reconstruction-4.1.6+26.2-matcha125-menu-canary28.jar`

SHA-256 `9e462df87c704542fa0f85d97d16857823c59b4192f2b03a4c333f3af3b91236`

Source `1ce48a4c2b288c0a18d71e625531e56b618b7ae7`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 178 Java tests and 55 private-tool tests. After building the eleven coordinated mods, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_migration.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v13.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. Open saved merchants and generate fresh merchants at every existing tier. Compare exact inputs, outputs, prices and restock behavior, including foods, Opal, four compass buybacks and Crystal Heart. Check a renamed, generic or wrong-component substitute is rejected wherever strict matching applies.
2. Craft the Toadstool Heart with both the saved and migrated canonical Crystal Heart and five foliage items. Reject Reinforced Hearts and ordinary potatoes. Verify recipe-book/JEI output, existing saved offers and current offers across reconnect, reload and server restart. Recheck the established private reconstruction boundaries.

The [1.12 testing procedure](https://github.com/Resivore/Mynx-Flavoured-Workbench/blob/c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd/projects/ribbits-26.2/TESTING.md) retains the full previous release checklist and historical evidence. Reuse its unaffected controls alongside the cases above.

Exercise all three Sorcerer ranks at 0, 5 and 30 XP. Load old Flask Sages below and at the Oracle threshold, a saved Oracle and both gate states. Verify the retired potion offer disappears and other offers retain their uses, ordering and restock state. Exercise all five Chef pools, same-day stability, next-day rollover and the saved Master specialty.
