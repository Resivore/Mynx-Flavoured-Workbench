# Testing

## Current gate

**CANARY 2 RETAINED — READY FOR FUTURE CONTROLLED SLOT ALLOCATION; NOT DEPLOYED / RUNTIME UNTESTED**

Use exact `matcha-vanilla-village-restoration-0.2.0-canary2.jar`, 10,544 bytes, SHA-256 `5d5164a89881239400cd2b6776b9b54177b699e2a16a1c97b761cde06e7d8bcd`.

Required identities are Minecraft `26.2`, Fabric Loader `0.19.3` or newer, Fabric API `0.157.0+26.2` or newer, mod ID `matcha_vanilla_village_restoration`, and external `Matcha_Flavoured_1_12.zip` SHA-256 `6209783021c358044abedabacee471faff5bd4080437d4e3b5e51963f1804248`. The retained C1 ZIP and its aggregate runtime pass are historical predecessor evidence only; they are not C2 runtime evidence.

## Future managed runtime test

1. When a canonical Test Slot is available, deploy this exact JAR through a verified serialized Test Instance Manager transition while preserving the other slot. Stop on any artifact, dependency, ownership, or slot mismatch.
2. Launch the Minecraft 26.2 Matcha stack and confirm the mod registers `matcha_vanilla_village_restoration:vanilla_villages` as `ALWAYS_ENABLED`, promotes that exact pack above lower packs when necessary, and produces no Fabric, Mixin, resource-reload, registry, or worldgen errors.
3. In a fresh world or wholly untouched terrain, generate new villages across representative plains, desert, savanna, snowy-plains, and taiga-family biomes. Do not use already-generated village blocks as evidence.
4. Confirm each new village uses its normal vanilla 26.2 biome-specific composition rather than Matcha's replacement composition, while the exact Matcha biome eligibility arrays remain effective.
5. Confirm Matcha remains the only owner of `minecraft:villages` and its sparse random-spread placement contract (spacing 80, separation 50, salt 10387312) remains effective; C2 must not supply a structure-set override.
6. Recheck representative cases after save/reload and, if acceptance requires it, on both client and dedicated-server authority paths.

Pass only when C2's precedence over Matcha and all five representative compositions are directly demonstrated against the exact identities above. Record `RUNTIME_FAIL` or `INCONCLUSIVE` for any inability to prove precedence, wrong or replacement composition, missing style, relevant load/worldgen error, placement-ownership drift, or client/server disagreement.

The retained 80/50/10387312 candidate lattice does not guarantee seed-identical realized starts at eligible-biome boundaries because the restored vanilla town-center anchor can affect the jigsaw biome check.
