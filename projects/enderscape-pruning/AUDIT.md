# Enderscape 3.0.2 Pruning Audit

This is a static, audit-only decision record. It does not remove, hide, replace, or rebalance anything; it creates no runtime artifact and supplies no Minecraft runtime evidence. “Safe” below means the narrowest recommendation supported by the exact inspected code/data, still subject to the future release-bound tests in `TESTING.md`.

## 1. Exact source identity

| Field | Exact value |
|---|---|
| Installed input | `originals/mods/enderscape-fabric-3.0.2+mc26.2.jar` |
| SHA-256 | `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b` |
| Mod ID / embedded version | `enderscape` / `3.0.2` |
| Target | Minecraft Java `26.2`, Fabric Loader `>=0.19.3` |
| Declared runtime capabilities | Fabric API `>=0.154.2`, Lithostitched `>=1.7.13`, TrimPatcher `>=1.0-mc26.2`, YACL `>=3.9.5` |
| Audit checkpoint | Workbench `main` at `f6614d253d5ec8429f398a218be36f9d192caec0` |
| Static inspection | Exact jar extraction plus Vineflower `1.12.0` decompilation under Java `25`; extracted/decompiled upstream material remained ignored and is not committed |

The jar is authoritative. Its metadata points to the public Penumbra-MC/Enderscape repository, but this audit does not substitute a possibly drifting source branch for the installed bytecode and resources.

Coverage totals provide a check on scope, not runtime proof: 240 Enderscape loot tables (213 block, 5 End City, 1 End Haven, 3 entity, 1 Mirestone, 1 post-supplement, 4 Rubblemite, 1 shearing, 9 Stronghold, 2 supplement), 368 recipes, 382 advancements, 197 structure templates, 38 configured features, and 49 placed features. Registration includes 199 block items, 28 additional non-item blocks, 34 non-block items, three entity types (`drifter`, `rubblemite`, `rustle`), three block-entity types (`magnia_sprout`, `magnia_radio`, `end_haven_core`), two fluids, ten potions, seven biomes, and six structures. No villager or wandering-trader acquisition path was found; a structure template's zombie villager is not a trade registration.

## 2. Executive findings

1. **Suppress acquisition; retain registries.** Mirror, Dagger, Rubble Shield, and Shadoline armor can be removed from *future ordinary gameplay* without deleting their item registrations. Actual unregistration is the wrong first move for an existing-world pack: inventories, item frames, recipe books, advancements, component variants, enchantments, and legacy Rubble Shield aliases can retain those IDs.
2. **The candidates have different coupling.** Shadoline armor is the cleanest: four crafting recipes, recipe advancements, creative exposure, item tags, and recycling recipes. Dagger is moderately coupled to Mirestone loot, Stun Burst, Resonance, the stun trigger, and the fueled-tool system. Rubble Shield is moderately coupled to four component variants, four recipes, a dash mechanic, a statistic/advancement, JEI subtype identity, and four legacy aliases. Mirror is the most coupled: one item with 17 data-component identities, nine direct loot occurrences, dyeing, lodestone teleportation and dispenser behavior, two bespoke enchantments, Resonance, fuel/range rules, and a three-advancement chain.
3. **No candidate-specific Enderscape config switch exists.** Enderscape exposes only global creative-tab switches and coarse built-in pack toggles. Candidate-specific hiding/acquisition suppression therefore needs data overrides plus narrow compatibility code for programmatic creative/JEI exposure. A built-in pack cannot cleanly remove these four families.
4. **Keep all Enderscape effects and potions.** The registered Low Gravity and Void potion/effect families connect environment damage, voided health, mob exceptions, purification, resistance, totem behavior, brewing, structure loot, arrows, and lingering/splash transforms. The reversed removal decision is supported by the code.
5. **Matcha compatibility is component compatibility.** Every audited Enderscape vanilla-food loot entry emits a plain/default stack unless an unrelated potion/stew component is explicitly applied. Several same-ID Matcha stacks have materially different `food`, `consumable`, lore, model, stack-size, or effect components. Replacing only the item ID does nothing; nesting current Matcha helper loot tables can also alter Enderscape's count ranges. The safest later path is to preserve each Enderscape pool, condition, weight, roll, and count function while applying a source-authoritative Matcha component patch.
6. **Enderscape-native foods require owner balancing, not ID substitution.** Drift Jelly Bottle, Puruberry, Murublight Bracket, and Chorus Cake Roll all restore vanilla hunger; the cake bypasses item food components entirely through direct `FoodData.eat`. Preserve their Low Gravity/Void effects, bottle remainder, placeable/block behavior, final teleport, and mob-food relationships while the owner selects Matcha heart values.
7. **Magnia is a broad world/content system.** Base Magnia blocks propagate polarity-specific power; sprouts move entities; Blistered/Polarized/Nebulite/Void Torch blocks source or transform power; Magnia Fields and End City structures place Magnia content. The Magnia Attractor is mechanically independent at runtime from placed Magnia fields but crafted from sprouts and shares fuel, Bundling, and Resonance. Removing world Magnia registrations would be a high-risk worldgen/save migration, not ordinary pruning.
8. **Built-in packs are useful but coarse.** New End Cities, New Strongholds, New Terrain, three fixes/visual packs, and two supplement switches can disable or replace broad behavior. They do not isolate individual biomes, native foods, Magnia, or removal-candidate items. Pack disablement affects future data/world generation; it does not erase blocks or references in existing chunks.

## 3. Enderscape subsystem map

| Subsystem | Primary identities | Acquisition/world link | Important outward coupling | Audit disposition |
|---|---|---|---|---|
| End biomes | `celestial_grove`, `corrupt_barrens`, `magnia_fields`, `veiled_woodlands`, `void_depths`, `void_skies`, `void_sky_islands` | Lithostitched biome additions; New Terrain built-in pack alters End dimension/noise | Native plants/foods, Void ambience, Magnia features, structures | Retain; only coarse supported pack controls exist |
| Structures | `center_gateway`, `large_center_gateway`, `end_city`, `end_haven`, `mirestone_ruins`, `stronghold` | Structure sets, 197 templates, processors and loot markers | Candidate loot, food loot, sprouts, advancements | Retain; override only selected loot paths later |
| Mirror family | `enderscape:mirror` plus Dye Color component | Structure loot and dye-copy recipe | Lodestone teleport, dispenser, fuel, Resonance, Transdimensional, advancements | Suppress acquisition/discovery; retain registry/code |
| Dagger | `enderscape:dagger` | Crafting and Mirestone loot | Backstab/stun, fuel, Stun Burst, Resonance, advancement | Suppress acquisition/discovery; retain registry/code |
| Rubble Shield | `enderscape:rubble_shield` plus four Variant values | Four recipes | Blocking/dash, durability, statistic, advancement, JEI subtype, old aliases | Suppress recipes/discovery; retain item/components/aliases |
| Shadoline armor | four armor item IDs | Four equipment recipes; recycling to nugget | Stealth attributes, armor tags, recipe unlocks | Simplest suppression; retain items and consider keeping recovery smelting |
| Nebulite tool loop | Mirror, Dagger, Magnia Attractor | Nebulite fuel and tool tags | Resonance affects all three; Attractor remains useful | Narrow tags only after owner decision |
| Enchantments | Bundling, Stun Burst, Rebound, Resonance, Transdimensional | Enchanting tags and explicit structure books | Candidate tools, elytra, bundle, Attractor | Keep Bundling/Rebound; keep Resonance; suppress obsolete acquisition but retain registries for the other two |
| Void survival loop | Void Corruption, Purification, Resistance; potions/arrows | Environment, native food, brewing, loot, totem | Voided health, custom damage, tagged mob healing/harm | Keep intact |
| Native foods | Drift Jelly Bottle, Puruberry, Murublight Bracket, Chorus Cake Roll | Crafting, plants/worldgen, mob interaction, loot | Low Gravity/Void effects, bottles, blocks, Drifter/Rustle | Matcha design decision required |
| Vanilla-food loot | bread, carrot, golden foods, chorus fruit, cod, potatoes, honey, stew | End City/End Haven/Mirestone/Stronghold tables | Matcha exact components and discovery | Patch components without changing loot math |
| Magnia world system | base/etched/sprout/blistered/polarized Magnia | Magnia Fields, recipes, End City processors/templates | Entity motion, redstone-like power, Attractor ingredients | Retain unless owner accepts migration and dead worldgen |
| Built-in packs | six named packs | Config-default registration | End structure sets/loot, dimension/noise, visuals/fixes | Supported coarse modularity only |

