# Testing

Mynx Ribbits Canary 13 is `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. Build/static checks do not establish Minecraft gameplay correctness. Read the exact current artifact filename/SHA-256 and implementation checkpoint in `WORKBENCH_STATUS.json`, and size/build evidence in the revision-18 log entry. Rehash the ignored private JAR before separately authorized deployment. Never deploy the resource-incomplete source-only JAR, and replace rather than install Canary 13 beside another JAR with the same `ribbits` mod ID.

## Canary 13 focused Naturalist fauna-controls checks — all pending

Use only the dedicated `Matcha Flavoured 26.2 Workbench` through a serialized
deployment that includes the exact retained Naturalist Canary 8 artifact. Record exact
artifact identities and observations; no row is satisfied by static evidence.

1. Start with Naturalist absent. Require Ribbits startup, manual/automatic Wandering
   Ribbit persistence, and existing native/Matcha offers to remain normal; the optional
   Naturalist provider must contribute no range or offer and no snails.
2. Start with exact Naturalist Canary 8. Each newly materialized merchant must gain
   exactly two Naturalist fauna offers selected without replacement from the approved
   19 baby eggs and nine buckets. Each has one use; verify prices, bucket variants,
   baby age, display names, and save/restart preservation. Existing snapshots must not
   reroll or gain fauna offers.
3. For scheduler-created merchants only, require exactly two brown tagged snails,
   both leashed to that merchant. Confirm persistence does not duplicate them and lease
   expiry/removal cleans them up. Egg- and command-created merchants must never gain
   companions or scheduler state.
4. Exercise each entry across repeated controlled materializations until all 28
   approved identities are observed. Stop with `FAIL` for a non-approved identity,
   duplicate selection, more/fewer than two offers or companions, wrong color/leash,
   missing cleanup, Naturalist linkage failure, or saved-offer drift.

## Canary 13 focused deployed Drop Leaf regression — all pending

Use only the dedicated `Matcha Flavoured 26.2 Workbench` through the serialized deployment procedure below. Record exact artifact identities and local/remote observations.

1. Equip Drop Leaf in chest/cape.
2. Descend from a safe height and deploy with the existing second-jump input.
3. Require the rain-leaf stem/canopy above and behind the player's back.
4. Require the actual green donor leaf artwork.
5. Require the donor PNG's transparent area not to render.
6. Require no magenta/black missing texture.
7. Require no fully visible square canopy.
8. Land and require normal retraction to the unchanged closed state.

## Earlier evidence boundary

The exact Canary 6 cohort and the user's scoped Phase C practical `PASS`, the exact Canary 4 external startup `FAIL`, and the bound Canary 3 Phase B report remain recorded in `CODEX_LOG.md`. Canary 11 has an exact external runtime `FAIL`: its rain geometry appeared through the correct back attachment, but the canopy resolved to the magenta/black missing texture. That result supplies no runtime evidence for Canary 13; Canary 12's item-atlas correction and Canary 13's fauna controls remain untested in Minecraft.

## Deployment preconditions

Run the matrix only with explicit deployment ownership through one serialized Test Instance Manager transition into one canonical slot in the dedicated Minecraft 26.2 Workbench. Do not deploy into a personal profile or touch the protected Matcha Flavoured 26.1.2 instance. Preserve the other slot exactly.

Rehash the complete intended cohort before deployment. At minimum this includes YUNG's API Compat.2 (1,260,939 bytes; SHA-256 `FF22A6B509BA559988D7A9352DC94AC612C4B099517ACAC7DA7C81322D797ED7`), Inventory Extended C4 (80,604 bytes; `A0CED554CB687F7119AA19AC3466C0DE3FFB0A888E994514223152335B043636`), Trinkets C5 (560,208 bytes; `4C1FA6AC36C0457483FD0D395B99BBD94C9334AAD6DEFECE7633BBF0552D1724`), the Trinkets/Inventory Extended companion C5 (4,305 bytes; `76C8C735219CEEBFC7EC9A7B103E4CD4F027E43B2489CBC71F8B025B103EFA6C`), Traveler Toolbelt C1 (308,008 bytes; `43D3370135F42EF74129B904325006F91BA32074AB8DBD81A649170D1366FCD6`), Elytra support (12,771 bytes; `FBE4770626260151B3FE87587C73092FEEEA5FE3192D09F5E68CC32F8876A379`), and the exact Matcha data/component stack represented by `Matcha_Flavoured_1_12.zip` (12,248,989 bytes; `6209783021C358044ABEDABACEE471FAFF5BD4080437D4E3B5E51963F1804248`). Optional MME/Compass Ribbon tests require their separately recorded exact identities.

## Visible player checks

1. Start a clean client and dedicated server, connect, reload resources/data, reconnect, and shut down cleanly. Require no relevant dependency, registry, item-model, atlas, GeckoLib, Trinkets, Mixin, payload, codec, scheduler, SavedData, or dedicated-server client-class error. Save complete logs.
2. Run `/give @s ribbits:wandering_ribbit_spawn_egg 1`. Confirm `Wandering Ribbit Spawn Egg` appears exactly once in the Ribbits tab, once in the vanilla Spawn Eggs tab, and once in Creative search. Test ordinary hand use, a dispenser, and Creative pick-block; each must produce `ribbits:wandering_ribbit` and preserve the shared green egg artwork.
3. Compare an egg-created merchant with `/summon ribbits:wandering_ribbit`. Both must interact normally, persist through save/restart, have no profession, breeding, rank, progress, or restock behavior, and remain free of automatic departure.
4. In the exact intended Matcha stack, every newly materialized automatic, command-created, and egg-created merchant must expose nine offers: the original five with the schema-3 map cost; ordinary Compass → 4 Glowcaps; Copper Compass → 2; Golden Compass → 4; and Titanium Compass → 8. Each buyback has two uses, zero merchant/player XP, zero price movement, and no restock.
5. Exercise component matching: a custom-renamed ordinary Compass still receives only the ordinary payout; custom-renamed Copper, Golden, and Titanium variants retain their own tier; legitimate lodestone targeting does not change a tier; a vanilla Compass custom-named `Titanium Compass` never receives the Titanium payout; and specialized variants never satisfy the ordinary buyback.
6. Exercise the original Chute, map, and three curiosity offers before and after save/restart. Their menu positions, outputs, use counts, and curiosity choices remain unchanged; only the newly materialized map cost loses its compass input; the ordinary and optional compass buybacks follow them.
7. Upgrade a saved Canary 7 merchant with partially used or exhausted offers. It must visibly retain its exact five offers, choices, and use counts. Only a newly materialized merchant receives the current schema-3 menu.
8. Keep a vanilla Wandering Trader and an automatic Wandering Ribbit alive together. Verify both remain present and usable and that the vanilla merchant's llamas, offers, and persistence are unchanged.
9. Smoke the retained Chute equipment/deployment/descent flow, successful and failed Ribbit Village map outputs, failed-map Sorcerer redemption, a newly generated swamp-hut Sorcerer/Cat/barrel, and representative permanent profession trades. Record only what is actually observed.

## Instrumented and saved-state checks

There is no public command that reports the scheduler's internal site decision. Use an instrumented build or controlled saved-state inspection; the absence of a merchant is never proof that a particular biome was rejected.

1. Drive candidate positions through all 20 allowed vanilla biomes: swamp, mangrove swamp; forest, flower forest, birch forest, old-growth birch forest, dark forest, pale garden; plains, sunflower plains, meadow, cherry grove; jungle, sparse jungle, bamboo jungle; taiga, old-growth pine taiga, old-growth spruce taiga; mushroom fields; and river. River/wetland candidates still require sturdy dry ground and dry body/head space.
2. Prove representative denials for snowy plains, ice spikes, snowy taiga, frozen river, snowy beach, grove, snowy slopes, frozen peak, jagged peak, stony peak, windswept hills, desert, badlands, savanna, ocean, lush cave, Nether, and End.
3. Put the player in plains while forcing a candidate in desert, then put the player in desert while forcing a candidate in plains. The first must reject and the second admit, proving evaluation at the candidate feet rather than the player's biome.
4. Exercise tag boundaries: allow+deny overlap rejects; an installed empty allow tag rejects every biome; a missing allow tag restores the exact 20-biome baseline plus only audited forest/jungle/taiga/river families behind the hard exclusions; an actually audited modded-family biome may pass; an unaudited modded biome remains excluded. Record real modded IDs instead of inventing them.
5. Recheck the unchanged 24–48-block annulus, 16-block clearance from every eligible player, 16 candidates per selected player, 64 total candidates, already-loaded entity-ready chunks only, no generation/tickets, world-border/collision/fluid/hazard checks, Peaceful `EVENT` spawning, first/retry/success timings, 48,000-tick visit, global lease, expiry/trading persistence, backward-time rebasing, and fair-player cursor. Confirm vanilla Wandering Trader delay, chance, UUID, population, and state are never read or changed.
6. Prove egg- and command-created merchants never acquire or mutate lease UUID, generation, dimension, expiry, cooldown, or cap state. Scheduler-created entities must retain the existing lease and portal restrictions, and manual merchants must not affect the automatic cap or cooldown.
7. Remove or deliberately drift one optional Matcha compass contract in an instrumented fixture. Materialization must keep all six native offers and persist a zero-width optional range; it must never partially add optional trades, crash, or substitute a generic item. With all exact contracts present, the optional range is schema 1/count 3 and native is schema 3/count 6. Confirm the unchanged map modifier runs exactly once during new-offer materialization and never again on menu open or reload.
8. Decode new snapshots and confirm stable sorted provider IDs, independent deterministic streams, native range 0–5, optional range 6–8, exact serialized components, and preserved use counts. Decode existing schema-1/five-offer snapshots and confirm their provider range and serialized offers remain authoritative without reroll or reset.
9. On a dedicated server with at least two clients, repeat startup, fair automatic selection, lease persistence, manual independence, nine-offer materialization, Chute/map smoke, save/restart, and log review. Record seed, positions, timings, provider ranges, offer components/use counts, exact cohort hashes, deployment ID, and every row actually run.

Stop with `FAIL` for any proven identity, startup, biome decision, tag precedence, placement, timing, lease, egg, trade, persistence, map, Chute, swamp-hut, or vanilla-trader violation. Use `INCONCLUSIVE` for missing identity, unavailable instrumentation, absence-only biome observations, incomplete logs, or partially run rows. Partial execution never implies a whole-candidate `PASS` or promotion.
