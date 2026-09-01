# Testing

## Accepted identity and evidence

**CANARY 2 ACCEPTED — USER-REPORTED EXTERNAL AGGREGATE PASS**

The accepted Workbench Stack v10 identity is exact
`matcha-vanilla-village-restoration-0.2.0-canary2.jar`, 10,544 bytes,
SHA-256 `5d5164a89881239400cd2b6776b9b54177b699e2a16a1c97b761cde06e7d8bcd`,
from source checkpoint `ee6070995b036128e6a7d281a6c67ae3f74b9284`.

The user reported an aggregate external runtime `PASS` for that exact current
canonical candidate. No individual procedure-row observations were supplied,
so none are inferred. The accepted-stack manager installed and physically
verified the artifact as accepted deployment
`f2a6a569-f3c4-43d1-8b35-c7b03dbb247f`; that accepted-baseline action is not
a Test Slot deployment and creates no Test Slot history. Retained Canary 1 and
its earlier aggregate result remain historical predecessor evidence only.

Required identities are Minecraft `26.2`, Fabric Loader `0.19.3` or newer,
Fabric API `0.157.0+26.2` or newer, mod ID
`matcha_vanilla_village_restoration`, and external
`Matcha_Flavoured_1_12.zip` SHA-256
`6209783021c358044abedabacee471faff5bd4080437d4e3b5e51963f1804248`.

## Future regression procedure

1. Verify the exact accepted version, filename, SHA-256, source checkpoint,
   dependencies, and enabled stack before testing. Stop on any identity or
   ownership mismatch.
2. Launch the Minecraft 26.2 Matcha stack and confirm the mod registers
   `matcha_vanilla_village_restoration:vanilla_villages` as `ALWAYS_ENABLED`,
   promotes that exact pack above lower packs when necessary, and produces no
   Fabric, Mixin, resource-reload, registry, or worldgen errors.
3. In a fresh world or wholly untouched terrain, generate new villages across
   representative plains, desert, savanna, snowy-plains, and taiga-family
   biomes. Do not use already-generated village blocks as evidence.
4. Confirm each new village uses its normal vanilla 26.2 biome-specific
   composition rather than Matcha's replacement composition, while the exact
   Matcha biome eligibility arrays remain effective.
5. Confirm Matcha remains the only owner of `minecraft:villages` and its sparse
   random-spread placement contract (spacing 80, separation 50, salt 10387312)
   remains effective; Canary 2 must not supply a structure-set override.
6. Recheck representative cases after save/reload and, if the regression scope
   requires it, on both client and dedicated-server authority paths.

Record only observations actually made. Record `FAIL` or `INCONCLUSIVE` for
any inability to prove precedence, wrong or replacement composition, missing
style, relevant load/worldgen error, placement-ownership drift, or
client/server disagreement.

The retained 80/50/10387312 candidate lattice does not guarantee seed-identical
realized starts at eligible-biome boundaries because the restored vanilla
town-center anchor can affect the jigsaw biome check.