## 4. Proposed-removal dependency matrix

### Summary

| Candidate | Registry/component shape | Coupling | Existing-world risk if unregistered | Safest future strategy |
|---|---|---|---|---|
| Mirror | One item ID; default plus 16 `enderscape:dye_color` component variants | High | High | Remove loot/dyeing/advancement acquisition, hide exact variants with narrow code, retain item/components/teleport code |
| Dagger | One item ID | Medium | Medium/high | Remove recipe and Mirestone loot, suppress related advancement/discovery, retain item and stun/fuel code for legacy stacks |
| Rubble Shield | One item ID; four `enderscape:rubble_shield/variant` values | Medium | High because legacy IDs alias to it | Remove four recipes/unlocks and discovery, retain current item/component and aliases; preserve legacy usability or define migration |
| Shadoline armor | Four item IDs | Low/medium | Medium | Remove four equipment recipes/unlocks and discovery; retain registries/attributes; owner decides whether recycling remains as legacy recovery |

### Mirror

- **Registry and variants:** `enderscape:mirror`; stack size 1, rare, enchantable value 1. The default component identity and 16 dye-color identities share this one registry item through `enderscape:dye_color`; there are not 17 item IDs.
- **Production/consumption recipes:** there is no base crafting recipe. Custom `enderscape:mirror_dying` takes a Mirror plus dye, copies the Mirror, and sets its Dye Color. No recipe consumes the output as an ingredient.
- **Loot acquisition:** nine direct sources: both End City vault tables, seven Stronghold/supplement paths listed in section 9. This is its only normal base acquisition in the exact jar.
- **Advancements:** `mirror_teleport` leads through `long_distance` to `transdimensional`; these triggers expect functional mirror teleports. Removing acquisition while leaving completed historical progress is harmless; deleting IDs/triggers is unnecessary risk.
- **Enchantments/tags:** Resonance extends maximum range by 1,250 blocks per level; Transdimensional permits cross-dimension travel. The item is in `c:tools`, `enderscape:enchantable/mirror`, `enderscape:nebulite_tools`, `enderscape:void_immune`, `enderscape:cannot_combine_in_anvil`, and vanilla vanishing-enchantability tags.
- **Mechanics:** it stores a lodestone teleport target, has base range 2,500, consumes more fuel with distance, uses the common Fueled Tool component with capacity 5, and rejects cross-dimension travel without Transdimensional. It can teleport the user or an entity through its registered dispenser behavior; target size is capped at 4 by 3 blocks.
- **Exposure:** default and 16 color variants enter Enderscape's tab; the same set can be programmatically added to vanilla Tools when the global config permits. Enderscape's JEI plugin registers a Dye Color subtype. Tooltip configuration controls dimension, coordinates, distance, and Shift-detail presentation but does not disable the item.
- **Structures/worldgen:** the item is loot only; no candidate ID occurs in any of the 197 structure NBT templates. Removing it does not justify removing structures.
- **Config/code assumptions:** no Mirror-specific enable/disable switch exists. Multiple item, component, dispenser, tooltip, fuel, enchantment, criterion, loot, and creative branches assume the registered item exists.
- **Collateral:** unregistering would strand colored, fueled, enchanted, or lodestone-bound legacy stacks and make Resonance/Transdimensional references unsafe. Removing Resonance with it would also damage the retained Magnia Attractor.
- **Recommendation:** use data overrides to remove the nine loot entries, the dyeing recipe, and future advancement visibility/unlocks as approved; use narrow compatibility code to filter all 17 exact discovery identities from programmatic tabs/JEI. Retain the item registration, data components, teleport/dispenser implementation, tags needed to decode legacy stacks, and enchantment registrations. This makes the Mirror unobtainable without turning old stacks into unknown data.

### Dagger

- **Registry:** `enderscape:dagger`; stack size 1, enchantability 15. Base attributes are +3 attack damage, +3 backstab damage, and -1.5 attack speed; minimum charged use is 1.
- **Production/consumption:** shaped recipe `" X" / "XX" / "S "` uses three `enderscape:shadoline_ingot` and one stick. No recipe consumes the Dagger. Mirestone Ruins also supplies it.
- **Loot:** `enderscape:chests/mirestone_ruins`, pool 1, one roll, weight 1 of 10.
- **Advancements:** recipe unlock plus `stun_attack`. The latter becomes unreachable from new play if the Dagger is suppressed.
- **Enchantments/tags:** Stun Burst is Dagger-only. Resonance increases its counter threshold by 2 per level. Tags include `c:tools`, `c:tools/melee_weapon`, `enderscape:enchantable/dagger`, `enderscape:nebulite_tools`, `enderscape:void_immune`, and vanilla fire-aspect/melee/sharpness/vanishing enchantability groups.
- **Mechanics:** one point of weapon item damage, cobweb mining speed 7.5, a four-hit base counter, 110-degree backstab angle, common fuel capacity 5 with hidden fuel HUD, and a stun attack with a 60-tick base duration, seven-second cooldown, and 1.5 shield-disable factor. Stun Burst turns the attack into an area effect.
- **Exposure:** Enderscape and optionally vanilla creative tabs expose it; ordinary JEI recipe discovery follows the recipe. No Dagger-specific config exists.
- **Structures/worldgen:** only the Mirestone loot marker/table; no template embeds the item.
- **Collateral:** suppressing the Dagger makes Stun Burst and `stun_attack` new-play dead content. It does not make Shadoline material, Rubble Chitin, or Resonance globally dead. Resonance still affects the Magnia Attractor.
- **Recommendation:** override the shaped recipe, recipe advancement, Mirestone loot entry, and Dagger-specific advancement exposure; filter it from creative/JEI with narrow code. Keep the item, fuel/counter components, backstab/stun code, tags required by stored stacks, and enchantment registry identities.

### Rubble Shield

- **Registry and variants:** one item, `enderscape:rubble_shield`, with `enderscape:rubble_shield/variant` values `enderscape:end_stone`, `enderscape:mirestone`, `enderscape:veradite`, and `enderscape:kurodite`.
- **Production/consumption:** four grouped shaped recipes use `CSC / C#C / _C_`: five Rubble Chitin (`C`), one Shadoline Ingot (`S`), and the corresponding block (`#`). No recipe consumes a shield.
- **Loot:** none. No structure template contains it.
- **Advancements:** four recipe advancements plus `rubble_shield_dash`; a custom statistic also tracks the dash.
- **Enchantments/tags:** it uses ordinary durability/vanishing shield enchantability rather than a bespoke Enderscape enchantment. Tags include `c:tools`, `c:tools/shield`, `enderscape:void_immune`, and vanilla durability enchantability.
- **Mechanics:** durability 336, offhand equipment, Rubble Chitin repair, three-second cooldown, active blocking, and a dash. Dash requires ground/not-liquid and food above six unless creative, costs five durability on ground or two while gliding, adds four exhaustion, and maintains a three-second attachment/state.
- **Exposure:** all four component variants are programmatically exposed; the JEI plugin registers the Variant component as a subtype. The global tab switches cannot hide only shields.
- **Compatibility aliases:** old registry IDs `enderscape:end_stone_rubble_shield`, `mirestone_rubble_shield`, `veradite_rubble_shield`, and `kurodite_rubble_shield` alias to the shared current item. This is strong evidence against deleting the current registration or component.
- **Collateral:** recipe suppression removes only new acquisition. Unregistering would threaten both current component stacks and older aliased saves.
- **Recommendation:** remove all four recipes and their unlock advancements, filter four variants from creative/JEI, and decide whether the dash advancement should be hidden while still accepting legacy completion. Retain item/component registrations, aliases, blocking/dash implementation, statistic/criterion decoding, and repair semantics for existing stacks.

