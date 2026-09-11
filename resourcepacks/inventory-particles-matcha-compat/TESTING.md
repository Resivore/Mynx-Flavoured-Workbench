# Testing

C2 is ACTIVE, not deployed, and runtime untested. Install `inventory-particles-matcha-compat-c2.zip` above Inventory Particles, Matcha, and Ribbits resources in the dedicated Matcha Flavoured 26.2 Workbench only; run `/reload` (or restart) and wait for Inventory Particles to finish its client reload/linking pass. Do not use the protected 26.1.2 gameplay instance.

## Exact stack acquisition

- Green Curry canonical: run `/recipe give @s food:green_curry`, then smelt a `minecraft:zombified_piglin_spawn_egg`; the authoritative `food:green_curry` recipe creates the canonical `minecraft:poisonous_potato` stack. Particle-only surrogate: `/give @s minecraft:poisonous_potato[minecraft:item_model="minecraft:green_curry"]`.
- Ramen canonical: run `/recipe give @s food:ramen`, then smelt a `minecraft:wither_skeleton_spawn_egg`; the authoritative `food:ramen` recipe creates the canonical `minecraft:poisonous_potato` stack. Particle-only surrogate: `/give @s minecraft:poisonous_potato[minecraft:item_model="minecraft:ramen"]`.
- Crystal Heart canonical: `/loot give @s loot minecraft:kleis_items/crystal_heart`. Particle-only surrogate: `/give @s minecraft:poisonous_potato[minecraft:item_model="minecraft:heart_container"]`.
- Glowcap: `/give @s ribbits:glowcap`; Toadstool Heart: `/give @s ribbits:toadstool_heart`. Obtain the Ribbit Village Explorer Map from its canonical Wandering Ribbit search/offer so it carries both `minecraft:map_id` and custom-data marker `ribbits:ribbit_village_explorer_map=true`.

The three surrogates deliberately contain only the component C2 matches; they are not complete gameplay-equivalent Matcha stacks.

## C2 checklist

| Stack | Expected C2 behavior |
| --- | --- |
| Ordinary `minecraft:poisonous_potato` | Unchanged upstream poison and sand behavior. |
| Green Curry | Green-tinted bowl particle only; no poisonous-potato poison/sand leak. |
| Ramen | Golden bowl particle only; no poisonous-potato poison/sand leak. |
| Crystal Heart | Red heart-sherd-style particle only; no poisonous-potato poison/sand leak. |
| Glowcap | Visible Glowcap texture only. |
| Toadstool Heart | Isolated Toadstool Heart texture only. C1 supplied no runtime result; record the observed C2 result. |
| Ribbit Village Explorer Map | Its own map texture only; no gray/sand/fallback particle. |
| Unaffected vanilla item, e.g. `/give @s minecraft:diamond` | Existing behavior unchanged. |
| Unrelated automatic/family item, e.g. `/give @s ribbits:swamp_daisy` | Inventory Particles' normal automatic/family behavior remains available. |

Report each row independently. Stop and report any missing particle, retained poisonous-potato particle, wrong Ribbits texture, or regression on the ordinary potato/controls. C1 is already user-reported partial RUNTIME_FAIL: Green Curry, Glowcap, and Village Explorer Map failed; Ramen and Crystal Heart were not tested; Toadstool Heart received no reported result.
