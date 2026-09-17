# Testing

Current C8 candidate: `matcha-jei-integration-0.3.4-canonical-food-catalog-canary8.jar`, 91,529 bytes, SHA-256 `d92ade2de4c095089838323a5116e6dcb6b053046111a3ed6370c75c7dafc34f`, source checkpoint `1b474782d0b308a7f47ea69f5b7294926ec99af8`. It is `STATIC_PASS`, `NOT_DEPLOYED`, and `RUNTIME_UNTESTED`: no managed deployment, Minecraft launch, or user runtime result has been recorded for this exact release.

Exact predecessor C7 `matcha-jei-integration-0.3.3-canonical-food-replacement-canary7.jar`, 84,082 bytes, SHA-256 `7849dea6733e36667ae62044a5ba6956559dc96c85db2f3db30154a5b1f1a25c`, source `e016421530f1f1b47ff43632fb69cd73d56f5bbd`, has a user-reported external runtime `FAIL` for its incomplete generalized duplicate-elimination contract. C7 successfully fixed the previously reported Glow Berries and Glow Berry Mash duplicates, but Braised Mushroom still appeared twice: one entry retained Matcha's health/food benefits and the other was plain. Do not classify the Glow fix itself as failed. Exact C6 retains its earlier duplicate-stack failure, exact C4 separately failed for incomplete global Creative Search, and C5 remains `RUNTIME_UNTESTED`.

The accepted baseline remains exact C3 `matcha-jei-integration-0.2.0-nonrecipe-discovery-canary1.jar`, SHA-256 `1e4941e2353c1696505f243e86eb58f0239dd1144d544897370b021f6748a455`. Its user-reported external aggregate `PASS` remains historical evidence for C3 only. Exact C2 remains repository/control rollback provenance.

## C8 client or singleplayer matrix

Under explicit runtime ownership, use a disposable world with Matcha Flavoured 1.12, accepted Frost Protection, JEI 30.18.0.144 when available, C8, and the same unrelated mods that produce the existing Polymer duplicate-tab warnings.

### A. Canonical food discovery

1. In Creative Search, search Braised Mushroom. Require exactly one result, and require that exact result to retain Matcha's food/consumable effect and heart information rather than the plain rabbit-foot identity.
2. Search `glow`, then verify exactly one Matcha-aware `minecraft:glow_berries` stack and exactly one Matcha-aware Glow Berry Mash (`minecraft:rotten_flesh`) stack. Require the latter to retain its heart/Aura information; this preserves C7's observed successful fix.
3. Search several other Matcha-modified foods across ordinary-food and non-food carriers. Require only the demonstrated canonical Matcha health identity when a plain default is superseded.
4. Hover every result. Require the intended exact food, consumable-effect, lore, stack-size, and use-remainder components where applicable.
5. Check a custom-model food sharing a carrier (for example cooked pufferfish beside cooked cod), a custom-named variant, and differing stack mechanics remain distinct rather than being collapsed.

### B. Search safety and unrelated identities

1. Search `crafting table`, several ordinary foods with no Matcha replacement, ordinary items from different vanilla tabs, and representative unrelated-mod items from the Polymer-warning tabs.
2. Require every control item to remain searchable. C8 must not rebuild, clear, or reconstruct global Creative Search.
3. Confirm Matcha component variants that are not a plain/default counterpart remain distinct: representative potions, suspicious stew, enchanted books, and another exact Matcha custom-model identity are useful controls.

### C. JEI

1. If JEI previously showed both the plain/default and Matcha food identities, search the same affected foods. Require the canonical Matcha identity to remain visible and the plain/default identity to be absent from the ingredient list.
2. Confirm a representative custom-model Matcha food and unrelated component variants remain visible.
3. Confirm the Blessing of Demeter exact entry opens its actual current vanilla recipe exactly once in the normal JEI category—no duplicate or fabricated recipe.
4. Recheck ordinary vanilla JEI searching plus representative C3 trade/acquisition surfaces. JEI's vanilla categories must remain the sole recipe owner.

### D. Lifecycle and logs

1. Run `/reload`, reopen Creative Search and JEI, and repeat A through C. Include a reload immediately after joining so a category rebuild can occur before deferred safe-tick reconciliation.
2. Disconnect and reconnect, then repeat those checks; also leave the world and require defaults hidden only by that server's synchronized catalog to be restored.
3. Require no stale or duplicate Matcha entry, no missing unrelated identity, and restoration of a default identity if its synchronized Matcha replacement is no longer present.
4. Check logs for new Creative or JEI errors. The existing Polymer duplicate-item-group warnings may remain only if they are the same pre-existing nonfatal condition.

## Deferred dedicated-server gate

Start a dedicated server with Matcha Flavoured, Fabric API, and C8 but without JEI. Join and reconnect from a client, confirm loaded Matcha recipes are awarded idempotently, and confirm no client-only or JEI classloading failure. This gate remains unperformed.

Record `FAIL` or `INCONCLUSIVE` for any crash, project-owned error, loss of a vanilla or unrelated Search entry, stale/double/missing Matcha entry, duplicate or fabricated recipe, incorrect exact identity or relationship, `/reload`/reconnect regression, or dedicated-server classloading failure. Do not apply a result to a different artifact, version, hash, or source checkpoint.