### Shadoline armor

- **Registry:** `enderscape:shadoline_helmet`, `shadoline_chestplate`, `shadoline_leggings`, and `shadoline_boots`.
- **Production:** four shaped recipes use Shadoline Ingots (`X`) and Rubble Chitin (`C`): helmet `XXX / C_C`; chestplate `X_X / XXX / CCC`; leggings `XXX / X_X / C_C`; boots `C_C / X_X`.
- **Consumption/recovery:** each armor item can be smelted or blasted into one Shadoline Nugget. Those recycling recipes and their recipe advancements are the only downstream recipe use.
- **Loot:** none. No structure template contains any piece.
- **Advancements:** four equipment recipe unlocks and the related smelting/blasting unlock paths. `explore_end` uses Shadoline Boots only as its display icon, not as a completion criterion; an implementation should replace that icon if hiding the armor, not discard exploration progress.
- **Enchantments/tags:** normal armor enchantment behavior; repair ingredient Shadoline Ingot. Tags include `c:armors`, `c:armors/humanoid`, equipment-slot tags, vanilla trimmable armor, and `enderscape:void_immune`.
- **Mechanics:** armor values 2/6/5/2, durability factor 25, enchantability 15, no toughness or knockback resistance. Per-piece stealth multipliers are 0.15/0.20/0.20/0.15; the global attribute/mixin path reduces mob visibility and game-event detection. Those code paths should remain for legacy-equipped pieces.
- **Exposure/config/worldgen:** creative/recipe discovery only; no dedicated config, loot, block, entity, biome, feature, or template dependency.
- **Collateral:** material and Rubble Chitin remain useful elsewhere. Suppressing armor does not justify removing Shadoline ingots/nuggets/blocks. Removing the recycling recipes would prevent graceful recovery of legacy armor into retained material.
- **Recommendation:** remove the four equipment recipes/unlocks and creative/JEI exposure while retaining the four item registrations and armor/stealth code. Prefer retaining the smelting/blasting recovery recipes for old stacks unless the owner explicitly wants complete recipe-book absence; replace only the `explore_end` display icon.

## 5. Enchantment dependency matrix

| Enchantment | Exact availability | Supported item/function | Explicit acquisition/reference paths | Effect of candidate suppression | Recommendation |
|---|---|---|---|---|---|
| `enderscape:bundling` | Max 1, weight 4; enchanting-table and non-treasure tags | Magnia Attractor | Generic enchanting/tag-driven loot; Attractor deposits an attracted exact item/component match into an existing inventory bundle | None | Keep |
| `enderscape:stun_burst` | Max 2, weight 2; enchanting-table and non-treasure | Dagger only | Generic enchanting/tag-driven loot; Dagger AOE radius 4×2 at I and 8×4 at II; cooldown multiplier 1.5/3 and duration multiplier 0.5/1 | New-play function becomes dead if Dagger is unobtainable | Remove from future acquisition/tags if Dagger is suppressed; retain registry for old Daggers/books |
| `enderscape:rebound` | Max 1, weight 2; treasure | Elytra | Generic treasure/tag-driven loot; explicit End City vault/supplement book choices | No candidate dependency; adds 0.45 bounce strength | Keep |
| `enderscape:resonance` | Max 3, weight 10; enchanting-table and non-treasure | `#enderscape:nebulite_tools`: Mirror, Dagger, Magnia Attractor | Generic enchanting/tag-driven loot; explicit End City vault/supplement books | Loses Mirror range and Dagger threshold roles, but still adds 100 Attractor pull ticks per level | Keep. Owner may later narrow the supported-item tag, but must preserve Attractor support and legacy decoding |
| `enderscape:transdimensional` | Max 1, weight 1; treasure | Mirror only | Generic treasure/tag-driven loot; explicit Stronghold library/mansion/supplement books and End City vault/supplement book choices | New-play function becomes dead if Mirror is unobtainable | Remove from future acquisition if Mirror is suppressed; retain registry for old Mirrors/books |

The exact table tags put Bundling, Resonance, and Stun Burst in `minecraft:in_enchanting_table` and `minecraft:non_treasure`; Rebound and Transdimensional are treasure. Generic enchanted-book mechanisms can therefore reach these tags transitively even when no Enderscape loot JSON names the enchantment.

Explicit structure paths matter:

- End City vault, elytra vault, and corresponding supplement book pools roll twice from weight total 264: empty 192, generic vanilla enchanted book 48, a Rebound-or-Resonance book 16, and a Transdimensional-or-Mending book 8.
- Stronghold library, mansion, and stronghold-library supplement contain a Transdimensional enchanted-book entry at weight 2 of 60.

Deleting Stun Burst or Transdimensional registrations would risk existing enchanted tools and books. Suppressing their tag membership and explicit future loot is enough to stop normal acquisition while leaving old NBT/components readable. Resonance must not be removed with Mirror/Dagger because the Magnia Attractor retains a concrete, non-thematic effect.

## 6. Potion/effect survival-loop map

### Registered effects

| Effect | Classification | Exact static behavior | Survival-loop role |
|---|---|---|---|
| `enderscape:low_gravity` | Neutral | Gravity attribute -60% of base; safe-fall-distance +90% of base | Mobility/fall control; Drift Jelly and structure potions/arrows |
| `enderscape:stunned` | Harmful marker | Attachment/mixin-controlled movement/action restrictions; applied by Dagger logic rather than a potion family | Legacy Dagger support; separate from Void loop |
| `enderscape:void_corruption` | Harmful | Adds `1 << amplifier` void ticks per tick and maintains at least 90 delay; at full voided health damages every `max(10, 40 >> amplifier)` ticks. Tagged void entities instead heal 1 health every `50 >> amplifier` ticks | Escalates environmental Void exposure; Murublight and registered potions carry it |
| `enderscape:void_purification` | Beneficial | On start rounds voided health down to even; every `80 >> amplifier` ticks removes two voided-health units with feedback. It damages tagged void mobs by 4 and heals tagged Drifters by 1 | Recovery counter-loop; Puruberry and potions carry it |
| `enderscape:void_resistance` | Beneficial marker | Damage gate rejects non-Outer-Void damage while present | High-value protection, including the modified totem path |

The underlying Void system is wider than these five registry entries. Maximum void ticks are 90; fully voided health kills the affected living entity. Voided non-living entities begin probabilistic destruction after 1,200 ticks (12.5% each subsequent tick). End ambience can apply voiding, while purifying-sky biomes apply ambient purification. Void Lachryma/conversion, custom damage, particle/sound feedback, and tagged mob exceptions all share this state. Enderscape also changes the totem's default components to grant 200 ticks of Void Resistance. Removing only potions would leave a fractured environmental system; removing effects would additionally threaten active-effect saves and potion/tipped-arrow component references.

### Registered potions and brewing

| Potion ID | Effect payload | Brewing path in this jar |
|---|---|---|
| `low_gravity` | Low Gravity 1,800 ticks | Awkward Potion + Drift Jelly |
| `long_low_gravity` | Low Gravity 4,800 ticks | Low Gravity + redstone |
| `void_purification` | Void Purification 1,800 ticks | Awkward Potion + Puruberry |
| `long_void_purification` | Void Purification 4,800 ticks | Void Purification + redstone |
| `strong_void_purification` | Void Purification 600 ticks, amplifier 1 | Void Purification + glowstone |
| `void_resistance` | Void Resistance 1,200 ticks | No base brewing recipe in the exact jar |
| `long_void_resistance` | Void Resistance 3,600 ticks | Void Resistance + redstone |
| `void_corruption` | Void Corruption 800 ticks | No base brewing recipe in the exact jar |
| `long_void_corruption` | Void Corruption 1,600 ticks | Void Corruption + redstone |
| `strong_void_corruption` | Void Corruption 400 ticks, amplifier 1 | Void Corruption + glowstone |

