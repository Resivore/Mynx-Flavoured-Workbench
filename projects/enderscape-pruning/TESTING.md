# Testing

This project currently contains an audit only. No pruning implementation, Canary artifact, deployment, Minecraft launch, or runtime evidence exists. The exact audit input is `enderscape-fabric-3.0.2+mc26.2.jar`, SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`.

Use this procedure only after the owner has resolved every decision in `AUDIT.md` and a separately identified implementation artifact has been built and retained. Bind every result to that exact filename, SHA-256, version, build time, and source checkpoint.

## A. Load and save compatibility

1. Copy a disposable 26.2 test world containing representative Enderscape chunks and player data. Never use a protected gameplay profile.
2. Before installing the candidate, place each mirror color, a fueled and enchanted mirror, dagger, each rubble-shield variant, every Shadoline armor piece, relevant enchanted books, Enderscape potions/arrows, a Magnia attractor, and Magnia blocks/sprouts/radio in inventories, containers, item frames, and loaded/unloaded chunks.
3. Load with the candidate. Require no missing-registry, unknown-component, data-fixer, recipe, advancement, loot-table, or worldgen errors. Require every legacy stack and block to remain readable and movable.
4. Save, quit, reload, and reconnect. Repeat with a second copy that has End City, Stronghold, End Haven, Mirestone Ruins, and Magnia Fields chunks already generated.
5. Remove the candidate but keep the exact upstream Enderscape jar, then reload another disposable copy. If the design is intended to be removable, require the retained upstream identities to survive. Never claim reversibility unless this succeeds.

## B. Candidate equipment pruning

For each owner-approved suppressed family—mirror, dagger, rubble shield, and/or Shadoline armor—test independently and together.

1. Search Creative Search and JEI for every default and component variant. Require the selected family to be absent from ordinary future discovery while unrelated Enderscape content remains visible.
2. Check Enderscape's own creative tab and every vanilla tab it populates. A global creative-tab config toggle is not sufficient evidence for candidate-specific hiding.
3. Check all recipes and recipe-book unlocks. Require suppressed crafting or dyeing paths to be absent as designed, with no broken recipe toast or dangling advancement.
4. Generate/open every audited structure and loot table. Require mirrors and daggers not to generate from the suppressed paths, while original roll counts, weights, and unrelated rewards remain unchanged.
5. Confirm existing stacks remain usable or recoverable according to the owner-approved legacy policy. Specifically test legacy rubble-shield aliases and component variants.
6. Confirm retained Resonance behavior on the Magnia attractor after mirror/dagger suppression. If the supported-item tag was narrowed, verify only the intended tools accept it.

## C. Matcha food and loot identity

1. Use the exact accepted Matcha Flavoured data authority plus the current compatibility candidate selected for the test. Record their exact identities.
2. Exercise every table in the food-loot inventory in `AUDIT.md`: End City chest, vault, elytra vault, spawner, supplement; End Haven; Mirestone Ruins; Stronghold altar, armory, bedroom, garden, mansion, secret, spawner; and affected block drops.
3. For bread, carrot, golden carrot, golden apple, enchanted golden apple, chorus fruit, and cod, compare the complete resulting data-component map—not just the base item ID or tooltip.
4. Require the intended Matcha identity while preserving Enderscape's original entry weight, pool rolls, count range, conditional biome gate, and other functions. A helper table with its own count function must not multiply or replace Enderscape's count.
5. Verify plain potato, poisonous potato, honey bottle, and suspicious stew remain unchanged unless a later owner-approved canonical Matcha identity explicitly covers them.
6. Search Creative and JEI after load, `/reload`, disconnect/reconnect, and a category rebuild. Require one intended canonical identity without removing legitimate component-distinct variants.

## D. Native Enderscape food and effects

1. Obtain and consume drift jelly in a bottle, puruberry, and murublight bracket. Verify their exact hunger, saturation, consume time, cooldown/remainder, and Low Gravity or Void effect duration.
2. Place and eat every bite of a chorus cake roll. Verify seven bites, the per-bite hunger path, comparator behavior, final teleport, and block removal.
3. Bottle a valid Drifter and verify glass-bottle conversion, stack/remainder behavior, and Low Gravity brewing.
4. Verify Puruberry vine drops/Fortune, Drifter food behavior, Rustle murublight conversion, and every owner-approved Matcha health rebalance without weakening Void Purification/Corruption or placement mechanics.
5. Brew and inspect every registered Enderscape potion, splash potion, lingering potion, and tipped-arrow variant. Confirm the intentionally unbrewed base Void Resistance and Void Corruption identities remain registry-safe.
6. Verify totem-granted Void Resistance, active Void Corruption/Purification loops, voided health, and tagged void-mob exceptions.

## E. Magnia mechanics

1. Build isolated alluring and repulsive sprout rigs. Test matching base-Magnia signal relay from power 15 through every attenuation step, redstone input, and the exact 14-block field extent.
2. Prove ordinary solid blocks do not stop the field. Then terminate it with sturdy, opposite-polarity Magnia-bearing blocks and verify the boundary.
3. Place magma blocks adjacent to sprouts and require the overheated state to disable the field; confirm removal restores operation.
4. Test item entities, experience orbs, loose and embedded arrows, minecarts, iron golems, sulfur cubes, unarmored living entities, each weak/medium/strong magnetic armor contribution, underwater attraction, and a player actively Creative-flying.
5. Verify fall-distance mitigation, temporary no-gravity behavior, pickup delay, motion synchronization, and return to normal gravity after leaving the field.
6. Pulse a polarized Magnia block. Require one polarity toggle on the rising edge, no second toggle on the falling edge, and power 15 only for its current polarity.
7. Random-tick powered sprouts beside matching base Magnia and verify blistered conversion and polarity selection; separately inspect a tie instead of assuming a deterministic tie-break.
8. Generate Magnia Fields, End City start-platform processing, and the two furniture templates containing sprouts. Require no missing blocks/features and no accidental loss of generated Magnia content.
9. Test the Magnia attractor independently: item/XP attraction, fuel capacity and burn threshold, underwater motion, Bundling into existing bundles, and Resonance threshold increments.

## F. Packs, structures, and regression

1. Test each Enderscape built-in pack independently: Improved Visuals, Fix Levitation Advancement, Fix Vanilla Recipes, New End Cities, New Strongholds, and New Terrain.
2. Exercise both New End Cities replacement loot and the separate vanilla End City post-supplement path; require no double supplement or unexpected loss.
3. Toggle the stronghold-library and End City supplement settings independently and verify only future loot evaluation changes.
4. Generate every Enderscape biome and structure, including gateways, End City, End Haven, Mirestone Ruins, and Stronghold. Reopen existing chunks after every relevant pack combination.
5. Check recipes, advancements, statistics, tooltips, creative tabs, JEI subtypes, logs, and server/client joining for dangling references.

Record `FAIL` or `INCONCLUSIVE` for any missing registry identity, corrupt or discarded legacy stack, world-load warning, changed loot probability/count, duplicate Matcha food identity, lost Enderscape effect, broken Magnia/worldgen loop, or untested owner decision. Static validation alone never satisfies this matrix.
