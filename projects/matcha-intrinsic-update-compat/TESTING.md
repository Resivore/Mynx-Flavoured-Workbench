# Testing

## Matcha 1.12.5 Stage 1, Canary 1

`matcha-intrinsic-update-compat-0.1.0-canary1.jar`

SHA-256 `3ab950e214252adb89364bab563c14d3c500a2ad75844773555b98b7f78026ef`

Source `e3a13df63e041949a8341a025ac4d937ebacdf83`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 1 test. After building the ten coordinated projects, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_stage1.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v12.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. For main hand, offhand and each armour slot, compare a freshly smithed item with a migrated item of the same type. Seed an intrinsic below and above its minimum and an unrelated enchantment. Verify the higher level and unrelated enchantment survive in the target slot, the other slots stay intact and the smithing marker is consumed as upstream specifies.
2. Repeat the updater and smithing processor after reload, alternate players and slots, and compare an empty/unenchanted item after an enchanted one. Check no stale held scratch storage leaks, no unknown functions appear, and equipment without these intrinsic update calls keeps upstream behavior.