The absent base recipes for Void Resistance and Void Corruption are a **surprising static edge**: those base potion identities are creative/command/external-content inputs in this jar, even though their redstone/glowstone upgrades are registered. This is not permission to delete them. Vanilla splash, lingering, and tipped-arrow transforms inherit registered potion contents; structure loot explicitly uses some Enderscape and vanilla potion identities. Current decision: retain the entire effect/potion/variant loop unchanged.

## 7. Magnia mechanics

### Blocks, polarity, and power

Base `enderscape:alluring_magnia` and `enderscape:repulsive_magnia` are polarity-bearing power relays, not entity movers. Each stores Magnia power 0–15 and exposes that value to comparators. A base block relays only a matching-polarity Magnia signal; consecutive blocks of the same exact base block attenuate by one per hop. Opposite polarity does not transmit. A non-polarized provider can source either polarity.

Exact power providers found in code:

- `enderscape:nebulite_block`: power 15 to either polarity.
- `enderscape:void_torch` and its wall form: power 15 to either polarity.
- Blistered Magnia: power 15 only for its selected polarity.
- Polarized Magnia: power 15 only for its current switched polarity.
- Base Alluring/Repulsive Magnia: relays its stored matching power, rather than creating an independent 15 signal.

Etched Magnia blocks, slabs, stairs, and walls retain a polarity identity but do not source/relay a power level. Their polarity still matters as a field terminator when their relevant face is sturdy.

### Sprouts and fields

Alluring and Repulsive Magnia Sprouts are separate waterloggable blocks and the actual entity movers. A sprout reads matching Magnia power from its rear and redstone from the other sides/above. It is disabled while overheated. The exact `#enderscape:overheats_magnia_sprouts` tag contains only `minecraft:magma_block`.

The field projects forward in the sprout's facing direction for a maximum of 14 blocks, with a 1.5-block-wide cross-section perpendicular to travel. **Ordinary walls and solid blocks do not stop it.** The field stops only when it reaches a block with the opposite Magnia polarity and a sturdy center face toward the field. Opposite base, etched, polarized, blistered, and related sturdy polarity-bearing forms can therefore terminate it; an arbitrary stone wall cannot.

Alluring motion points toward the sprout face, scales with distance divided by five, slows inside 1.5 blocks, and settles inside 0.1. Repulsive motion is a constant outward push. Field motion emits feedback, marks synchronization state, and sends a player-motion packet when needed.

### Affected entities and exact strengths

| Target | Operation | Alluring / repulsive base | Conditions and side effects |
|---|---|---|---|
| Item entities | Set velocity | 0.6 / 0.8 | Pickup delay set to 20; no gravity while moved; gravity restored after cooldown |
| Experience orbs | Set velocity | 0.6 / 0.8 | Same field-category handling as loose items |
| Arrows | Add velocity | 0.25 / 0.25 | In-ground arrows are excluded |
| Minecarts | Add velocity | 0.075 / 0.075 | Direct class behavior |
| Iron Golems | Add velocity | 0.075 / 0.075 | Direct class behavior regardless of worn armor |
| Sulfur Cubes | Add velocity | 0.075 / 0.075 | Direct Enderscape entity behavior |
| Other living entities | Add velocity | `0.01 × armor factor` / `0.02 × armor factor` | Only when equipped magnetism factor is positive; resets fall distance; nonplayers receive transient +10% total gravity |
| Players | As other living entities | Armor-scaled | Excluded while the player's abilities are actively flying; this is Creative flight state, not merely Creative mode |

The data tag `enderscape:affected_by_magnia` names Iron Golem and Minecart, but code also treats items, XP, arrows, Sulfur Cubes, and armor-magnetic living entities. Reading only that tag would miss most affected categories.

Magnetic equipment factors are additive across worn armor slots:

- Weak, factor 0.5 each: chainmail armor, netherite armor, matching horse/nautilus armor, and Ancient Debris.
- Medium, factor 1.0 each: iron armor, matching horse/nautilus armor, Raw Iron Block, and Netherite Block.
- Strong, factor 1.5 each: Iron Block.

Raw/solid blocks appearing in equipment tags are **surprising/dead for normal players** because the calculation inspects worn armor slots; they matter only if another system makes those stacks equippable. The exact static tag contents should nevertheless be preserved unless deliberately corrected in a separate design task.

The field resets fall distance while applying motion. It temporarily manages no-gravity for moved item-like entities and restores it after a cooldown. That makes partial removal of attachments/networking code risky even when the visible block remains.

### Blistered Magnia

A powered sprout has a one-in-three random-tick chance to turn an adjacent matching base Magnia block in front of it into Blistered Magnia. The new block selects polarity by the majority of adjacent base Magnia blocks; it selects none if no polarized neighbors exist. Equal-count ties depend on enum/map iteration in the exact implementation and should be treated as **runtime-uncertain**, not promised deterministic behavior. A polarized Blistered block emits power 15 for the selected polarity and light level 14, and schedules neighbor/polarity reevaluation after five ticks.

### Polarized Magnia

The shaped recipe is `SSS / PBP / SRS`: five Shadoline Ingots (`S`), two Popped Chorus Fruit (`P`), one Blistered Magnia (`B`), and one Redstone (`R`). It starts Alluring. A redstone rising edge toggles the polarity and marks the block powered; the falling edge only clears powered. It is therefore a **pulse-toggle**, not a block whose polarity directly follows current redstone level. It emits power 15 only for the selected polarity.

### Nebulite, Void Torch, Radio, and Attractor

Nebulite Block and both Void Torch forms are polarity-neutral 15-power sources. Magnia Radio does not implement the Magnia polarity/power interface and has no entity-field relationship; it is thematic/music machinery. The Radio has no ordinary recipe in the exact jar and is primarily a creative/persistence identity.

The Magnia Attractor is a separate hand-held system:

- Registry item: `enderscape:magnia_attractor`; crafted from one Alluring Sprout, one Repulsive Sprout, and four Shadoline Ingots (`A R / SSS / _S_`). Removing sprouts makes this recipe unreachable even though the item code itself does not query placed Magnia blocks.
- Fueled by `#enderscape:nebulite` items; capacity 6; starts enabled.
- Searches a player-centered range expanded 10 blocks horizontally and 4 vertically.
- Pulls tagged items and XP toward the player by +0.2 motion per tick, or +0.04 underwater.
- XP does not consume fuel. Item pulling consumes one fuel after a base threshold of 200 pull ticks. Resonance adds 100 ticks per level.
- Bundling attempts to place a pulled stack into an existing bundle only when the enchantment is present and only for an exact item/component match.
- Uses dedicated item/XP eligibility tags and a `pull_item_with_attractor` advancement.

Consequently, the Attractor is **technically independent while in use** but **acquisition-coupled** to sprouts and thematically/data-coupled to Nebulite, Bundling, and Resonance. Removing Mirror and Dagger leaves it as the retained Nebulite tool and the reason Resonance remains meaningful.

### Worldgen and progression coupling

Magnia Fields place Magnia arches, towers, spikes, and large spikes, including upward-facing Alluring and downward-facing Repulsive sprouts. End City processing can replace start-platform material with base Magnia. Two exact End City furniture templates contain sprouts: `end_city/house/furniture/furniture_7.nbt` contains Alluring and `furniture_16.nbt` contains Repulsive. No proposed equipment candidate appears in any structure template.

