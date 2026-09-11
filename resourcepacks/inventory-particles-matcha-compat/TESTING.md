# Testing

C1 is ACTIVE, not deployed, and runtime untested. Install `inventory-particles-matcha-compat-c1.zip` above Inventory Particles, Matcha, and Ribbits resources in the dedicated Matcha Flavoured 26.2 Workbench only; use `/reload` (or restart) and wait for Inventory Particles to finish its client reload/linking pass before inspecting slots.

| Item/stack | Registry ID | Distinguishing stack data | Current rule/problem | C1 rule | Status |
| --- | --- | --- | --- | --- | --- |
| Ordinary poisonous potato | `minecraft:poisonous_potato` | no `minecraft:item_model` among C1 values | built-in poison + sand | unchanged built-in poison + sand | implemented guard |
| Green Curry | `minecraft:poisonous_potato` | `minecraft:item_model=minecraft:green_curry` | poison + sand leak from backing ID | green-tinted bowl particle | implemented |
| Ramen | `minecraft:poisonous_potato` | `minecraft:item_model=minecraft:ramen` | poison + sand leak from backing ID | golden bowl particle | implemented |
| Crystal Heart | `minecraft:poisonous_potato` | `minecraft:item_model=minecraft:heart_container` | poison + sand leak from backing ID | red heart-sherd particle | implemented |
| Glowcap | `ribbits:glowcap` | none | automatic family/fallback can be generic | own default-item texture | implemented |
| Toadstool Heart | `ribbits:toadstool_heart` | none | automatic family/fallback can be generic | own default-item texture | implemented |
| Ribbit Village Explorer Map | `ribbits:ribbit_village_explorer_map` | none | automatic family/fallback can be generic | own default-item texture | implemented |

Check each listed C1 item, then an unaffected vanilla item and an unrelated modded item that previously received an automatic Inventory Particles family rule. Stop and report any missing particle, retained green poison/sand on a C1 Matcha stack, wrong item texture, or an ordinary poisonous potato losing its normal behavior. Food variants beyond Green Curry/Ramen are intentionally deferred until their visual choices are reviewed.
