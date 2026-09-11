# Testing

C4 is ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED. It has not occupied a Test Instance Manager slot and no runtime result is implied by the build. Test only in the dedicated Matcha Flavoured Minecraft Java 26.2 Workbench; never use the protected 26.1.2 gameplay instance.

## 1. Install and startup gate

1. Place `inventory-particles-matcha-compat-0.1.0-canary4.jar` in the dedicated 26.2 Workbench `mods` directory beside Inventory Particles 2.6.0+26.2+fabric and the ordinary Matcha/Ribbits stack. Do not enable C1-C3 ZIPs: they are failed immutable historical provenance, not C4 inputs.
2. Start or run `/reload`; wait for Inventory Particles configuration registration and VANILLA/MODDED linking to finish.
3. Confirm there is no resource reload failure, Inventory Particles parse failure, failed family link, missing-Mixin target crash, or selected resource-pack-list change. Record the reported particle-config and family totals.

Stop and retain the relevant log if the gate fails. Do not continue to visual rows after a reload/startup failure.

## 2. Exact stack acquisition

- Green Curry canonical: run `/recipe give @s food:green_curry`, then smelt a `minecraft:zombified_piglin_spawn_egg`. Particle-only surrogate: `/give @s minecraft:poisonous_potato[minecraft:item_model="minecraft:green_curry"]`.
- Ramen canonical: run `/recipe give @s food:ramen`, then smelt a `minecraft:wither_skeleton_spawn_egg`. Particle-only surrogate: `/give @s minecraft:poisonous_potato[minecraft:item_model="minecraft:ramen"]`.
- Crystal Heart canonical: `/loot give @s loot minecraft:kleis_items/crystal_heart`. Particle-only surrogate: `/give @s minecraft:poisonous_potato[minecraft:item_model="minecraft:heart_container"]`.
- Glowcap: `/give @s ribbits:glowcap`. Toadstool Heart: `/give @s ribbits:toadstool_heart`.
- Ribbit Village Explorer Map: obtain it through the canonical Wandering Ribbit search/offer so it has `minecraft:filled_map`, `minecraft:map_id`, and `minecraft:custom_data.ribbits:ribbit_village_explorer_map=true`. A handcrafted partial stack is not the canonical test case.

The three Matcha surrogates deliberately carry only the selection component; they are not complete gameplay-equivalent stacks.

## 3. C4 visual matrix

Report every row separately. A target must show its compat visual only; ordinary Inventory Particles spawners must not accumulate on it.

| Stack or context | Expected C4 behavior |
| --- | --- |
| Ordinary `minecraft:poisonous_potato` | Normal upstream behavior remains available. |
| Green Curry | Green-tinted bowl particles only; no poisonous-potato poison/sand leakage. |
| Ramen | Golden bowl particles only; no poisonous-potato poison/sand leakage. |
| Crystal Heart | Red heart-sherd-style particles only; no poisonous-potato poison/sand leakage. |
| Glowcap | Visible Glowcap particles only; no blank/default accumulation. |
| Toadstool Heart | Visible Toadstool Heart particles only. |
| Ribbit Village Explorer Map | Its map particle only; no gray, sand, or generic-filled-map fallback. |
| Unaffected vanilla item, for example `minecraft:diamond` | Existing Inventory Particles behavior unchanged. |
| Unrelated automatic/family item, for example `ribbits:swamp_daisy` | Existing automatic/family behavior unchanged. |

For at least Green Curry and the Ribbit Village Explorer Map, verify the exclusive result in a normal slot, a hovered slot, cursor particles, and a GUI action if practical. C4 gates the exact `ParticleSpawner.tickAndSpawn`, `spawn`, and `spawnFromCursor` routes used by all four audited renderer contexts.