Magnia Fields is one of four Enderscape biomes added to the vanilla End biome through code/Lithostitched (relative configured additions include 0.8 Magnia Fields, 0.7 Veiled Woodlands, 0.5 Corrupt Barrens, and 0.3 Celestial Grove). New Terrain changes broad dimension/noise definitions but there is no supported Magnia-Fields-only config switch. Removing base blocks/sprouts would leave configured/placed features, processors, templates, recipes, advancements, and old chunks dead or missing. Retain the registrations; any future worldgen reduction must be an explicit new-world/existing-world migration decision.

## 8. Enderscape-native food matrix

| Identity | Ordinary hunger behavior | Effects/direct code | Acquisition and loot | Matcha conflict and preservation boundary |
|---|---|---|---|---|
| `enderscape:drift_jelly_bottle` | Nutrition 6; saturation modifier 0.2 (2.4 saturation); always edible; stack 16 | Drink animation; Low Gravity 400 ticks; returns/converts to Glass Bottle | Shapeless one Drift Jelly Block + four bottles produces four; bottling a jelly-dripping Drifter; no direct loot entry | Restores vanilla hunger, unlike Matcha heart-effect foods. Preserve bottle remainder, drinking, Low Gravity, stack semantics, Drifter interaction, and brewing ingredient; owner must choose health/heart behavior |
| `enderscape:puruberry` | Nutrition 6; saturation modifier 1.0 (12 saturation); always edible; 3-second cooldown | Void Purification 300 ticks | Ripe vine drops 1–3 with Fortune uniform bonus capped at 6; End City conditional loot, End Haven, Mirestone Ruins; worldgen | Strong ordinary hunger conflicts with Matcha design. Preserve block/vine placement, purification, cooldown, Fortune, Drifter-food tag, and brewing; owner chooses health value |
| `enderscape:murublight_bracket` | Nutrition 4; saturation modifier 0.3 (2.4 saturation); always edible | Void Corruption 240 ticks | Placeable/worldgen block drop; Mirestone Ruins loot | Preserve placement/worldgen, corruption, Rustle food/conversion input, and loot balance. Changing/removing ordinary hunger must not break the mob loop |
| `enderscape:chorus_cake_roll` | Item has no ordinary Food or Consumable component. Placed block has seven bites; each calls `FoodData.eat(2, 0.1)` directly (2 hunger, about 0.4 saturation) and requires `canEat(false)` | Final bite attempts random teleport within ±8 up to 16 times, then destroys block; comparator output tracks bites | Recipe uses three Chorus Fruit, two Sugar, and one Drift Jelly Bottle; result is a placeable stack-size-1 block item | Component-only conversion will miss it. Requires narrow code compatibility to replace direct hunger with approved Matcha semantics while preserving seven bites, comparator, final teleport, and block behavior |

Other food-like paths found:

- `enderscape:healing` is an operator-blocks-only diagnostic item when the global vanilla-tab option is enabled. Use heals 5,000, calls `FoodData.eat(5000, 5000)`, clears cooldown, fire, freeze, negative effects, and voided health. It is not normal survival acquisition and should not be balanced as an ordinary food, but a creative filter must account for it if the owner wants zero debug exposure.
- Bulb Flower's recipe produces vanilla Suspicious Stew with a `suspicious_stew_effects` Glowing entry for 140 ticks. Stronghold Garden loot produces Suspicious Stew with Regeneration for a uniformly selected 7–10 seconds. These are component-bearing vanilla carriers, not Enderscape-native food IDs.
- Enderscape's Void effects manipulate voided health and tagged mobs; they are not generic player-health food replacements. No additional ordinary direct player-heal food was found beyond the operator healing item.
- Mob links: Drifters can be bottled for Drift Jelly and treat Chorus Fruit/Puruberry as food; Rustles consume Murublight Bracket for their conversion loop. Rubblemites drop Rubble Chitin, not food. No entity loot table outputs a player food.
- Tags: Drift Jelly Bottle is in `c:foods` and `c:drinks/drift_jelly`; Chorus Cake Roll is in `c:foods/edible_when_placed`; Puruberry is in `c:foods/berry`; mob food tags separately include Chorus Fruit/Puruberry and Murublight Bracket.

No exact canonical Matcha analogue establishes final heart values for these four Enderscape-native identities. Those values remain owner decisions. “Convert to Matcha semantics” must not mean discarding their Void/Low Gravity role or block/entity interactions.

## 9. Matcha-sensitive loot matrix

### Why same item ID is insufficient

Current Matcha Flavoured 1.12 is an always-enabled built-in data pack under `projects/matcha-flavoured-data/src/main/resources/resourcepacks/matcha_flavoured_1_12/data`. Relevant canonical stacks are component-bearing:

| Base carrier | Current Matcha identity/source relevant here |
|---|---|
| `minecraft:bread` | Effective recipe: heart lore, a Consumable that removes 12 negative effects and applies Regeneration III (amplifier 2) for 48 ticks, `food` 0/0 always edible, default bread model. Matcha's nested bread loot also adds its own count 1–3. |
| `minecraft:carrot` | Nested Matcha food table: `❣` lore and Regeneration III for 20 ticks; no Food override, so intrinsic carrot food remains. Nested helper count is 2–5. |
| `minecraft:golden_carrot` | Effective recipe/discovery authority: heart/night-vision presentation, Regeneration III for 24 ticks plus Night Vision 600 ticks, Food 0/0. A stale nested loot identity instead has Regeneration IV for 15 ticks plus Night Vision 600 and count 3–8. Current C8 source-aware logic prefers the unique effective recipe health identity; future work must resolve/own this discrepancy rather than blindly nest the stale table. |
| `minecraft:golden_apple` | Matcha identity applies Regeneration III for 48 ticks, Regeneration I for 600, Absorption for 2,400, Food 0/0. |
| `minecraft:enchanted_golden_apple` | Regeneration III for 48, Regeneration II for 600, Absorption for 2,400, Fire Resistance/Resistance for 6,000, Food 0/0. |
| `minecraft:chorus_fruit` | Matcha chorus-plant loot identity carries `❣`/Warping presentation and a Consumable that random-teleports and applies Regeneration III for 20 ticks; no Food override. Enderscape loot emits plain Chorus Fruit. |
| `minecraft:cod` | Matcha fishing identity adds `⭐` lore. No health difference is established, but it is still a component-distinct presentation identity. |

Matcha has no unambiguous canonical nested identity for raw Potato, Poisonous Potato, Honey Bottle, or general Suspicious Stew. Matcha itself uses plain instances of those carriers in unrelated contexts, and Poisonous Potato is shared by many custom named foods. Do not rewrite them solely by base ID.

Current Matcha × JEI C8 retains source provenance, prefers a unique effective health-bearing recipe identity over stale non-recipe variants when other identity dimensions are compatible, and suppresses only demonstrated plain defaults. JEI continues to own potion, tipped-arrow, stew, and shield subtype interpretation. Enderscape-native item IDs are absent from the current Matcha component manifest/catalog; they would appear as ordinary distinct entries unless a future companion deliberately contributes exact variants.

### A. Removal-candidate equipment occurrences

All weights below are declared entry weight over the declared pool weight before conditions; a condition can change effective probability. Counts default to one unless shown.

| Loot table | Pool/rolls | Candidate | Weight | Functions/components |
|---|---|---|---|---|
| `enderscape:chests/end_city/elytra_vault` | Pool 3, 1 roll | Mirror | 6/61 | Plain default Mirror |
| `enderscape:chests/end_city/vault` | Pool 3, 1 roll | Mirror | 6/61 | Plain default Mirror |
| `enderscape:chests/stronghold/armory` | Pool 0, 1–3 rolls | Mirror | 1/79 | Plain default Mirror |
| `enderscape:chests/stronghold/garden` | Pool 0, 2–3 rolls | Mirror | 1/79 | Plain default Mirror |
| `enderscape:chests/stronghold/library` | Pool 0, 2–10 rolls | Mirror | 2/60 | Plain default Mirror |
| `enderscape:chests/stronghold/mansion` | Pool 0, 2–6 rolls | Mirror | 2/60 | Plain default Mirror |
| `enderscape:chests/stronghold/secret` | Pool 1, 1–2 rolls | Mirror | 1/79 | Plain default Mirror |
| `enderscape:supplements/end_city_treasure` | Pool 3, 1 roll | Mirror | 6/61 | Plain default Mirror |
| `enderscape:supplements/stronghold_library` | Pool 0, 2–10 rolls | Mirror | 2/60 | Plain default Mirror |
| `enderscape:chests/mirestone_ruins` | Pool 1, 1 roll | Dagger | 1/10 | Plain default Dagger |

