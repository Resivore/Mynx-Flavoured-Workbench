# Testing

C3 is ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED. Use only the dedicated Matcha Flavoured 26.2 Workbench; never use the protected 26.1.2 gameplay instance. C2 is historical RUNTIME_FAIL / RESOURCE_RELOAD_FAIL and must not be installed: its invalid this_type:number condition made Minecraft remove the selected resource-pack list.

## 1. Mandatory resource-reload safety gate

Before inspecting any particle visual, enable inventory-particles-matcha-compat-c3.zip above Inventory Particles, Matcha, and Ribbits resources in the dedicated 26.2 Workbench, then:

1. Record the selected resource-pack list and its ordering.
2. Run /reload (or restart) and wait for the Inventory Particles reload/linking pass to finish.
3. Confirm the reload completes normally.
4. Confirm Inventory Particles reports zero failed particle configs and zero failed family configs.
5. Confirm the fallback family is present and accepted.
6. Confirm the selected resource-pack list and ordering are unchanged.

Stop immediately and retain the relevant log if any config/family parse failure, fallback-family failure, resource-reload failure, or resource-pack-list change occurs. Do not proceed to visual testing after a reload-gate failure.

## 2. Visual checklist after the gate passes

| Stack | Expected C3 behavior |
| --- | --- |
| Ordinary minecraft:poisonous_potato | Unchanged upstream poison and sand behavior. |
| Green Curry | Green-tinted bowl particle only; no poisonous-potato poison/sand leak. |
| Ramen | Golden bowl particle only; no poisonous-potato poison/sand leak. |
| Crystal Heart | Red heart-sherd-style particle only; no poisonous-potato poison/sand leak. |
| Glowcap | Visible Glowcap texture only. |
| Toadstool Heart | Isolated Toadstool Heart texture only. |
| Ribbit Village Explorer Map | Its own map texture only; no gray/sand/fallback particle. |
| Unaffected vanilla item, for example minecraft:diamond | Existing behavior unchanged. |
| Unrelated automatic/family item, for example ribbits:swamp_daisy | Inventory Particles automatic/family behavior remains available. |

Report every row independently. Stop and report any missing particle, retained poisonous-potato particle, wrong Ribbits texture, or regression on the ordinary potato or controls.

## Exact stack acquisition

- Green Curry canonical: run /recipe give @s food:green_curry, then smelt a minecraft:zombified_piglin_spawn_egg. Particle-only surrogate: /give @s minecraft:poisonous_potato[minecraft:item_model="minecraft:green_curry"].
- Ramen canonical: run /recipe give @s food:ramen, then smelt a minecraft:wither_skeleton_spawn_egg. Particle-only surrogate: /give @s minecraft:poisonous_potato[minecraft:item_model="minecraft:ramen"].
- Crystal Heart canonical: /loot give @s loot minecraft:kleis_items/crystal_heart. Particle-only surrogate: /give @s minecraft:poisonous_potato[minecraft:item_model="minecraft:heart_container"].
- Glowcap: /give @s ribbits:glowcap. Toadstool Heart: /give @s ribbits:toadstool_heart. Obtain the Ribbit Village Explorer Map from its canonical Wandering Ribbit search/offer so it carries minecraft:map_id and minecraft:custom_data.ribbits:ribbit_village_explorer_map=true.

The three surrogates deliberately contain only the component C3 matches; they are not complete gameplay-equivalent Matcha stacks. C1 remains a partial external runtime failure: Green Curry leaked poisonous-potato particles, Glowcap emitted nothing, and the Village Explorer Map showed gray/sand-like particles; Ramen and Crystal Heart were not tested, and no result was supplied for Toadstool Heart.
