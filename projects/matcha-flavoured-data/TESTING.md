# Testing

## Exact candidate

Canary 1 packages the exact server `data/` tree and exact `pack.mcmeta` from
`Matcha_Flavoured_1_12.zip` (12,248,989 bytes, SHA-256
`6209783021c358044abedabacee471faff5bd4080437d4e3b5e51963f1804248`) as the
Fabric built-in pack `matcha_flavoured_data:matcha_flavoured_1_12`. It is
registered with `PackActivationType.ALWAYS_ENABLED`; no client `assets/` are
in this mod. The original zip remains the separate client resource pack.

This candidate is not deployed and has no runtime result. Use only an
explicitly allocated Test Slot in the dedicated Matcha Flavoured 26.2
Workbench. Never access the protected 26.1.2 gameplay instance.

## Required runtime matrix

1. With this exact JAR and Matcha Frost Protection installed, create a fresh
   singleplayer world with no Matcha world datapack selected. The Create World
   screen must pass `Preparing for world creation...`, open normally, and
   create the world. Confirm `food:glow_berry_crumble`,
   `blessings:frost_walker_frost_protection`,
   `main:environmental/check_freezing_water_conditions`, and the expected
   Frost Protection-modified Blessing of Demeter result are available.
2. Repeat from a fresh world without Matcha Frost Protection. World creation
   must still complete and the same embedded Matcha data must be present.
3. Back up an existing disposable test world that enables the exact legacy
   `Matcha_Flavoured_1_12.zip`, then load it with this JAR also installed.
   Confirm representative recipes, functions, loot, and tags are unchanged
   and no duplicate behavior occurs. The static package test proves both
   layers are byte-identical and tag membership is idempotent; this step is
   still required before a runtime PASS.

Stop and record `FAIL` or `INCONCLUSIVE` for any reload/world-creation error,
missing resource, altered Frost Protection result, tag-content drift, or
duplicate behavior. Do not edit world files or enable/disable the legacy pack
automatically.

## Preferred legacy-world migration

After a successful backup and explicit verification that the embedded data is
working, disable or remove the old world-scoped Matcha datapack so the world
has one Matcha server-data source. Keep the original zip available as the
client resource pack. This is a user-directed cleanup, never an automated
world mutation by this mod.