There are no Rubble Shield or Shadoline armor loot entries. `enderscape:post_supplements/end_city_treasure` nests the original vanilla End City table plus Enderscape's supplement; it does not add a tenth direct Mirror entry. When New End Cities is enabled, the vanilla End City treasure table is instead replaced by a table that nests `enderscape:end_city/chest`; this route is distinct from the vanilla-piece post-supplement hook.

### B. Enderscape-native food occurrences

| Loot table/path | Item | Rolls/weight/count | Functions/conditions |
|---|---|---|---|
| `enderscape:blocks/murublight_bracket` | Murublight Bracket | Block drop | Ordinary block-state/survival drop path |
| `enderscape:blocks/puruberry_vine` ripe branch | Puruberry | Count 1–3; Fortune uniform bonus capped at 6 | Alternative after Silk Touch/shears handling |
| `enderscape:chests/end_city/chest` | Puruberry | Pool 0, 3–5 rolls; weight 10 of 118; count 2–4 | Only in Celestial Grove; without it, effective eligible weight is 108 |
| `enderscape:supplements/end_city_treasure` | Puruberry | Pool 0, 3–5 rolls; weight 10 of 115; count 2–4 | Same Celestial Grove condition; effective eligible weight 105 outside it |
| `enderscape:chests/end_haven/chest` | Puruberry | Pool 0, 2–3 rolls; weight 3/9; count 1–2 | Plain native item |
| `enderscape:chests/mirestone_ruins` | Murublight Bracket | Pool 0, 2–4 rolls; weight 8/78; count 1–2 | Plain native item |
| `enderscape:chests/mirestone_ruins` | Puruberry | Pool 0, 2–4 rolls; weight 3/78; count 2–8 | Plain native item |

Drift Jelly Bottle and Chorus Cake Roll have no direct loot occurrence in the 240 Enderscape tables. Their acquisition is recipe/entity and recipe/block respectively.

### C/D. Vanilla foods and Matcha-sensitive carriers

Every item in this table is emitted as a plain/default stack unless the final column states a component function. Enderscape applies the listed count after choosing the entry.

| Loot table | Pool and declared pool total | Item | Weight; count | Enderscape component/functions | Current Matcha disposition |
|---|---|---|---|---|---|
| `enderscape:chests/end_city/chest` | Pool 0, 3–5 rolls, total 118 | Chorus Fruit | 12; 2–6 | None | Patch exact canonical Matcha chorus components; keep Enderscape count |
| same | same | Bread | 8; 2–5 | None | Patch effective recipe-authoritative Matcha bread; do not nest helper count 1–3 |
| same | same | Honey Bottle | 6; 1–3 | None | Preserve plain until owner supplies canonical Matcha identity |
| `enderscape:supplements/end_city_treasure` | Pool 0, 3–5 rolls, total 115 | Chorus Fruit | 12; 2–6 | None | Same component patch |
| same | same | Bread | 8; 2–5 | None | Same component patch |
| same | same | Honey Bottle | 6; 1–3 | None | Preserve plain |
| `enderscape:chests/end_city/elytra_vault` | Pool 2, 0–1 rolls, total 51 | Golden Carrot | 40; 4–12 | None | Use current effective-recipe canonical identity, not stale nested helper |
| same | same | Golden Apple | 10; 1–2 | None | Patch canonical components |
| same | same | Enchanted Golden Apple | 1; 1 | None | Patch canonical components |
| `enderscape:chests/end_city/vault` | Pool 2, 0–1 rolls, total 51 | Golden Carrot | 40; 4–12 | None | Same |
| same | same | Golden Apple | 10; 1–2 | None | Same |
| same | same | Enchanted Golden Apple | 1; 1 | None | Same |
| `enderscape:chests/end_city/spawner/basic` | Pool 0, 1 roll, total 17 | Bread | 12; 2–8 | None | Patch canonical components |
| same | same | Golden Carrot | 4; 2–8 | None | Patch recipe-authoritative components |
| same | same | Golden Apple | 1; 1–3 | None | Patch canonical components |
| `enderscape:chests/end_haven/chest` | Pool 0, 2–3 rolls, total 9 | Bread | 5; 3–6 | None | Patch canonical components |
| same | same | Golden Carrot | 1; 3–6 | None | Patch recipe-authoritative components |
| same | Pool 1, 2–4 rolls, total 25 | Honey Bottle | 3; 2–3 | None | Preserve plain |
| `enderscape:chests/mirestone_ruins` | Pool 0, 2–4 rolls, total 78 | Chorus Fruit | 12; 2–6 | None | Patch canonical components |
| same | same | Bread | 8; 2–4 | None | Patch canonical components |
| `enderscape:chests/stronghold/altar` | Pool 0, 2–4 rolls, total 176 | Bread | 11; 1–6 | None | Patch canonical components |
| same | same | Carrot | 9; 1–4 | None | Patch canonical carrot components while retaining Enderscape count |
| same | same | Potato | 9; 1–4 | None | Preserve plain |
| same | same | Golden Apple | 1; 1 | None | Patch canonical components |
| same | same | Poisonous Potato | 1; 1 | None | Preserve plain; shared carrier is ambiguous |
| `enderscape:chests/stronghold/armory` | Pool 0, 1–3 rolls, total 79 | Golden Apple | 5; 1–3 | None | Patch canonical components |
| same | same | Golden Carrot | 5; 3–6 | None | Patch recipe-authoritative components |
| `enderscape:chests/stronghold/bedroom` | Pool 0, 2–3 rolls, total 20 | Bread | 2; 3–6 | None | Patch canonical components |
| same | same | Carrot | 2; 3–8 | None | Patch canonical components |
| same | same | Potato | 2; 3–8 | None | Preserve plain |
| `enderscape:chests/stronghold/garden` | Pool 0, 2–3 rolls, total 79 | Golden Apple | 5; 1–3 | None | Patch canonical components |
| same | same | Golden Carrot | 5; 3–6 | None | Patch recipe-authoritative components |
| same | Pool 1, 2–4 rolls, total 13 | Suspicious Stew | 1; 1 | `set_stew_effect` Regeneration, uniformly 7–10 seconds | Preserve Enderscape exact stew variant; no canonical Matcha replacement exists |
| `enderscape:chests/stronghold/mansion` | Pool 2, 2–4 rolls, total 42 | Cod | 1; 1 | None | Owner decides whether Matcha's fishing-only star presentation should extend to structure cod |
| `enderscape:chests/stronghold/secret` | Pool 1, 1–2 rolls, total 79 | Golden Apple | 5; 1–3 | None | Patch canonical components |
| same | same | Golden Carrot | 5; 3–6 | None | Patch recipe-authoritative components |
| `enderscape:chests/stronghold/spawner/basic` | Pool 0, 1–3 rolls, total 21 | Bread | 16; 1–3 | None | Patch canonical components |
| same | same | Golden Carrot | 4; 2–4 | None | Patch recipe-authoritative components |
| same | same | Golden Apple | 1; 1–2 | None | Patch canonical components |
| `enderscape:supplements/end_city_treasure` | Pool 2, 0–1 rolls, total 91 | Golden Carrot | 40; 4–12 | None | Patch recipe-authoritative components |
| same | same | Golden Apple | 10; 1–2 | None | Patch canonical components |
| same | same | Enchanted Golden Apple | 1; 1 | None | Patch canonical components |

Potion/food-adjacent component entries are already exact component variants and should not be collapsed into Matcha food substitution:

- End City chest pool 0 (3–5 rolls, total 118) and supplement pool 0 (total 115): Long Low Gravity tipped arrows, weight 6, count 4–8; Slow Falling potion, weight 1; Slowness lingering potion, weight 1; Low Gravity lingering potion, weight 1.
- End City spawner pool 1 (one roll, total 5): Strong Healing lingering potion, weight 1; Slow Falling lingering potion, weight 1.
- End Haven pool 1 (2–4 rolls, total 25): Slow Falling, Regeneration, and Low Gravity potions, each weight 1.
- Stronghold altar pool 0 (2–4 rolls, total 176): Healing potion and Strength potion, each weight 2.
- Stronghold armory/garden/secret relevant pool (declared total 79): Strong Healing and Strong Strength potions, each weight 5 and count 1–3.
- Stronghold infestation dispenser pool 0 (1–2 rolls, total 2): Infested splash potion and Long Weakness splash potion, each weight 1 and count 1–3.

No food trades were registered and no entity loot table adds a food. The inventory above covers all direct removal-candidate, native-food, vanilla-food, and potion-adjacent occurrences found in the exact Enderscape namespace plus the two vanilla-loot modification routes.

### Safest future Matcha loot method

Do not replace an Enderscape entry with a nested Matcha food table that has its own count. Bread (1–3), Carrot (2–5), and stale Golden Carrot (3–8) helpers would overwrite or compound Enderscape's original count and could also select an obsolete component identity. Instead:

1. Leave Enderscape's pool, rolls, conditions, entry weight, count range, and any potion/stew functions intact.
2. Resolve the current source-authoritative Matcha identity at implementation time, with effective recipe output winning only under the current canonicalizer's demonstrated rules.
3. Apply/copy only the identity-bearing component patch to the selected Enderscape stack, or create a purpose-built identity-only helper with no count/probability function.
4. Preserve an explicit upstream component fingerprint so Matcha changes fail visibly rather than silently emitting stale health behavior.
5. Let the existing Matcha × JEI/Creative layer deduplicate only the exact demonstrated default/canonical family. Do not add base-ID-wide suppression.

## 10. Built-in data-pack/config modularity

Enderscape 3.0.2 registers six built-in packs. Each pack is `DEFAULT_ENABLED` when its corresponding `default...` config value is true and otherwise `NORMAL` (available but not forced).

| Built-in pack | Type | Exact scope | Suitable for pruning? |
|---|---|---|---|
| `improved_visuals` | Resource pack | Dragon Egg models/textures, End Portal Frame side, Enderman and Endermite textures | Cleanly disables only these visual replacements; unrelated to candidate content |
| `fix_levitation_advancement` | Data pack | Replaces/fixes vanilla `end/levitate` advancement | Clean fix toggle only |
| `fix_vanilla_recipes` | Data pack | Replaces vanilla Purpur Pillar recipe | Clean recipe-fix toggle only |
| `new_end_cities` | Data pack | End City structure set, vanilla End City treasure replacement nesting `enderscape:end_city/chest`, and vanilla Elytra/Find End City advancements | Coarse structure/loot/progression switch; not a Mirror- or food-only control |
| `new_strongholds` | Data pack | Stronghold structure set and vanilla Follow Ender Eye advancement | Coarse structure/progression switch; not candidate-specific |
| `new_terrain` | Data pack | Vanilla and Enderscape End dimension, dimension type, and noise settings | Very coarse terrain control; affects worldgen contract, not an existing-chunk eraser |

Exact config fields also include:

- Exposure/pack/UI: `creativeTabEnabled`, `includeItemsInVanillaCreativeTabs`, `includeColoredMirrorsInCreativeTabs`, all six `default...` pack fields, `supplementVanillaEndCityTreasureLoot`, `supplementVanillaStrongholdLibraryLoot`, `editWorldEnderscapeDataPacksButton`, and `vanillaWorldWarning`.
- Presentation/vanilla behavior: foliage, grass, water, and fog colors; Elytra hunger/sounds/sneak; sound types; Chorus Flower, Enderman, Endermite, Ender Pearl, portal, skybox/fog, mirror tooltip coordinate/dimension/distance/Shift details, Nebulite HUD, Rubblemite, Shulker Bullet, Silverfish, structure music, tridents, and Void poof behavior.

The stronghold-library supplement uses Fabric's loot-table event to add nested `enderscape:supplements/stronghold_library` to `minecraft:chests/stronghold_library`. The End City supplement is different: a mixin changes vanilla End City piece marker loot to `enderscape:post_supplements/end_city_treasure`, which nests the original vanilla table plus Enderscape supplement. New End Cities can separately replace the vanilla table with Enderscape's own chest route. Future tests must guard against assuming those are the same path or accidentally applying the supplement twice.

What can be disabled cleanly through supported architecture: the six exact broad packs, both vanilla structure-loot supplements, the global Enderscape creative tab, global additions to vanilla tabs, colored Mirror additions, and listed ambience/vanilla presentation behaviors. What cannot be disabled cleanly: only Mirror/Dagger/Shield/armor, only Magnia Fields, individual native foods, individual enchantments, candidate loot while retaining other table contents, Chorus Cake hunger code, or Matcha-correct item components. Those require data overrides and/or a narrow compatibility mod.

## 11. Save/registry-risk analysis

| Proposed change | New-world effect | Existing-world/save risk | Static classification |
|---|---|---|---|
| Hide an item/component variant from creative/JEI | Discovery only | Existing stacks remain | Likely safe, but runtime discovery testing required |
| Remove candidate recipes and future loot while retaining item/component registrations | Stops new ordinary acquisition | Existing stacks, containers, frames, commands, and old chunks retain identities | Preferred existing-world approach; test recipe-book/advancement cleanup |
| Delete Mirror/Dagger/Shield/armor item registrations | Removes IDs entirely | Unknown/missing stack behavior, discarded inventories, component decode errors; legacy Shield aliases worsen risk | Dangerous; requires migration and runtime/save testing |
| Delete Stun Burst/Transdimensional registrations | Stops enchant identity | Existing enchanted tools/books and loot/advancement references may fail | Dangerous; suppress acquisition instead |
| Delete Resonance | Breaks retained Attractor role | Existing Attractors/books plus tag references affected | Incorrect under current decisions |
| Delete effects/potions | Breaks active effects and potion-content references | Entities, players, potions, arrows, loot, totem/default components and Void state threatened | Rejected; keep |
| Disable a built-in data pack | Changes future selected data/world generation | Existing chunks/blocks remain; saved world pack selection and future borders can differ | Supported but coarse; world-copy test required |
| Delete Magnia block/sprout/radio registrations | Prevents content entirely | Existing chunks/templates/block entities, processors, recipes and features can contain IDs | High-risk migration; not ordinary pruning |
| Remove a configured/placed feature, biome, structure, dimension, or noise setting | Prevents future generation or registry loading | Stored worldgen registries/chunk borders can fail or diverge | New-world-only unless migrated and tested |
| Replace food loot by base ID | May look correct superficially | Emits mechanically wrong Matcha stack or collapses legitimate variants | Unsafe design; components/source required |
| Override advancement/recipe data while retaining registry items | Stops future unlock/progression | Existing completion and recipe-book state may refer to removed IDs | Usually survivable but requires explicit migration/reload tests |

Specific observations:

- No proposed equipment item ID occurs inside the 197 structure NBT templates. This lowers direct chunk/template risk for those *items*, but does not make inventory unregistration safe.
- Alluring/Repulsive Sprouts occur in two End City furniture templates; base/sprout Magnia also appears through features/processors. Magnia removal is therefore a chunk and worldgen concern.
- Enderscape's data components remain globally registered as part of the retained mod. Stored Mirror dye/target/fuel and Shield variant values are safest when both their item and component decoders remain present.
- Recipe-book and advancement completion are saved independently from current availability. Data overrides should preserve identifiers where practical or explicitly test old state, rather than assuming removed JSON is invisible to saves.
- Potion Contents and active effects contain registry references. Splash/lingering/tipped-arrow forms multiply the number of stored locations.
- Disabling packs changes future evaluation/generation; it does not retroactively delete already generated End Cities, Strongholds, Magnia Fields, loot containers that already rolled, or blocks in chunks.
- Static decompilation cannot establish Minecraft's full error/recovery behavior for deliberately missing registries. Every such claim remains uncertain until tested on disposable copies.

Per candidate: acquisition suppression with retained identity is expected to be suitable for both new and existing worlds, subject to runtime tests. Actual Mirror, Dagger, or armor unregistration requires migration. Rubble Shield unregistration is especially dangerous because both current component stacks and four legacy aliases converge on it. None is “safe regardless” without testing.

## 12. Safe pruning strategy by subsystem

| Subsystem | Recommended future action | Explicitly avoid |
|---|---|---|
| Mirror | Override nine loot entries, dye recipe, recipe/advancement exposure as approved; filter all Dye Color discovery variants; retain registry/code/enchant decoders | Deleting the item or Dye Color component; removing Resonance |
| Dagger | Remove shaped recipe and Mirestone entry; filter discovery; suppress Stun Burst acquisition and Dagger-specific advancement exposure | Deleting legacy Dagger/stun/fuel decoding or Shadoline material |
| Rubble Shield | Remove four recipes/unlocks; filter four Variant identities; keep aliases and legacy dash/block behavior | Deleting shared item/component or aliases |
| Shadoline armor | Remove four crafting recipes/unlocks and discovery; replace exploration display icon; preferably keep recycle recipes | Removing Shadoline material or global stealth infrastructure needed by old armor |
| Enchantments | Keep Bundling/Rebound/Resonance. Retain Stun Burst/Transdimensional registries but remove future availability when their tools are suppressed | Registry deletion or base-ID tag changes that strand old books |
| Effects/potions | No pruning; preserve brewing, loot, totem, arrows, active-effect handling | Treating item-count reduction as more important than the integrated Void loop |
| Native foods | Introduce a purpose-built Matcha compatibility contract after heart decisions; special code for Chorus Cake Roll | Generic Food-component-only rewrite; losing effects, remainders, blocks, teleport, or mob feeding |
| Vanilla-food loot | Component-patch original Enderscape entries using current source authority; retain rolls/weights/counts/conditions | Nesting count-bearing Matcha helper tables or substituting by item ID alone |
| Magnia | Retain registries/worldgen by default; prune only after an explicit content/world migration decision. Attractor can remain even if world Magnia generation is reduced, but then needs an alternate approved acquisition route | Assuming ordinary walls stop fields; deleting blocks present in chunks/templates; removing Resonance |
| Built-in packs | Use supported toggles only for the exact broad behavior the owner wants disabled | Treating New Terrain/City/Stronghold toggles as fine-grained content switches |
| Creative/JEI | Narrow companion filter for exact candidate identities; integrate with current component-aware Matcha discovery | Global tab disable as a candidate-specific solution; clearing/rebuilding global Search; replacing JEI-owned subtype handlers |

The likely implementation shape is a small Workbench compatibility mod plus data-pack overrides, not a forked Enderscape jar. The data layer owns loot/recipes/advancements; code owns programmatic creative/JEI filtering, Chorus Cake direct hunger behavior, and any dynamic component patching that cannot be expressed safely in data. Registry identities and upstream mechanics remain supplied by exact Enderscape.

## 13. Owner decisions still required

1. Confirm final scope independently for Mirror, Dagger, all four Rubble Shield variants, and all four Shadoline armor pieces. “Suppress ordinary acquisition and discovery but keep old stacks usable” is the audit recommendation, not yet an owner decision.
2. Decide whether legacy candidate stacks should remain fully functional, be recyclable only, or be migrated. This especially affects Mirror teleportation, Dagger stun, Shield dash, and armor stealth.
3. Decide whether Shadoline armor smelting/blasting recovery remains visible after crafting suppression.
4. Decide whether `explore_end` should use a different display icon and whether candidate-specific gameplay advancements remain hidden, visible-but-legacy, or replaced.
5. Confirm Stun Burst and Transdimensional should leave future enchanting/loot while their registry identities remain. Confirm whether their already-enchanted books should still be applicable to old tools.
6. Confirm whether Resonance remains applicable to old Mirror/Dagger as well as retained Attractor, or whether its supported tag should be narrowed for new application only.
7. Assign Matcha heart/health behavior for Drift Jelly Bottle, Puruberry, Murublight Bracket, and each Chorus Cake Roll bite. No current canonical analogue makes those values unambiguous.
8. Decide whether structure-loot Cod should receive Matcha's fishing-source star lore; current source semantics do not prove that presentation is globally canonical.
9. Decide whether plain Potato, Poisonous Potato, Honey Bottle, and Enderscape's exact Suspicious Stew variants remain untouched. The audit recommends untouched absent a new canonical Matcha rule.
10. Resolve the Matcha Golden Carrot source discrepancy: effective recipe authority (Regeneration III/24 ticks) versus stale nested loot (Regeneration IV/15 ticks). Current C8 behavior favors the effective recipe identity.
11. Choose any Enderscape built-in packs/supplements to disable. New End Cities, New Strongholds, and New Terrain are broad world/progression choices and should be made separately from item pruning.
12. Decide whether Magnia Fields/world blocks/sprouts remain wholesale, whether only future worldgen should be reduced, and—if sprouts become unobtainable—how the retained Magnia Attractor would be acquired.
13. Decide whether the creative-only `enderscape:healing` diagnostic item and uncraftable Magnia Radio should remain exposed.
14. Decide whether surprising Magnia behavior (ordinary walls do not block fields, dead block entries in armor tags, Blistered tie ambiguity, base Void potions with no base brew) is accepted upstream behavior or a separate compatibility-fix scope.

## 14. Suggested implementation phases

These phases deliberately preserve decision gates; they do not silently select the answers above.

1. **Freeze identities and owner choices.** Record exact Enderscape, Matcha data, Matcha × JEI, Fabric, and dependency versions; resolve section 13; capture baseline worlds/inventories and exact component fingerprints.
2. **Data-only acquisition suppression.** Add the smallest recipe, loot, tag, and advancement overrides for only approved candidates. Retain every Enderscape registry and code path. Add static checks that enumerate all ten direct candidate loot occurrences and all approved recipes.
3. **Discovery compatibility.** Add exact-component filters for Enderscape/vanilla creative tabs and optional JEI without taking over JEI's global subtypes or rebuilding Creative Search. Prove old component stacks remain decodable.
4. **Matcha loot identity.** Add source-authoritative identity-only component patches to the exact tables in section 9. Preserve Enderscape's counts, weights, conditions, and rolls; lock tests against component and probability drift.
5. **Native-food compatibility.** Implement owner-selected health semantics. Handle Drift Jelly/Puruberry/Murublight through components where possible and Chorus Cake Roll through a narrow direct-code bridge. Preserve every listed effect/block/entity behavior.
6. **Optional world/pack decisions.** Only after separate approval, apply supported pack/supplement settings or narrow future-worldgen overrides. Do not remove biome/block/feature registrations from an existing-world release.
7. **Build and static validation.** Produce a separately versioned Canary only in a future implementation task; run focused JSON/component/bytecode checks, repository validation, exact artifact retention, and provenance recording.
8. **Runtime/save matrix.** Execute `TESTING.md` on disposable new and copied existing worlds. Bind evidence to the exact artifact. A pass must cover candidate non-acquisition, legacy stacks, structure loot, Matcha components, native foods, Void loop, Magnia, creative/JEI, reload/reconnect, and registry/log health.

No phase authorizes modifying the upstream jar, protected profiles, server-state record, or unrelated projects.
